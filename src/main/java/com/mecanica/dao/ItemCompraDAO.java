package com.mecanica.dao;

import com.mecanica.model.Compra;
import com.mecanica.model.ItemCompra;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ItemCompraDAO extends AbstractGenericDAO<ItemCompra, Long> {

    @Override
    protected String tabla() {
        return "item_compra";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"compra_id", "descripcion", "cantidad", "valor_unitario", "valor_total"};
    }

    @Override
    protected Object[] valoresColumnas(ItemCompra i) {
        return new Object[] {i.getCompra() == null ? null : i.getCompra().getId(), i.getDescripcion(),
                i.getCantidad(), i.getValorUnitario(), i.getValorTotal()};
    }

    @Override
    protected ItemCompra mapear(ResultSet rs) throws SQLException {
        ItemCompra i = new ItemCompra();
        i.setCompra(BD.referencia(rs, "compra_id", Compra::new, Compra::setId));
        i.setDescripcion(rs.getString("descripcion"));
        i.setCantidad(rs.getBigDecimal("cantidad"));
        i.setValorUnitario(rs.getBigDecimal("valor_unitario"));
        i.setValorTotal(rs.getBigDecimal("valor_total"));
        return i;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<ItemCompra> lista) throws SQLException {
        completar(conexion, lista, ItemCompra::getCompra, ItemCompra::setCompra, new CompraDAO());
    }

    @Override
    protected Long idDe(ItemCompra i) {
        return i.getId();
    }

    @Override
    protected void ponerId(ItemCompra i, Long id) {
        i.setId(id);
    }

    public List<ItemCompra> listarPorCompra(Compra compra) {
        return listar("WHERE compra_id = ? ORDER BY id", compra.getId());
    }
}
