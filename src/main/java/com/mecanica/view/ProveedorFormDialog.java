package com.mecanica.view;

import com.mecanica.model.Proveedor;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogo modal para crear o editar un Proveedor. Si se abre con un
 * proveedor existente el formulario aparece precargado y guarda sobre el
 * mismo registro; si se abre sin proveedor (null), crea uno nuevo.
 * Proveedor es una entidad simple, solo con datos de referencia (no tiene
 * saldo propio como el Cliente -- las deudas con el se manejan via
 * CierreProveedor).
 */
public class ProveedorFormDialog extends JDialog {

    private final JTextField campoNombre = new JTextField();
    private final JTextField campoDocumento = new JTextField();
    private final JTextField campoTelefono = new JTextField();
    private final JTextField campoContacto = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private Proveedor proveedor;
    private boolean confirmado;

    public ProveedorFormDialog(Window propietario, Proveedor proveedorExistente) {
        super(propietario, proveedorExistente == null ? "Nuevo Proveedor" : "Editar Proveedor",
                ModalityType.APPLICATION_MODAL);
        this.proveedor = proveedorExistente;
        armarPantalla();
        cargarDatos();
    }

    /** true si el usuario confirmo con GUARDAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    /** Proveedor nuevo o editado, listo para pasar al Controller. Valido solo si isConfirmado(). */
    public Proveedor getProveedor() {
        return proveedor;
    }

    private void armarPantalla() {
        setSize(420, 400);
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
        fila = agregarCampo(formulario, gbc, fila, "DOCUMENTO (RUC)", campoDocumento);
        fila = agregarCampo(formulario, gbc, fila, "TELEFONO", campoTelefono);
        fila = agregarCampo(formulario, gbc, fila, "CONTACTO", campoContacto);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = fila++;
        gbc.insets = new Insets(10, 0, 0, 0);
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
        add(formulario, BorderLayout.CENTER);
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

    private void cargarDatos() {
        if (proveedor == null) {
            return;
        }
        campoNombre.setText(proveedor.getNombre());
        campoDocumento.setText(proveedor.getDocumento());
        campoTelefono.setText(proveedor.getTelefono());
        campoContacto.setText(proveedor.getContacto());
    }

    private void onGuardar() {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            labelError.setText("El nombre es obligatorio.");
            return;
        }

        if (proveedor == null) {
            proveedor = new Proveedor();
        }
        proveedor.setNombre(nombre);
        proveedor.setDocumento(vacioComoNull(campoDocumento.getText()));
        proveedor.setTelefono(vacioComoNull(campoTelefono.getText()));
        proveedor.setContacto(vacioComoNull(campoContacto.getText()));

        confirmado = true;
        dispose();
    }

    private String vacioComoNull(String texto) {
        String valor = texto == null ? "" : texto.trim();
        return valor.isEmpty() ? null : valor;
    }
}
