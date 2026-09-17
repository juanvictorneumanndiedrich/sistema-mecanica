package com.mecanica.view;

import com.mecanica.controller.CompraController;
import com.mecanica.controller.ItemCompraController;
import com.mecanica.model.Compra;
import com.mecanica.model.ItemCompra;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialogo modal de detalle de una Compra ("notinha" del proveedor): muestra
 * los datos de cabecera (N° de nota, proveedor, fecha) y la lista de items
 * (cada cosa comprada, con cantidad y precio), con un formulario simple
 * para agregar un item nuevo y un boton para quitar el seleccionado. Cada
 * agregar/quitar llama al ItemCompraController (que ya ajusta el valorTotal
 * de la nota y el saldo del proveedor) y despues vuelve a leer la nota y
 * sus items desde la base, para que el total mostrado siempre sea el
 * persistido -- mismo patron de ItemOrdenServicioDialog.
 *
 * La nota se puede editar siempre mientras siga PENDIENTE, incluso mucho
 * despues de creada. Cuando ya quedo PAGADA (el pago al proveedor ya
 * cubrio su valor) los items quedan de solo lectura: el dialogo se abre en
 * modo consulta.
 */
public class ItemCompraDialog extends JDialog {

    private static final DecimalFormat FORMATO_VALOR = com.mecanica.util.Moneda.nuevoFormatoValor();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ItemCompraController itemController = new ItemCompraController();
    private final CompraController compraController = new CompraController();

    private final TablaItemsModel modeloItems = new TablaItemsModel();
    private final JTable tablaItems = new JTable(modeloItems);

    private final JLabel labelProveedor = new JLabel();
    private final JLabel labelFecha = new JLabel();
    private final JLabel labelEstado = new JLabel();
    private final JLabel labelValorTotal = new JLabel();

    private final JTextField campoDescripcion = new JTextField();
    private final JTextField campoCantidad = new JTextField();
    private final JTextField campoValorUnitario = new JTextField();
    private final JLabel labelErrorItem = new JLabel(" ");

    private final BotonPlano botonAgregar = new BotonPlano("AGREGAR ITEM");
    private final BotonPlano botonQuitar = new BotonPlano("QUITAR ITEM", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());
    private final BotonPlano botonVolver = new BotonPlano("VOLVER", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);

    /** false cuando la nota ya fue pagada: se abre solo para consultar. */
    private final boolean editable;

    private Compra compra;

    public ItemCompraDialog(Window propietario, Compra compra, boolean editable) {
        super(propietario, "Compra N° " + compra.getNumero() + " - " + compra.getProveedor().getNombre(),
                ModalityType.APPLICATION_MODAL);
        this.compra = compra;
        this.editable = editable;
        armarPantalla();
        actualizarEncabezado();
        cargarItems();
    }

    private void armarPantalla() {
        setSize(700, 580);
        setResizable(false);
        setLayout(new BorderLayout(0, 12));
        getContentPane().setBackground(Paleta.GRIS_FONDO);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(armarPanelEncabezado(), BorderLayout.NORTH);
        add(armarPanelItems(), BorderLayout.CENTER);
        add(armarPanelInferior(), BorderLayout.SOUTH);

        // Enter en los campos de "nuevo item" agrega el item.
        getRootPane().setDefaultButton(botonAgregar);

        setLocationRelativeTo(getOwner());
    }

