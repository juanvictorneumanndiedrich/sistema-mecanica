package com.mecanica.view;

import com.mecanica.controller.ItemOrdenServicioController;
import com.mecanica.controller.OrdenDeServicioController;
import com.mecanica.enums.EstadoOrdenServicio;
import com.mecanica.enums.TipoItemOrdenServicio;
import com.mecanica.model.Maquinario;
import com.mecanica.model.ItemOrdenServicio;
import com.mecanica.model.OrdenDeServicio;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialogo modal de detalle de una Orden de Servicio: muestra los datos de
 * cabecera (cliente, maquinario, fecha, estado, problema reportado) y la lista
 * de items (servicios/repuestos), con un formulario simple para agregar un
 * item nuevo y un boton para quitar el item seleccionado. Cada
 * agregar/quitar llama al ItemOrdenServicioController (que ya recalcula el
 * valorTotal de la OS) y despues vuelve a leer la OS y sus items desde la
 * base, para que el total mostrado siempre sea el que quedo persistido.
 *
 * Si la OS ya esta CONCLUIDA o CANCELADA los items quedan de solo lectura
 * -- no tiene sentido seguir agregando repuestos/servicios a una OS cerrada.
 *
 * La impresion (boton IMPRIMIR) genera solo la via fisica de la tabla de
 * items para la firma del cliente -- no se guarda ninguna firma digital.
 */
public class ItemOrdenServicioDialog extends JDialog {

    private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ItemOrdenServicioController itemController = new ItemOrdenServicioController();
    private final OrdenDeServicioController ordenDeServicioController = new OrdenDeServicioController();

    private final TablaItemsModel modeloItems = new TablaItemsModel();
    private final JTable tablaItems = new JTable(modeloItems);

    private final JLabel labelCliente = new JLabel();
    private final JLabel labelMaquinario = new JLabel();
    private final JLabel labelFecha = new JLabel();
    private final JLabel labelEstado = new JLabel();
    private final JTextArea areaProblema = new JTextArea();
    private final JLabel labelValorTotal = new JLabel();

    private final JComboBox<TipoItemOrdenServicio> comboTipo = new JComboBox<>(TipoItemOrdenServicio.values());
    private final JTextField campoDescripcion = new JTextField();
    private final JTextField campoCantidad = new JTextField();
    private final JTextField campoValorUnitario = new JTextField();
    private final JLabel labelErrorItem = new JLabel(" ");

    private final BotonPlano botonAgregar = new BotonPlano("AGREGAR ITEM");
    private final BotonPlano botonQuitar = new BotonPlano("QUITAR ITEM", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());
    private final BotonPlano botonImprimir = new BotonPlano("IMPRIMIR", Paleta.GRIS_TEXTO, Paleta.GRIS_TEXTO.brighter());
    private final BotonPlano botonVolver = new BotonPlano("VOLVER", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);

    private OrdenDeServicio os;

    public ItemOrdenServicioDialog(Window propietario, OrdenDeServicio os) {
        super(propietario, "Orden de Servicio N° " + os.getNumero(), ModalityType.APPLICATION_MODAL);
        this.os = os;
        armarPantalla();
        actualizarEncabezado();
        cargarItems();
    }

