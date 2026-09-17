package com.mecanica.controller;

import com.mecanica.dao.ChequePreDatadoDAO;
import com.mecanica.dao.CierreMensualDAO;
import com.mecanica.dao.ClienteDAO;
import com.mecanica.dao.EmpleadoDAO;
import com.mecanica.dao.ItemOrdenServicioDAO;
import com.mecanica.dao.MovimientoFinancieroDAO;
import com.mecanica.dao.ProveedorDAO;
import com.mecanica.dao.RetiroEmpleadoDAO;
import com.mecanica.dao.RetiroSocioDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.EstadoCheque;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.ChequePreDatado;
import com.mecanica.model.CierreMensual;
import com.mecanica.model.CierreSocioDetalle;
import com.mecanica.model.Cliente;
import com.mecanica.model.Empleado;
import com.mecanica.model.ItemOrdenServicio;
import com.mecanica.model.Maquinario;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.model.Proveedor;
import com.mecanica.model.RetiroEmpleado;
import com.mecanica.model.RetiroSocio;
import com.mecanica.util.NumeroALetras;
import com.mecanica.util.ReporteUtil;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma los datos de cada reporte impreso (JasperReports) y lo genera. Cada
 * metodo devuelve un JasperPrint listo para mostrar en el visor (ver
 * view.VisorReporte), que tiene los botones de imprimir y guardar en PDF.
 *
 * Los disenos estan en src/main/resources/reportes/*.jrxml. Aca se
 * convierten las entidades en filas simples (Map), con fechas ya formateadas
 * y valores en BigDecimal -- el formato de miles (1.250.000) lo aplica el
 * propio reporte.
 */
public class ReporteController {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final ChequePreDatadoDAO chequeDAO = new ChequePreDatadoDAO();
    private final MovimientoFinancieroDAO movimientoDAO = new MovimientoFinancieroDAO();
    private final CierreMensualDAO cierreDAO = new CierreMensualDAO();
    private final RetiroSocioDAO retiroSocioDAO = new RetiroSocioDAO();
    private final RetiroEmpleadoDAO retiroEmpleadoDAO = new RetiroEmpleadoDAO();
    private final ItemOrdenServicioDAO itemOrdenDAO = new ItemOrdenServicioDAO();

    private static final Collator ORDEN_ALFABETICO = Collator.getInstance(ReporteUtil.LOCALE);

    // ---------------------------------------------------------------- Listados

