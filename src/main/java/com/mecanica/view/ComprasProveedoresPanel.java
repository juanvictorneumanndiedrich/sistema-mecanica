package com.mecanica.view;

import com.mecanica.controller.ChequePreDatadoController;
import com.mecanica.controller.CompraController;
import com.mecanica.controller.ProveedorController;
import com.mecanica.enums.EstadoCompra;
import com.mecanica.model.Compra;
import com.mecanica.model.Proveedor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Pantalla real del area "Compras y Proveedores": lista de proveedores a la
 * izquierda (con busqueda por nombre, Nuevo/Editar/Eliminar y el saldo que
 * la mecanica le debe) y, a la derecha, las Compras ("notinhas") del
 * proveedor seleccionado.
 *
 * Cada Compra funciona como una nota: se abre con NUEVA COMPRA (numero
 * secuencial automatico) y se le van agregando items -- cada cosa comprada,
 * con cantidad y precio -- en el dialogo de detalle (ver ItemCompraDialog).
 * El valor de la nota entra en la cuenta del proveedor a medida que se
 * cargan los items, y borrar una nota descuenta su valor de esa cuenta.
 *
 * El pago NO es por notinha: REGISTRAR PAGO descuenta un valor del saldo
 * general del proveedor y genera el gasto en Financiero (ver
 * ProveedorController.registrarPagamento) -- igual al pago del cliente en
 * ClientesMaquinariosPanel. La columna Estado se calcula: lo que ya se pago
 * va cubriendo las notas de la mas vieja a la mas nueva, y una nota ya
 * PAGADA queda bloqueada (no se edita ni se borra).
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en ClientesMaquinariosPanel.
 */
public class ComprasProveedoresPanel extends JPanel implements PanelActualizable {

    private final ProveedorController proveedorController = new ProveedorController();
    private final CompraController compraController = new CompraController();
    private final ChequePreDatadoController chequeController = new ChequePreDatadoController();

    private final TablaProveedoresModel modeloProveedores = new TablaProveedoresModel();
    private final TablaComprasModel modeloCompras = new TablaComprasModel();

    private final JTable tablaProveedores = new JTable(modeloProveedores);
    private final JTable tablaCompras = new JTable(modeloCompras);

    private final JTextField campoBusqueda = new JTextField();
    private final JLabel labelDetalleTitulo = new JLabel("Proveedor");
    private final JLabel labelSaldoProveedor = new JLabel(" ");

