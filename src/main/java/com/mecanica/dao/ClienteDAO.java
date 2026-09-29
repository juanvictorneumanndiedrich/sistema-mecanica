package com.mecanica.dao;

import com.mecanica.model.Cliente;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO especifico de Cliente. Ejemplo de como extender AbstractGenericDAO
 * y agregar busquedas propias de la entidad -- los otros DAOs siguen
 * exactamente el mismo patron.
 */
public class ClienteDAO extends AbstractGenericDAO<Cliente, Long> {

    @Override
    protected String tabla() {
        return "cliente";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"nombre", "documento", "telefono", "direccion", "saldo"};
    }

    @Override
    protected Object[] valoresColumnas(Cliente c) {
        return new Object[] {c.getNombre(), c.getDocumento(), c.getTelefono(), c.getDireccion(), c.getSaldo()};
    }

    @Override
    protected Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setNombre(rs.getString("nombre"));
        c.setDocumento(rs.getString("documento"));
        c.setTelefono(rs.getString("telefono"));
        c.setDireccion(rs.getString("direccion"));
        c.setSaldo(rs.getBigDecimal("saldo"));
        return c;
    }

    @Override
    protected Long idDe(Cliente c) {
        return c.getId();
    }

    @Override
    protected void ponerId(Cliente c, Long id) {
        c.setId(id);
    }

    /** Los maquinarios del cliente se borraban en cascada junto con el cliente. */
    @Override
    protected void eliminarDependientes(Connection conexion, Long id) throws SQLException {
        BD.actualizar(conexion, "DELETE FROM maquinario WHERE cliente_id = ?", id);
    }

    /** Busqueda por nombre (que contenga el texto), usada en la pantalla de Clientes y Maquinarios. */
    public List<Cliente> buscarPorNombre(String nombre) {
        return listar("WHERE LOWER(nombre) LIKE LOWER(?) ORDER BY nombre", "%" + nombre + "%");
    }

    /** Busca por documento (CI/RUC) exato. */
    public Cliente buscarPorDocumento(String documento) {
        return primero("WHERE documento = ?", documento);
    }
}
