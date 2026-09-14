package com.mecanica.view;

import com.mecanica.enums.TipoRetiroEmpleado;
import com.mecanica.model.Empleado;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogo modal para registrar un Retiro de un Empleado fijo (el empleado
 * no se elige aca, solo se muestra como referencia -- viene ya
 * seleccionado desde EmpleadosSociosPanel). A diferencia del retiro de
 * socio, este SI genera un MovimientoFinanciero de gasto -- eso lo hace
 * RetiroEmpleadoController.registrarRetirada, no esta pantalla.
 */
public class RetiroEmpleadoDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JComboBox<TipoRetiroEmpleado> comboTipo = new JComboBox<>(TipoRetiroEmpleado.values());
    private final JTextField campoValor = new JTextField();
    private final JTextField campoFecha = new JTextField();
    private final JTextField campoObservacion = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private final Empleado empleado;
    private TipoRetiroEmpleado tipo;
    private BigDecimal valor;
    private LocalDate fecha;
    private String observacion;
    private boolean confirmado;

    public RetiroEmpleadoDialog(Window propietario, Empleado empleado) {
        super(propietario, "Registrar Retiro - " + empleado.getNombre(), ModalityType.APPLICATION_MODAL);
        this.empleado = empleado;
        armarPantalla();
    }

    /** true si el usuario confirmo con REGISTRAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    public TipoRetiroEmpleado getTipo() {
        return tipo;
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

        JLabel labelEmpleado = new JLabel("Empleado: " + empleado.getNombre());
        labelEmpleado.setForeground(Paleta.AZUL_OSCURO);
        labelEmpleado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        formulario.add(labelEmpleado, gbc);

        comboTipo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        int fila = agregarEtiqueta(formulario, gbc, 1, "TIPO *");
        gbc.gridy = fila++;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(comboTipo, gbc);

        fila = agregarCampo(formulario, gbc, fila, "VALOR (Gs.) *", campoValor);
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

    /** Acepta tanto "150000" como "150.000" (separador de miles paraguayo). */
    private void onRegistrar() {
        TipoRetiroEmpleado tipoElegido = (TipoRetiroEmpleado) comboTipo.getSelectedItem();
        if (tipoElegido == null) {
            labelError.setText("El tipo de retiro es obligatorio.");
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

        String textoFecha = campoFecha.getText().trim();
        LocalDate fechaIngresada;
        try {
            fechaIngresada = textoFecha.isEmpty() ? LocalDate.now() : LocalDate.parse(textoFecha, FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            labelError.setText("La fecha debe tener el formato DD/MM/AAAA.");
            return;
        }

        tipo = tipoElegido;
        valor = valorIngresado;
        fecha = fechaIngresada;
        observacion = campoObservacion.getText().trim();
        confirmado = true;
        dispose();
    }
}
