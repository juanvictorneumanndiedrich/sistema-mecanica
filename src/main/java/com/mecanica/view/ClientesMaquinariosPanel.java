package com.mecanica.view;

import com.mecanica.controller.ChequePreDatadoController;
import com.mecanica.controller.ClienteController;
import com.mecanica.controller.MaquinarioController;
import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Pantalla real del area "Clientes y Maquinarios": lista de clientes a la
 * izquierda (con busqueda por nombre) y, a la derecha, los maquinarios del
 * cliente seleccionado. Cada lado tiene sus propios botones
 * Nuevo/Editar/Eliminar, y el cliente tiene ademas "Registrar Pago"
 * (descuenta directamente su saldo general -- ver ClienteController).
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en LoginView.
 */
public class ClientesMaquinariosPanel extends JPanel implements PanelActualizable {

    private final ClienteController clienteController = new ClienteController();
    private final MaquinarioController maquinarioController = new MaquinarioController();
    private final ChequePreDatadoController chequeController = new ChequePreDatadoController();

    private final TablaClientesModel modeloClientes = new TablaClientesModel();
    private final TablaMaquinariosModel modeloMaquinarios = new TablaMaquinariosModel();

    private final JTable tablaClientes = new JTable(modeloClientes);
    private final JTable tablaMaquinarios = new JTable(modeloMaquinarios);

    private final JTextField campoBusqueda = new JTextField();
    private final JLabel labelMaquinariosTitulo = new JLabel("Maquinarios");

