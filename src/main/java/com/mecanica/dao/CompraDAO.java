package com.mecanica.dao;

import com.mecanica.model.Compra;
import com.mecanica.model.Proveedor;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CompraDAO extends AbstractGenericDAO<Compra, Long> {

    @Override
    protected String tabla() {
        return "compra";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"numero", "proveedor_id", "fecha", "valor_total"};
    }

    @Override
    protected Object[] valoresColumnas(Compra c) {
        return new Object[] {c.getNumero(), c.getProveedor() == null ? null : c.getProveedor().getId(), c.getFecha(),
                c.getValorTotal()};
    }

    @Override
    protected Compra mapear(ResultSet rs) throws SQLException {
        Compra c = new Compra();
        c.setNumero(BD.largo(rs, "numero"));
        c.setProveedor(BD.referencia(rs, "proveedor_id", Proveedor::new, Proveedor::setId));
        c.setFecha(BD.fecha(rs, "fecha"));
        c.setValorTotal(rs.getBigDecimal("valor_total"));
        return c;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<Compra> lista) throws SQLException {
        completar(conexion, lista, Compra::getProveedor, Compra::setProveedor, new ProveedorDAO());
    }

    @Override
    protected Long idDe(Compra c) {
        return c.getId();
    }

    @Override
    protected void ponerId(Compra c, Long id) {
        c.setId(id);
    }

    /** Los items de la nota se borraban en cascada junto con la nota. */
    @Override
    protected void eliminarDependientes(Connection conexion, Long id) throws SQLException {
        BD.actualizar(conexion, "DELETE FROM item_compra WHERE compra_id = ?", id);
    }

    public List<Compra> listarPorFornecedor(Proveedor proveedor) {
        return listar("WHERE proveedor_id = ? ORDER BY numero DESC", proveedor.getId());
    }

    /**
     * Lo usa el Controller para generar el proximo numero secuencial de la
     * notinha (numero actual + 1). Devuelve null si todavia no existe
     * ninguna Compra -- mismo patron de OrdenDeServicioDAO.buscarMayorNumero.
     */
    public Long buscarMayorNumero() {
        return BD.consultar(conexion -> BD.valor(conexion, Long.class, "SELECT MAX(numero) FROM compra"));
    }
}
