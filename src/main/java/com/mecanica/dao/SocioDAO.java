package com.mecanica.dao;

import com.mecanica.model.Socio;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SocioDAO extends AbstractGenericDAO<Socio, Long> {

    @Override
    protected String tabla() {
        return "socio";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"nombre", "documento", "telefono", "activo"};
    }

    @Override
    protected Object[] valoresColumnas(Socio s) {
        return new Object[] {s.getNombre(), s.getDocumento(), s.getTelefono(), s.isActivo()};
    }

    @Override
    protected Socio mapear(ResultSet rs) throws SQLException {
        Socio s = new Socio();
        s.setNombre(rs.getString("nombre"));
        s.setDocumento(rs.getString("documento"));
        s.setTelefono(rs.getString("telefono"));
        s.setActivo(rs.getBoolean("activo"));
        return s;
    }

    @Override
    protected Long idDe(Socio s) {
        return s.getId();
    }

    @Override
    protected void ponerId(Socio s, Long id) {
        s.setId(id);
    }

    public List<Socio> listarActivos() {
        return listar("WHERE activo = true ORDER BY nombre");
    }
}
