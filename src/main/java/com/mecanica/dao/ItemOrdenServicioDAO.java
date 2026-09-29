package com.mecanica.dao;

import com.mecanica.enums.TipoItemOrdenServicio;
import com.mecanica.model.ItemOrdenServicio;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.util.BD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ItemOrdenServicioDAO extends AbstractGenericDAO<ItemOrdenServicio, Long> {

    @Override
    protected String tabla() {
        return "item_orden_servicio";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"orden_de_servicio_id", "tipo", "descripcion", "cantidad", "valor_unitario",
                "valor_total"};
    }

    @Override
    protected Object[] valoresColumnas(ItemOrdenServicio i) {
        return new Object[] {i.getOrdenDeServicio() == null ? null : i.getOrdenDeServicio().getId(), i.getTipo(),
                i.getDescripcion(), i.getCantidad(), i.getValorUnitario(), i.getValorTotal()};
    }

    @Override
    protected ItemOrdenServicio mapear(ResultSet rs) throws SQLException {
        ItemOrdenServicio i = new ItemOrdenServicio();
        i.setOrdenDeServicio(BD.referencia(rs, "orden_de_servicio_id", OrdenDeServicio::new, OrdenDeServicio::setId));
        i.setTipo(BD.enumerado(rs, "tipo", TipoItemOrdenServicio.class));
        i.setDescripcion(rs.getString("descripcion"));
        i.setCantidad(rs.getBigDecimal("cantidad"));
        i.setValorUnitario(rs.getBigDecimal("valor_unitario"));
        i.setValorTotal(rs.getBigDecimal("valor_total"));
        return i;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<ItemOrdenServicio> lista) throws SQLException {
        completar(conexion, lista, ItemOrdenServicio::getOrdenDeServicio, ItemOrdenServicio::setOrdenDeServicio,
                new OrdenDeServicioDAO());
    }

    @Override
    protected Long idDe(ItemOrdenServicio i) {
        return i.getId();
    }

    @Override
    protected void ponerId(ItemOrdenServicio i, Long id) {
        i.setId(id);
    }

    public List<ItemOrdenServicio> listarPorOrdemDeServico(OrdenDeServicio ordenDeServicio) {
        return listar("WHERE orden_de_servicio_id = ? ORDER BY id", ordenDeServicio.getId());
    }

    /** Suma de los items de una OS (para mantener el valor total de la OS igual a la suma de sus items). */
    public BigDecimal sumarPorOrden(Connection conexion, Long ordenId) throws SQLException {
        return BD.valor(conexion, BigDecimal.class,
                "SELECT COALESCE(SUM(valor_total), 0) FROM item_orden_servicio WHERE orden_de_servicio_id = ?",
                ordenId);
    }
}
