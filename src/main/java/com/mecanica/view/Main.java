package com.mecanica.view;

import com.mecanica.model.Usuario;

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
        LoginView login = new LoginView();
        login.setVisible(true);

        Usuario usuario = login.getUsuarioAutenticado();
        if (usuario == null) {
            System.exit(0);
        }
        new MainView(usuario).setVisible(true);
    }

    private static void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // si falla, sigue con el look and feel por defecto de Swing
        }
    }
}
