package com.mecanica.controller;

import com.mecanica.dao.ViajeCobradoDAO;
import com.mecanica.model.Cliente;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.model.ViajeCobrado;
import com.mecanica.util.BD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller de los VIAJES. Un item de OS de tipo VIAJE se cobra al cliente
 * como cualquier otro (suma en su saldo general), pero esa plata NO entra en
 * Financiero: va a la pestaña "Viajes".
 *
 * Como el cliente paga el saldo general (no una OS especifica), el sistema
 * hace lo mismo que con las compras: los pagos van cubriendo las OS CERRADAS
 * del cliente, de la mas antigua a la mas nueva, y DENTRO de cada OS el viaje
 * es la ultima parte que se cubre. Entonces:
 *  - lo que un pago cubre de la parte "no viaje" va a Financiero, como siempre;
 *  - lo que cubre de la parte de viaje NUNCA va a Financiero: queda retenido;
 *  - el viaje es "todo o nada" por OS: recien cuando la OS queda paga entera
 *    todo su viaje aparece, de una sola vez, en la pestaña Viajes.
 *
 * Todo ocurre dentro de la misma transaccion del pago (ver
 * ClienteController.registrarPagamento y ChequePreDatadoController).
 */
public class ViajeController {

    private final ViajeCobradoDAO viajeDAO = new ViajeCobradoDAO();
    private final AuditoriaController auditoria = new AuditoriaController();

    /** Lista de la pestaña "Viajes", lo mas reciente primero. */
    public List<ViajeCobrado> listarTodos() {
        return viajeDAO.listarTodos();
    }

    /**
     * Cambia el estado del viaje de una OS entre "No pagado" y "Pagado" -- lo
     * unico que se puede editar de un viaje. Solo toca ese campo.
     */
    public void cambiarEstado(ViajeDeOrden viaje, boolean pagado) {
        if (viaje == null) {
            throw new IllegalArgumentException("Seleccione un viaje.");
        }
        BD.ejecutarEnTransaccion(conexion -> viajeDAO.actualizarPagadoPorOrden(conexion, viaje.getOrdenId(), pagado));
        auditoria.registrar("ESTADO DE VIAJE", "OS Nº " + viaje.getNumeroOs() + " - "
                + (viaje.getCliente() == null ? "" : viaje.getCliente().getNombre()) + " - "
                + AuditoriaController.gs(viaje.getValor()) + " - " + (pagado ? "Pagado" : "No pagado"));
    }

    /**
     * Junta en UNA fila por OS los pedazos de viaje que ya aparecen en la
     * pestaña (OS paga entera): el viaje de una OS se ve de una sola vez.
     */
    public static List<ViajeDeOrden> agruparPorOrden(List<ViajeCobrado> visibles) {
        Map<Long, ViajeDeOrden> porOrden = new LinkedHashMap<>();
        for (ViajeCobrado v : visibles) {
            Long ordenId = v.getOrdenDeServicio().getId();
            ViajeDeOrden fila = porOrden.get(ordenId);
            if (fila == null) {
                fila = new ViajeDeOrden(ordenId, v.getOrdenDeServicio().getNumero(), v.getCliente());
                porOrden.put(ordenId, fila);
            }
            fila.sumar(v);
        }
        return new ArrayList<>(porOrden.values());
    }

    /** El viaje de una OS ya paga entera, como lo muestra la pestaña Viajes (una fila por OS). */
    public static class ViajeDeOrden {
        private final Long ordenId;
        private final Long numeroOs;
        private final Cliente cliente;
        private LocalDate fecha;
        private BigDecimal valor = BigDecimal.ZERO;
        private boolean confirmado = true;
        private boolean pagado = true;

        ViajeDeOrden(Long ordenId, Long numeroOs, Cliente cliente) {
            this.ordenId = ordenId;
            this.numeroOs = numeroOs;
            this.cliente = cliente;
        }

        private void sumar(ViajeCobrado v) {
            valor = valor.add(v.getValor());
            if (fecha == null || (v.getFecha() != null && v.getFecha().isAfter(fecha))) {
                fecha = v.getFecha();
            }
            confirmado = confirmado && v.isConfirmado();
            pagado = pagado && v.isPagado();
        }

        public Long getOrdenId() {
            return ordenId;
        }

        public Long getNumeroOs() {
            return numeroOs;
        }

        public Cliente getCliente() {
            return cliente;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public BigDecimal getValor() {
            return valor;
        }

        /** false mientras el cheque con el que se pago siga pendiente. */
        public boolean isConfirmado() {
            return confirmado;
        }

        /** Estado editable: true = "Pagado", false = "No pagado". */
        public boolean isPagado() {
            return pagado;
        }
    }

