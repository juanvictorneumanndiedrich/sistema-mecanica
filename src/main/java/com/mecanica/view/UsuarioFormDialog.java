package com.mecanica.view;

import com.mecanica.enums.Permiso;
import com.mecanica.model.Usuario;
import com.mecanica.util.Validaciones;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dialogo modal para crear o editar un Usuario del sistema. Si se abre con
 * un usuario existente el formulario aparece precargado y guarda sobre el
 * mismo registro (nombre, login, activo y permisos); si se abre sin
 * usuario (null), crea uno nuevo y en ese caso pide ademas la contrasena
 * inicial, ya que un usuario nuevo necesita una para poder ingresar.
 *
 * Los permisos salen del enum Permiso: primero las 6 areas de la navegacion
 * principal (las mismas que MainView lee para armar el menu lateral) y
 * despues los permisos de accion, agrupados. Una casilla de accion que
 * depende de un area (ej. "Ver la pestana Socios") queda deshabilitada
 * mientras el area este desmarcada. No hay roles ni grupos: cada usuario
 * tiene su propia combinacion de permisos.
 *
 * La contrasena NUNCA se toca desde aca al editar un usuario existente
 * (el campo ni siquiera se muestra): para cambiarla existe una accion
 * aparte y deliberada, ver CambiarClaveDialog.
 */
public class UsuarioFormDialog extends JDialog {

    private final JTextField campoNombre = new JTextField();
    private final JTextField campoLogin = new JTextField();
    private final JPasswordField campoClaveInicial = new JPasswordField();
    private final JCheckBox checkActivo = new JCheckBox("Activo", true);

    /** Una casilla por permiso, en el orden del enum. */
    private final Map<Permiso, JCheckBox> checksPermisos = new EnumMap<>(Permiso.class);

    private final JLabel labelError = new JLabel(" ");

    private final boolean esNuevo;
    private Usuario usuario;
    private String claveInicial;
    private boolean confirmado;

    public UsuarioFormDialog(Window propietario, Usuario usuarioExistente) {
        super(propietario, usuarioExistente == null ? "Nuevo Usuario" : "Editar Usuario",
                ModalityType.APPLICATION_MODAL);
        this.usuario = usuarioExistente;
        this.esNuevo = usuarioExistente == null;
        armarPantalla();
        cargarDatos();
    }

    /** true si el usuario confirmo con GUARDAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    /** Usuario nuevo o editado, listo para pasar al Controller. Valido solo si isConfirmado(). */
    public Usuario getUsuario() {
        return usuario;
    }

    /** Contrasena en texto plano elegida al crear. Null cuando se edita un usuario existente. */
    public String getClaveInicial() {
        return claveInicial;
    }

    private void armarPantalla() {
        setSize(520, 760);
        setResizable(true);
        setLayout(new BorderLayout());

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Paleta.GRIS_FONDO);
        formulario.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int fila = agregarCampo(formulario, gbc, 0, "NOMBRE *", campoNombre);
        fila = agregarCampo(formulario, gbc, fila, "LOGIN *", campoLogin);

        if (esNuevo) {
            fila = agregarCampo(formulario, gbc, fila, "CONTRASENA INICIAL *", campoClaveInicial);
        }

        estilizarCheck(checkActivo);
        gbc.gridy = fila++;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(checkActivo, gbc);

