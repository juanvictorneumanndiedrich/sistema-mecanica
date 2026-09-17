package com.mecanica.view;

import com.mecanica.controller.ChequePreDatadoController;
import com.mecanica.controller.CierreMensualController;
import com.mecanica.controller.MovimientoFinancieroController;
import com.mecanica.controller.ReporteController;
import com.mecanica.enums.Permiso;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.ChequePreDatado;
import com.mecanica.model.CierreMensual;
import com.mecanica.model.CierreSocioDetalle;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.util.Sesion;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pantalla real del area "Financiero": extracto de MovimientoFinanciero
 * filtrado por periodo (fecha desde/hasta) y, opcionalmente, por tipo
 * (Ingreso/Egreso), con el saldo del periodo (ver
 * MovimientoFinancieroController.calcularSaldoPeriodo).
 *
 * Tiene tres pestañas: "Movimientos" (el extracto de siempre, mayormente de
 * solo lectura -- los movimientos los generan otros flujos del sistema:
 * pago de cliente, compra a proveedor, retiro de empleado, o un cheque
 * pre-datado que ya vencio y fue confirmado), "Cheques Pendientes" (los
 * cheques pre-datados que todavia no vencieron/no fueron confirmados -- ver
 * ChequePreDatadoController) y "Cierre Mensual" (el cierre real del negocio:
 * el usuario elige con checkbox que movimientos entran, sin importar la
 * fecha exacta, y el sistema calcula la ganancia y la reparte 50/50 entre
 * los socios -- ver CierreMensualController). La unica accion de escritura
 * en la pestaña de Movimientos es "Nuevo Movimiento", que abre
 * MovimientoManualDialog para registrar un movimiento manual (siempre con
 * categoria OTRO).
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en ClientesMaquinariosPanel.
 */
