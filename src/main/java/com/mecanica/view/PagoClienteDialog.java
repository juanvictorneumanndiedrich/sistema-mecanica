package com.mecanica.view;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogo modal para registrar un pago de un Cliente. Por defecto el valor
 * descuenta directamente el SALDO GENERAL del cliente y ya genera el
 * MovimientoFinanciero (ver ClienteController.registrarPagamento).
 *
 * Si se marca "CHEQUE PRE-DATADO", el saldo tambien se descuenta en el
 * acto, pero el MovimientoFinanciero (la entrada real en Financiero) queda
 * pendiente hasta que el cheque venza y sea confirmado en la pestaña
 * "Cheques Pendientes" de la pantalla Financiero -- ver
 * ChequePreDatadoController.registrarDeCliente/confirmar.
 */
public class PagoClienteDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField campoValor = new JTextField();
    private final JTextField campoDescripcion = new JTextField();
    private final JCheckBox checkChequePreDatado = new JCheckBox("ES UN CHEQUE PRE-DATADO");
    private final JTextField campoNumeroCheque = new JTextField();
    private final JTextField campoBanco = new JTextField();
    private final JTextField campoVencimiento = new JTextField();
    private final JPanel panelCheque = new JPanel(new GridBagLayout());
    private final JLabel labelError = new JLabel(" ");

    private BigDecimal valor;
    private String descripcion;
    private boolean chequePreDatado;
    private String numeroCheque;
    private String banco;
    private LocalDate fechaVencimiento;
    private boolean confirmado;

    public PagoClienteDialog(Window propietario, String nombreCliente) {
        super(propietario, "Registrar Pago - " + nombreCliente, ModalityType.APPLICATION_MODAL);
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

    public boolean isChequePreDatado() {
        return chequePreDatado;
    }

    public String getNumeroCheque() {
        return numeroCheque;
    }

    public String getBanco() {
        return banco;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    private void armarPantalla() {
        setSize(380, 460);
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

        JLabel labelValor = new JLabel("VALOR (Gs.) *");
        labelValor.setForeground(Paleta.GRIS_TEXTO);
        labelValor.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        formulario.add(labelValor, gbc);

        estilizarCampo(campoValor);
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoValor, gbc);

        JLabel labelDescripcion = new JLabel("DESCRIPCION");
        labelDescripcion.setForeground(Paleta.GRIS_TEXTO);
        labelDescripcion.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 2;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(labelDescripcion, gbc);

        estilizarCampo(campoDescripcion);
        gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoDescripcion, gbc);

        checkChequePreDatado.setOpaque(false);
        checkChequePreDatado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        checkChequePreDatado.setForeground(Paleta.GRIS_TEXTO);
        checkChequePreDatado.addActionListener(e -> alternarPanelCheque());
        gbc.gridy = 4;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(checkChequePreDatado, gbc);

        armarPanelCheque();
        panelCheque.setVisible(false);
        gbc.gridy = 5;
        gbc.insets = new Insets(6, 0, 0, 0);
        formulario.add(panelCheque, gbc);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 6;
        gbc.insets = new Insets(10, 0, 0, 0);
        formulario.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
        botonCancelar.addActionListener(e -> dispose());
        BotonPlano botonGuardar = new BotonPlano("REGISTRAR");
        botonGuardar.addActionListener(e -> onRegistrar());
        botones.add(botonCancelar);
        botones.add(botonGuardar);
        gbc.gridy = 7;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(botones, gbc);

        getRootPane().setDefaultButton(botonGuardar);
        add(formulario, BorderLayout.CENTER);
        setLocationRelativeTo(getOwner());
    }

    private void armarPanelCheque() {
        panelCheque.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel labelNumero = new JLabel("NÚMERO DE CHEQUE");
        labelNumero.setForeground(Paleta.GRIS_TEXTO);
        labelNumero.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        panelCheque.add(labelNumero, gbc);

        estilizarCampo(campoNumeroCheque);
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 0, 0);
        panelCheque.add(campoNumeroCheque, gbc);

        JLabel labelBanco = new JLabel("BANCO");
        labelBanco.setForeground(Paleta.GRIS_TEXTO);
        labelBanco.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        panelCheque.add(labelBanco, gbc);

        estilizarCampo(campoBanco);
        gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 0, 0);
        panelCheque.add(campoBanco, gbc);

        JLabel labelVencimiento = new JLabel("FECHA DE VENCIMIENTO (dd/mm/aaaa) *");
        labelVencimiento.setForeground(Paleta.GRIS_TEXTO);
        labelVencimiento.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 0, 0, 0);
        panelCheque.add(labelVencimiento, gbc);

        estilizarCampo(campoVencimiento);
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 0, 0, 0);
        panelCheque.add(campoVencimiento, gbc);
    }

    private void alternarPanelCheque() {
        panelCheque.setVisible(checkChequePreDatado.isSelected());
        revalidate();
        repaint();
    }

    private void estilizarCampo(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(0, 34));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
    }

    /** Acepta tanto "150000" como "150.000" (separador de miles paraguayo). */
    private void onRegistrar() {
        String textoValor = campoValor.getText().trim().replace(".", "").replace(",", ".");
        try {
            BigDecimal valorIngresado = new BigDecimal(textoValor);
            if (valorIngresado.compareTo(BigDecimal.ZERO) <= 0) {
                labelError.setText("El valor debe ser mayor que cero.");
                return;
            }
            valor = valorIngresado;
        } catch (NumberFormatException e) {
            labelError.setText("Ingrese un valor numerico valido.");
            return;
        }

        if (checkChequePreDatado.isSelected()) {
            String textoVencimiento = campoVencimiento.getText().trim();
            LocalDate vencimiento;
            try {
                vencimiento = LocalDate.parse(textoVencimiento, FORMATO_FECHA);
            } catch (DateTimeParseException e) {
                labelError.setText("Ingrese la fecha de vencimiento en formato dd/mm/aaaa.");
                return;
            }
            if (vencimiento.isBefore(LocalDate.now())) {
                labelError.setText("La fecha de vencimiento no puede ser anterior a hoy.");
                return;
            }
            chequePreDatado = true;
            fechaVencimiento = vencimiento;
            numeroCheque = campoNumeroCheque.getText().trim();
            banco = campoBanco.getText().trim();
        } else {
            chequePreDatado = false;
        }

        descripcion = campoDescripcion.getText().trim();
        confirmado = true;
        dispose();
    }
}
