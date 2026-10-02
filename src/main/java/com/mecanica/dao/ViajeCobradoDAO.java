package com.mecanica.dao;

import com.mecanica.model.Cliente;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.model.ViajeCobrado;
import com.mecanica.util.BD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ViajeCobradoDAO extends AbstractGenericDAO<ViajeCobrado, Long> {

    @Override
    protected String tabla() {
        return "viaje_cobrado";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"fecha", "valor", "confirmado", "os_quitada", "pagado", "cheque_id", "cliente_id", "orden_de_servicio_id"};
    }

    @Override
    protected Object[] valoresColumnas(ViajeCobrado v) {
        return new Object[] {v.getFecha(), v.getValor(), v.isConfirmado(), v.isOsQuitada(), v.isPagado(),
                v.getChequeId(),
                v.getCliente() == null ? null : v.getCliente().getId(),
                v.getOrdenDeServicio() == null ? null : v.getOrdenDeServicio().getId()};
    }

    @Override
    protected ViajeCobrado mapear(ResultSet rs) throws SQLException {
        ViajeCobrado v = new ViajeCobrado();
        v.setFecha(BD.fecha(rs, "fecha"));
        v.setValor(rs.getBigDecimal("valor"));
        v.setConfirmado(rs.getBoolean("confirmado"));
        v.setOsQuitada(rs.getBoolean("os_quitada"));
        v.setPagado(rs.getBoolean("pagado"));
        v.setChequeId(BD.largo(rs, "cheque_id"));
        v.setCliente(BD.referencia(rs, "cliente_id", Cliente::new, Cliente::setId));
        v.setOrdenDeServicio(BD.referencia(rs, "orden_de_servicio_id", OrdenDeServicio::new, OrdenDeServicio::setId));
        return v;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<ViajeCobrado> lista) throws SQLException {
        completar(conexion, lista, ViajeCobrado::getCliente, ViajeCobrado::setCliente, new ClienteDAO());
        completar(conexion, lista, ViajeCobrado::getOrdenDeServicio, ViajeCobrado::setOrdenDeServicio,
                new OrdenDeServicioDAO());
    }

    @Override
    protected Long idDe(ViajeCobrado v) {
        return v.getId();
    }

    @Override
    protected void ponerId(ViajeCobrado v, Long id) {
        v.setId(id);
    }

    /** Lista de la pestaña "Viajes": lo mas reciente primero. */
    @Override
    public List<ViajeCobrado> listarTodos() {
        return listar("ORDER BY fecha DESC, id DESC");
    }

    /** Suma lo que un cheque pre-datado lleva de viaje (0 si no lleva nada). */
    public BigDecimal sumarPorCheque(Connection conexion, Long chequeId) throws SQLException {
        return BD.valor(conexion, BigDecimal.class,
                "SELECT COALESCE(SUM(valor), 0) FROM viaje_cobrado WHERE cheque_id = ?", chequeId);
    }

    /** Cambia SOLO el estado pagado/no pagado del viaje de una OS (no toca ninguna otra columna). */
    public void actualizarPagadoPorOrden(Connection conexion, Long ordenId, boolean pagado) throws SQLException {
        BD.actualizar(conexion, "UPDATE viaje_cobrado SET pagado = ? WHERE orden_de_servicio_id = ? AND os_quitada",
                pagado, ordenId);
    }

    /** La OS quedo paga entera: todo su viaje retenido pasa a aparecer en la pestaña. */
    public void marcarOsQuitada(Connection conexion, Long ordenId) throws SQLException {
        BD.actualizar(conexion, "UPDATE viaje_cobrado SET os_quitada = true WHERE orden_de_servicio_id = ?", ordenId);
    }

    /** El cheque se confirmo: sus viajes pasan a cobrados, con la fecha de hoy. */
    public void confirmarPorCheque(Connection conexion, Long chequeId, LocalDate fecha) throws SQLException {
        BD.actualizar(conexion, "UPDATE viaje_cobrado SET confirmado = true, fecha = ? WHERE cheque_id = ?",
                fecha, chequeId);
    }
}