    private JComponent armarPanelEncabezado() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 0, 12, 0)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 4, 0);

        Font fonteDado = new Font("Segoe UI", Font.PLAIN, 13);
        for (JLabel label : new JLabel[]{labelProveedor, labelFecha, labelEstado}) {
            label.setFont(fonteDado);
            label.setForeground(Paleta.AZUL_OSCURO);
        }

        panel.add(labelProveedor, gbc);
        gbc.gridx = 1;
        panel.add(labelFecha, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(labelEstado, gbc);

        return panel;
    }

    private JComponent armarPanelItems() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        estilizarTabla(tablaItems);
        tablaItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarBotonQuitar();
            }
        });
        panel.add(new JScrollPane(tablaItems), BorderLayout.CENTER);
        panel.add(armarFormularioNuevoItem(), BorderLayout.SOUTH);

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

    private JComponent armarFormularioNuevoItem() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 0, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        campoDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoCantidad.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoValorUnitario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        for (JTextField campo : new JTextField[]{campoDescripcion, campoCantidad, campoValorUnitario}) {
            campo.setPreferredSize(new Dimension(0, 30));
            campo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)));
        }

        gbc.gridx = 0;
        gbc.weightx = 0.45;
        panel.add(etiquetada("DESCRIPCION", campoDescripcion), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.2;
        panel.add(etiquetada("CANTIDAD", campoCantidad), gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.2;
        panel.add(etiquetada("VALOR UNIT. (Gs.)", campoValorUnitario), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.15;
        gbc.insets = new Insets(18, 0, 0, 0);
        botonAgregar.addActionListener(e -> onAgregarItem());
        panel.add(botonAgregar, gbc);

        labelErrorItem.setForeground(Paleta.ROJO_ERROR);
        labelErrorItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        GridBagConstraints gbcError = new GridBagConstraints();
        gbcError.gridx = 0;
        gbcError.gridy = 2;
        gbcError.gridwidth = 4;
        gbcError.anchor = GridBagConstraints.WEST;
        gbcError.insets = new Insets(6, 0, 0, 0);
        panel.add(labelErrorItem, gbcError);

        return panel;
    }

    private JComponent etiquetada(String etiqueta, JComponent campo) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setOpaque(false);
        JLabel label = new JLabel(etiqueta);
        label.setForeground(Paleta.GRIS_TEXTO);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        panel.add(label, BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        return panel;
    }

    private JComponent armarPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        labelValorTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        labelValorTotal.setForeground(Paleta.AZUL_OSCURO);
        panel.add(labelValorTotal, BorderLayout.WEST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botonQuitar.addActionListener(e -> onQuitarItem());
        botonVolver.addActionListener(e -> dispose());
        botones.add(botonQuitar);
        botones.add(botonVolver);
        panel.add(botones, BorderLayout.EAST);

        return panel;
    }

    // ---------------------------------------------------------------- Encabezado

    private void actualizarEncabezado() {
        setTitle("Compra N° " + compra.getNumero() + " - " + compra.getProveedor().getNombre());
        labelProveedor.setText("Nota N° " + compra.getNumero() + "   Proveedor: " + compra.getProveedor().getNombre());
        labelFecha.setText("Fecha: " + compra.getFecha().format(FORMATO_FECHA));
        labelEstado.setText(editable
                ? "Estado: PENDIENTE"
                : "Estado: PAGADA (solo consulta, ya no se puede modificar)");
        labelEstado.setForeground(editable ? Paleta.AZUL_OSCURO : Paleta.GRIS_TEXTO);
        labelValorTotal.setText("VALOR TOTAL: Gs. " + FORMATO_VALOR.format(compra.getValorTotal()));

        campoDescripcion.setEnabled(editable);
        campoCantidad.setEnabled(editable);
        campoValorUnitario.setEnabled(editable);
        botonAgregar.setEnabled(editable);
        actualizarBotonQuitar();
    }

    private void actualizarBotonQuitar() {
        botonQuitar.setEnabled(editable && tablaItems.getSelectedRow() >= 0);
    }

    private void setHabilitado(boolean habilitado) {
        setCursor(habilitado ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private void mostrarErrorConexion() {
        JOptionPane.showMessageDialog(this,
                "No fue posible conectar a la base de datos.",
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------------------------------------------------------- Carga / recarga

    private void cargarItems() {
        setHabilitado(false);
        new SwingWorker<List<ItemCompra>, Void>() {
            Exception error;

            @Override
            protected List<ItemCompra> doInBackground() {
                try {
                    return itemController.listarPorCompra(compra);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    modeloItems.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                actualizarBotonQuitar();
            }
        }.execute();
    }

    /** Recarga la nota (para el valorTotal ya persistido) y sus items, en un solo viaje a la base. */
    private void refrescarTodo() {
        setHabilitado(false);
        new SwingWorker<Object[], Void>() {
            Exception error;

            @Override
            protected Object[] doInBackground() {
                try {
                    Compra compraActualizada = compraController.buscarPorId(compra.getId());
                    List<ItemCompra> items = itemController.listarPorCompra(compraActualizada);
                    return new Object[]{compraActualizada, items};
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    mostrarErrorConexion();
                    return;
                }
                try {
                    Object[] resultado = get();
                    compra = (Compra) resultado[0];
                    modeloItems.setDatos((List<ItemCompra>) resultado[1]);
                    actualizarEncabezado();
                } catch (Exception e) {
                    mostrarErrorConexion();
                }
            }
        }.execute();
    }

    private ItemCompra itemSeleccionado() {
        int fila = tablaItems.getSelectedRow();
        return fila < 0 ? null : modeloItems.getItem(fila);
    }

    // ---------------------------------------------------------------- Agregar / quitar item

    private void onAgregarItem() {
        String descripcion = campoDescripcion.getText().trim();
        if (descripcion.isEmpty()) {
            labelErrorItem.setText("La descripcion es obligatoria.");
            return;
        }

        BigDecimal cantidad;
        try {
            cantidad = new BigDecimal(campoCantidad.getText().trim().replace(",", "."));
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                labelErrorItem.setText("La cantidad debe ser mayor que cero.");
                return;
            }
        } catch (NumberFormatException e) {
            labelErrorItem.setText("Ingrese una cantidad numerica valida.");
            return;
        }

        // Acepta tanto "150000" como "150.000" (separador de miles paraguayo),
        // igual que ItemOrdenServicioDialog.
        BigDecimal valorUnitario;
        try {
            String texto = campoValorUnitario.getText().trim().replace(".", "").replace(",", ".");
            valorUnitario = new BigDecimal(texto);
            if (valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
                labelErrorItem.setText("El valor unitario no puede ser negativo.");
                return;
            }
        } catch (NumberFormatException e) {
            labelErrorItem.setText("Ingrese un valor unitario numerico valido.");
            return;
        }

        labelErrorItem.setText(" ");
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    itemController.agregar(compra, descripcion, cantidad, valorUnitario);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    labelErrorItem.setText(error.getMessage());
                    return;
                }
                campoDescripcion.setText("");
                campoCantidad.setText("");
                campoValorUnitario.setText("");
                refrescarTodo();
            }
        }.execute();
    }

    private void onQuitarItem() {
        ItemCompra item = itemSeleccionado();
        if (item == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Quitar el item \"" + item.getDescripcion() + "\" de esta compra?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    itemController.quitar(item);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ItemCompraDialog.this,
                            error.getMessage(), "No fue posible quitar el item", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                refrescarTodo();
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Modelo de tabla

    private static class TablaItemsModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Descripcion", "Cantidad", "Valor Unit. (Gs.)", "Valor Total (Gs.)"};
        private static final DecimalFormat FORMATO_CANTIDAD = com.mecanica.util.Moneda.nuevoFormatoCantidad();

        private List<ItemCompra> items = List.of();

        void setDatos(List<ItemCompra> items) {
            this.items = items;
            fireTableDataChanged();
        }

        ItemCompra getItem(int fila) {
            return items.get(fila);
        }

        @Override
        public int getRowCount() {
            return items.size();
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
            ItemCompra item = items.get(fila);
            switch (columna) {
                case 0:
                    return item.getDescripcion();
                case 1:
                    return FORMATO_CANTIDAD.format(item.getCantidad());
                case 2:
                    return FORMATO_VALOR.format(item.getValorUnitario());
                case 3:
                    return FORMATO_VALOR.format(item.getValorTotal());
                default:
                    return "";
            }
        }
    }
}
