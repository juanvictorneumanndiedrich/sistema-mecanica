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
        return new String[] {"nombre", "documento", "telefono", "direccion", "saldo", "alias_1", "alias_2",
                "alias_3"};
    }

    @Override
    protected Object[] valoresColumnas(Cliente c) {
        return new Object[] {c.getNombre(), c.getDocumento(), c.getTelefono(), c.getDireccion(), c.getSaldo(),
                c.getAlias1(), c.getAlias2(), c.getAlias3()};
    }

    @Override
    protected Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setNombre(rs.getString("nombre"));
        c.setDocumento(rs.getString("documento"));
        c.setTelefono(rs.getString("telefono"));
        c.setDireccion(rs.getString("direccion"));
        c.setSaldo(rs.getBigDecimal("saldo"));
        c.setAlias1(rs.getString("alias_1"));
        c.setAlias2(rs.getString("alias_2"));
        c.setAlias3(rs.getString("alias_3"));
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

    /** Busqueda por cualquiera de los tres alias (que contenga el texto). */
    public List<Cliente> buscarPorAlias(String alias) {
        String patron = "%" + alias + "%";
        return listar("WHERE LOWER(alias_1) LIKE LOWER(?) OR LOWER(alias_2) LIKE LOWER(?) "
                + "OR LOWER(alias_3) LIKE LOWER(?) ORDER BY nombre", patron, patron, patron);
    }

    /** Busqueda por documento CI/RUC (que contenga el texto, no tiene que ser exacto). */
    public List<Cliente> buscarPorDocumentoParecido(String documento) {
        return listar("WHERE LOWER(documento) LIKE LOWER(?) ORDER BY nombre", "%" + documento + "%");
    }

    /** Busca por documento (CI/RUC) exato. */
    public Cliente buscarPorDocumento(String documento) {
        return primero("WHERE documento = ?", documento);
    }
}
