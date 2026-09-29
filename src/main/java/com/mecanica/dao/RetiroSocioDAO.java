package com.mecanica.dao;

import com.mecanica.model.CierreMensual;
import com.mecanica.model.RetiroSocio;
import com.mecanica.model.Socio;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RetiroSocioDAO extends AbstractGenericDAO<RetiroSocio, Long> {

    @Override
    protected String tabla() {
        return "retiro_socio";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"socio_id", "fecha", "valor", "observacion", "cierre_id"};
    }

    @Override
    protected Object[] valoresColumnas(RetiroSocio r) {
        return new Object[] {r.getSocio() == null ? null : r.getSocio().getId(), r.getFecha(), r.getValor(),
                r.getObservacion(), r.getCierre() == null ? null : r.getCierre().getId()};
    }

    @Override
    protected RetiroSocio mapear(ResultSet rs) throws SQLException {
        RetiroSocio r = new RetiroSocio();
        r.setSocio(BD.referencia(rs, "socio_id", Socio::new, Socio::setId));
        r.setFecha(BD.fecha(rs, "fecha"));
        r.setValor(rs.getBigDecimal("valor"));
        r.setObservacion(rs.getString("observacion"));
        r.setCierre(BD.referencia(rs, "cierre_id", CierreMensual::new, CierreMensual::setId));
        return r;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<RetiroSocio> lista) throws SQLException {
        completar(conexion, lista, RetiroSocio::getSocio, RetiroSocio::setSocio, new SocioDAO());
        completar(conexion, lista, RetiroSocio::getCierre, RetiroSocio::setCierre, new CierreMensualDAO());
    }

    @Override
    protected Long idDe(RetiroSocio r) {
        return r.getId();
    }

    @Override
    protected void ponerId(RetiroSocio r, Long id) {
        r.setId(id);
    }

    /** Se usa en la liquidacion (division de ganancia 50/50): suma lo que el socio ya retiro en el periodo. */
    public List<RetiroSocio> listarPorSocioYPeriodo(Socio socio, LocalDate inicio, LocalDate fin) {
        return listar("WHERE socio_id = ? AND fecha BETWEEN ? AND ? ORDER BY fecha", socio.getId(), inicio, fin);
    }

    /** Retiros de socio que fueron descontados en un cierre mensual (para el reporte del acerto). */
    public List<RetiroSocio> listarPorCierre(CierreMensual cierre) {
        return listar("WHERE cierre_id = ? ORDER BY fecha, id", cierre.getId());
    }

    /** Retiros del socio que todavia no entraron en ningun cierre (dentro de la transaccion del cierre). */
    public List<RetiroSocio> listarSinCierre(Connection conexion, Socio socio) throws SQLException {
        return listar(conexion, "WHERE socio_id = ? AND cierre_id IS NULL ORDER BY fecha", socio.getId());
    }

    /** Marca el retiro como descontado en un cierre (dentro de la transaccion del cierre). */
    public void marcarCierre(Connection conexion, Long retiroId, Long cierreId) throws SQLException {
        BD.actualizar(conexion, "UPDATE retiro_socio SET cierre_id = ? WHERE id = ?", cierreId, retiroId);
    }
}