        Permiso.Grupo grupoActual = null;
        for (Permiso permiso : Permiso.values()) {
            if (permiso.getGrupo() != grupoActual) {
                grupoActual = permiso.getGrupo();
                JLabel labelGrupo = new JLabel("PERMISOS - " + grupoActual.getTitulo());
                labelGrupo.setForeground(Paleta.GRIS_TEXTO);
                labelGrupo.setFont(new Font("Segoe UI", Font.BOLD, 11));
                gbc.gridy = fila++;
                gbc.insets = new Insets(16, 0, 2, 0);
                formulario.add(labelGrupo, gbc);
            }
            JCheckBox check = new JCheckBox(permiso.getEtiqueta());
            estilizarCheck(check);
            checksPermisos.put(permiso, check);
            gbc.gridy = fila++;
            gbc.insets = new Insets(2, permiso.esArea() ? 0 : 14, 0, 0);
            formulario.add(check, gbc);
        }
        // Las casillas de accion que dependen de un area se habilitan solo con el area marcada.
        for (Permiso permiso : Permiso.values()) {
            if (permiso.esArea()) {
                checksPermisos.get(permiso).addActionListener(e -> actualizarDependencias());
            }
        }

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = fila++;
        gbc.insets = new Insets(12, 0, 0, 0);
        formulario.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
        botonCancelar.addActionListener(e -> dispose());
        BotonPlano botonGuardar = new BotonPlano("GUARDAR");
        botonGuardar.addActionListener(e -> onGuardar());
        botones.add(botonCancelar);
        botones.add(botonGuardar);
        gbc.gridy = fila;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(botones, gbc);

        getRootPane().setDefaultButton(botonGuardar);
        add(new JScrollPane(formulario), BorderLayout.CENTER);
        setLocationRelativeTo(getOwner());
    }

    /** Agrega una etiqueta + campo de texto en dos filas del grid y devuelve la fila siguiente libre. */
    private int agregarCampo(JPanel formulario, GridBagConstraints gbc, int fila, String etiqueta, JTextField campo) {
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

    private void estilizarCheck(JCheckBox check) {
        check.setOpaque(false);
        check.setForeground(Paleta.GRIS_TEXTO);
        check.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void cargarDatos() {
        if (usuario == null) {
            actualizarDependencias();
            return;
        }
        campoNombre.setText(usuario.getNombre());
        campoLogin.setText(usuario.getLogin());
        checkActivo.setSelected(usuario.isActivo());
        for (Permiso permiso : Permiso.values()) {
            checksPermisos.get(permiso).setSelected(usuario.tiene(permiso));
        }
        actualizarDependencias();
    }

    private void actualizarDependencias() {
        for (Permiso permiso : Permiso.values()) {
            Permiso area = permiso.area();
            if (area != null) {
                checksPermisos.get(permiso).setEnabled(checksPermisos.get(area).isSelected());
            }
        }
    }

    private void onGuardar() {
        labelError.setText(" ");

        String nombre = campoNombre.getText().trim();
        if (Validaciones.esVacio(nombre)) {
            labelError.setText("Debe ingresar el nombre del usuario.");
            return;
        }
        if (!Validaciones.soloLetras(nombre)) {
            labelError.setText("El nombre solo puede contener letras.");
            return;
        }

        String login = campoLogin.getText().trim();
        if (Validaciones.esVacio(login)) {
            labelError.setText("Debe ingresar el login del usuario.");
            return;
        }
        if (!Validaciones.soloLetrasYNumeros(login)) {
            labelError.setText("El login solo puede contener letras y numeros, sin espacios ni simbolos.");
            return;
        }

        String claveIngresada = null;
        if (esNuevo) {
            claveIngresada = new String(campoClaveInicial.getPassword());
            if (Validaciones.esVacio(claveIngresada)) {
                labelError.setText("La contrasena inicial es obligatoria.");
                return;
            }
            if (!Validaciones.longitudMinima(claveIngresada, 6)) {
                labelError.setText("La contrasena debe tener al menos 6 caracteres.");
                return;
            }
        }

        if (usuario == null) {
            usuario = new Usuario();
        }
        usuario.setNombre(nombre);
        usuario.setLogin(login);
        usuario.setActivo(checkActivo.isSelected());
        for (Permiso permiso : Permiso.values()) {
            usuario.setPermiso(permiso, checksPermisos.get(permiso).isSelected());
        }

        claveInicial = claveIngresada;
        confirmado = true;
        dispose();
    }
}