    public JasperPrint listadoClientes() throws JRException {
        List<Cliente> clientes = new ArrayList<>(clienteDAO.listarTodos());
        clientes.sort(Comparator.comparing(c -> ReporteUtil.texto(c.getNombre()), ORDEN_ALFABETICO));
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Cliente c : clientes) {
            filas.add(fila("nombre", c.getNombre(),
                    "documento", c.getDocumento(),
                    "telefono", c.getTelefono(),
                    "direccion", c.getDireccion(),
                    "saldo", ReporteUtil.valor(c.getSaldo())));
        }
        return ReporteUtil.generar("listado_clientes", "Listado de Clientes", null, null, filas);
    }

    public JasperPrint listadoProveedores() throws JRException {
        List<Proveedor> proveedores = new ArrayList<>(proveedorDAO.listarTodos());
        proveedores.sort(Comparator.comparing(p -> ReporteUtil.texto(p.getNombre()), ORDEN_ALFABETICO));
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Proveedor p : proveedores) {
            filas.add(fila("nombre", p.getNombre(),
                    "documento", p.getDocumento(),
                    "telefono", p.getTelefono(),
                    "contacto", p.getContacto(),
                    "saldo", ReporteUtil.valor(p.getSaldo())));
        }
        return ReporteUtil.generar("listado_proveedores", "Listado de Proveedores", null, null, filas);
    }

    public JasperPrint listadoEmpleados() throws JRException {
        List<Empleado> empleados = new ArrayList<>(empleadoDAO.listarTodos());
        // Activos primero, despues por nombre.
        empleados.sort(Comparator.comparing((Empleado e) -> !e.isActivo())
                .thenComparing(e -> ReporteUtil.texto(e.getNombre()), ORDEN_ALFABETICO));
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Empleado e : empleados) {
            filas.add(fila("nombre", e.getNombre(),
                    "documento", e.getDocumento(),
                    "cargo", e.getCargo(),
                    "telefono", e.getTelefono(),
                    "fechaAdmision", ReporteUtil.fecha(e.getFechaAdmision()),
                    "activo", e.isActivo(),
                    "salarioBase", ReporteUtil.valor(e.getSalarioBase())));
        }
        return ReporteUtil.generar("listado_empleados", "Listado de Empleados", null, null, filas);
    }

    public JasperPrint chequesPendientes() throws JRException {
        LocalDate hoy = LocalDate.now();
        List<Map<String, Object>> filas = new ArrayList<>();
        for (ChequePreDatado ch : chequeDAO.listarPorEstado(EstadoCheque.PENDIENTE)) {
            boolean deCliente = ch.getCliente() != null;
            filas.add(fila("origen", deCliente ? "Cliente" : "Proveedor",
                    "esCliente", deCliente,
                    "nombre", deCliente ? ch.getCliente().getNombre()
                            : (ch.getProveedor() != null ? ch.getProveedor().getNombre() : ""),
                    "numero", ch.getNumeroCheque(),
                    "banco", ch.getBanco(),
                    "fechaRegistro", ReporteUtil.fecha(ch.getFechaRegistro()),
                    "fechaVencimiento", ReporteUtil.fecha(ch.getFechaVencimiento()),
                    "vencido", ch.getFechaVencimiento() != null && !ch.getFechaVencimiento().isAfter(hoy),
                    "descripcion", ch.getDescripcion(),
                    "valor", ReporteUtil.valor(ch.getValor())));
        }
        return ReporteUtil.generar("cheques_pendientes", "Cheques Pendientes",
                "Pendientes al " + ReporteUtil.fecha(hoy), null, filas);
    }

    /**
     * Movimientos del periodo. tipo = null trae ingresos y egresos; ENTRADA
     * solo ingresos; SALIDA solo egresos.
     */
    public JasperPrint movimientosFinancieros(LocalDate desde, LocalDate hasta, TipoMovimientoFinanciero tipo)
            throws JRException {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new IllegalArgumentException("El periodo del reporte es invalido.");
        }
        List<Map<String, Object>> filas = new ArrayList<>();
        for (MovimientoFinanciero m : movimientoDAO.listarPorPeriodo(desde, hasta)) {
            if (tipo != null && m.getTipo() != tipo) {
                continue;
            }
            boolean ingreso = m.getTipo() == TipoMovimientoFinanciero.ENTRADA;
            filas.add(fila("fecha", ReporteUtil.fecha(m.getFecha()),
                    "tipo", ingreso ? "Ingreso" : "Egreso",
                    "esIngreso", ingreso,
                    "categoria", categoriaLegible(m.getCategoria()),
                    "deQuien", deQuien(m),
                    "descripcion", m.getDescripcion(),
                    "descuento", m.getDescuentoValor(),
                    "valor", ReporteUtil.valor(m.getValor())));
        }
        String filtro = tipo == null ? "Ingresos y egresos"
                : tipo == TipoMovimientoFinanciero.ENTRADA ? "Solo ingresos" : "Solo egresos";
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("MOSTRAR_INGRESOS", tipo != TipoMovimientoFinanciero.SALIDA);
        parametros.put("MOSTRAR_EGRESOS", tipo != TipoMovimientoFinanciero.ENTRADA);
        return ReporteUtil.generar("movimientos_financieros", "Movimientos Financieros",
                "Del " + ReporteUtil.fecha(desde) + " al " + ReporteUtil.fecha(hasta) + "  ·  " + filtro,
                parametros, filas);
    }

    // ---------------------------------------------------------------- Documentos

    /** Documento del acerto de un cierre mensual: ganancia, parte de cada socio y sus descuentos. */
    public JasperPrint cierreMensual(CierreMensual seleccionado) throws JRException {
        if (seleccionado == null || seleccionado.getId() == null) {
            throw new IllegalArgumentException("Seleccione un cierre del historico.");
        }
        CierreMensual cierre = cierreDAO.buscarConDetalles(seleccionado.getId());
        if (cierre == null) {
            throw new IllegalArgumentException("Ese cierre ya no existe.");
        }

        // Retiros de este cierre agrupados por socio.
        Map<Long, List<RetiroSocio>> retirosPorSocio = new HashMap<>();
        for (RetiroSocio r : retiroSocioDAO.listarPorCierre(cierre)) {
            retirosPorSocio.computeIfAbsent(r.getSocio().getId(), k -> new ArrayList<>()).add(r);
        }

        List<CierreSocioDetalle> detalles = new ArrayList<>(cierre.getDetalles());
        detalles.sort(Comparator.comparing(d -> ReporteUtil.texto(d.getSocio().getNombre()), ORDEN_ALFABETICO));

        List<Map<String, Object>> filas = new ArrayList<>();
        for (CierreSocioDetalle d : detalles) {
            List<RetiroSocio> retiros = retirosPorSocio.getOrDefault(d.getSocio().getId(), List.of());
            if (retiros.isEmpty()) {
                // Una fila "vacia" para que el socio igual aparezca, con "Sin retiros".
                filas.add(filaSocio(d, null));
            }
            for (RetiroSocio r : retiros) {
                filas.add(filaSocio(d, r));
            }
        }

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TOTAL_ENTRADAS", ReporteUtil.valor(cierre.getTotalEntradas()));
        parametros.put("TOTAL_SALIDAS", ReporteUtil.valor(cierre.getTotalSalidas()));
        parametros.put("GANANCIA", ReporteUtil.valor(cierre.getGananciaTotal()));
        parametros.put("CANTIDAD_SOCIOS", detalles.size());
        parametros.put("CANTIDAD_MOVIMIENTOS", (int) movimientoDAO.contarPorCierre(cierre.getId()));
        parametros.put("DESCRIPCION_CIERRE", cierre.getDescripcion() == null || cierre.getDescripcion().isBlank()
                ? null : cierre.getDescripcion());
        parametros.put("FECHA_CIERRE", ReporteUtil.fecha(cierre.getFechaCierre()));
        return ReporteUtil.generar("cierre_mensual", "Cierre Mensual", "Acerto de socios", parametros, filas);
    }

    private Map<String, Object> filaSocio(CierreSocioDetalle d, RetiroSocio r) {
        String descripcion = null;
        if (r != null) {
            descripcion = r.getObservacion() == null || r.getObservacion().isBlank() ? "Retiro" : r.getObservacion();
        }
        return fila("socioId", d.getSocio().getId(),
                "socioNombre", d.getSocio().getNombre(),
                "socioDocumento", d.getSocio().getDocumento(),
                "parteGanancia", ReporteUtil.valor(d.getParteGanancia()),
                "yaRetirado", ReporteUtil.valor(d.getYaRetirado()),
                "valorARecibir", ReporteUtil.valor(d.getValorARecibir()),
                "retiroFecha", r == null ? null : ReporteUtil.fecha(r.getFecha()),
                "retiroDescripcion", descripcion,
                "retiroValor", r == null ? null : ReporteUtil.valor(r.getValor()));
    }

    /**
     * Recibo de un pago de salario ya hecho (MovimientoFinanciero de categoria
     * SALARIO_EMPLEADO), con los vales/adelantos descontados en ese pago.
     */
    public JasperPrint reciboSalario(MovimientoFinanciero pago) throws JRException {
        if (pago == null || pago.getCategoria() != CategoriaMovimientoFinanciero.SALARIO_EMPLEADO) {
            throw new IllegalArgumentException("Seleccione un pago de salario.");
        }
        Empleado empleado = pago.getEmpleado();
        List<RetiroEmpleado> retiros = retiroEmpleadoDAO.listarPorPagoSalario(pago);

        BigDecimal totalDescuentos = BigDecimal.ZERO;
        List<Map<String, Object>> filas = new ArrayList<>();
        for (RetiroEmpleado r : retiros) {
            totalDescuentos = totalDescuentos.add(ReporteUtil.valor(r.getValor()));
            filas.add(fila("fecha", ReporteUtil.fecha(r.getFecha()),
                    "tipo", ReporteUtil.enumLegible(r.getTipo()),
                    "descripcion", r.getObservacion(),
                    "valor", ReporteUtil.valor(r.getValor())));
        }
        // El salario base es el que se pago en ese momento (valor del movimiento),
        // no el salario actual del empleado, que puede haber cambiado despues.
        BigDecimal salarioBase = ReporteUtil.valor(pago.getValor());
        BigDecimal neto = salarioBase.subtract(totalDescuentos);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("EMPLEADO", empleado != null ? empleado.getNombre() : "");
        parametros.put("DOCUMENTO", empleado != null ? empleado.getDocumento() : null);
        parametros.put("CARGO", empleado != null ? empleado.getCargo() : null);
        parametros.put("CONCEPTO", conceptoLegible(pago.getDescripcion()));
        parametros.put("FECHA_PAGO", ReporteUtil.fecha(pago.getFecha()));
        parametros.put("SALARIO_BASE", salarioBase);
        parametros.put("TOTAL_DESCUENTOS", totalDescuentos);
        parametros.put("NETO", neto);
        parametros.put("NETO_EN_LETRAS", NumeroALetras.guaranies(neto));
        return ReporteUtil.generar("recibo_salario", "Recibo de Salario", null, parametros, filas);
    }

    /** Via impresa de la Orden de Servicio, con sus items y lugar para la firma del cliente. */
    public JasperPrint ordenServicio(OrdenDeServicio os) throws JRException {
        if (os == null) {
            throw new IllegalArgumentException("Seleccione una orden de servicio.");
        }
        List<Map<String, Object>> filas = new ArrayList<>();
        for (ItemOrdenServicio item : itemOrdenDAO.listarPorOrdemDeServico(os)) {
            filas.add(fila("tipo", ReporteUtil.enumLegible(item.getTipo()),
                    "descripcion", item.getDescripcion(),
                    "cantidad", item.getCantidad(),
                    "valorUnitario", ReporteUtil.valor(item.getValorUnitario()),
                    "valorTotal", ReporteUtil.valor(item.getValorTotal())));
        }
        Cliente cliente = os.getCliente();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("CLIENTE", cliente != null ? cliente.getNombre() : "");
        parametros.put("CLIENTE_DOCUMENTO", cliente != null ? cliente.getDocumento() : null);
        parametros.put("CLIENTE_TELEFONO", cliente != null ? cliente.getTelefono() : null);
        parametros.put("CLIENTE_DIRECCION", cliente != null ? cliente.getDireccion() : null);
        parametros.put("MAQUINARIO", maquinarioLegible(os.getMaquinario()));
        parametros.put("FECHA_APERTURA", ReporteUtil.fecha(os.getFechaApertura()));
        parametros.put("FECHA_CIERRE", os.getFechaCierre() == null ? null : ReporteUtil.fecha(os.getFechaCierre()));
        parametros.put("ESTADO", ReporteUtil.enumLegible(os.getEstado()));
        parametros.put("PROBLEMA", os.getProblemaReportado());
        parametros.put("VALOR_TOTAL", ReporteUtil.valor(os.getValorTotal()));
        String numero = os.getNumero() == null ? "" : String.format("%06d", os.getNumero());
        return ReporteUtil.generar("orden_servicio", "Orden de Servicio", "N° " + numero, parametros, filas);
    }

    // ---------------------------------------------------------------- Ayudas

    private static Map<String, Object> fila(Object... claveValor) {
        Map<String, Object> fila = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            fila.put((String) claveValor[i], claveValor[i + 1]);
        }
        return fila;
    }

    private static String deQuien(MovimientoFinanciero m) {
        if (m.getCliente() != null) {
            return m.getCliente().getNombre();
        }
        if (m.getProveedor() != null) {
            return m.getProveedor().getNombre();
        }
        if (m.getEmpleado() != null) {
            return m.getEmpleado().getNombre();
        }
        return "";
    }

    private static String categoriaLegible(CategoriaMovimientoFinanciero categoria) {
        if (categoria == null) {
            return "";
        }
        switch (categoria) {
            case PAGO_CLIENTE:
                return "Pago de cliente";
            case COMPRA_PROVEEDOR:
                return "Pago a proveedor";
            case SALARIO_EMPLEADO:
                return "Salario de empleado";
            case DEVOLUCION_CLIENTE:
                return "Devolucion a cliente";
            case DEVOLUCION_PROVEEDOR:
                return "Devolucion de proveedor";
            default:
                return "Otro";
        }
    }

    private static String maquinarioLegible(Maquinario maquinario) {
        if (maquinario == null) {
            return "Servicio general (sin maquinaria)";
        }
        StringBuilder texto = new StringBuilder(ReporteUtil.enumLegible(maquinario.getTipo()));
        if (maquinario.getMarca() != null && !maquinario.getMarca().isBlank()) {
            texto.append(" - ").append(maquinario.getMarca());
        }
        if (maquinario.getModelo() != null && !maquinario.getModelo().isBlank()) {
            texto.append(' ').append(maquinario.getModelo());
        }
        if (maquinario.getIdentificacion() != null && !maquinario.getIdentificacion().isBlank()) {
            texto.append(" (").append(maquinario.getIdentificacion()).append(')');
        }
        return texto.toString();
    }

    /**
     * Los pagos de salario anteriores al 2026-09-17 guardaron el periodo con
     * fechas en formato 2026-09-01; aca se muestran como 01/09/2026.
     */
    private static String conceptoLegible(String descripcion) {
        if (descripcion == null) {
            return "salario";
        }
        return descripcion.replaceAll("(\\d{4})-(\\d{2})-(\\d{2})", "$3/$2/$1");
    }
}
