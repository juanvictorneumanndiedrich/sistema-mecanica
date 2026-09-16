package com.mecanica.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogo modal para registrar un pago hecho a un Proveedor. El valor
 * descuenta directamente el SALDO GENERAL del proveedor y ya genera el
 * MovimientoFinanciero (ver ProveedorController.registrarPagamento).
 *
 * Si se marca "CHEQUE PRE-DATADO", el saldo tambien se descuenta en el
 * acto, pero el MovimientoFinanciero (el gasto real en Financiero) queda
 * pendiente hasta que el cheque venza y sea confirmado en la pestaña
 * "Cheques Pendientes" de la pantalla Financiero -- ver
 * ChequePreDatadoController.registrarDeProveedor/confirmar.
 *
 * DESCUENTO -- mismo esquema (y mismo orden de pantalla) de
 * PagoClienteDialog: el porcentaje/monto se calcula SOBRE EL SALDO que se
 * le debe al proveedor, la pantalla muestra en vivo cuanto se perdona y
 * cuanto queda para saldar, y recien ahi se tipea a mano cuanto se esta
 * pagando ahora. Lo tipeado sale ENTERO en Financiero; la cuenta del
 * proveedor baja por (valor pagado + descuento).
 */
public class PagoProveedorDialog extends JDialog {

    private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JLabel labelResumen = new JLabel(" ");
    private final JTextField campoValor = new JTextField();
    private final JTextField campoDescripcion = new JTextField();
    private final JCheckBox checkChequePreDatado = new JCheckBox("ES UN CHEQUE PRE-DATADO");
    private final JTextField campoNumeroCheque = new JTextField();
    private final JTextField campoBanco = new JTextField();
    private final JTextField campoVencimiento = new JTextField();
    private final JPanel panelCheque = new JPanel(new GridBagLayout());
    private final JCheckBox checkDescuento = new JCheckBox("APLICAR DESCUENTO");
    private final JRadioButton radioDescuentoPorcentaje = new JRadioButton("PORCENTAJE (%)", true);
    private final JRadioButton radioDescuentoValor = new JRadioButton("VALOR FIJO (Gs.)");
    private final JTextField campoDescuento = new JTextField();
    private final JPanel panelDescuento = new JPanel(new GridBagLayout());
    private final JLabel labelError = new JLabel(" ");

    private final BigDecimal saldoActual;

    private BigDecimal valor;
    private BigDecimal descuentoValor = BigDecimal.ZERO;
    private String descripcion;
    private boolean chequePreDatado;
    private String numeroCheque;
    private String banco;
    private LocalDate fechaVencimiento;
    private boolean confirmado;

    public PagoProveedorDialog(Window propietario, String nombreProveedor, BigDecimal saldoActual) {
        super(propietario, "Registrar Pago - " + nombreProveedor, ModalityType.APPLICATION_MODAL);
        this.saldoActual = saldoActual == null ? BigDecimal.ZERO : saldoActual;
        armarPantalla();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    /** Lo que se esta pagando ahora -- sale ENTERO en Financiero. */
    public BigDecimal getValor() {
        return valor;
    }

    /** Descuento en Gs. calculado sobre el saldo (BigDecimal.ZERO cuando no se aplico ninguno). */
    public BigDecimal getDescuentoValor() {
        return descuentoValor;
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
        setSize(400, 680);
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

        boolean hayDeuda = saldoActual.compareTo(BigDecimal.ZERO) > 0;

        JLabel labelSaldo = new JLabel("Saldo actual: Gs. " + FORMATO_VALOR.format(saldoActual));
        labelSaldo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelSaldo.setForeground(hayDeuda ? Paleta.ROJO_ERROR : Paleta.AZUL_OSCURO);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        formulario.add(labelSaldo, gbc);

        checkDescuento.setOpaque(false);
        checkDescuento.setFont(new Font("Segoe UI", Font.BOLD, 11));
        checkDescuento.setForeground(Paleta.GRIS_TEXTO);
        checkDescuento.setEnabled(hayDeuda);
        if (!hayDeuda) {
            checkDescuento.setToolTipText("No hay deuda con este proveedor -- no hay nada para descontar.");
        }
        checkDescuento.addActionListener(e -> alternarPanelDescuento());
        gbc.gridy = 1;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(checkDescuento, gbc);

        armarPanelDescuento();
        panelDescuento.setVisible(false);
        gbc.gridy = 2;
        gbc.insets = new Insets(6, 0, 0, 0);
        formulario.add(panelDescuento, gbc);

        labelResumen.setForeground(Paleta.AZUL_OSCURO);
        labelResumen.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelResumen.setVisible(false);
        gbc.gridy = 3;
        gbc.insets = new Insets(8, 0, 0, 0);
        formulario.add(labelResumen, gbc);

        JLabel labelValor = new JLabel("VALOR QUE SE PAGA AHORA (Gs.) *");
        labelValor.setForeground(Paleta.GRIS_TEXTO);
        labelValor.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 4;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(labelValor, gbc);

        estilizarCampo(campoValor);
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoValor, gbc);

        JLabel labelDescripcion = new JLabel("DESCRIPCION");
        labelDescripcion.setForeground(Paleta.GRIS_TEXTO);
        labelDescripcion.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 7;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(labelDescripcion, gbc);

        estilizarCampo(campoDescripcion);
        gbc.gridy = 8;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoDescripcion, gbc);

