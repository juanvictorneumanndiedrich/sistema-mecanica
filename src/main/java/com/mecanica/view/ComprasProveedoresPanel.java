package com.mecanica.view;

import com.mecanica.controller.CierreProveedorController;
import com.mecanica.controller.CompraController;
import com.mecanica.controller.ProveedorController;
import com.mecanica.enums.EstadoCierreProveedor;
import com.mecanica.enums.FormaPagoCompra;
import com.mecanica.model.CierreProveedor;
import com.mecanica.model.Compra;
import com.mecanica.model.Proveedor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pantalla real del area "Compras y Proveedores": lista de proveedores a la
 * izquierda (con busqueda por nombre, Nuevo/Editar/Eliminar) y, a la
 * derecha, dos pestanas para el proveedor seleccionado -- "Compras"
 * (registro de compras nuevas, con su forma de pago y estado) y "Cierres"
 * (el cierre de cuenta del proveedor, en sus 2 etapas manuales: abrir el
 * cierre juntando las compras pendientes, y despues marcarlo como pagado --
 * ver CierreProveedorController).
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en ClientesEquiposPanel.
 */
public class ComprasProveedoresPanel extends JPanel {

    private final ProveedorController proveedorController = new ProveedorController();
    private final CompraController compraController = new CompraController();
    private final CierreProveedorController cierreProveedorController = new CierreProveedorController();

    private final TablaProveedoresModel modeloProveedores = new TablaProveedoresModel();
    private final TablaComprasModel modeloCompras = new TablaComprasModel();
    private final TablaCierresModel modeloCierres = new TablaCierresModel();

    private final JTable tablaProveedores = new JTable(modeloProveedores);
    private final JTable tablaCompras = new JTable(modeloCompras);
    private final JTable tablaCierres = new JTable(modeloCierres);

    private final JTextField campoBusqueda = new JTextField();
    private final JLabel labelDetalleTitulo = new JLabel("Proveedor");

