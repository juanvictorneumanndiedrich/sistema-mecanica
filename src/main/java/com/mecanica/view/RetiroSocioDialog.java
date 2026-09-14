package com.mecanica.view;

import com.mecanica.model.Socio;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogo modal para registrar un Retiro de un Socio fijo (el socio no
 * se elige aca, solo se muestra como referencia -- viene ya seleccionado
 * desde EmpleadosSociosPanel). IMPORTANTE: a diferencia del retiro de
 * empleado, este retiro NO genera MovimientoFinanciero -- solo queda
 * registrado para ser descontado de la parte del socio en la liquidacion
 * (ver SocioController.calcularLiquidacion y RetiroSocioController).
 */
public class RetiroSocioDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField campoValor = new JTextField();
    private final JTextField campoFecha = new JTextField();
    private final JTextField campoObservacion = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private final Socio socio;
    private BigDecimal valor;
    private LocalDate fecha;
    private String observacion;
    private boolean confirmado;

    public RetiroSocioDialog(Window propietario, Socio socio) {
        super(propietario, "Registrar Retiro - " + socio.getNombre(), ModalityType.APPLICATION_MODAL);
        this.socio = socio;
        armarPantalla();
    }

    /** true si el usuario confirmo con REGISTRAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getObservacion() {
        return observacion;
    }

    private void armarPantalla() {
        setSize(420, 420);
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

        JLabel labelSocio = new JLabel("Socio: " + socio.getNombre());
        labelSocio.setForeground(Paleta.AZUL_OSCURO);
        labelSocio.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        formulario.add(labelSocio, gbc);

        int fila = agregarCampo(formulario, gbc, 1, "VALOR (Gs.) *", campoValor);
        fila = agregarCampo(formulario, gbc, fila, "FECHA (DD/MM/AAAA)", campoFecha);
        campoFecha.setText(LocalDate.now().format(FORMATO_FECHA));
        fila = agregarCampo(formulario, gbc, fila, "OBSERVACION", campoObservacion);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = fila++;
        gbc.insets = new Insets(10, 0, 0, 0);
        formulario.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
        botonCancelar.addActionListener(e -> dispose());
        BotonPlano botonRegistrar = new BotonPlano("REGISTRAR");
        botonRegistrar.addActionListener(e -> onRegistrar());
        botones.add(botonCancelar);
        botones.add(botonRegistrar);
        gbc.gridy = fila;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(botones, gbc);

        getRootPane().setDefaultButton(botonRegistrar);
        add(formulario, BorderLayout.CENTER);
        setLocationRelativeTo(getOwner());
    }

    /** Agrega una etiqueta + campo de texto en dos filas del grid y devuelve la fila siguiente libre. */
    private int agregarCampo(JPanel formulario, GridBagConstraints gbc, int fila, String etiqueta, JTextField campo) {
        JLabel label = new JLabel(etiqueta);
        label.setForeground(Paleta.GRIS_TEXTO);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = fila;
        gbc.insets = new Insets(14, 0, 0, 0);
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

    /** Acepta tanto "150000" como "150.000" (separador de miles paraguayo). */
    private void onRegistrar() {
        String textoValor = campoValor.getText().trim().replace(".", "").replace(",", ".");
        BigDecimal valorIngresado;
        try {
            valorIngresado = new BigDecimal(textoValor);
            if (valorIngresado.compareTo(BigDecimal.ZERO) <= 0) {
                labelError.setText("El valor debe ser mayor que cero.");
                return;
            }
        } catch (NumberFormatException e) {
            labelError.setText("Ingrese un valor numerico valido.");
            return;
        }

        String textoFecha = campoFecha.getText().trim();
        LocalDate fechaIngresada;
        try {
            fechaIngresada = textoFecha.isEmpty() ? LocalDate.now() : LocalDate.parse(textoFecha, FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            labelError.setText("La fecha debe tener el formato DD/MM/AAAA.");
            return;
        }

        valor = valorIngresado;
        fecha = fechaIngresada;
        observacion = campoObservacion.getText().trim();
        confirmado = true;
        dispose();
    }
}
