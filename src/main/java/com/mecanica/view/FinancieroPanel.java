package com.mecanica.view;

import com.mecanica.controller.ChequePreDatadoController;
import com.mecanica.controller.MovimientoFinancieroController;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.ChequePreDatado;
import com.mecanica.model.MovimientoFinanciero;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla real del area "Financiero": extracto de MovimientoFinanciero
 * filtrado por periodo (fecha desde/hasta) y, opcionalmente, por tipo
 * (Ingreso/Egreso), con el saldo del periodo (ver
 * MovimientoFinancieroController.calcularSaldoPeriodo).
 *
 * Tiene dos pestañas: "Movimientos" (el extracto de siempre, mayormente de
 * solo lectura -- los movimientos los generan otros flujos del sistema:
 * pago de cliente, compra a proveedor, retiro de empleado, o un cheque
 * pre-datado que ya vencio y fue confirmado) y "Cheques Pendientes" (los
 * cheques pre-datados que todavia no vencieron/no fueron confirmados -- ver
 * ChequePreDatadoController). La unica accion de escritura en la pestaña de
 * Movimientos es "Nuevo Movimiento", que abre MovimientoManualDialog para
 * registrar un movimiento manual (siempre con categoria OTRO).
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en ClientesMaquinariosPanel.
 */
