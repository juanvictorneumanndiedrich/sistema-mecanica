package com.mecanica.dao;

import com.mecanica.model.CierreMensual;
import com.mecanica.model.CierreSocioDetalle;
import com.mecanica.model.Socio;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO del detalle por socio de un CierreMensual. Antes estos registros se
 * grababan y leian "de arrastre" junto con el cierre; ahora lo hace este DAO,
 * llamado desde CierreMensualDAO/CierreMensualController.
 */
public class CierreSocioDetalleDAO extends AbstractGenericDAO<CierreSocioDetalle, Long> {

    @Override
    protected String tabla() {
        return "cierre_socio_detalle";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"cierre_id", "socio_id", "parteganancia", "yaretirado", "valorarecibir"};
    }

    @Override
    protected Object[] valoresColumnas(CierreSocioDetalle d) {
        return new Object[] {d.getCierre() == null ? null : d.getCierre().getId(),
                d.getSocio() == null ? null : d.getSocio().getId(), d.getParteGanancia(), d.getYaRetirado(),
                d.getValorARecibir()};
    }

    @Override
    protected CierreSocioDetalle mapear(ResultSet rs) throws SQLException {
        CierreSocioDetalle d = new CierreSocioDetalle();
        d.setCierre(BD.referencia(rs, "cierre_id", CierreMensual::new, CierreMensual::setId));
        d.setSocio(BD.referencia(rs, "socio_id", Socio::new, Socio::setId));
        d.setParteGanancia(rs.getBigDecimal("parteganancia"));
        d.setYaRetirado(rs.getBigDecimal("yaretirado"));
        d.setValorARecibir(rs.getBigDecimal("valorarecibir"));
        return d;
    }

    /** Solo el socio: el cierre lo pone CierreMensualDAO (es el mismo objeto que se esta cargando). */
    @Override
    protected void completarReferencias(Connection conexion, List<CierreSocioDetalle> lista) throws SQLException {
        completar(conexion, lista, CierreSocioDetalle::getSocio, CierreSocioDetalle::setSocio, new SocioDAO());
    }

    @Override
    protected Long idDe(CierreSocioDetalle d) {
        return d.getId();
    }

    @Override
    protected void ponerId(CierreSocioDetalle d, Long id) {
        d.setId(id);
    }

    /** Detalles de varios cierres de una vez. */
    public List<CierreSocioDetalle> listarPorCierres(Connection conexion, Long[] cierreIds) throws SQLException {
        return listar(conexion, "WHERE cierre_id = ANY(?) ORDER BY id", (Object) cierreIds);
    }
}
