package com.mecanica.view;

import com.mecanica.enums.TipoEquipo;
import com.mecanica.model.Cliente;
import com.mecanica.model.Equipo;

import javax.swing.*;
import java.awt.*;

/**
 * Dialogo modal para crear o editar un Equipo, siempre vinculado al
 * Cliente que esta seleccionado en ClientesEquiposPanel (el cliente no se
 * elige aca, solo se muestra como referencia).
 */
public class EquipoFormDialog extends JDialog {

    private final JComboBox<TipoEquipo> comboTipo = new JComboBox<>(TipoEquipo.values());
    private final JTextField campoMarca = new JTextField();
    private final JTextField campoModelo = new JTextField();
    private final JTextField campoIdentificacion = new JTextField();
    private final JTextField campoObservacion = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private final Cliente cliente;
    private Equipo equipo;
    private boolean confirmado;

    public EquipoFormDialog(Window propietario, Cliente cliente, Equipo equipoExistente) {
        super(propietario, equipoExistente == null ? "Nuevo Equipo" : "Editar Equipo",
                ModalityType.APPLICATION_MODAL);
        this.cliente = cliente;
        this.equipo = equipoExistente;
        armarPantalla();
        cargarDatos();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    private void armarPantalla() {
        setSize(420, 440);
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

        JLabel labelCliente = new JLabel("Cliente: " + cliente.getNombre());
        labelCliente.setForeground(Paleta.AZUL_OSCURO);
        labelCliente.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        formulario.add(labelCliente, gbc);

        comboTipo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        int fila = agregarEtiqueta(formulario, gbc, 1, "TIPO *");
        gbc.gridy = fila++;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(comboTipo, gbc);

        fila = agregarCampo(formulario, gbc, fila, "MARCA", campoMarca);
        fila = agregarCampo(formulario, gbc, fila, "MODELO", campoModelo);
        fila = agregarCampo(formulario, gbc, fila, "IDENTIFICACION (placa u otro)", campoIdentificacion);
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

    private void cargarDatos() {
        if (equipo == null) {
            return;
        }
        comboTipo.setSelectedItem(equipo.getTipo());
        campoMarca.setText(equipo.getMarca());
        campoModelo.setText(equipo.getModelo());
        campoIdentificacion.setText(equipo.getIdentificacion());
        campoObservacion.setText(equipo.getObservacion());
    }

    private void onGuardar() {
        TipoEquipo tipo = (TipoEquipo) comboTipo.getSelectedItem();
        if (tipo == null) {
            labelError.setText("El tipo de equipo es obligatorio.");
            return;
        }

        if (equipo == null) {
            equipo = new Equipo();
            equipo.setCliente(cliente);
        }
        equipo.setTipo(tipo);
        equipo.setMarca(vacioComoNull(campoMarca.getText()));
        equipo.setModelo(vacioComoNull(campoModelo.getText()));
        equipo.setIdentificacion(vacioComoNull(campoIdentificacion.getText()));
        equipo.setObservacion(vacioComoNull(campoObservacion.getText()));

        confirmado = true;
        dispose();
    }

    private String vacioComoNull(String texto) {
        String valor = texto == null ? "" : texto.trim();
        return valor.isEmpty() ? null : valor;
    }
}
