package com.mecanica.view;

import com.mecanica.controller.CompraController;
import com.mecanica.model.Compra;
import com.mecanica.model.Proveedor;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogo modal para abrir una Compra nueva ("notinha") a un Proveedor --
 * mismo patron de OrdenServicioFormDialog: solo pide la fecha, siempre
 * vinculada al Proveedor que esta seleccionado en ComprasProveedoresPanel.
 * El numero de la nota NO se escribe a mano: lo genera el Controller,
 * secuencial y sin repetir (ver CompraController.abrir).
 *
 * Al confirmar ya persiste la Compra (ABIERTA), porque los items que se
 * agregan despues (ver ItemCompraDialog) necesitan una Compra ya guardada
 * para vincularse.
 */
public class CompraFormDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CompraController compraController = new CompraController();

    private final JTextField campoFecha = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private final Proveedor proveedor;
    private Compra compraCreada;
    private boolean confirmado;

    public CompraFormDialog(Window propietario, Proveedor proveedor) {
        super(propietario, "Nueva Compra", ModalityType.APPLICATION_MODAL);
        this.proveedor = proveedor;
        armarPantalla();
    }

    /** true si el usuario confirmo con ABRIR COMPRA (y la compra ya quedo persistida). */
    public boolean isConfirmado() {
        return confirmado;
    }

    /** Compra recien creada, ya ABIERTA y con numero. Valida solo si isConfirmado(). */
    public Compra getCompraCreada() {
        return compraCreada;
    }

    private void armarPantalla() {
        setSize(420, 300);
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

        JLabel labelProveedor = new JLabel("Proveedor: " + proveedor.getNombre());
        labelProveedor.setForeground(Paleta.AZUL_OSCURO);
        labelProveedor.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        formulario.add(labelProveedor, gbc);

        JLabel labelAviso = new JLabel("El N° de la nota se genera automaticamente.");
        labelAviso.setForeground(Paleta.GRIS_TEXTO);
        labelAviso.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 1;
        gbc.insets = new Insets(6, 0, 0, 0);
        formulario.add(labelAviso, gbc);

        campoFecha.setText(LocalDate.now().format(FORMATO_FECHA));
        int fila = agregarCampo(formulario, gbc, 2, "FECHA (dd/mm/aaaa) *", campoFecha);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = fila++;
        gbc.insets = new Insets(10, 0, 0, 0);
        formulario.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
        botonCancelar.addActionListener(e -> dispose());
        BotonPlano botonGuardar = new BotonPlano("ABRIR COMPRA");
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

    private int agregarEtiqueta(JPanel formulario, GridBagConstraints gbc, int fila, String etiqueta) {
        JLabel label = new JLabel(etiqueta);
        label.setForeground(Paleta.GRIS_TEXTO);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = fila;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(label, gbc);
        return fila + 1;
    }

    /** Agrega una etiqueta + campo de texto y devuelve la fila siguiente libre. */
    private int agregarCampo(JPanel formulario, GridBagConstraints gbc, int fila, String etiqueta, JTextField campo) {
        fila = agregarEtiqueta(formulario, gbc, fila, etiqueta);

        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(0, 34));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        gbc.gridy = fila;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campo, gbc);

        return fila + 1;
    }

    private void onGuardar() {
        LocalDate fechaIngresada;
        try {
            fechaIngresada = LocalDate.parse(campoFecha.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            labelError.setText("Ingrese una fecha valida (dd/mm/aaaa).");
            return;
        }
        labelError.setText(" ");

        setHabilitado(false);
        new SwingWorker<Compra, Void>() {
            RuntimeException error;

            @Override
            protected Compra doInBackground() {
                try {
                    return compraController.abrir(proveedor, fechaIngresada);
                } catch (RuntimeException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    labelError.setText(error.getMessage());
                    return;
                }
                try {
                    compraCreada = get();
                } catch (Exception e) {
                    labelError.setText("No fue posible abrir la compra.");
                    return;
                }
                confirmado = true;
                dispose();
            }
        }.execute();
    }

    private void setHabilitado(boolean habilitado) {
        setCursor(habilitado ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
}
