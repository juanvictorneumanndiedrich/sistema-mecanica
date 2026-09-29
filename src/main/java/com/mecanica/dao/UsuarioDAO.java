package com.mecanica.dao;

import com.mecanica.model.Usuario;
import com.mecanica.util.BD;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UsuarioDAO extends AbstractGenericDAO<Usuario, Long> {

    @Override
    protected String tabla() {
        return "usuario";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"nombre", "login", "clave", "activo",
                "permiso_clientes_maquinarios", "permiso_ordenes_servicio", "permiso_compras_proveedores",
                "permiso_financiero", "permiso_empleados_socios", "permiso_usuarios",
                "permiso_socios", "permiso_editar_empleados", "permiso_retiros_empleado", "permiso_pagar_salario",
                "permiso_cierre_mensual", "permiso_movimiento_manual", "permiso_eliminar_registros",
                "permiso_cancelar_os", "permiso_retirar_saldo", "mostrar_ayuda", "tour_visto"};
    }

    @Override
    protected Object[] valoresColumnas(Usuario u) {
        return new Object[] {u.getNombre(), u.getLogin(), u.getClave(), u.isActivo(),
                u.isPermisoClientesMaquinarios(), u.isPermisoOrdenesServicio(), u.isPermisoComprasProveedores(),
                u.isPermisoFinanciero(), u.isPermisoEmpleadosSocios(), u.isPermisoUsuarios(),
                u.isPermisoSocios(), u.isPermisoEditarEmpleados(), u.isPermisoRetirosEmpleado(),
                u.isPermisoPagarSalario(), u.isPermisoCierreMensual(), u.isPermisoMovimientoManual(),
                u.isPermisoEliminarRegistros(), u.isPermisoCancelarOs(), u.isPermisoRetirarSaldo(),
                u.isMostrarAyuda(), u.isTourVisto()};
    }

    @Override
    protected Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setNombre(rs.getString("nombre"));
        u.setLogin(rs.getString("login"));
        u.setClave(rs.getString("clave"));
        u.setActivo(rs.getBoolean("activo"));
        u.setPermisoClientesMaquinarios(rs.getBoolean("permiso_clientes_maquinarios"));
        u.setPermisoOrdenesServicio(rs.getBoolean("permiso_ordenes_servicio"));
        u.setPermisoComprasProveedores(rs.getBoolean("permiso_compras_proveedores"));
        u.setPermisoFinanciero(rs.getBoolean("permiso_financiero"));
        u.setPermisoEmpleadosSocios(rs.getBoolean("permiso_empleados_socios"));
        u.setPermisoUsuarios(rs.getBoolean("permiso_usuarios"));
        u.setPermisoSocios(rs.getBoolean("permiso_socios"));
        u.setPermisoEditarEmpleados(rs.getBoolean("permiso_editar_empleados"));
        u.setPermisoRetirosEmpleado(rs.getBoolean("permiso_retiros_empleado"));
        u.setPermisoPagarSalario(rs.getBoolean("permiso_pagar_salario"));
        u.setPermisoCierreMensual(rs.getBoolean("permiso_cierre_mensual"));
        u.setPermisoMovimientoManual(rs.getBoolean("permiso_movimiento_manual"));
        u.setPermisoEliminarRegistros(rs.getBoolean("permiso_eliminar_registros"));
        u.setPermisoCancelarOs(rs.getBoolean("permiso_cancelar_os"));
        u.setPermisoRetirarSaldo(rs.getBoolean("permiso_retirar_saldo"));
        u.setMostrarAyuda(rs.getBoolean("mostrar_ayuda"));
        u.setTourVisto(rs.getBoolean("tour_visto"));
        return u;
    }

    @Override
    protected Long idDe(Usuario u) {
        return u.getId();
    }

    @Override
    protected void ponerId(Usuario u, Long id) {
        u.setId(id);
    }

    /** Se usa en la pantalla de login. */
    public Usuario buscarPorLogin(String login) {
        return primero("WHERE login = ?", login);
    }

    /**
     * Guarda solo las dos opciones de la ayuda en pantalla, sin tocar el resto
     * del usuario (clave, permisos...).
     */
    public void guardarOpcionesAyuda(Long id, boolean mostrarAyuda, boolean tourVisto) {
        BD.ejecutarEnTransaccion(conexion -> BD.actualizar(conexion,
                "UPDATE usuario SET mostrar_ayuda = ?, tour_visto = ? WHERE id = ?", mostrarAyuda, tourVisto, id));
    }

    public List<Usuario> listarActivos() {
        return listar("WHERE activo = true ORDER BY nombre");
    }
}
