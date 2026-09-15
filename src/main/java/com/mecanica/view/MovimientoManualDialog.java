package com.mecanica.view;

import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.util.Validaciones;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogo modal para registrar un movimiento financiero manual, fuera de
 * los flujos automaticos del sistema (pago de cliente, compra a
 * proveedor, retiro de empleado). La categoria queda fija en OTRO -- ver
 * MovimientoFinancieroController.registrarMovimientoManual -- por eso no
 * es un campo editable aca, solo se muestra como referencia.
 */
public class MovimientoManualDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField campoFecha = new JTextField();
    private final JComboBox<TipoMovimientoFinanciero> comboTipo = new JComboBox<>(TipoMovimientoFinanciero.values());
    private final JTextField campoValor = new JTextField();
    private final JTextField campoDescripcion = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private LocalDate fecha;
    private TipoMovimientoFinanciero tipo;
    private BigDecimal valor;
    private String descripcion;
    private boolean confirmado;

    public MovimientoManualDialog(Window propietario) {
        super(propietario, "Nuevo Movimiento Manual", ModalityType.APPLICATION_MODAL);
        armarPantalla();
    }

    /** true si el usuario confirmo con REGISTRAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public TipoMovimientoFinanciero getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    private void armarPantalla() {
        setSize(380, 440);
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

        campoFecha.setText(LocalDate.now().format(FORMATO_FECHA));

        JLabel labelFecha = new JLabel("FECHA (DD/MM/AAAA) *");
        labelFecha.setForeground(Paleta.GRIS_TEXTO);
        labelFecha.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        formulario.add(labelFecha, gbc);

        estilizarCampo(campoFecha);
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoFecha, gbc);

        JLabel labelTipo = new JLabel("TIPO *");
        labelTipo.setForeground(Paleta.GRIS_TEXTO);
        labelTipo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 2;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(labelTipo, gbc);

        comboTipo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboTipo.setBackground(Paleta.BLANCO);
        comboTipo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object item, int indice,
                                                            boolean seleccionado, boolean tieneFoco) {
                Component c = super.getListCellRendererComponent(lista, item, indice, seleccionado, tieneFoco);
                if (item == TipoMovimientoFinanciero.ENTRADA) {
                    setText("ENTRADA (ingreso)");
                } else if (item == TipoMovimientoFinanciero.SALIDA) {
                    setText("SALIDA (egreso)");
                }
                return c;
            }
        });
        gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(comboTipo, gbc);

        JLabel labelValor = new JLabel("VALOR (Gs.) *");
        labelValor.setForeground(Paleta.GRIS_TEXTO);
        labelValor.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 4;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(labelValor, gbc);

        estilizarCampo(campoValor);
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoValor, gbc);

        JLabel labelDescripcion = new JLabel("DESCRIPCION *");
        labelDescripcion.setForeground(Paleta.GRIS_TEXTO);
        labelDescripcion.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 6;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(labelDescripcion, gbc);

        estilizarCampo(campoDescripcion);
        gbc.gridy = 7;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoDescripcion, gbc);

        // Categoria fija: los movimientos manuales siempre quedan como OTRO
        // (ver MovimientoFinancieroController.registrarMovimientoManual), asi
        // que se muestra solo como referencia y no se puede editar.
        JLabel labelCategoria = new JLabel("CATEGORIA: OTRO (fija para movimientos manuales)");
        labelCategoria.setForeground(Paleta.GRIS_DESHABILITADO);
        labelCategoria.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gbc.gridy = 8;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(labelCategoria, gbc);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 9;
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
        gbc.gridy = 10;
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

    /** Acepta tanto "150000" como "150.000" (separador de miles paraguayo) para el valor. */
    private void onRegistrar() {
        labelError.setText(" ");

        LocalDate fechaIngresada;
        try {
            fechaIngresada = LocalDate.parse(campoFecha.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            labelError.setText("Ingrese la fecha en formato dd/mm/aaaa.");
            return;
        }

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

        String descripcionIngresada = campoDescripcion.getText().trim();
        if (Validaciones.esVacio(descripcionIngresada)) {
            labelError.setText("Debe ingresar una descripcion para el movimiento.");
            return;
        }

        fecha = fechaIngresada;
        tipo = (TipoMovimientoFinanciero) comboTipo.getSelectedItem();
        valor = valorIngresado;
        descripcion = descripcionIngresada;
        confirmado = true;
        dispose();
    }
}
