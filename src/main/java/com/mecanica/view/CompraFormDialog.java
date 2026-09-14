package com.mecanica.view;

import com.mecanica.enums.FormaPagoCompra;
import com.mecanica.model.Proveedor;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogo modal para registrar una Compra nueva, siempre vinculada al
 * Proveedor que esta seleccionado en ComprasProveedoresPanel (el proveedor
 * no se elige aca, solo se muestra como referencia). Solo da alta -- no
 * existe edicion de una compra ya registrada. La forma de pago tiene
 * exactamente los 2 escenarios de FormaPagoCompra (ver
 * CompraController.registrarCompra): PAGO_INMEDIATO o
 * CARGADA_EN_CUENTA_PROVEEDOR.
 */
public class CompraFormDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField campoFecha = new JTextField();
    private final JTextField campoDescripcion = new JTextField();
    private final JTextField campoValor = new JTextField();
    private final JComboBox<FormaPagoCompra> comboFormaPago = new JComboBox<>(FormaPagoCompra.values());
    private final JLabel labelError = new JLabel(" ");

    private final Proveedor proveedor;
    private LocalDate fecha;
    private String descripcion;
    private BigDecimal valor;
    private FormaPagoCompra formaPago;
    private boolean confirmado;

    public CompraFormDialog(Window propietario, Proveedor proveedor) {
        super(propietario, "Nueva Compra", ModalityType.APPLICATION_MODAL);
        this.proveedor = proveedor;
        armarPantalla();
    }

    /** true si el usuario confirmo con REGISTRAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public FormaPagoCompra getFormaPago() {
        return formaPago;
    }

    private void armarPantalla() {
        setSize(420, 460);
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

        campoFecha.setText(LocalDate.now().format(FORMATO_FECHA));
        int fila = agregarCampo(formulario, gbc, 1, "FECHA (dd/mm/aaaa) *", campoFecha);
        fila = agregarCampo(formulario, gbc, fila, "DESCRIPCION", campoDescripcion);
        fila = agregarCampo(formulario, gbc, fila, "VALOR (Gs.) *", campoValor);

        comboFormaPago.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fila = agregarEtiqueta(formulario, gbc, fila, "FORMA DE PAGO *");
        gbc.gridy = fila++;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(comboFormaPago, gbc);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = fila++;
        gbc.insets = new Insets(10, 0, 0, 0);
        formulario.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
        botonCancelar.addActionListener(e -> dispose());
        BotonPlano botonGuardar = new BotonPlano("REGISTRAR");
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

        // Acepta tanto "150000" como "150.000" (separador de miles paraguayo).
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

        FormaPagoCompra formaSeleccionada = (FormaPagoCompra) comboFormaPago.getSelectedItem();
        if (formaSeleccionada == null) {
            labelError.setText("La forma de pago es obligatoria.");
            return;
        }

        fecha = fechaIngresada;
        valor = valorIngresado;
        formaPago = formaSeleccionada;
        descripcion = vacioComoNull(campoDescripcion.getText());

        confirmado = true;
        dispose();
    }

    private String vacioComoNull(String texto) {
        String valorTexto = texto == null ? "" : texto.trim();
        return valorTexto.isEmpty() ? null : valorTexto;
    }
}