    private final BotonPlano botonEditarCliente = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarCliente = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());
    private final BotonPlano botonPagoCliente = new BotonPlano("REGISTRAR PAGO", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonRetirarSaldoCliente = new BotonPlano("RETIRAR SALDO", Paleta.VERDE_EXITO, Paleta.VERDE_EXITO.brighter());
    private final BotonPlano botonNuevoMaquinario = new BotonPlano("NUEVO", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEditarMaquinario = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarMaquinario = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());

    public ClientesMaquinariosPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                armarPanelClientes(), armarPanelMaquinarios());
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        add(splitPane, BorderLayout.CENTER);

        botonEditarCliente.setEnabled(false);
        botonEliminarCliente.setEnabled(false);
        botonPagoCliente.setEnabled(false);
        botonRetirarSaldoCliente.setEnabled(false);
        actualizarEstadoBotonesMaquinario();

        cargarClientes(null);
    }

    /** Recarga la lista de clientes (sin filtro de busqueda) al entrar en esta area. */
    @Override
    public void actualizar() {
        campoBusqueda.setText("");
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
        tablaClientes.getColumnModel().getColumn(3).setCellRenderer(new ColorSaldoRenderer(modeloClientes));
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
        botonRetirarSaldoCliente.addActionListener(e -> onRetirarSaldo());
        botones.add(botonEditarCliente);
        botones.add(botonEliminarCliente);
        botones.add(botonPagoCliente);
        botones.add(botonRetirarSaldoCliente);
        panel.add(botones, BorderLayout.SOUTH);

        return panel;
    }

    // ---------------------------------------------------------------- Maquinarios

    private JComponent armarPanelMaquinarios() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        labelMaquinariosTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        labelMaquinariosTitulo.setForeground(Paleta.AZUL_OSCURO);
        panel.add(labelMaquinariosTitulo, BorderLayout.NORTH);

        estilizarTabla(tablaMaquinarios);
        tablaMaquinarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotonesMaquinario();
            }
        });
        panel.add(new JScrollPane(tablaMaquinarios), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botonNuevoMaquinario.addActionListener(e -> onNuevoMaquinario());
        botonEditarMaquinario.addActionListener(e -> onEditarMaquinario());
        botonEliminarMaquinario.addActionListener(e -> onEliminarMaquinario());
        botones.add(botonNuevoMaquinario);
        botones.add(botonEditarMaquinario);
        botones.add(botonEliminarMaquinario);
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

    private void cargarMaquinarios(Cliente cliente) {
        if (cliente == null) {
            modeloMaquinarios.setDatos(List.of());
            actualizarEstadoBotonesMaquinario();
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
                try {
                    modeloMaquinarios.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                actualizarEstadoBotonesMaquinario();
            }
        }.execute();
    }

    private void onSeleccionarCliente() {
        Cliente seleccionado = clienteSeleccionado();
        boolean hay = seleccionado != null;
        botonEditarCliente.setEnabled(hay);
        botonEliminarCliente.setEnabled(hay);
        botonPagoCliente.setEnabled(hay);
        botonRetirarSaldoCliente.setEnabled(hay && seleccionado.getSaldo() != null
                && seleccionado.getSaldo().compareTo(BigDecimal.ZERO) < 0);
        labelMaquinariosTitulo.setText(hay ? "Maquinarios de " + seleccionado.getNombre() : "Maquinarios");
        cargarMaquinarios(seleccionado);
    }

    private Cliente clienteSeleccionado() {
        int fila = tablaClientes.getSelectedRow();
        return fila < 0 ? null : modeloClientes.getCliente(fila);
    }

    private Maquinario maquinarioSeleccionado() {
        int fila = tablaMaquinarios.getSelectedRow();
        return fila < 0 ? null : modeloMaquinarios.getMaquinario(fila);
    }

    private void actualizarEstadoBotonesMaquinario() {
        botonNuevoMaquinario.setEnabled(clienteSeleccionado() != null);
        boolean hayMaquinario = maquinarioSeleccionado() != null;
        botonEditarMaquinario.setEnabled(hayMaquinario);
        botonEliminarMaquinario.setEnabled(hayMaquinario);
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
                    JOptionPane.showMessageDialog(ClientesMaquinariosPanel.this,
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
                "Eliminar el cliente \"" + seleccionado.getNombre() + "\" y todos sus maquinarios?",
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
                    JOptionPane.showMessageDialog(ClientesMaquinariosPanel.this,
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
        PagoClienteDialog dialogo = new PagoClienteDialog(ventana(), seleccionado.getNombre(),
                seleccionado.getSaldo());
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
                    if (dialogo.isChequePreDatado()) {
                        chequeController.registrarDeCliente(seleccionado, dialogo.getNumeroCheque(),
                                dialogo.getBanco(), dialogo.getFechaVencimiento(), dialogo.getValor(),
                                dialogo.getDescuentoValor(), dialogo.getDescripcion());
                    } else {
                        clienteController.registrarPagamento(seleccionado, dialogo.getValor(),
                                dialogo.getDescuentoValor(), dialogo.getDescripcion());
                    }
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesMaquinariosPanel.this,
                            error.getMessage(), "No fue posible registrar el pago", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarClientes(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    /** Retira en efectivo un credito a favor que el cliente ya tiene (saldo negativo). */
    private void onRetirarSaldo() {
        Cliente seleccionado = clienteSeleccionado();
        if (seleccionado == null) {
            return;
        }
        BigDecimal credito = seleccionado.getSaldo() == null ? BigDecimal.ZERO : seleccionado.getSaldo().negate();
        RetirarSaldoDialog dialogo = new RetirarSaldoDialog(ventana(), seleccionado.getNombre(), credito);
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
                    clienteController.retirarSaldo(seleccionado, dialogo.getValor(), dialogo.getDescripcion());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesMaquinariosPanel.this,
                            error.getMessage(), "No fue posible retirar el saldo", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarClientes(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    // ---------------------------------------------------------------- CRUD Maquinario

    private void onNuevoMaquinario() {
        Cliente cliente = clienteSeleccionado();
        if (cliente == null) {
            return;
        }
        MaquinarioFormDialog dialogo = new MaquinarioFormDialog(ventana(), cliente, null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarMaquinario(dialogo.getMaquinario(), cliente);
        }
    }

    private void onEditarMaquinario() {
        Maquinario seleccionado = maquinarioSeleccionado();
        Cliente cliente = clienteSeleccionado();
        if (seleccionado == null || cliente == null) {
            return;
        }
        MaquinarioFormDialog dialogo = new MaquinarioFormDialog(ventana(), cliente, seleccionado);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarMaquinario(dialogo.getMaquinario(), cliente);
        }
    }

    private void guardarMaquinario(Maquinario maquinario, Cliente cliente) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    maquinarioController.guardar(maquinario);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesMaquinariosPanel.this,
                            error.getMessage(), "No fue posible guardar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarMaquinarios(cliente);
            }
        }.execute();
    }

    private void onEliminarMaquinario() {
        Maquinario seleccionado = maquinarioSeleccionado();
        Cliente cliente = clienteSeleccionado();
        if (seleccionado == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Eliminar este maquinario?",
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
                    maquinarioController.eliminar(seleccionado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ClientesMaquinariosPanel.this,
                            "No fue posible eliminar: el maquinario tiene ordenes de servicio vinculadas.",
                            "No fue posible eliminar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarMaquinarios(cliente);
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Colores de tabla

    /**
     * Pinta la columna "Saldo (Gs.)": rojo cuando el cliente debe (saldo
     * positivo) y verde cuando tiene credito a favor (saldo negativo). Un
     * saldo en cero se deja con el color normal de la tabla.
     */
    private static class ColorSaldoRenderer extends DefaultTableCellRenderer {
        private final TablaClientesModel modelo;

        ColorSaldoRenderer(TablaClientesModel modelo) {
            this.modelo = modelo;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                boolean conFoco, int fila, int columna) {
            Component componente = super.getTableCellRendererComponent(tabla, valor, seleccionado, conFoco, fila, columna);
            Cliente cliente = modelo.getCliente(tabla.convertRowIndexToModel(fila));
            BigDecimal saldo = cliente.getSaldo() == null ? BigDecimal.ZERO : cliente.getSaldo();
            int comparacion = saldo.compareTo(BigDecimal.ZERO);
            if (comparacion > 0) {
                componente.setForeground(Paleta.ROJO_ERROR);
            } else if (comparacion < 0) {
                componente.setForeground(Paleta.VERDE_EXITO);
            }
            return componente;
        }
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

    private static class TablaMaquinariosModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Tipo", "Marca", "Modelo", "Identificacion"};

        private List<Maquinario> maquinarios = List.of();

        void setDatos(List<Maquinario> maquinarios) {
            this.maquinarios = maquinarios;
            fireTableDataChanged();
        }

        Maquinario getMaquinario(int fila) {
            return maquinarios.get(fila);
        }

        @Override
        public int getRowCount() {
            return maquinarios.size();
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
            Maquinario maquinario = maquinarios.get(fila);
            switch (columna) {
                case 0:
                    return maquinario.getTipo();
                case 1:
                    return maquinario.getMarca() == null ? "" : maquinario.getMarca();
                case 2:
                    return maquinario.getModelo() == null ? "" : maquinario.getModelo();
                case 3:
                    return maquinario.getIdentificacion() == null ? "" : maquinario.getIdentificacion();
                default:
                    return "";
            }
        }
    }
}
