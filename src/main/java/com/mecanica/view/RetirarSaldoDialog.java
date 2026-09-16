package com.mecanica.view;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Dialogo modal generico para retirar en efectivo un credito a favor ya
 * existente -- se usa tanto para el Cliente (ClienteController.retirarSaldo)
 * como para el Proveedor (ProveedorController.retirarSaldo). Muestra el
 * credito disponible y no deja confirmar un valor mayor que ese credito
 * (la validacion final, contra el saldo real en el banco, la hace igual el
 * Controller).
 */
public class RetirarSaldoDialog extends JDialog {

    private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");

    private final JLabel labelCredito = new JLabel();
    private final JTextField campoValor = new JTextField();
    private final JTextField campoDescripcion = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private final BigDecimal creditoDisponible;

    private BigDecimal valor;
    private String descripcion;
    private boolean confirmado;

    public RetirarSaldoDialog(Window propietario, String nombreEntidad, BigDecimal creditoDisponible) {
        super(propietario, "Retirar Saldo - " + nombreEntidad, ModalityType.APPLICATION_MODAL);
        this.creditoDisponible = creditoDisponible == null ? BigDecimal.ZERO : creditoDisponible;
        armarPantalla();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getDescripcion() {
        return descripcion;
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

        labelCredito.setText("CREDITO DISPONIBLE: Gs. " + FORMATO_VALOR.format(creditoDisponible));
        labelCredito.setForeground(Paleta.VERDE_EXITO);
        labelCredito.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        formulario.add(labelCredito, gbc);

        JLabel labelValor = new JLabel("VALOR A RETIRAR (Gs.) *");
        labelValor.setForeground(Paleta.GRIS_TEXTO);
        labelValor.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 1;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(labelValor, gbc);

        estilizarCampo(campoValor);
        gbc.gridy = 2;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoValor, gbc);

        JLabel labelDescripcion = new JLabel("DESCRIPCION");
        labelDescripcion.setForeground(Paleta.GRIS_TEXTO);
        labelDescripcion.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 3;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(labelDescripcion, gbc);

        estilizarCampo(campoDescripcion);
        gbc.gridy = 4;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoDescripcion, gbc);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 5;
        gbc.insets = new Insets(10, 0, 0, 0);
        formulario.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
        botonCancelar.addActionListener(e -> dispose());
        BotonPlano botonGuardar = new BotonPlano("RETIRAR");
        botonGuardar.addActionListener(e -> onRetirar());
        botones.add(botonCancelar);
        botones.add(botonGuardar);
        gbc.gridy = 6;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(botones, gbc);

        getRootPane().setDefaultButton(botonGuardar);
        add(formulario, BorderLayout.CENTER);
        setLocationRelativeTo(getOwner());
    }

    private void estilizarCampo(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(0, 34));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
    }

    /** Acepta tanto "150000" como "150.000" (separador de miles paraguayo). */
    private void onRetirar() {
        String textoValor = campoValor.getText().trim().replace(".", "").replace(",", ".");
        BigDecimal valorIngresado;
        try {
            valorIngresado = new BigDecimal(textoValor);
        } catch (NumberFormatException e) {
            labelError.setText("Ingrese un valor numerico valido.");
            return;
        }
        if (valorIngresado.compareTo(BigDecimal.ZERO) <= 0) {
            labelError.setText("El valor debe ser mayor que cero.");
            return;
        }
        if (valorIngresado.compareTo(creditoDisponible) > 0) {
            labelError.setText("El valor no puede ser mayor que el credito disponible.");
            return;
        }

        valor = valorIngresado;
        descripcion = campoDescripcion.getText().trim();
        confirmado = true;
        dispose();
    }
}