    /**
     * Reparte un pago del cliente entre "viaje" y "resto" y anota la parte de
     * viaje (una fila por OS tocada). Devuelve cuanto del pago fue a viaje --
     * el Controller que llama le resta eso al valor del MovimientoFinanciero.
     *
     * @param saldoAntes   saldo del cliente ANTES de aplicar el pago
     * @param saldoDespues saldo del cliente DESPUES (ya con el pago y el descuento)
     * @param valorPagado  la plata que realmente entra (sin el descuento, que es plata que no entra)
     * @param chequeId     id del cheque si el pago es con cheque pre-datado (las filas quedan sin confirmar); null si no
     */
    BigDecimal repartirPago(Connection conexion, Cliente cliente, BigDecimal saldoAntes, BigDecimal saldoDespues,
            BigDecimal valorPagado, Long chequeId) throws SQLException {
        List<OsCobrable> ordenes = ordenesCerradas(conexion, cliente.getId());
        BigDecimal totalOrdenes = BigDecimal.ZERO;
        for (OsCobrable os : ordenes) {
            totalOrdenes = totalOrdenes.add(os.total);
        }
        // cuanto de las OS cerradas ya estaba pago antes y queda pago despues del pago
        BigDecimal pagadoAntes = limitar(totalOrdenes.subtract(saldoAntes), totalOrdenes);
        BigDecimal pagadoDespues = limitar(totalOrdenes.subtract(saldoDespues), totalOrdenes);

        BigDecimal restante = valorPagado; // nunca va a viaje mas plata que la que realmente entra
        BigDecimal totalViaje = BigDecimal.ZERO;
        BigDecimal inicio = BigDecimal.ZERO;
        for (OsCobrable os : ordenes) {
            BigDecimal inicioViaje = inicio.add(os.total).subtract(os.viaje);
            BigDecimal fin = inicio.add(os.total);
            BigDecimal desde = pagadoAntes.max(inicioViaje);
            BigDecimal hasta = pagadoDespues.min(fin);
            BigDecimal parte = hasta.subtract(desde).min(restante);
            // la OS queda paga entera con ESTE pago (antes no lo estaba)
            boolean seQuita = pagadoAntes.compareTo(fin) < 0 && pagadoDespues.compareTo(fin) >= 0;
            if (os.viaje.signum() > 0) {
                if (parte.signum() > 0) {
                    OrdenDeServicio referencia = new OrdenDeServicio();
                    referencia.setId(os.id);
                    ViajeCobrado cobrado = new ViajeCobrado();
                    cobrado.setFecha(LocalDate.now());
                    cobrado.setCliente(cliente);
                    cobrado.setOrdenDeServicio(referencia);
                    cobrado.setValor(parte);
                    cobrado.setChequeId(chequeId);
                    cobrado.setConfirmado(chequeId == null);
                    // retenido hasta que la OS este paga entera
                    cobrado.setOsQuitada(pagadoDespues.compareTo(fin) >= 0);
                    viajeDAO.guardar(conexion, cobrado);
                    totalViaje = totalViaje.add(parte);
                    restante = restante.subtract(parte);
                }
                if (seQuita) {
                    // los pedazos de viaje que quedaron retenidos en pagos anteriores aparecen ahora
                    viajeDAO.marcarOsQuitada(conexion, os.id);
                }
            }
            inicio = fin;
        }
        return totalViaje;
    }

    /** Lo que un cheque pre-datado lleva de viaje (no entra en Financiero cuando se confirme). */
    BigDecimal viajeDeCheque(Connection conexion, Long chequeId) throws SQLException {
        return viajeDAO.sumarPorCheque(conexion, chequeId);
    }

    /** El cheque se confirmo: sus viajes pasan a cobrados. */
    void confirmarDeCheque(Connection conexion, Long chequeId) throws SQLException {
        viajeDAO.confirmarPorCheque(conexion, chequeId, LocalDate.now());
    }

    /** Deja el valor entre 0 y el maximo. */
    private static BigDecimal limitar(BigDecimal valor, BigDecimal maximo) {
        return valor.max(BigDecimal.ZERO).min(maximo);
    }

    /** OS cerradas del cliente, de la mas antigua a la mas nueva, con su total y lo que de eso es viaje. */
    private List<OsCobrable> ordenesCerradas(Connection conexion, Long clienteId) throws SQLException {
        String sql = "SELECT o.id, o.valor_total, "
                + "COALESCE(SUM(i.valor_total) FILTER (WHERE i.tipo = 'VIAJE'), 0) AS viaje "
                + "FROM orden_de_servicio o LEFT JOIN item_orden_servicio i ON i.orden_de_servicio_id = o.id "
                + "WHERE o.cliente_id = ? AND o.estado = 'CONCLUIDA' "
                + "GROUP BY o.id, o.valor_total ORDER BY o.numero";
        List<OsCobrable> ordenes = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            BD.asignar(ps, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ordenes.add(new OsCobrable(rs.getLong("id"), rs.getBigDecimal("valor_total"),
                            rs.getBigDecimal("viaje")));
                }
            }
        }
        return ordenes;
    }

    private static class OsCobrable {
        final Long id;
        final BigDecimal total;
        final BigDecimal viaje;

        OsCobrable(Long id, BigDecimal total, BigDecimal viaje) {
            this.id = id;
            this.total = total;
            this.viaje = viaje.min(total);
        }
    }
}
