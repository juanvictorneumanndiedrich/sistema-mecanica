package com.mecanica.view;

import com.mecanica.util.Validaciones;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Dialogo modal para cambiar la contrasena de un Usuario ya existente.
 * Es una accion separada y deliberada del formulario de edicion normal
 * (UsuarioFormDialog no toca la clave): se abre solo cuando en la
 * pantalla de Usuarios y Permisos se aprieta "CAMBIAR CONTRASENA" con
 * una fila seleccionada. El boton CAMBIAR queda deshabilitado hasta que
 * las dos contrasenas ingresadas coincidan y no esten vacias.
 *
 * Este dialogo solo junta los datos -- quien llama a
 * UsuarioController.cambiarClave(...) es UsuariosPanel, en un
 * SwingWorker, igual que el resto de las operaciones contra la base.
 */
public class CambiarClaveDialog extends JDialog {

    private final JPasswordField campoNuevaClave = new JPasswordField();
    private final JPasswordField campoConfirmarClave = new JPasswordField();
    private final JLabel labelError = new JLabel(" ");
    private final BotonPlano botonGuardar = new BotonPlano("CAMBIAR");

    private String nuevaClave;
    private boolean confirmado;

    public CambiarClaveDialog(Window propietario, String nombreUsuario) {
        super(propietario, "Cambiar Contrasena - " + nombreUsuario, ModalityType.APPLICATION_MODAL);
        armarPantalla();
    }

    /** true si el usuario confirmo con CAMBIAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    /** Nueva contrasena en texto plano, lista para UsuarioController.cambiarClave. Valida solo si isConfirmado(). */
    public String getNuevaClave() {
        return nuevaClave;
    }

    private void armarPantalla() {
        setSize(380, 300);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Paleta.GRIS_FONDO);
        formulario.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int fila = agregarCampo(formulario, gbc, 0, "NUEVA CONTRASENA *", campoNuevaClave);
        fila = agregarCampo(formulario, gbc, fila, "CONFIRMAR CONTRASENA *", campoConfirmarClave);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = fila++;
        gbc.insets = new Insets(10, 0, 0, 0);
        formulario.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
        botonCancelar.addActionListener(e -> dispose());
        botonGuardar.setEnabled(false);
        botonGuardar.addActionListener(e -> onGuardar());
        botones.add(botonCancelar);
        botones.add(botonGuardar);
        gbc.gridy = fila;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(botones, gbc);

        DocumentListener validador = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validar();
            }
        };
        campoNuevaClave.getDocument().addDocumentListener(validador);
        campoConfirmarClave.getDocument().addDocumentListener(validador);

        getRootPane().setDefaultButton(botonGuardar);
        add(formulario, BorderLayout.CENTER);
        setLocationRelativeTo(getOwner());
    }

    /** Agrega una etiqueta + campo de clave en dos filas del grid y devuelve la fila siguiente libre. */
    private int agregarCampo(JPanel formulario, GridBagConstraints gbc, int fila, String etiqueta, JPasswordField campo) {
        JLabel label = new JLabel(etiqueta);
        label.setForeground(Paleta.GRIS_TEXTO);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = fila;
        gbc.insets = new Insets(fila == 0 ? 0 : 14, 0, 0, 0);
        formulario.add(label, gbc);

        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(0, 34));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        gbc.gridy = fila + 1;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campo, gbc);

        return fila + 2;
    }

    /** Habilita CAMBIAR solo cuando las dos contrasenas coinciden y no estan vacias. */
    private void validar() {
        String nueva = new String(campoNuevaClave.getPassword());
        String confirmar = new String(campoConfirmarClave.getPassword());

        boolean longitudOk = Validaciones.longitudMinima(nueva, 6);
        boolean coinciden = !nueva.isEmpty() && nueva.equals(confirmar);
        botonGuardar.setEnabled(coinciden && longitudOk);

        if (nueva.isEmpty() && confirmar.isEmpty()) {
            labelError.setText(" ");
        } else if (!longitudOk) {
            labelError.setText("La contrasena debe tener al menos 6 caracteres.");
        } else if (!coinciden) {
            labelError.setText("Las contrasenas no coinciden.");
        } else {
            labelError.setText(" ");
        }
    }

    private void onGuardar() {
        nuevaClave = new String(campoNuevaClave.getPassword());
        confirmado = true;
        dispose();
    }
}
