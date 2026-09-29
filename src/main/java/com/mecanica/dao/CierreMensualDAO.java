package com.mecanica.dao;

import com.mecanica.model.CierreMensual;
import com.mecanica.model.CierreSocioDetalle;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CierreMensualDAO extends AbstractGenericDAO<CierreMensual, Long> {

    @Override
    protected String tabla() {
        return "cierre_mensual";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"fechacierre", "descripcion", "totalentradas", "totalsalidas", "gananciatotal"};
    }

    @Override
    protected Object[] valoresColumnas(CierreMensual c) {
        return new Object[] {c.getFechaCierre(), c.getDescripcion(), c.getTotalEntradas(), c.getTotalSalidas(),
                c.getGananciaTotal()};
    }

    @Override
    protected CierreMensual mapear(ResultSet rs) throws SQLException {
        CierreMensual c = new CierreMensual();
        c.setFechaCierre(BD.fecha(rs, "fechacierre"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setTotalEntradas(rs.getBigDecimal("totalentradas"));
        c.setTotalSalidas(rs.getBigDecimal("totalsalidas"));
        c.setGananciaTotal(rs.getBigDecimal("gananciatotal"));
        return c;
    }

    @Override
    protected Long idDe(CierreMensual c) {
        return c.getId();
    }

    @Override
    protected void ponerId(CierreMensual c, Long id) {
        c.setId(id);
    }

    /** Los detalles por socio se borraban en cascada junto con el cierre. */
    @Override
    protected void eliminarDependientes(Connection conexion, Long id) throws SQLException {
        BD.actualizar(conexion, "DELETE FROM cierre_socio_detalle WHERE cierre_id = ?", id);
    }

    /**
     * Historico de cierres, del mas reciente al mas antiguo, con los
     * detalles por socio ya cargados, para poder mostrar el reporte sin
     * volver a consultar la base.
     */
    public List<CierreMensual> listarOrdenados() {
        return BD.consultar(conexion -> {
            List<CierreMensual> cierres = listar(conexion, "ORDER BY fechacierre DESC, id DESC");
            cargarDetalles(conexion, cierres);
            return cierres;
        });
    }

    /** Un cierre con sus detalles por socio ya cargados (para imprimir el acerto). */
    public CierreMensual buscarConDetalles(Long id) {
        return BD.consultar(conexion -> {
            List<CierreMensual> cierres = listar(conexion, "WHERE id = ?", id);
            cargarDetalles(conexion, cierres);
            return cierres.isEmpty() ? null : cierres.get(0);
        });
    }

    private void cargarDetalles(Connection conexion, List<CierreMensual> cierres) throws SQLException {
        if (cierres.isEmpty()) {
            return;
        }
        Map<Long, CierreMensual> porId = new HashMap<>();
        for (CierreMensual c : cierres) {
            porId.put(c.getId(), c);
        }
        List<CierreSocioDetalle> detalles = new CierreSocioDetalleDAO()
                .listarPorCierres(conexion, porId.keySet().toArray(new Long[0]));
        for (CierreSocioDetalle d : detalles) {
            CierreMensual cierre = porId.get(d.getCierre().getId());
            d.setCierre(cierre);
            cierre.getDetalles().add(d);
        }
    }
}
