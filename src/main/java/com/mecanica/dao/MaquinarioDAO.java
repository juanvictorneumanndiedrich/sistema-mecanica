package com.mecanica.dao;

import com.mecanica.enums.TipoMaquinario;
import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MaquinarioDAO extends AbstractGenericDAO<Maquinario, Long> {

    @Override
    protected String tabla() {
        return "maquinario";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"cliente_id", "tipo", "marca", "modelo", "identificacion", "observacion"};
    }

    @Override
    protected Object[] valoresColumnas(Maquinario m) {
        return new Object[] {m.getCliente() == null ? null : m.getCliente().getId(), m.getTipo(), m.getMarca(),
                m.getModelo(), m.getIdentificacion(), m.getObservacion()};
    }

    @Override
    protected Maquinario mapear(ResultSet rs) throws SQLException {
        Maquinario m = new Maquinario();
        m.setCliente(BD.referencia(rs, "cliente_id", Cliente::new, Cliente::setId));
        m.setTipo(BD.enumerado(rs, "tipo", TipoMaquinario.class));
        m.setMarca(rs.getString("marca"));
        m.setModelo(rs.getString("modelo"));
        m.setIdentificacion(rs.getString("identificacion"));
        m.setObservacion(rs.getString("observacion"));
        return m;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<Maquinario> lista) throws SQLException {
        completar(conexion, lista, Maquinario::getCliente, Maquinario::setCliente, new ClienteDAO());
    }

    @Override
    protected Long idDe(Maquinario m) {
        return m.getId();
    }

    @Override
    protected void ponerId(Maquinario m, Long id) {
        m.setId(id);
    }

    /** Se usa en la pantalla de Clientes y Maquinarios, para listar los maquinarios de un cliente. */
    public List<Maquinario> listarPorCliente(Cliente cliente) {
        return listar("WHERE cliente_id = ? ORDER BY identificacion", cliente.getId());
    }
}