public class FinancieroPanel extends JPanel implements PanelActualizable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat FORMATO_SALDO = com.mecanica.util.Moneda.nuevoFormatoValor();
    private static final String TODOS_LOS_TIPOS = "(todos)";
    private static final String TIPO_INGRESO = "Ingreso";
    private static final String TIPO_EGRESO = "Egreso";

    private final MovimientoFinancieroController movimientoFinancieroController = new MovimientoFinancieroController();
    private final ChequePreDatadoController chequeController = new ChequePreDatadoController();
    private final CierreMensualController cierreMensualController = new CierreMensualController();
    private final ReporteController reporteController = new ReporteController();

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

    private final TablaPendientesCierreModel modeloPendientesCierre = new TablaPendientesCierreModel();
    private final JTable tablaPendientesCierre = new JTable(modeloPendientesCierre);
    private final JTextField campoDescripcionCierre = new JTextField();
    private final BotonPlano botonCerrarMes = new BotonPlano("CERRAR MES", Paleta.VERDE_EXITO, Paleta.VERDE_EXITO.brighter());
    private final JLabel labelErrorCierre = new JLabel(" ");

    private final TablaHistoricoCierresModel modeloHistoricoCierres = new TablaHistoricoCierresModel();
    private final BotonPlano botonImprimirCierre = new BotonPlano("IMPRIMIR ACERTO", Paleta.GRIS_TEXTO, Paleta.GRIS_TEXTO.brighter());
    private final JTable tablaHistoricoCierres = new JTable(modeloHistoricoCierres);
    private final JTextArea areaReporteCierre = new JTextArea();

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
        if (Sesion.tiene(Permiso.CIERRE_MENSUAL)) {
            pestañas.addTab("Cierre Mensual", armarPestañaCierreMensual());
        }
        add(pestañas, BorderLayout.CENTER);

        buscarMovimientos();
        cargarChequesPendientes();
        cargarPendientesCierre();
        cargarHistoricoCierres();
    }

    /** Vuelve a buscar los movimientos, los cheques y el cierre pendiente al entrar en esta area. */
    @Override
    public void actualizar() {
        buscarMovimientos();
        cargarChequesPendientes();
        cargarPendientesCierre();
        cargarHistoricoCierres();
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
        botonNuevoMovimiento.setVisible(Sesion.tiene(Permiso.MOVIMIENTO_MANUAL));
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

        BotonPlano botonImprimirMovimientos = new BotonPlano("IMPRIMIR", Paleta.GRIS_TEXTO, Paleta.GRIS_TEXTO.brighter());
        botonImprimirMovimientos.addActionListener(e -> onImprimirMovimientos());
        JPanel panelBotonImprimir = new JPanel(new BorderLayout());
        panelBotonImprimir.setOpaque(false);
        panelBotonImprimir.add(Box.createVerticalStrut(18), BorderLayout.NORTH);
        panelBotonImprimir.add(botonImprimirMovimientos, BorderLayout.CENTER);
        panel.add(panelBotonImprimir);

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
        tablaMovimientos.getColumnModel().getColumn(7).setCellRenderer(new ColorValorRenderer(modeloMovimientos));
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
        BotonPlano botonImprimirCheques = new BotonPlano("IMPRIMIR LISTADO", Paleta.GRIS_TEXTO, Paleta.GRIS_TEXTO.brighter());
        botonImprimirCheques.addActionListener(e ->
                VisorReporte.mostrar(this, "Cheques Pendientes", reporteController::chequesPendientes));
        JPanel accionesCheques = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        accionesCheques.setOpaque(false);
        accionesCheques.add(botonImprimirCheques);
        accionesCheques.add(botonConfirmarCheque);
        pie.add(accionesCheques, BorderLayout.EAST);
        panel.add(pie, BorderLayout.SOUTH);

        return panel;
    }

    // ---------------------------------------------------------------- Pestaña Cierre Mensual

    private JComponent armarPestañaCierreMensual() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel explicacion = new JLabel(
                "<html>Marque los movimientos que entran en el cierre de ahora -- no tienen que ser "
                        + "todos los del mes: puede dejar alguno para el cierre siguiente, o incluir uno "
                        + "de un mes anterior. Al cerrar, se calcula la ganancia y se reparte 50/50 entre "
                        + "los socios (descontando lo que cada uno ya retiro).</html>");
        explicacion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        explicacion.setForeground(Paleta.GRIS_TEXTO);
        panel.add(explicacion, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                armarPanelPendientesCierre(), armarPanelHistoricoCierre());
        split.setResizeWeight(0.6);
        split.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        split.setOpaque(false);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private JComponent armarPanelPendientesCierre() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        estilizarTabla(tablaPendientesCierre);
        tablaPendientesCierre.getColumnModel().getColumn(0).setMaxWidth(30);
        panel.add(new JScrollPane(tablaPendientesCierre), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(0, 8));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel filaDescripcion = new JPanel(new BorderLayout(0, 4));
        filaDescripcion.setOpaque(false);
        JLabel labelDescripcion = new JLabel("DESCRIPCION DEL CIERRE (opcional)");
        labelDescripcion.setForeground(Paleta.GRIS_TEXTO);
        labelDescripcion.setFont(new Font("Segoe UI", Font.BOLD, 10));
        campoDescripcionCierre.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoDescripcionCierre.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        filaDescripcion.add(labelDescripcion, BorderLayout.NORTH);
        filaDescripcion.add(campoDescripcionCierre, BorderLayout.CENTER);
        pie.add(filaDescripcion, BorderLayout.NORTH);

        JPanel filaBoton = new JPanel(new BorderLayout());
        filaBoton.setOpaque(false);
        filaBoton.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        labelErrorCierre.setForeground(Paleta.ROJO_ERROR);
        labelErrorCierre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filaBoton.add(labelErrorCierre, BorderLayout.WEST);
        botonCerrarMes.addActionListener(e -> onCerrarMes());
        filaBoton.add(botonCerrarMes, BorderLayout.EAST);
        pie.add(filaBoton, BorderLayout.SOUTH);

        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent armarPanelHistoricoCierre() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JLabel titulo = new JLabel("Historico de cierres");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        panel.add(titulo, BorderLayout.NORTH);

        estilizarTabla(tablaHistoricoCierres);
        tablaHistoricoCierres.setPreferredScrollableViewportSize(new Dimension(320, 140));
        ListSelectionListener alSeleccionar = e -> {
            if (!e.getValueIsAdjusting()) {
                onVerHistoricoCierre();
            }
        };
        tablaHistoricoCierres.getSelectionModel().addListSelectionListener(alSeleccionar);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(new JScrollPane(tablaHistoricoCierres), BorderLayout.NORTH);

        estilizarAreaReporte(areaReporteCierre);
        centro.add(new JScrollPane(areaReporteCierre), BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        JPanel pieHistorico = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pieHistorico.setOpaque(false);
        pieHistorico.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        botonImprimirCierre.setEnabled(false);
        botonImprimirCierre.addActionListener(e -> onImprimirCierre());
        pieHistorico.add(botonImprimirCierre);
        panel.add(pieHistorico, BorderLayout.SOUTH);

        return panel;
    }

    private void estilizarAreaReporte(JTextArea area) {
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBackground(Paleta.BLANCO);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
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
                "Confirmar que el cheque de Gs. " + com.mecanica.util.Moneda.nuevoFormatoValor().format(seleccionado.getValor())
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

    // ---------------------------------------------------------------- Carga de datos (Cierre Mensual)

    private void cargarPendientesCierre() {
        labelErrorCierre.setText(" ");
        setHabilitado(false);
        new SwingWorker<List<MovimientoFinanciero>, Void>() {
            Exception error;

            @Override
            protected List<MovimientoFinanciero> doInBackground() {
                try {
                    return movimientoFinancieroController.listarPendientesDeCierre();
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                List<MovimientoFinanciero> resultado;
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
                modeloPendientesCierre.setDatos(resultado);
            }
        }.execute();
    }

    private void cargarHistoricoCierres() {
        setHabilitado(false);
        new SwingWorker<List<CierreMensual>, Void>() {
            Exception error;

            @Override
            protected List<CierreMensual> doInBackground() {
                try {
                    return cierreMensualController.listarHistorico();
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                List<CierreMensual> resultado;
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
                modeloHistoricoCierres.setDatos(resultado);
            }
        }.execute();
    }

    private void onVerHistoricoCierre() {
        int filaSeleccionada = tablaHistoricoCierres.getSelectedRow();
        botonImprimirCierre.setEnabled(filaSeleccionada >= 0);
        if (filaSeleccionada < 0) {
            return;
        }
        CierreMensual seleccionado = modeloHistoricoCierres.getCierre(
                tablaHistoricoCierres.convertRowIndexToModel(filaSeleccionada));
        areaReporteCierre.setText(formatearReporteCierre(seleccionado));
    }

    private void onCerrarMes() {
        labelErrorCierre.setText(" ");
        List<MovimientoFinanciero> seleccionados = modeloPendientesCierre.getSeleccionados();
        if (seleccionados.isEmpty()) {
            labelErrorCierre.setText("Marque al menos un movimiento para cerrar el mes.");
            return;
        }

        BigDecimal entradas = BigDecimal.ZERO;
        BigDecimal salidas = BigDecimal.ZERO;
        for (MovimientoFinanciero m : seleccionados) {
            if (m.getTipo() == TipoMovimientoFinanciero.ENTRADA) {
                entradas = entradas.add(m.getValor());
            } else {
                salidas = salidas.add(m.getValor());
            }
        }
        BigDecimal ganancia = entradas.subtract(salidas);

        int opcion = JOptionPane.showConfirmDialog(this,
                "Cerrar el mes con " + seleccionados.size() + " movimiento(s) marcado(s)?\n\n"
                        + "Total entradas: Gs. " + FORMATO_SALDO.format(entradas) + "\n"
                        + "Total gastos:   Gs. " + FORMATO_SALDO.format(salidas) + "\n"
                        + "GANANCIA:       Gs. " + FORMATO_SALDO.format(ganancia) + "\n\n"
                        + "Esto va a repartir la ganancia 50/50 entre los socios y no se puede deshacer.",
                "Confirmar cierre mensual", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        String descripcion = campoDescripcionCierre.getText().trim();

        setHabilitado(false);
        new SwingWorker<CierreMensual, Void>() {
            RuntimeException error;

            @Override
            protected CierreMensual doInBackground() {
                try {
                    return cierreMensualController.cerrar(seleccionados, descripcion.isBlank() ? null : descripcion);
                } catch (RuntimeException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                CierreMensual resultado = null;
                try {
                    resultado = get();
                } catch (Exception e) {
                    error = new RuntimeException(e.getMessage(), e);
                }
                if (error != null || resultado == null) {
                    JOptionPane.showMessageDialog(FinancieroPanel.this,
                            error != null ? error.getMessage() : "No fue posible cerrar el mes.",
                            "No fue posible cerrar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                campoDescripcionCierre.setText("");
                areaReporteCierre.setText(formatearReporteCierre(resultado));
                cargarPendientesCierre();
                cargarHistoricoCierres();
                buscarMovimientos();

                CierreMensual cerrado = resultado;
                int imprimir = JOptionPane.showConfirmDialog(FinancieroPanel.this,
                        "Mes cerrado. Desea imprimir el documento del acerto de los socios?",
                        "Imprimir acerto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (imprimir == JOptionPane.YES_OPTION) {
                    VisorReporte.mostrar(FinancieroPanel.this, "Cierre Mensual",
                            () -> reporteController.cierreMensual(cerrado));
                }
            }
        }.execute();
    }

    private void onImprimirCierre() {
        int filaSeleccionada = tablaHistoricoCierres.getSelectedRow();
        if (filaSeleccionada < 0) {
            return;
        }
        CierreMensual seleccionado = modeloHistoricoCierres.getCierre(
                tablaHistoricoCierres.convertRowIndexToModel(filaSeleccionada));
        VisorReporte.mostrar(this, "Cierre Mensual", () -> reporteController.cierreMensual(seleccionado));
    }

    /** Imprime los movimientos con los mismos filtros de la pantalla (DESDE, HASTA y TIPO). */
    private void onImprimirMovimientos() {
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
        VisorReporte.mostrar(this, "Movimientos Financieros",
                () -> reporteController.movimientosFinancieros(inicio, fin, tipo));
    }

    private String formatearReporteCierre(CierreMensual cierre) {
        StringBuilder texto = new StringBuilder();
        texto.append("Fecha del cierre: ").append(cierre.getFechaCierre().format(FORMATO_FECHA)).append('\n');
        if (cierre.getDescripcion() != null && !cierre.getDescripcion().isBlank()) {
            texto.append("Descripcion:      ").append(cierre.getDescripcion()).append('\n');
        }
        texto.append('\n');
        texto.append("Total entradas:  Gs. ").append(FORMATO_SALDO.format(cierre.getTotalEntradas())).append('\n');
        texto.append("Total gastos:    Gs. ").append(FORMATO_SALDO.format(cierre.getTotalSalidas())).append('\n');
        texto.append("GANANCIA:        Gs. ").append(FORMATO_SALDO.format(cierre.getGananciaTotal())).append("\n\n");

        texto.append("Reparto entre socios:\n");
        for (CierreSocioDetalle detalle : cierre.getDetalles()) {
            texto.append("  ").append(detalle.getSocio().getNombre()).append('\n');
            texto.append("    Parte de la ganancia: Gs. ").append(FORMATO_SALDO.format(detalle.getParteGanancia())).append('\n');
            texto.append("    Ya retiro:            Gs. ").append(FORMATO_SALDO.format(detalle.getYaRetirado())).append('\n');
            texto.append("    A recibir:            Gs. ").append(FORMATO_SALDO.format(detalle.getValorARecibir())).append('\n');
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
        private static final String[] COLUMNAS =
                {"Fecha", "Tipo", "Categoria", "De quien", "Descripcion", "Descuento (Gs.)", "Descuento (%)",
                        "Valor (Gs.)"};
        private static final DecimalFormat FORMATO_VALOR = com.mecanica.util.Moneda.nuevoFormatoValor();
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

        /** Nombre del cliente, proveedor o empleado de origen -- vacio en movimientos manuales. */
        private String deQuien(MovimientoFinanciero movimiento) {
            if (movimiento.getCliente() != null) {
                return movimiento.getCliente().getNombre();
            }
            if (movimiento.getProveedor() != null) {
                return movimiento.getProveedor().getNombre();
            }
            if (movimiento.getEmpleado() != null) {
                return movimiento.getEmpleado().getNombre();
            }
            return "";
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
                    return movimiento.getCategoria() == null ? "" : movimiento.getCategoria().getEtiqueta();
                case 3:
                    return deQuien(movimiento);
                case 4:
                    return movimiento.getDescripcion() == null ? "" : movimiento.getDescripcion();
                case 5:
                    return movimiento.getDescuentoValor() == null
                            ? ""
                            : FORMATO_VALOR.format(movimiento.getDescuentoValor());
                case 6:
                    return movimiento.getDescuentoPorcentaje() == null
                            ? ""
                            : movimiento.getDescuentoPorcentaje().toPlainString() + "%";
                case 7:
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
        private static final DecimalFormat FORMATO_VALOR = com.mecanica.util.Moneda.nuevoFormatoValor();
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

    /**
     * Tabla de la pestaña "Cierre Mensual": los movimientos que todavia no
     * entraron en ningun cierre, con una columna de checkbox (columna 0)
     * para elegir cuales entran en el cierre de ahora. Por defecto todos
     * quedan marcados -- el usuario desmarca los que quiere dejar para un
     * cierre siguiente.
     */
    private static class TablaPendientesCierreModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"", "Fecha", "Tipo", "Categoria", "Descripcion", "Valor (Gs.)"};
        private static final DecimalFormat FORMATO_VALOR = com.mecanica.util.Moneda.nuevoFormatoValor();
        private static final DateTimeFormatter FORMATO_FECHA_TABLA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private List<MovimientoFinanciero> movimientos = List.of();
        private final Set<Long> marcados = new HashSet<>();

        void setDatos(List<MovimientoFinanciero> movimientos) {
            this.movimientos = movimientos;
            marcados.clear();
            for (MovimientoFinanciero m : movimientos) {
                marcados.add(m.getId());
            }
            fireTableDataChanged();
        }

        List<MovimientoFinanciero> getSeleccionados() {
            List<MovimientoFinanciero> resultado = new ArrayList<>();
            for (MovimientoFinanciero m : movimientos) {
                if (marcados.contains(m.getId())) {
                    resultado.add(m);
                }
            }
            return resultado;
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
        public Class<?> getColumnClass(int columna) {
            return columna == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return columna == 0;
        }

        @Override
        public void setValueAt(Object valor, int fila, int columna) {
            if (columna != 0) {
                return;
            }
            Long id = movimientos.get(fila).getId();
            if (Boolean.TRUE.equals(valor)) {
                marcados.add(id);
            } else {
                marcados.remove(id);
            }
            fireTableCellUpdated(fila, columna);
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            MovimientoFinanciero movimiento = movimientos.get(fila);
            switch (columna) {
                case 0:
                    return marcados.contains(movimiento.getId());
                case 1:
                    return movimiento.getFecha() == null ? "" : movimiento.getFecha().format(FORMATO_FECHA_TABLA);
                case 2:
                    return movimiento.getTipo() == TipoMovimientoFinanciero.ENTRADA ? "Ingreso" : "Egreso";
                case 3:
                    return movimiento.getCategoria() == null ? "" : movimiento.getCategoria().getEtiqueta();
                case 4:
                    return movimiento.getDescripcion() == null ? "" : movimiento.getDescripcion();
                case 5:
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

    /** Tabla del historico de cierres ya hechos, en la pestaña "Cierre Mensual". */
    private static class TablaHistoricoCierresModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Descripcion", "Ganancia (Gs.)"};
        private static final DecimalFormat FORMATO_VALOR = com.mecanica.util.Moneda.nuevoFormatoValor();
        private static final DateTimeFormatter FORMATO_FECHA_TABLA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private List<CierreMensual> cierres = List.of();

        void setDatos(List<CierreMensual> cierres) {
            this.cierres = cierres;
            fireTableDataChanged();
        }

        CierreMensual getCierre(int fila) {
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
            CierreMensual cierre = cierres.get(fila);
            switch (columna) {
                case 0:
                    return cierre.getFechaCierre() == null ? "" : cierre.getFechaCierre().format(FORMATO_FECHA_TABLA);
                case 1:
                    return cierre.getDescripcion() == null ? "" : cierre.getDescripcion();
                case 2:
                    return FORMATO_VALOR.format(cierre.getGananciaTotal() == null ? BigDecimal.ZERO : cierre.getGananciaTotal());
                default:
                    return "";
            }
        }
    }
}
