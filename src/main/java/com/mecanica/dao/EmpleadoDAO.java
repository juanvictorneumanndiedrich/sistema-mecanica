package com.mecanica.dao;

import com.mecanica.model.Empleado;
import com.mecanica.util.BD;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class EmpleadoDAO extends AbstractGenericDAO<Empleado, Long> {

    @Override
    protected String tabla() {
        return "empleado";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"nombre", "documento", "telefono", "cargo", "salario_base", "fecha_admision", "activo"};
    }

    @Override
    protected Object[] valoresColumnas(Empleado e) {
        return new Object[] {e.getNombre(), e.getDocumento(), e.getTelefono(), e.getCargo(), e.getSalarioBase(),
                e.getFechaAdmision(), e.isActivo()};
    }

    @Override
    protected Empleado mapear(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setNombre(rs.getString("nombre"));
        e.setDocumento(rs.getString("documento"));
        e.setTelefono(rs.getString("telefono"));
        e.setCargo(rs.getString("cargo"));
        e.setSalarioBase(rs.getBigDecimal("salario_base"));
        e.setFechaAdmision(BD.fecha(rs, "fecha_admision"));
        e.setActivo(rs.getBoolean("activo"));
        return e;
    }

    @Override
    protected Long idDe(Empleado e) {
        return e.getId();
    }

    @Override
    protected void ponerId(Empleado e, Long id) {
        e.setId(id);
    }

    public List<Empleado> listarActivos() {
        return listar("WHERE activo = true ORDER BY nombre");
    }

    public List<Empleado> buscarPorNombre(String nombre) {
        return listar("WHERE LOWER(nombre) LIKE LOWER(?) ORDER BY nombre", "%" + nombre + "%");
    }
}
