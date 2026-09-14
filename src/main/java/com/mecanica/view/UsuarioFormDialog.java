package com.mecanica.view;

import com.mecanica.model.Usuario;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogo modal para crear o editar un Usuario del sistema. Si se abre con
 * un usuario existente el formulario aparece precargado y guarda sobre el
 * mismo registro (nombre, login, activo y permisos); si se abre sin
 * usuario (null), crea uno nuevo y en ese caso pide ademas la contrasena
 * inicial, ya que un usuario nuevo necesita una para poder ingresar.
 *
 * Los permisos son 6 casillas independientes, una por cada area de la
 * navegacion principal -- las mismas 6 que MainView lee de Usuario para
 * armar el menu lateral. No hay roles ni grupos: cada usuario tiene su
 * propia combinacion de permisos.
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

    private final JCheckBox checkClientesMaquinarios = new JCheckBox("Clientes y Maquinarios");
    private final JCheckBox checkOrdenesServicio = new JCheckBox("Ordenes de Servicio");
    private final JCheckBox checkComprasProveedores = new JCheckBox("Compras y Proveedores");
    private final JCheckBox checkFinanciero = new JCheckBox("Financiero");
    private final JCheckBox checkEmpleadosSocios = new JCheckBox("Empleados y Socios");
    private final JCheckBox checkUsuarios = new JCheckBox("Usuarios y Permisos");

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
        setSize(460, esNuevo ? 660 : 600);
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

        int fila = agregarCampo(formulario, gbc, 0, "NOMBRE *", campoNombre);
        fila = agregarCampo(formulario, gbc, fila, "LOGIN *", campoLogin);

        if (esNuevo) {
            fila = agregarCampo(formulario, gbc, fila, "CONTRASENA INICIAL *", campoClaveInicial);
        }

        estilizarCheck(checkActivo);
        gbc.gridy = fila++;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(checkActivo, gbc);

        JLabel labelPermisos = new JLabel("PERMISOS POR AREA");
        labelPermisos.setForeground(Paleta.GRIS_TEXTO);
        labelPermisos.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = fila++;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(labelPermisos, gbc);

        JCheckBox[] checks = {
                checkClientesMaquinarios, checkOrdenesServicio, checkComprasProveedores,
                checkFinanciero, checkEmpleadosSocios, checkUsuarios
        };
        for (int i = 0; i < checks.length; i++) {
            estilizarCheck(checks[i]);
            gbc.gridy = fila++;
            gbc.insets = new Insets(i == 0 ? 4 : 2, 0, 0, 0);
            formulario.add(checks[i], gbc);
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
            return;
        }
        campoNombre.setText(usuario.getNombre());
        campoLogin.setText(usuario.getLogin());
        checkActivo.setSelected(usuario.isActivo());
        checkClientesMaquinarios.setSelected(usuario.isPermisoClientesMaquinarios());
        checkOrdenesServicio.setSelected(usuario.isPermisoOrdenesServicio());
        checkComprasProveedores.setSelected(usuario.isPermisoComprasProveedores());
        checkFinanciero.setSelected(usuario.isPermisoFinanciero());
        checkEmpleadosSocios.setSelected(usuario.isPermisoEmpleadosSocios());
        checkUsuarios.setSelected(usuario.isPermisoUsuarios());
    }

    private void onGuardar() {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            labelError.setText("El nombre es obligatorio.");
            return;
        }
        String login = campoLogin.getText().trim();
        if (login.isEmpty()) {
            labelError.setText("El login es obligatorio.");
            return;
        }

        String claveIngresada = null;
        if (esNuevo) {
            claveIngresada = new String(campoClaveInicial.getPassword());
            if (claveIngresada.isBlank()) {
                labelError.setText("La contrasena inicial es obligatoria.");
                return;
            }
        }

        if (usuario == null) {
            usuario = new Usuario();
        }
        usuario.setNombre(nombre);
        usuario.setLogin(login);
        usuario.setActivo(checkActivo.isSelected());
        usuario.setPermisoClientesMaquinarios(checkClientesMaquinarios.isSelected());
        usuario.setPermisoOrdenesServicio(checkOrdenesServicio.isSelected());
        usuario.setPermisoComprasProveedores(checkComprasProveedores.isSelected());
        usuario.setPermisoFinanciero(checkFinanciero.isSelected());
        usuario.setPermisoEmpleadosSocios(checkEmpleadosSocios.isSelected());
        usuario.setPermisoUsuarios(checkUsuarios.isSelected());

        claveInicial = claveIngresada;
        confirmado = true;
        dispose();
    }
}
