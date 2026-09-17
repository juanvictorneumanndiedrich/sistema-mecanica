package com.mecanica.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.mecanica.dao.UsuarioDAO;
import com.mecanica.enums.Permiso;
import com.mecanica.model.Usuario;
import com.mecanica.util.Sesion;

/**
 * Controller de Usuario: registro, edicion, permisos y autenticacion
 * (login). La clave nunca se guarda en texto plano -- siempre es un hash
 * SHA-256 (suficiente para el alcance academico del proyecto).
 */
public class UsuarioController {

    /** Clave con la que se crea el usuario "admin" inicial; hay que cambiarla al entrar. */
    private static final String CLAVE_PROVISORIA = "admin";

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final AuditoriaController auditoria = new AuditoriaController();

    /** Registra un usuario nuevo, recibiendo la clave en texto plano para generar el hash. */
    public Usuario registrar(Usuario usuario, String claveEnTextoPlano) {
        Sesion.exigir(Permiso.USUARIOS);
        validarDatosBasicos(usuario);
        if (claveEnTextoPlano == null || claveEnTextoPlano.isBlank()) {
            throw new IllegalArgumentException("La clave es obligatoria.");
        }
        if (usuarioDAO.buscarPorLogin(usuario.getLogin()) != null) {
            throw new IllegalArgumentException("Ya existe un usuario con ese login.");
        }
        usuario.setClave(hashClave(claveEnTextoPlano));
        Usuario guardado = usuarioDAO.guardar(usuario);
        auditoria.registrar("USUARIO CREADO", guardado.getNombre() + " (" + guardado.getLogin() + ")");
        return guardado;
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
        for (Permiso permiso : Permiso.values()) {
            admin.setPermiso(permiso, true);
        }
        admin.setClave(hashClave(CLAVE_PROVISORIA));
        usuarioDAO.guardar(admin);
        return true;
    }

    private static final String ACCION_MIGRACION_PERMISOS = "MIGRACION DE PERMISOS (2026-09-17)";

    /**
     * Migracion de datos que corre una unica vez (deja una marca en el
     * registro de actividad para no repetirse en cada arranque): cuando el
     * sistema agrego estos 9 permisos de accion mas finos, el
     * hbm2ddl.auto=update los dejo en "false" para TODOS los usuarios que ya
     * existian -- aunque antes de que existieran estos permisos, tener el
     * area ya alcanzaba para hacer todo lo de adentro. Sin este paso,
     * cualquiera que actualice el sistema pierde de golpe, por ejemplo, la
     * posibilidad de cancelar una OS o pagar un salario, hasta que alguien
     * entre a Usuarios y Permisos y marque las casillas nuevas a mano.
     *
     * <p>A cada usuario que ya tenia el area correspondiente se le prenden
     * los permisos de accion de esa area. Los 3 permisos "delicados"
     * (eliminar registros, cancelar OS, retirar saldo) no pertenecen a
     * ninguna area en particular -- se los prende a quien ya tenia el area
     * de Usuarios y Permisos, el dato mas cercano a "administrador" que
     * existia antes de esta migracion.
     *
     * @return la cantidad de usuarios a los que se les agrego algun permiso.
     */
    public int migrarPermisosDeAccionSiHaceFalta() {
        if (auditoria.yaSeRegistro(ACCION_MIGRACION_PERMISOS)) {
            return 0;
        }
        int afectados = 0;
        for (Usuario usuario : usuarioDAO.listarTodos()) {
            boolean cambio = false;
            for (Permiso permiso : Permiso.values()) {
                if (permiso.esArea() || usuario.tiene(permiso)) {
                    continue;
                }
                Permiso area = permiso.area();
                boolean deberiaTener = area != null ? usuario.tiene(area) : usuario.tiene(Permiso.USUARIOS);
                if (deberiaTener) {
                    usuario.setPermiso(permiso, true);
                    cambio = true;
                }
            }
            if (cambio) {
                usuarioDAO.guardar(usuario);
                afectados++;
            }
        }
        auditoria.registrar(ACCION_MIGRACION_PERMISOS,
                afectados + " usuario(s) recibieron automaticamente los permisos nuevos de su area, "
                        + "segun los permisos que ya tenian antes de esta actualizacion.");
        return afectados;
    }

