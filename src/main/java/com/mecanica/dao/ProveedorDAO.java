package com.mecanica.dao;

import com.mecanica.model.Proveedor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ProveedorDAO extends AbstractGenericDAO<Proveedor, Long> {

    @Override
    protected String tabla() {
        return "proveedor";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"nombre", "documento", "telefono", "contacto", "saldo"};
    }

    @Override
    protected Object[] valoresColumnas(Proveedor p) {
        return new Object[] {p.getNombre(), p.getDocumento(), p.getTelefono(), p.getContacto(), p.getSaldo()};
    }

    @Override
    protected Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setNombre(rs.getString("nombre"));
        p.setDocumento(rs.getString("documento"));
        p.setTelefono(rs.getString("telefono"));
        p.setContacto(rs.getString("contacto"));
        p.setSaldo(rs.getBigDecimal("saldo"));
        return p;
    }

    @Override
    protected Long idDe(Proveedor p) {
        return p.getId();
    }

    @Override
    protected void ponerId(Proveedor p, Long id) {
        p.setId(id);
    }

    public List<Proveedor> buscarPorNombre(String nombre) {
        return listar("WHERE LOWER(nombre) LIKE LOWER(?) ORDER BY nombre", "%" + nombre + "%");
    }

    public Proveedor buscarPorDocumento(String documento) {
        return primero("WHERE documento = ?", documento);
    }
}
