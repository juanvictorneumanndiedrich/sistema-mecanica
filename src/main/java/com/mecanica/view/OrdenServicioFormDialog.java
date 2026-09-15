package com.mecanica.view;

import com.mecanica.controller.ClienteController;
import com.mecanica.controller.MaquinarioController;
import com.mecanica.controller.OrdenDeServicioController;
import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;
import com.mecanica.model.OrdenDeServicio;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * Dialogo modal para abrir una nueva Orden de Servicio. Primero se elige el
 * Cliente (combo cargado al abrir el dialogo) y, en funcion de ese cliente,
 * se recarga el combo de Maquinario (solo los maquinarios de ese cliente) -- despues
 * se describe el problema reportado. A diferencia de ClienteFormDialog /
 * MaquinarioFormDialog, este dialogo ya persiste la OS al confirmar (llama a
 * OrdenDeServicioController.abrir), porque el numero secuencial y la fecha
 * de apertura los genera el Controller en el momento de guardar -- no hay
 * forma de "armar el objeto y guardar despues" como con Cliente/Maquinario.
 */
public class OrdenServicioFormDialog extends JDialog {

    private final ClienteController clienteController = new ClienteController();
    private final MaquinarioController maquinarioController = new MaquinarioController();
    private final OrdenDeServicioController ordenDeServicioController = new OrdenDeServicioController();

    private final JComboBox<Cliente> comboCliente = new JComboBox<>();
    private final JComboBox<Maquinario> comboMaquinario = new JComboBox<>();
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
        comboCliente.addActionListener(e -> cargarMaquinarios(clienteSeleccionado()));
        gbc.gridy = fila++;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(comboCliente, gbc);

        fila = agregarEtiqueta(formulario, gbc, fila, "MAQUINARIO");
        comboMaquinario.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboMaquinario.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Maquinario eq) {
                    setText(formatoMaquinario(eq));
                } else {
                    setText("Servicio general (sin maquinaria)");
                }
                return this;
            }
        });
        gbc.gridy = fila++;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(comboMaquinario, gbc);

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

    private String formatoMaquinario(Maquinario maquinario) {
        StringBuilder texto = new StringBuilder(maquinario.getTipo().toString());
        if (maquinario.getIdentificacion() != null && !maquinario.getIdentificacion().isBlank()) {
            texto.append(" - ").append(maquinario.getIdentificacion());
        } else if (maquinario.getMarca() != null && !maquinario.getMarca().isBlank()) {
            texto.append(" - ").append(maquinario.getMarca());
            if (maquinario.getModelo() != null && !maquinario.getModelo().isBlank()) {
                texto.append(" ").append(maquinario.getModelo());
            }
        }
        return texto.toString();
    }

    private Cliente clienteSeleccionado() {
        return (Cliente) comboCliente.getSelectedItem();
    }

    private Maquinario maquinarioSeleccionado() {
        return (Maquinario) comboMaquinario.getSelectedItem();
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
                    cargarMaquinarios(clienteSeleccionado());
                }
            }
        }.execute();
    }

    private void cargarMaquinarios(Cliente cliente) {
        if (cliente == null) {
            comboMaquinario.setModel(new DefaultComboBoxModel<>());
            return;
        }
        setHabilitado(false);
        new SwingWorker<List<Maquinario>, Void>() {
            Exception error;

            @Override
            protected List<Maquinario> doInBackground() {
                try {
                    return maquinarioController.listarPorCliente(cliente);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                List<Maquinario> maquinarios = List.of();
                try {
                    maquinarios = get();
                } catch (Exception e) {
                    error = e;
                }
                // El primer item queda null a proposito -- representa "Servicio
                // general (sin maquinaria)", ver el renderer del combo. Siempre
                // esta disponible, tenga o no maquinarios el cliente.
                Vector<Maquinario> opciones = new Vector<>();
                opciones.add(null);
                opciones.addAll(maquinarios);
                comboMaquinario.setModel(new DefaultComboBoxModel<>(opciones));
                if (error != null) {
                    labelError.setText("No fue posible cargar los maquinarios del cliente.");
                } else if (maquinarios.isEmpty()) {
                    labelError.setText("Este cliente no tiene maquinarios registrados; puede abrir la OS como servicio general.");
                } else {
                    labelError.setText(" ");
                }
            }
        }.execute();
    }

    private void setHabilitado(boolean habilitado) {
        setCursor(habilitado ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        comboCliente.setEnabled(habilitado);
        comboMaquinario.setEnabled(habilitado);
        botonGuardar.setEnabled(habilitado);
        botonCancelar.setEnabled(habilitado);
    }

    // ---------------------------------------------------------------- Guardar

    private void onGuardar() {
        Cliente cliente = clienteSeleccionado();
        Maquinario maquinario = maquinarioSeleccionado();
        String problema = campoProblema.getText().trim();

        if (cliente == null) {
            labelError.setText("Seleccione un cliente.");
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
                    return ordenDeServicioController.abrir(cliente, maquinario, problema);
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
