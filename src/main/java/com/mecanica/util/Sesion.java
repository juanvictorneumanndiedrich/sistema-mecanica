package com.mecanica.util;

import com.mecanica.enums.Permiso;
import com.mecanica.model.Usuario;

/**
 * Guarda el usuario que esta usando el sistema en este momento (el que hizo
 * login). La usan las pantallas para mostrar u ocultar botones/pestanas
 * segun los permisos, los Controllers para rechazar acciones sin permiso
 * (exigir) y el registro de actividad para saber quien hizo cada cosa.
 */
public final class Sesion {

    private static volatile Usuario usuarioActual;

    private Sesion() {
        // clase utilitaria: no debe ser instanciada
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static void cerrar() {
        usuarioActual = null;
    }

    public static Usuario getUsuario() {
        return usuarioActual;
    }

    /**
     * true si el usuario logueado tiene el permiso. Un permiso de accion que
     * pertenece a un area (ej. SOCIOS dentro de EMPLEADOS_SOCIOS) exige
     * tambien el permiso del area.
     */
    public static boolean tiene(Permiso permiso) {
        Usuario usuario = usuarioActual;
        if (usuario == null || !usuario.tiene(permiso)) {
            return false;
        }
        Permiso area = permiso.area();
        return area == null || usuario.tiene(area);
    }

    /**
     * Corta la accion si el usuario logueado no tiene el permiso. Se llama al
     * principio de los metodos delicados de los Controllers, como segunda
     * barrera ademas de ocultar el boton en la pantalla. Sin sesion (por
     * ejemplo, en una prueba fuera de la aplicacion) no bloquea.
     */
    public static void exigir(Permiso permiso) {
        if (usuarioActual != null && !tiene(permiso)) {
            throw new IllegalStateException("Su usuario no tiene permiso para: " + permiso.getEtiqueta() + ".");
        }
    }

    /** true si el usuario dado es el mismo que esta logueado. */
    public static boolean esUsuarioActual(Usuario usuario) {
        Usuario actual = usuarioActual;
        return actual != null && usuario != null && actual.getId() != null && actual.getId().equals(usuario.getId());
    }
}
