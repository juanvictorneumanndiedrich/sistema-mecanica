package com.mecanica.view;

import com.mecanica.model.Empleado;
import com.mecanica.util.Validaciones;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Dialogo modal para crear o editar un Empleado. Si se abre con un
 * empleado existente el formulario aparece precargado y guarda sobre el
 * mismo registro; si se abre sin empleado (null), crea uno nuevo. El
 * salario base cargado aca es el que usa EmpleadoController.calcularCierreMensual
 * para descontar los vales/adelantos del periodo.
 */
public class EmpleadoFormDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat FORMATO_SALARIO = new DecimalFormat("#,##0");

    private final JTextField campoNombre = new JTextField();
    private final JTextField campoDocumento = new JTextField();
    private final JTextField campoTelefono = new JTextField();
    private final JTextField campoCargo = new JTextField();
    private final JTextField campoSalarioBase = new JTextField();
    private final JTextField campoFechaAdmision = new JTextField();
    private final JCheckBox checkActivo = new JCheckBox("Activo");
    private final JLabel labelError = new JLabel(" ");

    private Empleado empleado;
    private boolean confirmado;

    public EmpleadoFormDialog(Window propietario, Empleado empleadoExistente) {
        super(propietario, empleadoExistente == null ? "Nuevo Empleado" : "Editar Empleado",
                ModalityType.APPLICATION_MODAL);
        this.empleado = empleadoExistente;
        armarPantalla();
        cargarDatos();
    }

    /** true si el usuario confirmo con GUARDAR (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    /** Empleado nuevo o editado, listo para pasar al Controller. Valido solo si isConfirmado(). */
    public Empleado getEmpleado() {
        return empleado;
    }

    private void armarPantalla() {
        setSize(420, 620);
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
        fila = agregarCampo(formulario, gbc, fila, "DOCUMENTO (CI)", campoDocumento);
        fila = agregarCampo(formulario, gbc, fila, "TELEFONO", campoTelefono);
        fila = agregarCampo(formulario, gbc, fila, "CARGO", campoCargo);
        fila = agregarCampo(formulario, gbc, fila, "SALARIO BASE (Gs.)", campoSalarioBase);
        fila = agregarCampo(formulario, gbc, fila, "FECHA DE ADMISION (DD/MM/AAAA)", campoFechaAdmision);

        checkActivo.setSelected(true);
        checkActivo.setOpaque(false);
        checkActivo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        checkActivo.setForeground(Paleta.GRIS_TEXTO);
        gbc.gridy = fila++;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(checkActivo, gbc);

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
        if (empleado == null) {
            return;
        }
        campoNombre.setText(empleado.getNombre());
        campoDocumento.setText(empleado.getDocumento());
        campoTelefono.setText(empleado.getTelefono());
        campoCargo.setText(empleado.getCargo());
        if (empleado.getSalarioBase() != null) {
            campoSalarioBase.setText(FORMATO_SALARIO.format(empleado.getSalarioBase()));
        }
        if (empleado.getFechaAdmision() != null) {
            campoFechaAdmision.setText(empleado.getFechaAdmision().format(FORMATO_FECHA));
        }
        checkActivo.setSelected(empleado.isActivo());
    }

    private void onGuardar() {
        labelError.setText(" ");

        String nombre = campoNombre.getText().trim();
        if (Validaciones.esVacio(nombre)) {
            labelError.setText("Debe ingresar el nombre del empleado.");
            return;
        }
        if (!Validaciones.soloLetras(nombre)) {
            labelError.setText("El nombre solo puede contener letras.");
            return;
        }

        String documento = campoDocumento.getText().trim();
        if (!documento.isEmpty() && !Validaciones.documentoValido(documento)) {
            labelError.setText("El documento debe contener solo numeros, con un guion opcional.");
            return;
        }

        String telefono = campoTelefono.getText().trim();
        if (!telefono.isEmpty() && !Validaciones.soloNumeros(telefono)) {
            labelError.setText("El telefono debe contener solo numeros.");
            return;
        }

        String cargo = campoCargo.getText().trim();
        if (!cargo.isEmpty() && !Validaciones.soloLetras(cargo)) {
            labelError.setText("El cargo solo puede contener letras.");
            return;
        }

        BigDecimal salarioBase = null;
        String textoSalario = campoSalarioBase.getText().trim();
        if (!textoSalario.isEmpty()) {
            try {
                salarioBase = new BigDecimal(textoSalario.replace(".", "").replace(",", "."));
                if (salarioBase.compareTo(BigDecimal.ZERO) < 0) {
                    labelError.setText("El salario base no puede ser negativo.");
                    return;
                }
            } catch (NumberFormatException e) {
                labelError.setText("Ingrese un salario base numerico valido.");
                return;
            }
        }

        LocalDate fechaAdmision = null;
        String textoFecha = campoFechaAdmision.getText().trim();
        if (!textoFecha.isEmpty()) {
            try {
                fechaAdmision = LocalDate.parse(textoFecha, FORMATO_FECHA);
            } catch (DateTimeParseException e) {
                labelError.setText("La fecha de admision debe tener el formato DD/MM/AAAA.");
                return;
            }
        }

        if (empleado == null) {
            empleado = new Empleado();
        }
        empleado.setNombre(nombre);
        empleado.setDocumento(vacioComoNull(documento));
        empleado.setTelefono(vacioComoNull(telefono));
        empleado.setCargo(vacioComoNull(cargo));
        empleado.setSalarioBase(salarioBase);
        empleado.setFechaAdmision(fechaAdmision);
        empleado.setActivo(checkActivo.isSelected());

        confirmado = true;
        dispose();
    }

    private String vacioComoNull(String texto) {
        String valor = texto == null ? "" : texto.trim();
        return valor.isEmpty() ? null : valor;
    }
}