    private final BotonPlano botonEditarProveedor = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarProveedor = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());
    private final BotonPlano botonNuevaCompra = new BotonPlano("NUEVA COMPRA", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonAbrirCierre = new BotonPlano("ABRIR CIERRE", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonMarcarPagado = new BotonPlano("MARCAR COMO PAGADO", Paleta.AZUL, Paleta.AZUL_CLARO);

    public ComprasProveedoresPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                armarPanelProveedores(), armarPanelDetalle());
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        add(splitPane, BorderLayout.CENTER);

        botonEditarProveedor.setEnabled(false);
        botonEliminarProveedor.setEnabled(false);
        actualizarEstadoBotonesDetalle();
        actualizarEstadoBotonMarcarPagado();

        cargarProveedores(null);
    }

    // ---------------------------------------------------------------- Proveedores

    private JComponent armarPanelProveedores() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Proveedores");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        encabezado.add(titulo, BorderLayout.WEST);

        BotonPlano botonNuevoProveedor = new BotonPlano("NUEVO PROVEEDOR");
        botonNuevoProveedor.addActionListener(e -> onNuevoProveedor());
        encabezado.add(botonNuevoProveedor, BorderLayout.EAST);
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
        campoBusqueda.addActionListener(e -> cargarProveedores(campoBusqueda.getText().trim()));

        JPanel panelBusqueda = new JPanel(new BorderLayout(0, 4));
        panelBusqueda.setOpaque(false);
        panelBusqueda.add(labelBuscar, BorderLayout.NORTH);
        panelBusqueda.add(campoBusqueda, BorderLayout.CENTER);
        centro.add(panelBusqueda, BorderLayout.NORTH);

        estilizarTabla(tablaProveedores);
        tablaProveedores.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSeleccionarProveedor();
            }
        });
        centro.add(new JScrollPane(tablaProveedores), BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botonEditarProveedor.addActionListener(e -> onEditarProveedor());
        botonEliminarProveedor.addActionListener(e -> onEliminarProveedor());
        botones.add(botonEditarProveedor);
        botones.add(botonEliminarProveedor);
        panel.add(botones, BorderLayout.SOUTH);

        return panel;
    }

    // ---------------------------------------------------------------- Detalle (Compras / Cierres)

    private JComponent armarPanelDetalle() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        labelDetalleTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        labelDetalleTitulo.setForeground(Paleta.AZUL_OSCURO);
        panel.add(labelDetalleTitulo, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pestanas.addTab("Compras", armarPanelCompras());
        pestanas.addTab("Cierres", armarPanelCierres());
        panel.add(pestanas, BorderLayout.CENTER);

        return panel;
    }

    private JComponent armarPanelCompras() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(botonNuevaCompra, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        estilizarTabla(tablaCompras);
        panel.add(new JScrollPane(tablaCompras), BorderLayout.CENTER);

        botonNuevaCompra.addActionListener(e -> onNuevaCompra());

        return panel;
    }

    private JComponent armarPanelCierres() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(botonAbrirCierre, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        estilizarTabla(tablaCierres);
        tablaCierres.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotonMarcarPagado();
            }
        });
        panel.add(new JScrollPane(tablaCierres), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(botonMarcarPagado);
        panel.add(botones, BorderLayout.SOUTH);

        botonAbrirCierre.addActionListener(e -> onAbrirCierre());
        botonMarcarPagado.addActionListener(e -> onMarcarComoPagado());

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

    private void cargarProveedores(String filtroNombre) {
        setHabilitado(false);
        new SwingWorker<List<Proveedor>, Void>() {
            Exception error;

            @Override
            protected List<Proveedor> doInBackground() {
                try {
                    return (filtroNombre == null || filtroNombre.isEmpty())
                            ? proveedorController.listarTodos()
                            : proveedorController.buscarPorNombre(filtroNombre);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    modeloProveedores.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                onSeleccionarProveedor();
            }
        }.execute();
    }

    private void cargarCompras(Proveedor proveedor) {
        if (proveedor == null) {
            modeloCompras.setDatos(List.of());
            return;
        }

        setHabilitado(false);
        new SwingWorker<List<Compra>, Void>() {
            Exception error;

            @Override
            protected List<Compra> doInBackground() {
                try {
                    return compraController.listarPorFornecedor(proveedor);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    modeloCompras.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
            }
        }.execute();
    }

    private void cargarCierres(Proveedor proveedor) {
        if (proveedor == null) {
            modeloCierres.setDatos(List.of());
            actualizarEstadoBotonMarcarPagado();
            return;
        }

        setHabilitado(false);
        new SwingWorker<List<CierreProveedor>, Void>() {
            Exception error;

            @Override
            protected List<CierreProveedor> doInBackground() {
                try {
                    return cierreProveedorController.listarPorProveedor(proveedor);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    modeloCierres.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                actualizarEstadoBotonMarcarPagado();
            }
        }.execute();
    }

    private void onSeleccionarProveedor() {
        Proveedor seleccionado = proveedorSeleccionado();
        boolean hay = seleccionado != null;
        botonEditarProveedor.setEnabled(hay);
        botonEliminarProveedor.setEnabled(hay);
        labelDetalleTitulo.setText(hay ? seleccionado.getNombre() : "Proveedor");
        actualizarEstadoBotonesDetalle();
        cargarCompras(seleccionado);
        cargarCierres(seleccionado);
    }

    private Proveedor proveedorSeleccionado() {
        int fila = tablaProveedores.getSelectedRow();
        return fila < 0 ? null : modeloProveedores.getProveedor(fila);
    }

    private CierreProveedor cierreSeleccionado() {
        int fila = tablaCierres.getSelectedRow();
        return fila < 0 ? null : modeloCierres.getCierre(fila);
    }

    private void actualizarEstadoBotonesDetalle() {
        boolean hayProveedor = proveedorSeleccionado() != null;
        botonNuevaCompra.setEnabled(hayProveedor);
        botonAbrirCierre.setEnabled(hayProveedor);
    }

    private void actualizarEstadoBotonMarcarPagado() {
        CierreProveedor cierre = cierreSeleccionado();
        botonMarcarPagado.setEnabled(cierre != null && cierre.getEstado() == EstadoCierreProveedor.CERRADO);
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

    // ---------------------------------------------------------------- CRUD Proveedor

    private void onNuevoProveedor() {
        ProveedorFormDialog dialogo = new ProveedorFormDialog(ventana(), null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarProveedor(dialogo.getProveedor());
        }
    }

    private void onEditarProveedor() {
        Proveedor seleccionado = proveedorSeleccionado();
        if (seleccionado == null) {
            return;
        }
        ProveedorFormDialog dialogo = new ProveedorFormDialog(ventana(), seleccionado);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarProveedor(dialogo.getProveedor());
        }
    }

    private void guardarProveedor(Proveedor proveedor) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    proveedorController.guardar(proveedor);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ComprasProveedoresPanel.this,
                            error.getMessage(), "No fue posible guardar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarProveedores(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    private void onEliminarProveedor() {
        Proveedor seleccionado = proveedorSeleccionado();
        if (seleccionado == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Eliminar el proveedor \"" + seleccionado.getNombre() + "\"?",
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
                    proveedorController.eliminar(seleccionado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ComprasProveedoresPanel.this,
                            "No fue posible eliminar: el proveedor tiene compras u otros registros vinculados.",
                            "No fue posible eliminar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarProveedores(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Compra

    private void onNuevaCompra() {
        Proveedor proveedor = proveedorSeleccionado();
        if (proveedor == null) {
            return;
        }
        CompraFormDialog dialogo = new CompraFormDialog(ventana(), proveedor);
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
                    compraController.registrarCompra(proveedor, dialogo.getFecha(), dialogo.getDescripcion(),
                            dialogo.getValor(), dialogo.getFormaPago());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ComprasProveedoresPanel.this,
                            error.getMessage(), "No fue posible registrar la compra", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarCompras(proveedor);
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Cierre de Proveedor

    private void onAbrirCierre() {
        Proveedor proveedor = proveedorSeleccionado();
        if (proveedor == null) {
            return;
        }

        setHabilitado(false);
        new SwingWorker<List<Compra>, Void>() {
            Exception error;

            @Override
            protected List<Compra> doInBackground() {
                try {
                    return compraController.listarPendientesDeCierre(proveedor);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                List<Compra> pendientes = List.of();
                try {
                    pendientes = get();
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                    return;
                }
                if (pendientes.isEmpty()) {
                    JOptionPane.showMessageDialog(ComprasProveedoresPanel.this,
                            "No hay compras pendientes para cerrar con este proveedor.",
                            "Sin compras pendientes", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                abrirDialogoDeCierre(proveedor, pendientes);
            }
        }.execute();
    }

    private void abrirDialogoDeCierre(Proveedor proveedor, List<Compra> pendientes) {
        CierreProveedorDialog dialogo = new CierreProveedorDialog(ventana(), proveedor, pendientes);
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
                    cierreProveedorController.abrirCierre(proveedor, dialogo.getFecha());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ComprasProveedoresPanel.this,
                            error.getMessage(), "No fue posible abrir el cierre", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarCompras(proveedor);
                cargarCierres(proveedor);
            }
        }.execute();
    }

    private void onMarcarComoPagado() {
        Proveedor proveedor = proveedorSeleccionado();
        CierreProveedor seleccionado = cierreSeleccionado();
        if (proveedor == null || seleccionado == null) {
            return;
        }

        CierreProveedorDialog dialogo = new CierreProveedorDialog(ventana(), seleccionado);
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
                    cierreProveedorController.marcarComoPagado(seleccionado, dialogo.getFecha());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ComprasProveedoresPanel.this,
                            error.getMessage(), "No fue posible marcar como pagado", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarCierres(proveedor);
                cargarCompras(proveedor);
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Modelos de tabla

    private static class TablaProveedoresModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Nombre", "Documento", "Telefono", "Contacto"};

        private List<Proveedor> proveedores = List.of();

        void setDatos(List<Proveedor> proveedores) {
            this.proveedores = proveedores;
            fireTableDataChanged();
        }

        Proveedor getProveedor(int fila) {
            return proveedores.get(fila);
        }

        @Override
        public int getRowCount() {
            return proveedores.size();
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
            Proveedor proveedor = proveedores.get(fila);
            switch (columna) {
                case 0:
                    return proveedor.getNombre();
                case 1:
                    return proveedor.getDocumento() == null ? "" : proveedor.getDocumento();
                case 2:
                    return proveedor.getTelefono() == null ? "" : proveedor.getTelefono();
                case 3:
                    return proveedor.getContacto() == null ? "" : proveedor.getContacto();
                default:
                    return "";
            }
        }
    }

    private static class TablaComprasModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Descripcion", "Valor (Gs.)", "Forma de Pago", "Estado"};
        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");

        private List<Compra> compras = List.of();

        void setDatos(List<Compra> compras) {
            this.compras = compras;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return compras.size();
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
            Compra compra = compras.get(fila);
            switch (columna) {
                case 0:
                    return compra.getFecha() == null ? "" : compra.getFecha().format(FORMATO_FECHA);
                case 1:
                    return compra.getDescripcion() == null ? "" : compra.getDescripcion();
                case 2:
                    return FORMATO_VALOR.format(compra.getValor() == null ? BigDecimal.ZERO : compra.getValor());
                case 3:
                    return compra.getFormaPago();
                case 4:
                    return estadoDeCompra(compra);
                default:
                    return "";
            }
        }

        /**
         * Estado visible de la compra: no es un campo del modelo, se deriva de
         * la forma de pago y, si esta cargada en cuenta, del cierre al que
         * eventualmente fue vinculada (ver Compra.cierreProveedor).
         */
        private String estadoDeCompra(Compra compra) {
            if (compra.getFormaPago() == FormaPagoCompra.PAGO_INMEDIATO) {
                return "Pagada";
            }
            CierreProveedor cierre = compra.getCierreProveedor();
            if (cierre == null) {
                return "Pendiente de Cierre";
            }
            return cierre.getEstado() == EstadoCierreProveedor.PAGADO ? "Pagada (Cierre)" : "En Cierre";
        }
    }

    private static class TablaCierresModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha Cierre", "Fecha Pago", "Valor Total (Gs.)", "Estado"};
        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");

        private List<CierreProveedor> cierres = List.of();

        void setDatos(List<CierreProveedor> cierres) {
            this.cierres = cierres;
            fireTableDataChanged();
        }

        CierreProveedor getCierre(int fila) {
            return cierres.get(fila);
        }

        @Override
        public int getRowCount() {
            return cierres.size();
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
            CierreProveedor cierre = cierres.get(fila);
            switch (columna) {
                case 0:
                    return cierre.getFechaCierre() == null ? "" : cierre.getFechaCierre().format(FORMATO_FECHA);
                case 1:
                    return cierre.getFechaPago() == null ? "" : cierre.getFechaPago().format(FORMATO_FECHA);
                case 2:
                    return FORMATO_VALOR.format(cierre.getValorTotal() == null ? BigDecimal.ZERO : cierre.getValorTotal());
                case 3:
                    return cierre.getEstado();
                default:
                    return "";
            }
        }
    }
}
