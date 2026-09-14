package com.mecanica.view;

import com.mecanica.controller.ClienteController;
import com.mecanica.controller.EquipoController;
import com.mecanica.controller.OrdenDeServicioController;
import com.mecanica.model.Cliente;
import com.mecanica.model.Equipo;
import com.mecanica.model.OrdenDeServicio;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * Dialogo modal para abrir una nueva Orden de Servicio. Primero se elige el
 * Cliente (combo cargado al abrir el dialogo) y, en funcion de ese cliente,
 * se recarga el combo de Equipo (solo los equipos de ese cliente) -- despues
 * se describe el problema reportado. A diferencia de ClienteFormDialog /
 * EquipoFormDialog, este dialogo ya persiste la OS al confirmar (llama a
 * OrdenDeServicioController.abrir), porque el numero secuencial y la fecha
 * de apertura los genera el Controller en el momento de guardar -- no hay
 * forma de "armar el objeto y guardar despues" como con Cliente/Equipo.
 */
public class OrdenServicioFormDialog extends JDialog {

    private final ClienteController clienteController = new ClienteController();
    private final EquipoController equipoController = new EquipoController();
    private final OrdenDeServicioController ordenDeServicioController = new OrdenDeServicioController();

    private final JComboBox<Cliente> comboCliente = new JComboBox<>();
    private final JComboBox<Equipo> comboEquipo = new JComboBox<>();
    private final JTextArea campoProblema = new JTextArea();
    private final JLabel labelError = new JLabel(" ");

    private final BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
    private final BotonPlano botonGuardar = new BotonPlano("ABRIR OS");

    private OrdenDeServicio ordenCreada;
    private boolean confirmado;

    public OrdenServicioFormDialog(Window propietario) {
        super(propietario, "Nueva Orden de Servicio", ModalityType.APPLICATION_MODAL);
        armarPantalla();
        cargarClientes();
    }

    /** true si el usuario confirmo con ABRIR OS (y la OS ya quedo persistida). */
    public boolean isConfirmado() {
        return confirmado;
    }

    /** OS recien creada, ya con numero y fecha de apertura. Valida solo si isConfirmado(). */
    public OrdenDeServicio getOrdenCreada() {
        return ordenCreada;
    }

