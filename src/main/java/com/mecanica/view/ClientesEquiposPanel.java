package com.mecanica.view;

import com.mecanica.controller.ClienteController;
import com.mecanica.controller.EquipoController;
import com.mecanica.model.Cliente;
import com.mecanica.model.Equipo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Pantalla real del area "Clientes y Equipos": lista de clientes a la
 * izquierda (con busqueda por nombre) y, a la derecha, los equipos del
 * cliente seleccionado. Cada lado tiene sus propios botones
 * Nuevo/Editar/Eliminar, y el cliente tiene ademas "Registrar Pago"
 * (descuenta directamente su saldo general -- ver ClienteController).
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en LoginView.
 */
public class ClientesEquiposPanel extends JPanel {

    private final ClienteController clienteController = new ClienteController();
    private final EquipoController equipoController = new EquipoController();

    private final TablaClientesModel modeloClientes = new TablaClientesModel();
    private final TablaEquiposModel modeloEquipos = new TablaEquiposModel();

    private final JTable tablaClientes = new JTable(modeloClientes);
    private final JTable tablaEquipos = new JTable(modeloEquipos);

    private final JTextField campoBusqueda = new JTextField();
    private final JLabel labelEquiposTitulo = new JLabel("Equipos");

    private final BotonPlano botonEditarCliente = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarCliente = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());
    private final BotonPlano botonPagoCliente = new BotonPlano("REGISTRAR PAGO", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonNuevoEquipo = new BotonPlano("NUEVO", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEditarEquipo = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarEquipo = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());

    public ClientesEquiposPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                armarPanelClientes(), armarPanelEquipos());
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        add(splitPane, BorderLayout.CENTER);

        botonEditarCliente.setEnabled(false);
        botonEliminarCliente.setEnabled(false);
        botonPagoCliente.setEnabled(false);
        actualizarEstadoBotonesEquipo();

        cargarClientes(null);
    }

    // ---------------------------------------------------------------- Clientes

    private JComponent armarPanelClientes() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Clientes");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        encabezado.add(titulo, BorderLayout.WEST);

        BotonPlano botonNuevoCliente = new BotonPlano("NUEVO CLIENTE");
        botonNuevoCliente.addActionListener(e -> onNuevoCliente());
        encabezado.add(botonNuevoCliente, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);

        JLabel labelBuscar = new JLabel("BUSCAR POR NOMBRE (ENTER PARA BUSCAR)");
        labelBuscar.setForeground(Paleta.GRIS_TEXTO);
        labelBuscar.setFont(new Font("Segoe UI", Font.BOLD, 10));

        campoBusqueda.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoBusqueda.setPreferredSize(new Dimension(0, 32));
        campoBusqueda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        campoBusqueda.addActionListener(e -> cargarClientes(campoBusqueda.getText().trim()));

        JPanel panelBusqueda = new JPanel(new BorderLayout(0, 4));
        panelBusqueda.setOpaque(false);
        panelBusqueda.add(labelBuscar, BorderLayout.NORTH);
        panelBusqueda.add(campoBusqueda, BorderLayout.CENTER);
        centro.add(panelBusqueda, BorderLayout.NORTH);

        estilizarTabla(tablaClientes);
        tablaClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSeleccionarCliente();
            }
        });
        centro.add(new JScrollPane(tablaClientes), BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botonEditarCliente.addActionListener(e -> onEditarCliente());
        botonEliminarCliente.addActionListener(e -> onEliminarCliente());
        botonPagoCliente.addActionListener(e -> onRegistrarPago());
        botones.add(botonEditarCliente);
        botones.add(botonEliminarCliente);
        botones.add(botonPagoCliente);
        panel.add(botones, BorderLayout.SOUTH);

        return panel;
    }

    // ---------------------------------------------------------------- Equipos

    private JComponent armarPanelEquipos() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        labelEquiposTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        labelEquiposTitulo.setForeground(Paleta.AZUL_OSCURO);
        panel.add(labelEquiposTitulo, BorderLayout.NORTH);

        estilizarTabla(tablaEquipos);
        tablaEquipos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotonesEquipo();
            }
        });
        panel.add(new JScrollPane(tablaEquipos), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botonNuevoEquipo.addActionListener(e -> onNuevoEquipo());
        botonEditarEquipo.addActionListener(e -> onEditarEquipo());
        botonEliminarEquipo.addActionListener(e -> onEliminarEquipo());
        botones.add(botonNuevoEquipo);
        botones.add(botonEditarEquipo);
        botones.add(botonEliminarEquipo);
        panel.add(botones, BorderLayout.SOUTH);

        return panel;
    }

    private void estilizarTabla(JTable tabla) {
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(26);
        tabla.setSelectionBackground(Paleta.AZUL_TENUE);
        tabla.setSelectionForeground(Paleta.AZUL_OSCURO);
        tabla.setGridColor(Paleta.GRIS_BORDE);
        tabla.setShowVerticalLines(false);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setForeground(Paleta.GRIS_TEXTO);
        tabla.setFillsViewportHeight(true);
    }

    // ---------------------------------------------------------------- Carga de datos

    private void cargarClientes(String filtroNombre) {
        setHabilitado(false);
        new SwingWorker<List<Cliente>, Void>() {
            Exception error;

            @Override
            protected List<Cliente> doInBackground() {
                try {
                    return (filtroNombre == null || filtroNombre.isEmpty())
                            ? clienteController.listarTodos()
                            : clienteController.buscarPorNombre(filtroNombre);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    modeloClientes.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                onSeleccionarCliente();
            }
        }.execute();
    }

    private void cargarEquipos(Cliente cliente) {
        if (cliente == null) {
            modeloEquipos.setDatos(List.of());
            actualizarEstadoBotonesEquipo();
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
                try {
                    modeloEquipos.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                actualizarEstadoBotonesEquipo();
            }
        }.execute();
    }

    private void onSeleccionarCliente() {
        Cliente seleccionado = clienteSeleccionado();
        boolean hay = seleccionado != null;
        botonEditarCliente.setEnabled(hay);
        botonEliminarCliente.setEnabled(hay);
        botonPagoCliente.setEnabled(hay);
        labelEquiposTitulo.setText(hay ? "Equipos de " + seleccionado.getNombre() : "Equipos");
        cargarEquipos(seleccionado);
    }

    private Cliente clienteSeleccionado() {
        int fila = tablaClientes.getSelectedRow();
        return fila < 0 ? null : modeloClientes.getCliente(fila);
    }

    private Equipo equipoSeleccionado() {
        int fila = tablaEquipos.getSelectedRow();
        return fila < 0 ? null : modeloEquipos.getEquipo(fila);
    }

    private void actualizarEstadoBotonesEquipo() {
        botonNuevoEquipo.setEnabled(clienteSeleccionado() != null);
        boolean hayEquipo = equipoSeleccionado() != null;
        botonEditarEquipo.setEnabled(hayEquipo);
        botonEliminarEquipo.setEnabled(hayEquipo);
    }

    private void setHabilitado(boolean habilitado) {
        setCursor(habilitado ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private void mostrarErrorConexion() {
        JOptionPane.showMessageDialog(this,
                "No fue posible conectar a la base de datos.",
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    private Window ventana() {
        return SwingUtilities.getWindowAncestor(this);
    }

    // ---------------------------------------------------------------- CRUD Cliente

    private void onNuevoCliente() {
        ClienteFormDialog dialogo = new ClienteFormDialog(ventana(), null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarCliente(dialogo.getCliente());
        }
    }

    private void onEditarCliente() {
        Cliente seleccionado = clienteSeleccionado();
        if (seleccionado == null) {
            return;
        }
        ClienteFormDialog dialogo = new ClienteFormDialog(ventana(), seleccionado);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarCliente(dialogo.getCliente());
        }
    }

    private void guardarCliente(Cliente cliente) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    clienteController.guardar(cliente);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesEquiposPanel.this,
                            error.getMessage(), "No fue posible guardar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarClientes(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    private void onEliminarCliente() {
        Cliente seleccionado = clienteSeleccionado();
        if (seleccionado == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Eliminar el cliente \"" + seleccionado.getNombre() + "\" y todos sus equipos?",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    clienteController.eliminar(seleccionado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesEquiposPanel.this,
                            "No fue posible eliminar: el cliente tiene ordenes de servicio u otros registros vinculados.",
                            "No fue posible eliminar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarClientes(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    private void onRegistrarPago() {
        Cliente seleccionado = clienteSeleccionado();
        if (seleccionado == null) {
            return;
        }
        PagoClienteDialog dialogo = new PagoClienteDialog(ventana(), seleccionado.getNombre());
        dialogo.setVisible(true);
        if (!dialogo.isConfirmado()) {
            return;
        }

        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    clienteController.registrarPagamento(seleccionado, dialogo.getValor(), dialogo.getDescripcion());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesEquiposPanel.this,
                            error.getMessage(), "No fue posible registrar el pago", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarClientes(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    // ---------------------------------------------------------------- CRUD Equipo

    private void onNuevoEquipo() {
        Cliente cliente = clienteSeleccionado();
        if (cliente == null) {
            return;
        }
        EquipoFormDialog dialogo = new EquipoFormDialog(ventana(), cliente, null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarEquipo(dialogo.getEquipo(), cliente);
        }
    }

    private void onEditarEquipo() {
        Equipo seleccionado = equipoSeleccionado();
        Cliente cliente = clienteSeleccionado();
        if (seleccionado == null || cliente == null) {
            return;
        }
        EquipoFormDialog dialogo = new EquipoFormDialog(ventana(), cliente, seleccionado);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarEquipo(dialogo.getEquipo(), cliente);
        }
    }

    private void guardarEquipo(Equipo equipo, Cliente cliente) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    equipoController.guardar(equipo);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesEquiposPanel.this,
                            error.getMessage(), "No fue posible guardar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarEquipos(cliente);
            }
        }.execute();
    }

    private void onEliminarEquipo() {
        Equipo seleccionado = equipoSeleccionado();
        Cliente cliente = clienteSeleccionado();
        if (seleccionado == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Eliminar este equipo?",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    equipoController.eliminar(seleccionado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesEquiposPanel.this,
                            "No fue posible eliminar: el equipo tiene ordenes de servicio vinculadas.",
                            "No fue posible eliminar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarEquipos(cliente);
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Modelos de tabla

    private static class TablaClientesModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Nombre", "Documento", "Telefono", "Saldo (Gs.)"};
        private static final DecimalFormat FORMATO_SALDO = new DecimalFormat("#,##0");

        private List<Cliente> clientes = List.of();

        void setDatos(List<Cliente> clientes) {
            this.clientes = clientes;
            fireTableDataChanged();
        }

        Cliente getCliente(int fila) {
            return clientes.get(fila);
        }

        @Override
        public int getRowCount() {
            return clientes.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Cliente cliente = clientes.get(fila);
            switch (columna) {
                case 0:
                    return cliente.getNombre();
                case 1:
                    return cliente.getDocumento() == null ? "" : cliente.getDocumento();
                case 2:
                    return cliente.getTelefono() == null ? "" : cliente.getTelefono();
                case 3:
                    return FORMATO_SALDO.format(cliente.getSaldo() == null ? BigDecimal.ZERO : cliente.getSaldo());
                default:
                    return "";
            }
        }
    }

    private static class TablaEquiposModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Tipo", "Marca", "Modelo", "Identificacion"};

        private List<Equipo> equipos = List.of();

        void setDatos(List<Equipo> equipos) {
            this.equipos = equipos;
            fireTableDataChanged();
        }

        Equipo getEquipo(int fila) {
            return equipos.get(fila);
        }

        @Override
        public int getRowCount() {
            return equipos.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Equipo equipo = equipos.get(fila);
            switch (columna) {
                case 0:
                    return equipo.getTipo();
                case 1:
                    return equipo.getMarca() == null ? "" : equipo.getMarca();
                case 2:
                    return equipo.getModelo() == null ? "" : equipo.getModelo();
                case 3:
                    return equipo.getIdentificacion() == null ? "" : equipo.getIdentificacion();
                default:
                    return "";
            }
        }
    }
}
