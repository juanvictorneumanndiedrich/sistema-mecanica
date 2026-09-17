package com.mecanica.view;

import com.mecanica.controller.AuditoriaController;
import com.mecanica.controller.UsuarioController;
import com.mecanica.model.Usuario;
import com.mecanica.util.Sesion;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de la aplicacion desktop. Muestra el login y, si la
 * autenticacion sale bien, abre la ventana principal.
 */
public class Main {

    public static void main(String[] args) {
        aplicarLookAndFeel();
        SwingUtilities.invokeLater(Main::iniciarSesion);
    }

    /**
     * Abre el login (es un dialogo modal, o sea que bloquea hasta que se
     * cierre) y sigue segun el resultado: si hay usuario autenticado abre
     * la MainView, y si no, termina la aplicacion. Tambien se usa al
     * cerrar sesion desde la MainView, para volver al login.
     */
    static void iniciarSesion() {
        ControlInactividad.detener();
        Sesion.cerrar();

        LoginView login = new LoginView();
        login.setVisible(true);

        Usuario usuario = login.getUsuarioAutenticado();
        if (usuario == null) {
            System.exit(0);
        }
        Sesion.iniciar(usuario);

        UsuarioController usuarioController = new UsuarioController();
        if (usuarioController.usaClaveProvisoria(usuario) && !exigirCambioDeClave(usuarioController, usuario)) {
            // no cambio la clave provisoria: no entra, vuelve al login
            SwingUtilities.invokeLater(Main::iniciarSesion);
            return;
        }

        new AuditoriaController().registrar("INICIO DE SESION", null);
        MainView ventana = new MainView(usuario);
        ventana.setVisible(true);
        ControlInactividad.iniciar(ventana::cerrarPorInactividad);
    }

    /**
     * El usuario entro con la clave provisoria "admin": no se abre el
     * sistema hasta que la cambie. Devuelve true si la cambio.
     */
    private static boolean exigirCambioDeClave(UsuarioController usuarioController, Usuario usuario) {
        JOptionPane.showMessageDialog(null,
                "Su usuario todavia usa la contrasena provisoria \"admin\".\n"
                        + "Por seguridad, debe elegir una contrasena nueva antes de continuar.",
                "Cambiar contrasena", JOptionPane.WARNING_MESSAGE);
        while (true) {
            CambiarClaveDialog dialogo = new CambiarClaveDialog(null, usuario.getNombre());
            dialogo.setVisible(true);
            if (!dialogo.isConfirmado()) {
                Sesion.cerrar();
                return false;
            }
            try {
                usuarioController.cambiarClave(usuario, dialogo.getNuevaClave());
                JOptionPane.showMessageDialog(null, "Contrasena actualizada.", "Listo",
                        JOptionPane.INFORMATION_MESSAGE);
                return true;
            } catch (RuntimeException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "No fue posible cambiar la contrasena.\n\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // si falla, sigue con el look and feel por defecto de Swing
        }
    }
}