    /**
     * Actualiza nombre/login/permisos/activo de un usuario ya existente, sin
     * tocar la clave (para eso, ver cambiarClave).
     */
    public Usuario actualizarDatos(Usuario usuario) {
        Sesion.exigir(Permiso.USUARIOS);
        validarDatosBasicos(usuario);

        // Proteccion anti-bloqueo: nadie puede dejar el sistema sin
        // administrador, ni quitarse a si mismo el acceso a esta pantalla.
        boolean quedaAdministrador = usuario.isActivo() && usuario.isPermisoUsuarios();
        if (Sesion.esUsuarioActual(usuario)) {
            if (!usuario.isActivo()) {
                throw new IllegalArgumentException("No puede desactivar su propio usuario.");
            }
            if (!usuario.isPermisoUsuarios()) {
                throw new IllegalArgumentException(
                        "No puede quitarse a si mismo el permiso de Usuarios y Permisos.");
            }
        }
        if (!quedaAdministrador && !hayOtroAdministrador(usuario)) {
            throw new IllegalArgumentException(
                    "Debe quedar al menos un usuario activo con permiso de Usuarios y Permisos.");
        }

        Usuario guardado = usuarioDAO.guardar(usuario);
        if (Sesion.esUsuarioActual(guardado)) {
            // los cambios de permisos propios valen para esta sesion (menu: al volver a entrar)
            Sesion.iniciar(guardado);
        }
        auditoria.registrar("USUARIO EDITADO", guardado.getNombre() + " (" + guardado.getLogin() + ")"
                + (guardado.isActivo() ? "" : " - inactivo") + " - permisos: " + resumenPermisos(guardado));
        return guardado;
    }

    public void cambiarClave(Usuario usuario, String nuevaClaveEnTextoPlano) {
        if (!Sesion.esUsuarioActual(usuario)) {
            Sesion.exigir(Permiso.USUARIOS);
        }
        if (nuevaClaveEnTextoPlano == null || nuevaClaveEnTextoPlano.isBlank()) {
            throw new IllegalArgumentException("La nueva clave es obligatoria.");
        }
        if (nuevaClaveEnTextoPlano.length() < 6) {
            throw new IllegalArgumentException("La clave debe tener al menos 6 caracteres.");
        }
        usuario.setClave(hashClave(nuevaClaveEnTextoPlano));
        usuarioDAO.guardar(usuario);
        auditoria.registrar("CLAVE CAMBIADA", usuario.getNombre() + " (" + usuario.getLogin() + ")");
    }

    /**
     * true si el usuario todavia tiene la clave provisoria "admin" (la del
     * usuario inicial). En ese caso, al entrar, el sistema obliga a cambiarla
     * antes de abrir la ventana principal.
     */
    public boolean usaClaveProvisoria(Usuario usuario) {
        return usuario != null && hashClave(CLAVE_PROVISORIA).equals(usuario.getClave());
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
        Sesion.exigir(Permiso.USUARIOS);
        if (Sesion.esUsuarioActual(usuario)) {
            throw new IllegalArgumentException("No puede eliminar su propio usuario.");
        }
        if (!hayOtroAdministrador(usuario)) {
            throw new IllegalArgumentException(
                    "Debe quedar al menos un usuario activo con permiso de Usuarios y Permisos.");
        }
        usuarioDAO.eliminar(usuario);
        auditoria.registrar("USUARIO ELIMINADO", usuario.getNombre() + " (" + usuario.getLogin() + ")");
    }

    /** true si existe OTRO usuario (distinto del dado) activo y con permiso de Usuarios y Permisos. */
    private boolean hayOtroAdministrador(Usuario usuario) {
        for (Usuario otro : usuarioDAO.listarActivos()) {
            boolean esElMismo = usuario.getId() != null && usuario.getId().equals(otro.getId());
            if (!esElMismo && otro.isPermisoUsuarios()) {
                return true;
            }
        }
        return false;
    }

    /** Lista corta de los permisos marcados, para el registro de actividad. */
    public static String resumenPermisos(Usuario usuario) {
        StringBuilder texto = new StringBuilder();
        for (Permiso permiso : Permiso.values()) {
            if (usuario.tiene(permiso)) {
                if (texto.length() > 0) {
                    texto.append(", ");
                }
                texto.append(permiso.name().toLowerCase().replace('_', ' '));
            }
        }
        return texto.length() == 0 ? "ninguno" : texto.toString();
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