public class FinancieroPanel extends JPanel implements PanelActualizable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat FORMATO_SALDO = new DecimalFormat("#,##0");
    private static final String TODOS_LOS_TIPOS = "(todos)";
    private static final String TIPO_INGRESO = "Ingreso";
    private static final String TIPO_EGRESO = "Egreso";

    private final MovimientoFinancieroController movimientoFinancieroController = new MovimientoFinancieroController();
    private final ChequePreDatadoController chequeController = new ChequePreDatadoController();

    private final TablaMovimientosModel modeloMovimientos = new TablaMovimientosModel();
    private final JTable tablaMovimientos = new JTable(modeloMovimientos);

    private final JTextField campoFechaInicio = new JTextField();
    private final JTextField campoFechaFin = new JTextField();
    private final JComboBox<String> comboTipo = new JComboBox<>();
    private final JLabel labelError = new JLabel(" ");
    private final JLabel labelSaldo = new JLabel(" ");

    private final TablaChequesPendientesModel modeloCheques = new TablaChequesPendientesModel();
    private final JTable tablaCheques = new JTable(modeloCheques);
    private final BotonPlano botonConfirmarCheque = new BotonPlano("CONFIRMAR CHEQUE (VENCIDO)");
    private final JLabel labelErrorCheques = new JLabel(" ");

    public FinancieroPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        LocalDate hoy = LocalDate.now();
        campoFechaInicio.setText(hoy.withDayOfMonth(1).format(FORMATO_FECHA));
        campoFechaFin.setText(hoy.format(FORMATO_FECHA));

        comboTipo.addItem(TODOS_LOS_TIPOS);
        comboTipo.addItem(TIPO_INGRESO);
        comboTipo.addItem(TIPO_EGRESO);

        JLabel titulo = new JLabel("Financiero");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        add(titulo, BorderLayout.NORTH);

        JTabbedPane pestañas = new JTabbedPane();
        pestañas.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pestañas.addTab("Movimientos", armarPestañaMovimientos());
        pestañas.addTab("Cheques Pendientes", armarPestañaCheques());
        add(pestañas, BorderLayout.CENTER);

        buscarMovimientos();
        cargarChequesPendientes();
    }

    /** Vuelve a buscar los movimientos y los cheques pendientes al entrar en esta area. */
    @Override
    public void actualizar() {
        buscarMovimientos();
        cargarChequesPendientes();
    }

    // ---------------------------------------------------------------- Pestaña Movimientos

    private JComponent armarPestañaMovimientos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        panel.add(armarEncabezadoMovimientos(), BorderLayout.NORTH);
        panel.add(armarCentroMovimientos(), BorderLayout.CENTER);
        panel.add(armarPieMovimientos(), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent armarEncabezadoMovimientos() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);

        BotonPlano botonNuevoMovimiento = new BotonPlano("NUEVO MOVIMIENTO");
        botonNuevoMovimiento.addActionListener(e -> onNuevoMovimiento());
        encabezado.add(botonNuevoMovimiento, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        panel.add(armarFiltros(), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent armarFiltros() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);

        panel.add(armarCampoFiltro("DESDE", campoFechaInicio, 100));
        panel.add(armarCampoFiltro("HASTA", campoFechaFin, 100));

        JPanel panelTipo = new JPanel(new BorderLayout(0, 4));
        panelTipo.setOpaque(false);
        JLabel labelTipo = new JLabel("TIPO");
        labelTipo.setForeground(Paleta.GRIS_TEXTO);
        labelTipo.setFont(new Font("Segoe UI", Font.BOLD, 10));
        comboTipo.setPreferredSize(new Dimension(150, 32));
        comboTipo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panelTipo.add(labelTipo, BorderLayout.NORTH);
        panelTipo.add(comboTipo, BorderLayout.CENTER);
        panel.add(panelTipo);

        BotonPlano botonFiltrar = new BotonPlano("FILTRAR");
        botonFiltrar.addActionListener(e -> buscarMovimientos());
        JPanel panelBoton = new JPanel(new BorderLayout());
        panelBoton.setOpaque(false);
        // Espacio arriba para alinear con los campos, que tienen una etiqueta encima.
        panelBoton.add(Box.createVerticalStrut(18), BorderLayout.NORTH);
        panelBoton.add(botonFiltrar, BorderLayout.CENTER);
        panel.add(panelBoton);

        return panel;
    }

    private JComponent armarCampoFiltro(String etiqueta, JTextField campo, int ancho) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);

        JLabel label = new JLabel(etiqueta);
        label.setForeground(Paleta.GRIS_TEXTO);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));

        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setPreferredSize(new Dimension(ancho, 32));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        campo.addActionListener(e -> buscarMovimientos());

        panel.add(label, BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        return panel;
    }

    private JComponent armarCentroMovimientos() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        estilizarTabla(tablaMovimientos);
        tablaMovimientos.getColumnModel().getColumn(4).setCellRenderer(new ColorValorRenderer(modeloMovimientos));
        panel.add(new JScrollPane(tablaMovimientos), BorderLayout.CENTER);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(labelError, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent armarPieMovimientos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        labelSaldo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        labelSaldo.setForeground(Paleta.AZUL_OSCURO);
        panel.add(labelSaldo, BorderLayout.EAST);
        return panel;
    }

    // ---------------------------------------------------------------- Pestaña Cheques Pendientes

    private JComponent armarPestañaCheques() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel explicacion = new JLabel(
                "Cheques pre-datados (de clientes o para proveedores) que ya descontaron el saldo, "
                        + "pero todavia no generaron el movimiento en Financiero -- eso pasa recien al confirmar.");
        explicacion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        explicacion.setForeground(Paleta.GRIS_TEXTO);
        panel.add(explicacion, BorderLayout.NORTH);

        estilizarTabla(tablaCheques);
        tablaCheques.getColumnModel().getColumn(4).setCellRenderer(new ColorVencimientoRenderer(modeloCheques));
        panel.add(new JScrollPane(tablaCheques), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        labelErrorCheques.setForeground(Paleta.ROJO_ERROR);
        labelErrorCheques.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pie.add(labelErrorCheques, BorderLayout.WEST);

        botonConfirmarCheque.addActionListener(e -> onConfirmarCheque());
        pie.add(botonConfirmarCheque, BorderLayout.EAST);
        panel.add(pie, BorderLayout.SOUTH);

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

    // ---------------------------------------------------------------- Carga de datos (Movimientos)

    /** Lee los filtros de pantalla, valida las fechas y recarga tabla + saldo. */
    private void buscarMovimientos() {
        labelError.setText(" ");

        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = LocalDate.parse(campoFechaInicio.getText().trim(), FORMATO_FECHA);
            fin = LocalDate.parse(campoFechaFin.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            labelError.setText("Ingrese las fechas en formato dd/mm/aaaa.");
            return;
        }
        if (inicio.isAfter(fin)) {
            labelError.setText("La fecha DESDE no puede ser posterior a la fecha HASTA.");
            return;
        }

        String tipoSeleccionado = (String) comboTipo.getSelectedItem();
        TipoMovimientoFinanciero tipo;
        if (TIPO_INGRESO.equals(tipoSeleccionado)) {
            tipo = TipoMovimientoFinanciero.ENTRADA;
        } else if (TIPO_EGRESO.equals(tipoSeleccionado)) {
            tipo = TipoMovimientoFinanciero.SALIDA;
        } else {
            tipo = null;
        }

        LocalDate inicioFinal = inicio;
        LocalDate finFinal = fin;
        TipoMovimientoFinanciero tipoFinal = tipo;

        setHabilitado(false);
        new SwingWorker<ResultadoFiltro, Void>() {
            Exception error;

            @Override
            protected ResultadoFiltro doInBackground() {
                try {
                    List<MovimientoFinanciero> movimientos =
                            movimientoFinancieroController.listarPorPeriodo(inicioFinal, finFinal);
                    if (tipoFinal != null) {
                        List<MovimientoFinanciero> filtrados = new ArrayList<>();
                        for (MovimientoFinanciero m : movimientos) {
                            if (m.getTipo() == tipoFinal) {
                                filtrados.add(m);
                            }
                        }
                        movimientos = filtrados;
                    }
                    // El saldo es siempre del periodo completo (no cambia con el filtro de tipo).
                    BigDecimal saldo = movimientoFinancieroController.calcularSaldoPeriodo(inicioFinal, finFinal);
                    return new ResultadoFiltro(movimientos, saldo);
                } catch (Exception e) {
                    error = e;
                    return new ResultadoFiltro(List.of(), BigDecimal.ZERO);
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                ResultadoFiltro resultado;
                try {
                    resultado = get();
                } catch (Exception e) {
                    error = e;
                    resultado = new ResultadoFiltro(List.of(), BigDecimal.ZERO);
                }
                if (error != null) {
                    mostrarErrorConexion();
                    return;
                }
                modeloMovimientos.setDatos(resultado.movimientos);
                labelSaldo.setText("SALDO DEL PERIODO: Gs. " + FORMATO_SALDO.format(resultado.saldo));
            }
        }.execute();
    }

    private void onNuevoMovimiento() {
        MovimientoManualDialog dialogo = new MovimientoManualDialog(ventana());
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
                    movimientoFinancieroController.registrarMovimientoManual(
                            dialogo.getTipo(), dialogo.getValor(), dialogo.getFecha(), dialogo.getDescripcion());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(FinancieroPanel.this,
                            error.getMessage(), "No fue posible registrar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                buscarMovimientos();
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Carga de datos (Cheques)

    private void cargarChequesPendientes() {
        labelErrorCheques.setText(" ");
        setHabilitado(false);
        new SwingWorker<List<ChequePreDatado>, Void>() {
            Exception error;

            @Override
            protected List<ChequePreDatado> doInBackground() {
                try {
                    return chequeController.listarPendientes();
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                List<ChequePreDatado> resultado;
                try {
                    resultado = get();
                } catch (Exception e) {
                    error = e;
                    resultado = List.of();
                }
                if (error != null) {
                    mostrarErrorConexion();
                    return;
                }
                modeloCheques.setDatos(resultado);
            }
        }.execute();
    }

    private void onConfirmarCheque() {
        labelErrorCheques.setText(" ");
        int filaSeleccionada = tablaCheques.getSelectedRow();
        if (filaSeleccionada < 0) {
            labelErrorCheques.setText("Seleccione un cheque en la lista.");
            return;
        }
        ChequePreDatado seleccionado = modeloCheques.getCheque(tablaCheques.convertRowIndexToModel(filaSeleccionada));

        int opcion = JOptionPane.showConfirmDialog(this,
                "Confirmar que el cheque de Gs. " + new DecimalFormat("#,##0").format(seleccionado.getValor())
                        + " ya vencio y se compenso? Esto va a generar el movimiento correspondiente en Financiero.",
                "Confirmar cheque", JOptionPane.YES_NO_OPTION);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    chequeController.confirmar(seleccionado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(FinancieroPanel.this,
                            error.getMessage(), "No fue posible confirmar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarChequesPendientes();
                buscarMovimientos();
            }
        }.execute();
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

    // ---------------------------------------------------------------- Auxiliares

    /** Resultado de una busqueda: la lista de movimientos ya filtrada y el saldo del periodo. */
    private static class ResultadoFiltro {
        final List<MovimientoFinanciero> movimientos;
        final BigDecimal saldo;

        ResultadoFiltro(List<MovimientoFinanciero> movimientos, BigDecimal saldo) {
            this.movimientos = movimientos;
            this.saldo = saldo;
        }
    }

    // ---------------------------------------------------------------- Colores de tabla

    /**
     * Pinta la columna "Valor (Gs.)": verde para entradas (ingresos) y rojo
     * para salidas (gastos), sin importar si la fila esta seleccionada.
     */
    private static class ColorValorRenderer extends DefaultTableCellRenderer {
        private final TablaMovimientosModel modelo;

        ColorValorRenderer(TablaMovimientosModel modelo) {
            this.modelo = modelo;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                boolean conFoco, int fila, int columna) {
            Component componente = super.getTableCellRendererComponent(tabla, valor, seleccionado, conFoco, fila, columna);
            MovimientoFinanciero movimiento = modelo.getMovimiento(tabla.convertRowIndexToModel(fila));
            componente.setForeground(movimiento.getTipo() == TipoMovimientoFinanciero.ENTRADA
                    ? Paleta.VERDE_EXITO
                    : Paleta.ROJO_ERROR);
            return componente;
        }
    }

    /** Pinta la columna "Vencimiento": rojo si ya vencio (listo para confirmar), color normal si todavia no. */
    private static class ColorVencimientoRenderer extends DefaultTableCellRenderer {
        private final TablaChequesPendientesModel modelo;

        ColorVencimientoRenderer(TablaChequesPendientesModel modelo) {
            this.modelo = modelo;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                boolean conFoco, int fila, int columna) {
            Component componente = super.getTableCellRendererComponent(tabla, valor, seleccionado, conFoco, fila, columna);
            ChequePreDatado cheque = modelo.getCheque(tabla.convertRowIndexToModel(fila));
            boolean vencido = cheque.getFechaVencimiento() != null
                    && !cheque.getFechaVencimiento().isAfter(LocalDate.now());
            componente.setForeground(vencido ? Paleta.ROJO_ERROR : Paleta.GRIS_TEXTO);
            return componente;
        }
    }

    private static class TablaMovimientosModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Tipo", "Categoria", "Descripcion", "Valor (Gs.)"};
        private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");
        private static final DateTimeFormatter FORMATO_FECHA_TABLA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private List<MovimientoFinanciero> movimientos = List.of();

        void setDatos(List<MovimientoFinanciero> movimientos) {
            this.movimientos = movimientos;
            fireTableDataChanged();
        }

        MovimientoFinanciero getMovimiento(int fila) {
            return movimientos.get(fila);
        }

        @Override
        public int getRowCount() {
            return movimientos.size();
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
            MovimientoFinanciero movimiento = movimientos.get(fila);
            switch (columna) {
                case 0:
                    return movimiento.getFecha() == null ? "" : movimiento.getFecha().format(FORMATO_FECHA_TABLA);
                case 1:
                    return movimiento.getTipo() == TipoMovimientoFinanciero.ENTRADA ? "Ingreso" : "Egreso";
                case 2:
                    return movimiento.getCategoria() == null ? "" : movimiento.getCategoria().name().replace("_", " ");
                case 3:
                    return movimiento.getDescripcion() == null ? "" : movimiento.getDescripcion();
                case 4:
                    String valorFormateado = FORMATO_VALOR.format(
                            movimiento.getValor() == null ? BigDecimal.ZERO : movimiento.getValor());
                    return movimiento.getTipo() == TipoMovimientoFinanciero.SALIDA
                            ? "-" + valorFormateado
                            : valorFormateado;
                default:
                    return "";
            }
        }
    }

    /** Tabla de la pestaña "Cheques Pendientes": mezcla cheques de clientes y de proveedores. */
    private static class TablaChequesPendientesModel extends AbstractTableModel {
        private static final String[] COLUMNAS =
                {"Origen", "Nombre", "N° Cheque", "Banco", "Vencimiento", "Valor (Gs.)"};
        private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");
        private static final DateTimeFormatter FORMATO_FECHA_TABLA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private List<ChequePreDatado> cheques = List.of();

        void setDatos(List<ChequePreDatado> cheques) {
            this.cheques = cheques;
            fireTableDataChanged();
        }

        ChequePreDatado getCheque(int fila) {
            return cheques.get(fila);
        }

        @Override
        public int getRowCount() {
            return cheques.size();
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
            ChequePreDatado cheque = cheques.get(fila);
            boolean esDeCliente = cheque.getCliente() != null;
            switch (columna) {
                case 0:
                    return esDeCliente ? "Cliente" : "Proveedor";
                case 1:
                    return esDeCliente ? cheque.getCliente().getNombre() : cheque.getProveedor().getNombre();
                case 2:
                    return cheque.getNumeroCheque() == null ? "" : cheque.getNumeroCheque();
                case 3:
                    return cheque.getBanco() == null ? "" : cheque.getBanco();
                case 4:
                    return cheque.getFechaVencimiento() == null ? "" : cheque.getFechaVencimiento().format(FORMATO_FECHA_TABLA);
                case 5:
                    return FORMATO_VALOR.format(cheque.getValor() == null ? BigDecimal.ZERO : cheque.getValor());
                default:
                    return "";
            }
        }
    }
}
