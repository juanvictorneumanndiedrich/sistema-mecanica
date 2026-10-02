package com.mecanica.view;

import com.mecanica.controller.ItemOrdenServicioController;
import com.mecanica.controller.OrdenDeServicioController;
import com.mecanica.controller.ReporteController;
import com.mecanica.enums.EstadoOrdenServicio;
import com.mecanica.enums.TipoItemOrdenServicio;
import com.mecanica.model.ItemOrdenServicio;
import com.mecanica.model.Maquinario;
import com.mecanica.model.OrdenDeServicio;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
 * La impresion (boton IMPRIMIR) abre la via impresa de la OS en JasperReports
 * (ver ReporteController.ordenServicio), con lugar para la firma del cliente
 * -- no se guarda ninguna firma digital.
 */
public class ItemOrdenServicioDialog extends JDialog {

    private static final DecimalFormat FORMATO_VALOR = com.mecanica.util.Moneda.nuevoFormatoValor();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ItemOrdenServicioController itemController = new ItemOrdenServicioController();
    private final OrdenDeServicioController ordenDeServicioController = new OrdenDeServicioController();
    private final ReporteController reporteController = new ReporteController();

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
    private final BotonPlano botonEditar = new BotonPlano("EDITAR ITEM");
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

        // Enter en los campos de "nuevo item" agrega el item (no hay un solo boton de
        // "guardar" en este dialogo: agregar es la accion mas comun al escribir y presionar Enter).
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
        configurarColumnas();
        tablaItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarBotonQuitar();
            }
        });
        panel.add(new JScrollPane(tablaItems), BorderLayout.CENTER);
        panel.add(armarFormularioNuevoItem(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Tipo angosto (abreviado), descripcion ancha y COMPLETA: si no entra en
     * una linea, el renglon crece y el texto sigue en la linea de abajo.
     */
    private void configurarColumnas() {
        TableColumnModel columnas = tablaItems.getColumnModel();
        int[] anchos = {60, 420, 75, 120, 130};
        for (int i = 0; i < anchos.length; i++) {
            columnas.getColumn(i).setPreferredWidth(anchos[i]);
        }
        columnas.getColumn(0).setMaxWidth(80);
        columnas.getColumn(2).setMaxWidth(100);
        columnas.getColumn(1).setCellRenderer(new DescripcionCompletaRenderer());
        // recalcular la altura de los renglones cuando cambian los items o el ancho de la tabla
        modeloItems.addTableModelListener(e -> SwingUtilities.invokeLater(this::ajustarAlturaRenglones));
        tablaItems.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ajustarAlturaRenglones();
            }
        });
    }

    /** Cada renglon toma la altura que necesita la descripcion completa (minimo 26). */
    private void ajustarAlturaRenglones() {
        for (int fila = 0; fila < tablaItems.getRowCount(); fila++) {
            Component celda = tablaItems.prepareRenderer(tablaItems.getCellRenderer(fila, 1), fila, 1);
            int alto = Math.max(26, celda.getPreferredSize().height + 4);
            if (tablaItems.getRowHeight(fila) != alto) {
                tablaItems.setRowHeight(fila, alto);
            }
        }
    }

    /** Muestra la descripcion entera, cortando en varias lineas en vez de terminar con "...". */
    private static class DescripcionCompletaRenderer extends JTextArea implements TableCellRenderer {
        DescripcionCompletaRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                boolean conFoco, int fila, int columna) {
            setText(valor == null ? "" : valor.toString());
            setFont(tabla.getFont());
            setForeground(seleccionado ? tabla.getSelectionForeground() : tabla.getForeground());
            setBackground(seleccionado ? tabla.getSelectionBackground() : tabla.getBackground());
            int ancho = tabla.getColumnModel().getColumn(columna).getWidth();
            setSize(ancho, Short.MAX_VALUE); // para que el alto preferido tenga en cuenta el corte de lineas
            return this;
        }
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
        Ayuda.tooltip(comboTipo, "SERVICIO = mano de obra del taller. REPUESTO = pieza o material. VIAJE = se cobra al cliente, pero esa plata no entra en Financiero (va en su propia pestaña).");
        campoDescripcion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoCantidad.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoValorUnitario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoCantidad.setText("1"); // cantidad por defecto, se puede cambiar
        Ayuda.tooltip(campoValorUnitario, "Puede quedar vacio (item sin valor). La OS no se puede cerrar "
                + "hasta ponerle valor o quitarlo.");
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
        botonEditar.addActionListener(e -> onEditarItem());
        Ayuda.tooltip(botonEditar, "Editar el item seleccionado (solo con la OS abierta).");
        botones.add(botonEditar);
        botonQuitar.addActionListener(e -> onQuitarItem());
        Ayuda.tooltip(botonQuitar, "Quitar de la OS el item seleccionado en la lista.");
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
        boolean haySeleccion = esEditable() && tablaItems.getSelectedRow() >= 0;
        botonQuitar.setEnabled(haySeleccion);
        botonEditar.setEnabled(haySeleccion);
    }

    private String formatoMaquinario(Maquinario maquinario) {
        if (maquinario == null) {
            return "Servicio general (sin maquinaria)";
        }
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

        BigDecimal valorUnitario = leerValorUnitario(campoValorUnitario.getText());
        if (valorUnitario == null) {
            labelErrorItem.setText("Ingrese un valor unitario numerico valido (o dejelo vacio).");
            return;
        }
        if (valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            labelErrorItem.setText("El valor unitario no puede ser negativo.");
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
                campoCantidad.setText("1");
                campoValorUnitario.setText("");
                refrescarTodo();
            }
        }.execute();
    }

    /**
     * Lee el valor unitario aceptando "150000" o "150.000" (separador de miles
     * paraguayo). Vacio = cero (item sin valor todavia). Null si es invalido.
     */
    private static BigDecimal leerValorUnitario(String textoIngresado) {
        String texto = textoIngresado.trim().replace(".", "").replace(",", ".");
        if (texto.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Abre el item seleccionado en un formulario para cambiar tipo, descripcion, cantidad y valor. */
    private void onEditarItem() {
        ItemOrdenServicio item = itemSeleccionado();
        if (item == null || !esEditable()) {
            return;
        }
        JComboBox<TipoItemOrdenServicio> tipoEdicion = new JComboBox<>(TipoItemOrdenServicio.values());
        tipoEdicion.setSelectedItem(item.getTipo());
        JTextField descripcionEdicion = new JTextField(item.getDescripcion(), 30);
        // sin separador de miles: "1500" y no "1.500" (que al leer se tomaria como 1,5)
        JTextField cantidadEdicion = new JTextField(item.getCantidad().stripTrailingZeros().toPlainString());
        JTextField valorEdicion = new JTextField(item.getValorUnitario() == null
                || item.getValorUnitario().signum() == 0 ? "" : FORMATO_VALOR.format(item.getValorUnitario()));
        JLabel errorEdicion = new JLabel(" ");
        errorEdicion.setForeground(Paleta.ROJO_ERROR);

        JPanel formulario = new JPanel(new GridLayout(0, 1, 0, 4));
        formulario.add(new JLabel("TIPO"));
        formulario.add(tipoEdicion);
        formulario.add(new JLabel("DESCRIPCION"));
        formulario.add(descripcionEdicion);
        formulario.add(new JLabel("CANTIDAD"));
        formulario.add(cantidadEdicion);
        formulario.add(new JLabel("VALOR UNIT. (Gs.) - vacio = sin valor"));
        formulario.add(valorEdicion);
        formulario.add(errorEdicion);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(this, formulario, "Editar item",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }
            String descripcion = descripcionEdicion.getText().trim();
            if (descripcion.isEmpty()) {
                errorEdicion.setText("La descripcion es obligatoria.");
                continue;
            }
            BigDecimal cantidad;
            try {
                cantidad = new BigDecimal(cantidadEdicion.getText().trim().replace(",", "."));
            } catch (NumberFormatException e) {
                errorEdicion.setText("Ingrese una cantidad numerica valida.");
                continue;
            }
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                errorEdicion.setText("La cantidad debe ser mayor que cero.");
                continue;
            }
            BigDecimal valorUnitario = leerValorUnitario(valorEdicion.getText());
            if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
                errorEdicion.setText("Ingrese un valor unitario valido (o dejelo vacio).");
                continue;
            }
            guardarEdicion(item, (TipoItemOrdenServicio) tipoEdicion.getSelectedItem(), descripcion, cantidad,
                    valorUnitario);
            return;
        }
    }

    private void guardarEdicion(ItemOrdenServicio item, TipoItemOrdenServicio tipo, String descripcion,
                                BigDecimal cantidad, BigDecimal valorUnitario) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    itemController.editar(item, tipo, descripcion, cantidad, valorUnitario);
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
                            error.getMessage(), "No fue posible editar el item", JOptionPane.ERROR_MESSAGE);
                    return;
                }
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
        OrdenDeServicio actual = os;
        VisorReporte.mostrar(this, "Orden de Servicio N° " + actual.getNumero(),
                () -> reporteController.ordenServicio(actual));
    }

    // ---------------------------------------------------------------- Modelo de tabla

    private static class TablaItemsModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Tipo", "Descripcion", "Cantidad", "Valor Unit. (Gs.)", "Valor Total (Gs.)"};
        private static final DecimalFormat FORMATO_CANTIDAD = com.mecanica.util.Moneda.nuevoFormatoCantidad();

        private List<ItemOrdenServicio> items = List.of();

        void setDatos(List<ItemOrdenServicio> items) {
            this.items = items;
            fireTableDataChanged();
        }

        ItemOrdenServicio getItem(int fila) {
            return items.get(fila);
        }

        /** Item guardado sin precio todavia (valor cero): la OS no se puede cerrar asi. */
        private static boolean sinValor(ItemOrdenServicio item) {
            return item.getValorUnitario() == null || item.getValorUnitario().signum() == 0;
        }

        /** Tipo corto para la columna angosta: Serv. / Rep. / Viaje. */
        private static String tipoAbreviado(TipoItemOrdenServicio tipo) {
            if (tipo == null) {
                return "";
            }
            switch (tipo) {
                case SERVICIO:
                    return "Serv.";
                case REPUESTO:
                    return "Rep.";
                case VIAJE:
                    return "Viaje";
                default:
                    return tipo.name();
            }
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
                    return tipoAbreviado(item.getTipo());
                case 1:
                    return item.getDescripcion();
                case 2:
                    return FORMATO_CANTIDAD.format(item.getCantidad());
                case 3:
                    return sinValor(item) ? "Sin valor" : FORMATO_VALOR.format(item.getValorUnitario());
                case 4:
                    return sinValor(item) ? "Sin valor" : FORMATO_VALOR.format(item.getValorTotal());
                default:
                    return "";
            }
        }
    }
}
