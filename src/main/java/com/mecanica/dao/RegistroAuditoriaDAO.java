package com.mecanica.dao;

import com.mecanica.model.RegistroAuditoria;
import com.mecanica.util.BD;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RegistroAuditoriaDAO extends AbstractGenericDAO<RegistroAuditoria, Long> {

    @Override
    protected String tabla() {
        return "registro_auditoria";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"fecha_hora", "usuario_login", "usuario_nombre", "accion", "detalle"};
    }

    @Override
    protected Object[] valoresColumnas(RegistroAuditoria r) {
        return new Object[] {r.getFechaHora(), r.getUsuarioLogin(), r.getUsuarioNombre(), r.getAccion(),
                r.getDetalle()};
    }

    @Override
    protected RegistroAuditoria mapear(ResultSet rs) throws SQLException {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setFechaHora(BD.fechaHora(rs, "fecha_hora"));
        r.setUsuarioLogin(rs.getString("usuario_login"));
        r.setUsuarioNombre(rs.getString("usuario_nombre"));
        r.setAccion(rs.getString("accion"));
        r.setDetalle(rs.getString("detalle"));
        return r;
    }

    @Override
    protected Long idDe(RegistroAuditoria r) {
        return r.getId();
    }

    @Override
    protected void ponerId(RegistroAuditoria r, Long id) {
        r.setId(id);
    }

    /**
     * Registros entre las dos fechas (inclusive), del mas nuevo al mas viejo.
     * Con login null o vacio trae los de todos los usuarios.
     */
    public List<RegistroAuditoria> listar(LocalDate desde, LocalDate hasta, String login) {
        boolean filtrarUsuario = login != null && !login.isBlank();
        String sql = "WHERE fecha_hora >= ? AND fecha_hora < ?"
                + (filtrarUsuario ? " AND usuario_login = ?" : "")
                + " ORDER BY fecha_hora DESC";
        return filtrarUsuario
                ? listar(sql, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(), login)
                : listar(sql, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    /** true si ya existe un registro con esa accion exacta (para pasos de migracion que solo deben correr una vez). */
    public boolean existeAccion(String accion) {
        Long total = BD.consultar(conexion -> BD.valor(conexion, Long.class,
                "SELECT COUNT(*) FROM registro_auditoria WHERE accion = ?", accion));
        return total != null && total > 0;
    }
}