    private void armarPantalla() {
        setSize(460, 500);
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

        int fila = agregarEtiqueta(formulario, gbc, 0, "CLIENTE *");
        comboCliente.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboCliente.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Cliente c) {
                    setText(c.getNombre());
                }
                return this;
            }
        });
        comboCliente.addActionListener(e -> cargarEquipos(clienteSeleccionado()));
        gbc.gridy = fila++;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(comboCliente, gbc);

        fila = agregarEtiqueta(formulario, gbc, fila, "EQUIPO *");
        comboEquipo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboEquipo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Equipo eq) {
                    setText(formatoEquipo(eq));
                }
                return this;
            }
        });
        gbc.gridy = fila++;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(comboEquipo, gbc);

        fila = agregarEtiqueta(formulario, gbc, fila, "PROBLEMA REPORTADO *");
        campoProblema.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campoProblema.setLineWrap(true);
        campoProblema.setWrapStyleWord(true);
        campoProblema.setRows(5);
        campoProblema.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        JScrollPane scrollProblema = new JScrollPane(campoProblema);
        scrollProblema.setBorder(BorderFactory.createEmptyBorder());
        gbc.gridy = fila++;
        gbc.insets = new Insets(4, 0, 0, 0);
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        formulario.add(scrollProblema, gbc);
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = fila++;
        gbc.insets = new Insets(10, 0, 0, 0);
        formulario.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botonCancelar.addActionListener(e -> dispose());
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
        gbc.insets = new Insets(fila == 0 ? 0 : 14, 0, 0, 0);
        formulario.add(label, gbc);
        return fila + 1;
    }

    private String formatoEquipo(Equipo equipo) {
        StringBuilder texto = new StringBuilder(equipo.getTipo().toString());
        if (equipo.getIdentificacion() != null && !equipo.getIdentificacion().isBlank()) {
            texto.append(" - ").append(equipo.getIdentificacion());
        } else if (equipo.getMarca() != null && !equipo.getMarca().isBlank()) {
            texto.append(" - ").append(equipo.getMarca());
            if (equipo.getModelo() != null && !equipo.getModelo().isBlank()) {
                texto.append(" ").append(equipo.getModelo());
            }
        }
        return texto.toString();
    }

    private Cliente clienteSeleccionado() {
        return (Cliente) comboCliente.getSelectedItem();
    }

    private Equipo equipoSeleccionado() {
        return (Equipo) comboEquipo.getSelectedItem();
    }

    // ---------------------------------------------------------------- Carga de combos

    private void cargarClientes() {
        setHabilitado(false);
        new SwingWorker<List<Cliente>, Void>() {
            Exception error;

            @Override
            protected List<Cliente> doInBackground() {
                try {
                    return clienteController.listarTodos();
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                List<Cliente> clientes = List.of();
                try {
                    clientes = get();
                } catch (Exception e) {
                    error = e;
                }
                comboCliente.setModel(new DefaultComboBoxModel<>(new Vector<>(clientes)));
                if (error != null) {
                    labelError.setText("No fue posible cargar los clientes.");
                } else if (clientes.isEmpty()) {
                    labelError.setText("No hay clientes registrados. Registre un cliente primero.");
                    botonGuardar.setEnabled(false);
                } else {
                    cargarEquipos(clienteSeleccionado());
                }
            }
        }.execute();
    }

    private void cargarEquipos(Cliente cliente) {
        if (cliente == null) {
            comboEquipo.setModel(new DefaultComboBoxModel<>());
            return;
        }
        setHabilitado(false);
        new SwingWorker<List<Equipo>, Void>() {
            Exception error;

            @Override
            protected List<Equipo> doInBackground() {
                try {
                    return equipoController.listarPorCliente(cliente);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                List<Equipo> equipos = List.of();
                try {
                    equipos = get();
                } catch (Exception e) {
                    error = e;
                }
                comboEquipo.setModel(new DefaultComboBoxModel<>(new Vector<>(equipos)));
                if (error != null) {
                    labelError.setText("No fue posible cargar los equipos del cliente.");
                } else if (equipos.isEmpty()) {
                    labelError.setText("Este cliente no tiene equipos registrados.");
                } else {
                    labelError.setText(" ");
                }
            }
        }.execute();
    }

    private void setHabilitado(boolean habilitado) {
        setCursor(habilitado ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        comboCliente.setEnabled(habilitado);
        comboEquipo.setEnabled(habilitado);
        botonGuardar.setEnabled(habilitado);
        botonCancelar.setEnabled(habilitado);
    }

    // ---------------------------------------------------------------- Guardar

    private void onGuardar() {
        Cliente cliente = clienteSeleccionado();
        Equipo equipo = equipoSeleccionado();
        String problema = campoProblema.getText().trim();

        if (cliente == null) {
            labelError.setText("Seleccione un cliente.");
            return;
        }
        if (equipo == null) {
            labelError.setText("Seleccione un equipo del cliente.");
            return;
        }
        if (problema.isEmpty()) {
            labelError.setText("Describa el problema reportado.");
            return;
        }
        labelError.setText(" ");

        setHabilitado(false);
        new SwingWorker<OrdenDeServicio, Void>() {
            RuntimeException error;

            @Override
            protected OrdenDeServicio doInBackground() {
                try {
                    return ordenDeServicioController.abrir(cliente, equipo, problema);
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
                    ordenCreada = get();
                } catch (Exception e) {
                    labelError.setText("No fue posible abrir la orden de servicio.");
                    return;
                }
                confirmado = true;
                dispose();
            }
        }.execute();
    }
}