        checkChequePreDatado.setOpaque(false);
        checkChequePreDatado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        checkChequePreDatado.setForeground(Paleta.GRIS_TEXTO);
        checkChequePreDatado.addActionListener(e -> alternarPanelCheque());
        gbc.gridy = 9;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(checkChequePreDatado, gbc);

        armarPanelCheque();
        panelCheque.setVisible(false);
        gbc.gridy = 10;
        gbc.insets = new Insets(6, 0, 0, 0);
        formulario.add(panelCheque, gbc);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 11;
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
        gbc.gridy = 12;
        gbc.insets = new Insets(16, 0, 0, 0);
        formulario.add(botones, gbc);

        vigilarCampo(campoDescuento);

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

    private void armarPanelDescuento() {
        panelDescuento.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(radioDescuentoPorcentaje);
        grupo.add(radioDescuentoValor);
        JPanel panelRadios = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelRadios.setOpaque(false);
        for (JRadioButton radio : new JRadioButton[] { radioDescuentoPorcentaje, radioDescuentoValor }) {
            radio.setOpaque(false);
            radio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            radio.setForeground(Paleta.GRIS_TEXTO);
            radio.addActionListener(e -> actualizarResumen());
            panelRadios.add(radio);
        }
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        panelDescuento.add(panelRadios, gbc);

        JLabel labelDescuento = new JLabel("DESCUENTO SOBRE EL SALDO (% o Gs.) *");
        labelDescuento.setForeground(Paleta.GRIS_TEXTO);
        labelDescuento.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 1;
        gbc.insets = new Insets(8, 0, 0, 0);
        panelDescuento.add(labelDescuento, gbc);

        estilizarCampo(campoDescuento);
        gbc.gridy = 2;
        gbc.insets = new Insets(4, 0, 0, 0);
        panelDescuento.add(campoDescuento, gbc);
    }

    private void alternarPanelDescuento() {
        panelDescuento.setVisible(checkDescuento.isSelected());
        actualizarResumen();
        revalidate();
        repaint();
    }

    /** Recalcula el resumen en vivo cada vez que el usuario tipea el descuento. */
    private void vigilarCampo(JTextField campo) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarResumen();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarResumen();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarResumen();
            }
        });
    }

    /**
     * Descuento convertido a Gs., SIEMPRE calculado sobre el saldo actual
     * (es la cuenta con el proveedor la que recibe el descuento, no el monto
     * que se entrega). Devuelve ZERO si no hay descuento valido tipeado.
     */
    private BigDecimal calcularDescuentoEnGs() {
        if (!checkDescuento.isSelected()) {
            return BigDecimal.ZERO;
        }
        BigDecimal ingresado = parsearONull(campoDescuento);
        if (ingresado == null || ingresado.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (radioDescuentoPorcentaje.isSelected()) {
            return saldoActual.multiply(ingresado).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return ingresado;
    }

    /** Muestra en vivo cuanto se perdona y cuanto queda para saldar la cuenta. */
    private void actualizarResumen() {
        BigDecimal descuentoEnGs = calcularDescuentoEnGs();
        if (descuentoEnGs.compareTo(BigDecimal.ZERO) <= 0 || descuentoEnGs.compareTo(saldoActual) > 0) {
            labelResumen.setVisible(false);
            return;
        }
        BigDecimal porcentaje = descuentoEnGs.multiply(BigDecimal.valueOf(100))
                .divide(saldoActual, 2, RoundingMode.HALF_UP);
        labelResumen.setText("<html>Descuento: Gs. " + FORMATO_VALOR.format(descuentoEnGs)
                + " (" + porcentaje.toPlainString() + "%)<br>"
                + "Para saldar la cuenta se paga: Gs. "
                + FORMATO_VALOR.format(saldoActual.subtract(descuentoEnGs)) + "</html>");
        labelResumen.setVisible(true);
    }

    /** Lee un campo numerico tolerando "150.000"; devuelve null si esta vacio o invalido. */
    private BigDecimal parsearONull(JTextField campo) {
        String texto = campo.getText().trim().replace(".", "").replace(",", ".");
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return null;
        }
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
        labelError.setText(" ");

        BigDecimal valorIngresado = parsearONull(campoValor);
        if (valorIngresado == null) {
            labelError.setText("Ingrese un valor numerico valido.");
            return;
        }
        if (valorIngresado.compareTo(BigDecimal.ZERO) <= 0) {
            labelError.setText("El valor debe ser mayor que cero.");
            return;
        }
        valor = valorIngresado;

        if (checkDescuento.isSelected()) {
            BigDecimal descuentoIngresado = parsearONull(campoDescuento);
            if (descuentoIngresado == null) {
                labelError.setText("Ingrese un valor de descuento numerico valido.");
                return;
            }
            if (descuentoIngresado.compareTo(BigDecimal.ZERO) <= 0) {
                labelError.setText("El descuento debe ser mayor que cero.");
                return;
            }
            BigDecimal descuentoEnGs = calcularDescuentoEnGs();
            if (descuentoEnGs.compareTo(saldoActual) > 0) {
                labelError.setText("El descuento no puede ser mayor que el saldo (Gs. "
                        + FORMATO_VALOR.format(saldoActual) + ").");
                return;
            }
            descuentoValor = descuentoEnGs;
        } else {
            descuentoValor = BigDecimal.ZERO;
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
