package com.mecanica.view;

import com.mecanica.controller.MovimientoFinancieroController;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.MovimientoFinanciero;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
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
 * filtrado por periodo (fecha desde/hasta) y, opcionalmente, por categoria,
 * con el saldo del periodo (ver MovimientoFinancieroController.calcularSaldoPeriodo).
 *
 * Es una pantalla mayormente de solo lectura -- los movimientos los generan
 * otros flujos del sistema (pago de cliente, compra a proveedor, retiro de
 * empleado). La unica accion de escritura aca es "Nuevo Movimiento", que
 * abre MovimientoManualDialog para registrar un movimiento manual (siempre
 * con categoria OTRO, ver MovimientoFinancieroController.registrarMovimientoManual).
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en ClientesMaquinariosPanel.
 */
public class FinancieroPanel extends JPanel implements PanelActualizable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat FORMATO_SALDO = new DecimalFormat("#,##0");
    private static final String TODAS_LAS_CATEGORIAS = "(todas)";

    private final MovimientoFinancieroController movimientoFinancieroController = new MovimientoFinancieroController();

    private final TablaMovimientosModel modeloMovimientos = new TablaMovimientosModel();
    private final JTable tablaMovimientos = new JTable(modeloMovimientos);

    private final JTextField campoFechaInicio = new JTextField();
    private final JTextField campoFechaFin = new JTextField();
    private final JComboBox<String> comboCategoria = new JComboBox<>();
    private final JLabel labelError = new JLabel(" ");
    private final JLabel labelSaldo = new JLabel(" ");

    public FinancieroPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        LocalDate hoy = LocalDate.now();
        campoFechaInicio.setText(hoy.withDayOfMonth(1).format(FORMATO_FECHA));
        campoFechaFin.setText(hoy.format(FORMATO_FECHA));

        comboCategoria.addItem(TODAS_LAS_CATEGORIAS);
        for (CategoriaMovimientoFinanciero categoria : CategoriaMovimientoFinanciero.values()) {
            comboCategoria.addItem(categoria.name());
        }

        add(armarEncabezado(), BorderLayout.NORTH);
        add(armarCentro(), BorderLayout.CENTER);
        add(armarPie(), BorderLayout.SOUTH);

        buscarMovimientos();
    }

    /** Vuelve a buscar los movimientos con el mismo periodo/categoria filtrados al entrar en esta area. */
    @Override
    public void actualizar() {
        buscarMovimientos();
    }

    // ---------------------------------------------------------------- Armado de pantalla

    private JComponent armarEncabezado() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Financiero");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        encabezado.add(titulo, BorderLayout.WEST);

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

        JPanel panelCategoria = new JPanel(new BorderLayout(0, 4));
        panelCategoria.setOpaque(false);
        JLabel labelCategoria = new JLabel("CATEGORIA");
        labelCategoria.setForeground(Paleta.GRIS_TEXTO);
        labelCategoria.setFont(new Font("Segoe UI", Font.BOLD, 10));
        comboCategoria.setPreferredSize(new Dimension(190, 32));
        comboCategoria.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panelCategoria.add(labelCategoria, BorderLayout.NORTH);
        panelCategoria.add(comboCategoria, BorderLayout.CENTER);
        panel.add(panelCategoria);

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

    private JComponent armarCentro() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        estilizarTabla(tablaMovimientos);
        panel.add(new JScrollPane(tablaMovimientos), BorderLayout.CENTER);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(labelError, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent armarPie() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        labelSaldo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        labelSaldo.setForeground(Paleta.AZUL_OSCURO);
        panel.add(labelSaldo, BorderLayout.EAST);
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

        String categoriaSeleccionada = (String) comboCategoria.getSelectedItem();
        CategoriaMovimientoFinanciero categoria = TODAS_LAS_CATEGORIAS.equals(categoriaSeleccionada)
                ? null
                : CategoriaMovimientoFinanciero.valueOf(categoriaSeleccionada);

        LocalDate inicioFinal = inicio;
        LocalDate finFinal = fin;
        CategoriaMovimientoFinanciero categoriaFinal = categoria;

        setHabilitado(false);
        new SwingWorker<ResultadoFiltro, Void>() {
            Exception error;

            @Override
            protected ResultadoFiltro doInBackground() {
                try {
                    List<MovimientoFinanciero> movimientos =
                            movimientoFinancieroController.listarPorPeriodo(inicioFinal, finFinal);
                    if (categoriaFinal != null) {
                        List<MovimientoFinanciero> filtrados = new ArrayList<>();
                        for (MovimientoFinanciero m : movimientos) {
                            if (m.getCategoria() == categoriaFinal) {
                                filtrados.add(m);
                            }
                        }
                        movimientos = filtrados;
                    }
                    // El saldo es siempre del periodo completo (no cambia con el filtro de categoria).
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
}