    private void armarPantalla() {
        setSize(760, 640);
        setResizable(false);
        setLayout(new BorderLayout(0, 12));
        getContentPane().setBackground(Paleta.GRIS_FONDO);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        add(armarPanelEncabezado(), BorderLayout.NORTH);
        add(armarPanelItems(), BorderLayout.CENTER);
        add(armarPanelInferior(), BorderLayout.SOUTH);

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
        for (JLabel label : new JLabel[]{labelCliente, labelMaquinario, labelFecha, labelEstado}) {
            label.setFont(fonteDado);
            label.setForeground(Paleta.AZUL_OSCURO);
        }

        panel.add(labelCliente, gbc);
        gbc.gridx = 1;
        panel.add(labelMaquinario, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(labelFecha, gbc);
        gbc.gridx = 1;
        panel.add(labelEstado, gbc);

        areaProblema.setEditable(false);
        areaProblema.setLineWrap(true);
        areaProblema.setWrapStyleWord(true);
        areaProblema.setOpaque(false);
        areaProblema.setFont(fonteDado);
        areaProblema.setForeground(Paleta.GRIS_TEXTO);
        areaProblema.setRows(2);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 0, 0);
        panel.add(areaProblema, gbc);

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

        comboTipo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        gbc.weightx = 0.16;
        panel.add(etiquetada("TIPO", comboTipo), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.34;
        panel.add(etiquetada("DESCRIPCION", campoDescripcion), gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.15;
        panel.add(etiquetada("CANTIDAD", campoCantidad), gbc);
        gbc.gridx = 3;
        gbc.weightx = 0.2;
        panel.add(etiquetada("VALOR UNIT. (Gs.)", campoValorUnitario), gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.15;
        gbc.insets = new Insets(18, 0, 0, 0);
        botonAgregar.addActionListener(e -> onAgregarItem());
        panel.add(botonAgregar, gbc);

        labelErrorItem.setForeground(Paleta.ROJO_ERROR);
        labelErrorItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        GridBagConstraints gbcError = new GridBagConstraints();
        gbcError.gridx = 0;
        gbcError.gridy = 2;
        gbcError.gridwidth = 5;
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
        botonImprimir.addActionListener(e -> onImprimir());
        botonVolver.addActionListener(e -> dispose());
        botones.add(botonQuitar);
        botones.add(botonImprimir);
        botones.add(botonVolver);
        panel.add(botones, BorderLayout.EAST);

        return panel;
    }

    // ---------------------------------------------------------------- Estado / encabezado

    private boolean esEditable() {
        return os.getEstado() != EstadoOrdenServicio.CONCLUIDA && os.getEstado() != EstadoOrdenServicio.CANCELADA;
    }

    private void actualizarEncabezado() {
        setTitle("Orden de Servicio N° " + os.getNumero());
        labelCliente.setText("Cliente: " + os.getCliente().getNombre());
        labelMaquinario.setText("Maquinario: " + formatoMaquinario(os.getMaquinario()));
        labelFecha.setText("Fecha apertura: " + os.getFechaApertura().format(FORMATO_FECHA)
                + (os.getFechaCierre() == null ? "" : "   Fecha cierre: " + os.getFechaCierre().format(FORMATO_FECHA)));
        labelEstado.setText("Estado: " + os.getEstado());
        areaProblema.setText(os.getProblemaReportado() == null ? "" : os.getProblemaReportado());
        labelValorTotal.setText("VALOR TOTAL: Gs. " + FORMATO_VALOR.format(os.getValorTotal()));

        boolean editable = esEditable();
        comboTipo.setEnabled(editable);
        campoDescripcion.setEnabled(editable);
        campoCantidad.setEnabled(editable);
        campoValorUnitario.setEnabled(editable);
        botonAgregar.setEnabled(editable);
        actualizarBotonQuitar();
    }

    private void actualizarBotonQuitar() {
        botonQuitar.setEnabled(esEditable() && tablaItems.getSelectedRow() >= 0);
    }

    private String formatoMaquinario(Maquinario maquinario) {
        StringBuilder texto = new StringBuilder(maquinario.getTipo().toString());
        if (maquinario.getIdentificacion() != null && !maquinario.getIdentificacion().isBlank()) {
            texto.append(" - ").append(maquinario.getIdentificacion());
        } else if (maquinario.getMarca() != null && !maquinario.getMarca().isBlank()) {
            texto.append(" - ").append(maquinario.getMarca());
        }
        return texto.toString();
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
        new SwingWorker<List<ItemOrdenServicio>, Void>() {
            Exception error;

            @Override
            protected List<ItemOrdenServicio> doInBackground() {
                try {
                    return itemController.listarPorOrdemDeServico(os);
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

    /** Recarga la OS (para el valorTotal/estado ya persistidos) y sus items, en un solo viaje a la base. */
    private void refrescarTodo() {
        setHabilitado(false);
        new SwingWorker<Object[], Void>() {
            Exception error;

            @Override
            protected Object[] doInBackground() {
                try {
                    OrdenDeServicio osActualizada = ordenDeServicioController.buscarPorId(os.getId());
                    List<ItemOrdenServicio> items = itemController.listarPorOrdemDeServico(osActualizada);
                    return new Object[]{osActualizada, items};
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
                    os = (OrdenDeServicio) resultado[0];
                    modeloItems.setDatos((List<ItemOrdenServicio>) resultado[1]);
                    actualizarEncabezado();
                } catch (Exception e) {
                    mostrarErrorConexion();
                }
            }
        }.execute();
    }

    private ItemOrdenServicio itemSeleccionado() {
        int fila = tablaItems.getSelectedRow();
        return fila < 0 ? null : modeloItems.getItem(fila);
    }

    // ---------------------------------------------------------------- Agregar / quitar item

    private void onAgregarItem() {
        TipoItemOrdenServicio tipo = (TipoItemOrdenServicio) comboTipo.getSelectedItem();
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
        // igual que PagoClienteDialog.
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
                    itemController.agregar(os, tipo, descripcion, cantidad, valorUnitario);
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
        ItemOrdenServicio item = itemSeleccionado();
        if (item == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Quitar el item \"" + item.getDescripcion() + "\" de esta orden de servicio?",
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
                    JOptionPane.showMessageDialog(ItemOrdenServicioDialog.this,
                            error.getMessage(), "No fue posible quitar el item", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                refrescarTodo();
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Impresion (solo via fisica, sin firma digital)

    private void onImprimir() {
        try {
            MessageFormat encabezado = new MessageFormat(
                    "Orden de Servicio N° " + os.getNumero() + " - " + os.getCliente().getNombre());
            tablaItems.print(JTable.PrintMode.FIT_WIDTH, encabezado, null);
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this,
                    "No fue posible imprimir: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------- Modelo de tabla

    private static class TablaItemsModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Tipo", "Descripcion", "Cantidad", "Valor Unit. (Gs.)", "Valor Total (Gs.)"};
        private static final DecimalFormat FORMATO_CANTIDAD = new DecimalFormat("#,##0.###");

        private List<ItemOrdenServicio> items = List.of();

        void setDatos(List<ItemOrdenServicio> items) {
            this.items = items;
            fireTableDataChanged();
        }

        ItemOrdenServicio getItem(int fila) {
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
            ItemOrdenServicio item = items.get(fila);
            switch (columna) {
                case 0:
                    return item.getTipo();
                case 1:
                    return item.getDescripcion();
                case 2:
                    return FORMATO_CANTIDAD.format(item.getCantidad());
                case 3:
                    return FORMATO_VALOR.format(item.getValorUnitario());
                case 4:
                    return FORMATO_VALOR.format(item.getValorTotal());
                default:
                    return "";
            }
        }
    }
}
