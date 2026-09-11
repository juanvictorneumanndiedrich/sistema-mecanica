package com.mecanica.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.mecanica.dao.UsuarioDAO;
import com.mecanica.model.Usuario;

/**
 * Controller de Usuario: registro, edicion, permisos y autenticacion
 * (login). La clave nunca se guarda en texto plano -- siempre es un hash
 * SHA-256 (suficiente para el alcance academico del proyecto).
 */
public class UsuarioController {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /** Registra un usuario nuevo, recibiendo la clave en texto plano para generar el hash. */
    public Usuario registrar(Usuario usuario, String claveEnTextoPlano) {
        validarDatosBasicos(usuario);
        if (claveEnTextoPlano == null || claveEnTextoPlano.isBlank()) {
            throw new IllegalArgumentException("La clave es obligatoria.");
        }
        if (usuarioDAO.buscarPorLogin(usuario.getLogin()) != null) {
            throw new IllegalArgumentException("Ya existe un usuario con ese login.");
        }
        usuario.setClave(hashClave(claveEnTextoPlano));
        return usuarioDAO.guardar(usuario);
    }

    /**
     * Si todavia no existe ningun usuario -- base recien creada, o schema
     * recreado -- crea uno inicial "admin" (clave "admin") con todos los
     * permisos, para que se pueda entrar al sistema por primera vez.
     * Devuelve true solo cuando lo acaba de crear, asi la pantalla de
     * login puede avisar los datos al usuario.
     *
     * Esa clave es provisoria: lo primero que hay que hacer despues de
     * entrar es cambiarla en Usuarios y Permisos.
     */
    public boolean asegurarUsuarioInicial() {
        if (!usuarioDAO.listarTodos().isEmpty()) {
            return false;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setLogin("admin");
        admin.setActivo(true);
        admin.setPermisoClientesEquipos(true);
        admin.setPermisoOrdenesServicio(true);
        admin.setPermisoComprasProveedores(true);
        admin.setPermisoFinanciero(true);
        admin.setPermisoEmpleadosSocios(true);
        admin.setPermisoUsuarios(true);
        registrar(admin, "admin");
        return true;
    }

    /**
     * Actualiza nombre/login/permisos/activo de un usuario ya existente, sin
     * tocar la clave (para eso, ver cambiarClave).
     */
    public Usuario actualizarDatos(Usuario usuario) {
        validarDatosBasicos(usuario);
        return usuarioDAO.guardar(usuario);
    }

    public void cambiarClave(Usuario usuario, String nuevaClaveEnTextoPlano) {
        if (nuevaClaveEnTextoPlano == null || nuevaClaveEnTextoPlano.isBlank()) {
            throw new IllegalArgumentException("La nueva clave es obligatoria.");
        }
        usuario.setClave(hashClave(nuevaClaveEnTextoPlano));
        usuarioDAO.guardar(usuario);
    }

    /** Se usa en la pantalla de login. Devuelve null si login/clave no coinciden o el usuario esta inactivo. */
    public Usuario autenticar(String login, String claveEnTextoPlano) {
        Usuario usuario = usuarioDAO.buscarPorLogin(login);
        if (usuario == null || !usuario.isActivo()) {
            return null;
        }
        return hashClave(claveEnTextoPlano).equals(usuario.getClave()) ? usuario : null;
    }

    public Usuario buscarPorId(Long id) {
        return usuarioDAO.buscarPorId(id);
    }

    public List<Usuario> listarTodos() {
        return usuarioDAO.listarTodos();
    }

    public List<Usuario> listarActivos() {
        return usuarioDAO.listarActivos();
    }

    public void eliminar(Usuario usuario) {
        usuarioDAO.eliminar(usuario);
    }

    private void validarDatosBasicos(Usuario usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio.");
        }
        if (usuario.getLogin() == null || usuario.getLogin().isBlank()) {
            throw new IllegalArgumentException("El login es obligatorio.");
        }
    }

    private String hashClave(String claveEnTextoPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(claveEnTextoPlano.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash no disponible.", e);
        }
    }
}