    private final BotonPlano botonEditarProveedor = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarProveedor = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());
    private final BotonPlano botonPagoProveedor = new BotonPlano("REGISTRAR PAGO", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonRetirarSaldoProveedor = new BotonPlano("RETIRAR SALDO", Paleta.VERDE_EXITO, Paleta.VERDE_EXITO.brighter());
    private final BotonPlano botonNuevaCompra = new BotonPlano("NUEVA COMPRA", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonVerEditarCompra = new BotonPlano("VER / EDITAR ITEMS", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarCompra = new BotonPlano("ELIMINAR NOTA", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());

    public ComprasProveedoresPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                armarPanelProveedores(), armarPanelDetalle());
        splitPane.setResizeWeight(0.45);
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        add(splitPane, BorderLayout.CENTER);

        botonEditarProveedor.setEnabled(false);
        botonEliminarProveedor.setEnabled(false);
        botonPagoProveedor.setEnabled(false);
        botonRetirarSaldoProveedor.setEnabled(false);
        actualizarEstadoBotonesDetalle();
        actualizarEstadoBotonesCompra();

        cargarProveedores(null);
    }

    /** Recarga la lista de proveedores (sin filtro de busqueda) al entrar en esta area. */
    @Override
    public void actualizar() {
        campoBusqueda.setText("");
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
        tablaProveedores.getColumnModel().getColumn(3).setCellRenderer(new ColorSaldoRenderer(modeloProveedores));
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
        botonPagoProveedor.addActionListener(e -> onRegistrarPago());
        botonRetirarSaldoProveedor.addActionListener(e -> onRetirarSaldo());
        botones.add(botonEditarProveedor);
        botones.add(botonEliminarProveedor);
        botones.add(botonPagoProveedor);
        botones.add(botonRetirarSaldoProveedor);
        panel.add(botones, BorderLayout.SOUTH);

        return panel;
    }

    // ---------------------------------------------------------------- Detalle (Compras)

    private JComponent armarPanelDetalle() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);

        labelDetalleTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        labelDetalleTitulo.setForeground(Paleta.AZUL_OSCURO);
        encabezado.add(labelDetalleTitulo, BorderLayout.WEST);

        labelSaldoProveedor.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelSaldoProveedor.setForeground(Paleta.GRIS_TEXTO);
        encabezado.add(labelSaldoProveedor, BorderLayout.EAST);

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(armarPanelCompras(), BorderLayout.CENTER);

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
        tablaCompras.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotonesCompra();
            }
        });
        panel.add(new JScrollPane(tablaCompras), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(botonVerEditarCompra);
        botones.add(botonEliminarCompra);
        panel.add(botones, BorderLayout.SOUTH);

        botonNuevaCompra.addActionListener(e -> onNuevaCompra());
        botonVerEditarCompra.addActionListener(e -> onVerEditarCompra());
        botonEliminarCompra.addActionListener(e -> onEliminarCompra());

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

    /**
     * Recarga los proveedores y vuelve a marcar el que estaba seleccionado
     * -- importante porque casi toda accion de compra cambia el saldo del
     * proveedor, y seria molesto perder la seleccion en cada cambio.
     */
    private void cargarProveedores(String filtroNombre) {
        Proveedor antes = proveedorSeleccionado();
        Long idAntes = antes == null ? null : antes.getId();

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
                seleccionarProveedorPorId(idAntes);
                onSeleccionarProveedor();
            }
        }.execute();
    }

    private void seleccionarProveedorPorId(Long id) {
        if (id == null) {
            return;
        }
        for (int fila = 0; fila < modeloProveedores.getRowCount(); fila++) {
            if (id.equals(modeloProveedores.getProveedor(fila).getId())) {
                tablaProveedores.setRowSelectionInterval(fila, fila);
                return;
            }
        }
    }

    private void cargarCompras(Proveedor proveedor) {
        if (proveedor == null) {
            modeloCompras.setDatos(List.of(), BigDecimal.ZERO);
            actualizarEstadoBotonesCompra();
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
                    modeloCompras.setDatos(get(), proveedor.getSaldo());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                actualizarEstadoBotonesCompra();
            }
        }.execute();
    }

    private void onSeleccionarProveedor() {
        Proveedor seleccionado = proveedorSeleccionado();
        boolean hay = seleccionado != null;
        botonEditarProveedor.setEnabled(hay);
        botonEliminarProveedor.setEnabled(hay);
        botonPagoProveedor.setEnabled(hay);
        botonRetirarSaldoProveedor.setEnabled(hay && seleccionado.getSaldo() != null
                && seleccionado.getSaldo().compareTo(BigDecimal.ZERO) < 0);
        labelDetalleTitulo.setText(hay ? seleccionado.getNombre() : "Proveedor");
        actualizarSaldoMostrado(seleccionado);
        actualizarEstadoBotonesDetalle();
        cargarCompras(seleccionado);
    }

    /** Muestra el saldo del proveedor al lado del titulo: rojo si la mecanica le debe. */
    private void actualizarSaldoMostrado(Proveedor proveedor) {
        if (proveedor == null) {
            labelSaldoProveedor.setText(" ");
            return;
        }
        BigDecimal saldo = proveedor.getSaldo() == null ? BigDecimal.ZERO : proveedor.getSaldo();
        labelSaldoProveedor.setText("SALDO: Gs. " + new DecimalFormat("#,##0").format(saldo));
        int comparacion = saldo.compareTo(BigDecimal.ZERO);
        if (comparacion > 0) {
            labelSaldoProveedor.setForeground(Paleta.ROJO_ERROR);
        } else if (comparacion < 0) {
            labelSaldoProveedor.setForeground(Paleta.VERDE_EXITO);
        } else {
            labelSaldoProveedor.setForeground(Paleta.GRIS_TEXTO);
        }
    }

    private Proveedor proveedorSeleccionado() {
        int fila = tablaProveedores.getSelectedRow();
        return fila < 0 ? null : modeloProveedores.getProveedor(fila);
    }

    private Compra compraSeleccionada() {
        int fila = tablaCompras.getSelectedRow();
        return fila < 0 ? null : modeloCompras.getCompra(fila);
    }

    /** true cuando la nota seleccionada todavia se puede editar/borrar (no esta pagada). */
    private boolean compraSeleccionadaEditable() {
        Compra compra = compraSeleccionada();
        return compra != null && !modeloCompras.estaPagada(compra);
    }

    private void actualizarEstadoBotonesDetalle() {
        boolean hayProveedor = proveedorSeleccionado() != null;
        botonNuevaCompra.setEnabled(hayProveedor);
    }

    private void actualizarEstadoBotonesCompra() {
        boolean hay = compraSeleccionada() != null;
        botonVerEditarCompra.setEnabled(hay);
        botonEliminarCompra.setEnabled(compraSeleccionadaEditable());
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

    /** Pago al proveedor: descuenta del saldo general, no de una notinha especifica. */
    private void onRegistrarPago() {
        Proveedor seleccionado = proveedorSeleccionado();
        if (seleccionado == null) {
            return;
        }
        PagoProveedorDialog dialogo = new PagoProveedorDialog(ventana(),
                seleccionado.getNombre(), seleccionado.getSaldo());
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
                        chequeController.registrarDeProveedor(seleccionado, dialogo.getNumeroCheque(),
                                dialogo.getBanco(), dialogo.getFechaVencimiento(), dialogo.getValor(),
                                dialogo.getDescuentoValor(), dialogo.getDescripcion());
                    } else {
                        proveedorController.registrarPagamento(seleccionado, dialogo.getValor(),
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
                    JOptionPane.showMessageDialog(ComprasProveedoresPanel.this,
                            error.getMessage(), "No fue posible registrar el pago", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarProveedores(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    /** El proveedor devuelve en efectivo un credito a favor que la mecanica ya tiene (saldo negativo). */
    private void onRetirarSaldo() {
        Proveedor seleccionado = proveedorSeleccionado();
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
                    proveedorController.retirarSaldo(seleccionado, dialogo.getValor(), dialogo.getDescripcion());
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
                            error.getMessage(), "No fue posible retirar el saldo", JOptionPane.ERROR_MESSAGE);
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
        Compra creada = dialogo.getCompraCreada();
        if (creada != null) {
            // Recien abierta la nota, se abre de una el detalle para cargar los items.
            new ItemCompraDialog(ventana(), creada, true).setVisible(true);
        }
        // Recarga tambien los proveedores, porque cargar items cambio el saldo.
        cargarProveedores(campoBusqueda.getText().trim());
    }

    private void onVerEditarCompra() {
        Compra seleccionada = compraSeleccionada();
        if (seleccionada == null) {
            return;
        }
        boolean editable = !modeloCompras.estaPagada(seleccionada);
        new ItemCompraDialog(ventana(), seleccionada, editable).setVisible(true);
        cargarProveedores(campoBusqueda.getText().trim());
    }

    /** Borrar la nota descuenta su valor de la cuenta del proveedor (ver CompraController.eliminar). */
    private void onEliminarCompra() {
        Compra seleccionada = compraSeleccionada();
        if (seleccionada == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Eliminar la nota N° " + seleccionada.getNumero() + " con todos sus items? Su valor (Gs. "
                        + new DecimalFormat("#,##0").format(seleccionada.getValorTotal())
                        + ") se va a descontar de la cuenta del proveedor.",
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
                    compraController.eliminar(seleccionada);
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
                            error.getMessage(), "No fue posible eliminar la nota", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarProveedores(campoBusqueda.getText().trim());
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Colores de tabla

    /**
     * Pinta la columna "Saldo (Gs.)" de los proveedores: rojo cuando la
     * mecanica le debe (saldo positivo) y verde cuando quedo pagado de mas
     * (saldo negativo). Saldo cero queda con el color normal de la tabla.
     */
    private static class ColorSaldoRenderer extends DefaultTableCellRenderer {
        private final TablaProveedoresModel modelo;

        ColorSaldoRenderer(TablaProveedoresModel modelo) {
            this.modelo = modelo;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                boolean conFoco, int fila, int columna) {
            Component componente = super.getTableCellRendererComponent(tabla, valor, seleccionado, conFoco, fila, columna);
            Proveedor proveedor = modelo.getProveedor(tabla.convertRowIndexToModel(fila));
            BigDecimal saldo = proveedor.getSaldo() == null ? BigDecimal.ZERO : proveedor.getSaldo();
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

    private static class TablaProveedoresModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Nombre", "Documento", "Telefono", "Saldo (Gs.)"};
        private static final DecimalFormat FORMATO_SALDO = new DecimalFormat("#,##0");

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
                    return FORMATO_SALDO.format(proveedor.getSaldo() == null ? BigDecimal.ZERO : proveedor.getSaldo());
                default:
                    return "";
            }
        }
    }

    /**
     * Notas del proveedor. El Estado no viene de la base: se calcula con
     * CompraController.calcularPagadas a partir del saldo del proveedor --
     * lo ya pagado va cubriendo las notas de la mas vieja a la mas nueva.
     */
    private static class TablaComprasModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"N°", "Fecha", "Valor Total (Gs.)", "Estado"};
        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");

        private List<Compra> compras = List.of();
        private Set<Long> pagadas = Set.of();

        void setDatos(List<Compra> compras, BigDecimal saldoProveedor) {
            this.compras = compras;
            this.pagadas = CompraController.calcularPagadas(compras, saldoProveedor);
            fireTableDataChanged();
        }

        Compra getCompra(int fila) {
            return compras.get(fila);
        }

        boolean estaPagada(Compra compra) {
            return pagadas.contains(compra.getId());
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
                    return compra.getNumero();
                case 1:
                    return compra.getFecha() == null ? "" : compra.getFecha().format(FORMATO_FECHA);
                case 2:
                    return FORMATO_VALOR.format(compra.getValorTotal() == null ? BigDecimal.ZERO : compra.getValorTotal());
                case 3:
                    return estaPagada(compra) ? EstadoCompra.PAGADA : EstadoCompra.PENDIENTE;
                default:
                    return "";
            }
        }
    }
}
