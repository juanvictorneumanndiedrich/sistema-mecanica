package com.mecanica.view;

import com.mecanica.model.Cliente;
import com.mecanica.util.Validaciones;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogo modal para crear o editar un Cliente. Si se abre con un cliente
 * existente el formulario aparece precargado y guarda sobre el mismo
 * registro; si se abre sin cliente (null), crea uno nuevo. El saldo no se
 * edita aca -- se mueve solo con "Registrar Pago" (ver ClienteController).
 */
public class ClienteFormDialog extends JDialog {

    private final JTextField campoNombre = new JTextField();
    private final JTextField campoDocumento = new JTextField();
    private final JTextField campoTelefono = new JTextField();
    private final JTextField campoDireccion = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private Cliente cliente;
    private boolean confirmado;

    public ClienteFormDialog(Window propietario, Cliente clienteExistente) {
        super(propietario, clienteExistente == null ? "Nuevo Cliente" : "Editar Cliente",
                ModalityType.APPLICATION_MODAL);
        this.cliente = clienteExistente;
        armarPantalla();
        cargarDatos();
    }

    /** true si el usuario confirmo con GUARDAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    /** Cliente nuevo o editado, listo para pasar al Controller. Valido solo si isConfirmado(). */
    public Cliente getCliente() {
        return cliente;
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
        fila = agregarCampo(formulario, gbc, fila, "DOCUMENTO (CI/RUC) *", campoDocumento);
        fila = agregarCampo(formulario, gbc, fila, "TELEFONO", campoTelefono);
        fila = agregarCampo(formulario, gbc, fila, "DIRECCION", campoDireccion);

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
        if (cliente == null) {
            return;
        }
        campoNombre.setText(cliente.getNombre());
        campoDocumento.setText(cliente.getDocumento());
        campoTelefono.setText(cliente.getTelefono());
        campoDireccion.setText(cliente.getDireccion());
    }

    private void onGuardar() {
        labelError.setText(" ");

        String nombre = campoNombre.getText().trim();
        if (Validaciones.esVacio(nombre)) {
            labelError.setText("Debe ingresar el nombre del cliente.");
            return;
        }
        if (!Validaciones.nombreORazonSocial(nombre)) {
            labelError.setText("El nombre no puede contener simbolos (se permite punto, guion y apostrofe).");
            return;
        }

        String documento = campoDocumento.getText().trim();
        if (Validaciones.esVacio(documento)) {
            labelError.setText("Debe ingresar el documento (CI/RUC) del cliente.");
            return;
        }
        if (!Validaciones.documentoValido(documento)) {
            labelError.setText("El documento debe contener solo numeros, con un guion opcional (ej: 80012345-6).");
            return;
        }

        String telefono = campoTelefono.getText().trim();
        if (!telefono.isEmpty() && !Validaciones.soloNumeros(telefono)) {
            labelError.setText("El telefono debe contener solo numeros.");
            return;
        }

        if (cliente == null) {
            cliente = new Cliente();
        }
        cliente.setNombre(nombre);
        cliente.setDocumento(documento);
        cliente.setTelefono(vacioComoNull(telefono));
        cliente.setDireccion(vacioComoNull(campoDireccion.getText()));

        confirmado = true;
        dispose();
    }

    private String vacioComoNull(String texto) {
        String valor = texto == null ? "" : texto.trim();
        return valor.isEmpty() ? null : valor;
    }
}
