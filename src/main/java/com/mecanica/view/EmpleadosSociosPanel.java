package com.mecanica.view;

import com.mecanica.controller.EmpleadoController;
import com.mecanica.controller.RetiroEmpleadoController;
import com.mecanica.controller.RetiroSocioController;
import com.mecanica.controller.SocioController;
import com.mecanica.model.Empleado;
import com.mecanica.model.RetiroEmpleado;
import com.mecanica.model.Socio;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Pantalla real del area "Empleados y Socios": una pestana "Empleados" y
 * una pestana "Socios", cada una con su propia lista (Nuevo/Editar/
 * Eliminar), un boton "Registrar Retiro" y un panel de reporte que
 * calcula, para un periodo elegido, el cierre mensual del empleado
 * (EmpleadoController.calcularCierreMensual) o la liquidacion 50/50 de
 * los socios (SocioController.calcularLiquidacion).
 *
 * IMPORTANTE sobre los retiros: el retiro de EMPLEADO genera gasto
 * (MovimientoFinanciero) automaticamente dentro de
 * RetiroEmpleadoController.registrarRetirada -- esta pantalla solo llama
 * al Controller. El retiro de SOCIO NO genera ningun MovimientoFinanciero
 * (se descuenta recien en la liquidacion) -- ver RetiroSocioController.
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en ClientesEquiposPanel.
 */
public class EmpleadosSociosPanel extends JPanel {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");

    private final EmpleadoController empleadoController = new EmpleadoController();
    private final SocioController socioController = new SocioController();
    private final RetiroEmpleadoController retiroEmpleadoController = new RetiroEmpleadoController();
    private final RetiroSocioController retiroSocioController = new RetiroSocioController();

    private final TablaEmpleadosModel modeloEmpleados = new TablaEmpleadosModel();
    private final TablaSociosModel modeloSocios = new TablaSociosModel();

    private final JTable tablaEmpleados = new JTable(modeloEmpleados);
    private final JTable tablaSocios = new JTable(modeloSocios);

    private final BotonPlano botonEditarEmpleado = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarEmpleado = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());
    private final BotonPlano botonRetiroEmpleado = new BotonPlano("REGISTRAR RETIRO", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonCalcularCierre = new BotonPlano("CALCULAR");

    private final BotonPlano botonEditarSocio = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminarSocio = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());
    private final BotonPlano botonRetiroSocio = new BotonPlano("REGISTRAR RETIRO", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonCalcularLiquidacion = new BotonPlano("CALCULAR");

    private final JTextField campoInicioEmpleado = new JTextField();
    private final JTextField campoFinEmpleado = new JTextField();
    private final JTextArea areaReporteEmpleado = new JTextArea();

    private final JTextField campoGananciaTotal = new JTextField();
    private final JTextField campoInicioSocio = new JTextField();
    private final JTextField campoFinSocio = new JTextField();
    private final JTextArea areaReporteSocio = new JTextArea();

    public EmpleadosSociosPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pestanias.setBackground(Paleta.GRIS_FONDO);
        pestanias.addTab("Empleados", armarTabEmpleados());
        pestanias.addTab("Socios", armarTabSocios());
        add(pestanias, BorderLayout.CENTER);

        actualizarEstadoBotonesEmpleado();
        actualizarEstadoBotonesSocio();

        cargarEmpleados();
        cargarSocios();
    }

    // ---------------------------------------------------------------- Pestana Empleados

    private JComponent armarTabEmpleados() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Paleta.GRIS_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Empleados");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        encabezado.add(titulo, BorderLayout.WEST);

        BotonPlano botonNuevoEmpleado = new BotonPlano("NUEVO EMPLEADO");
        botonNuevoEmpleado.addActionListener(e -> onNuevoEmpleado());
        encabezado.add(botonNuevoEmpleado, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                armarListaEmpleados(), armarReporteEmpleados());
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JComponent armarListaEmpleados() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        estilizarTabla(tablaEmpleados);
        tablaEmpleados.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotonesEmpleado();
            }
        });
        panel.add(new JScrollPane(tablaEmpleados), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botonEditarEmpleado.addActionListener(e -> onEditarEmpleado());
        botonEliminarEmpleado.addActionListener(e -> onEliminarEmpleado());
        botonRetiroEmpleado.addActionListener(e -> onRegistrarRetiroEmpleado());
        botones.add(botonEditarEmpleado);
        botones.add(botonEliminarEmpleado);
        botones.add(botonRetiroEmpleado);
        panel.add(botones, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent armarReporteEmpleados() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel titulo = new JLabel("Cierre Mensual del Empleado Seleccionado");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);

        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filtro.setOpaque(false);
        filtro.add(crearEtiquetaFiltro("DESDE"));
        estilizarCampoFiltro(campoInicioEmpleado);
        filtro.add(campoInicioEmpleado);
        filtro.add(crearEtiquetaFiltro("HASTA"));
        estilizarCampoFiltro(campoFinEmpleado);
        filtro.add(campoFinEmpleado);
        botonCalcularCierre.addActionListener(e -> onCalcularCierre());
        filtro.add(botonCalcularCierre);
        centro.add(filtro, BorderLayout.NORTH);

        LocalDate hoy = LocalDate.now();
        campoInicioEmpleado.setText(hoy.withDayOfMonth(1).format(FORMATO_FECHA));
        campoFinEmpleado.setText(hoy.format(FORMATO_FECHA));

        estilizarAreaReporte(areaReporteEmpleado);
        centro.add(new JScrollPane(areaReporteEmpleado), BorderLayout.CENTER);

        panel.add(centro, BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------- Pestana Socios

    private JComponent armarTabSocios() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Paleta.GRIS_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Socios");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        encabezado.add(titulo, BorderLayout.WEST);

        BotonPlano botonNuevoSocio = new BotonPlano("NUEVO SOCIO");
        botonNuevoSocio.addActionListener(e -> onNuevoSocio());
        encabezado.add(botonNuevoSocio, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                armarListaSocios(), armarReporteSocios());
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JComponent armarListaSocios() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        estilizarTabla(tablaSocios);
        tablaSocios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotonesSocio();
            }
        });
        panel.add(new JScrollPane(tablaSocios), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botonEditarSocio.addActionListener(e -> onEditarSocio());
        botonEliminarSocio.addActionListener(e -> onEliminarSocio());
        botonRetiroSocio.addActionListener(e -> onRegistrarRetiroSocio());
        botones.add(botonEditarSocio);
        botones.add(botonEliminarSocio);
        botones.add(botonRetiroSocio);
        panel.add(botones, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent armarReporteSocios() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel titulo = new JLabel("Liquidacion del Periodo (division fija 50% / 50%)");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);

        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filtro.setOpaque(false);
        filtro.add(crearEtiquetaFiltro("GANANCIA TOTAL (Gs.)"));
        estilizarCampoFiltro(campoGananciaTotal);
        campoGananciaTotal.setPreferredSize(new Dimension(120, 30));
        filtro.add(campoGananciaTotal);
        filtro.add(crearEtiquetaFiltro("DESDE"));
        estilizarCampoFiltro(campoInicioSocio);
        filtro.add(campoInicioSocio);
        filtro.add(crearEtiquetaFiltro("HASTA"));
        estilizarCampoFiltro(campoFinSocio);
        filtro.add(campoFinSocio);
        botonCalcularLiquidacion.addActionListener(e -> onCalcularLiquidacion());
        filtro.add(botonCalcularLiquidacion);
        centro.add(filtro, BorderLayout.NORTH);

        LocalDate hoy = LocalDate.now();
        campoInicioSocio.setText(hoy.withDayOfMonth(1).format(FORMATO_FECHA));
        campoFinSocio.setText(hoy.format(FORMATO_FECHA));

        estilizarAreaReporte(areaReporteSocio);
        centro.add(new JScrollPane(areaReporteSocio), BorderLayout.CENTER);

        panel.add(centro, BorderLayout.CENTER);
        return panel;
    }

    // ---------------------------------------------------------------- Estilo comun

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

    private JLabel crearEtiquetaFiltro(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(Paleta.GRIS_TEXTO);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        return label;
    }

    private void estilizarCampoFiltro(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setPreferredSize(new Dimension(88, 30));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));
    }

    private void estilizarAreaReporte(JTextArea area) {
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(Paleta.BLANCO);
        area.setForeground(Paleta.AZUL_OSCURO);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    }

    // ---------------------------------------------------------------- Carga de datos

    private void cargarEmpleados() {
        setHabilitado(false);
        new SwingWorker<List<Empleado>, Void>() {
            Exception error;

            @Override
            protected List<Empleado> doInBackground() {
                try {
                    return empleadoController.listarTodos();
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    modeloEmpleados.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                actualizarEstadoBotonesEmpleado();
            }
        }.execute();
    }

    private void cargarSocios() {
        setHabilitado(false);
        new SwingWorker<List<Socio>, Void>() {
            Exception error;

            @Override
            protected List<Socio> doInBackground() {
                try {
                    return socioController.listarTodos();
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    modeloSocios.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                actualizarEstadoBotonesSocio();
            }
        }.execute();
    }

    private Empleado empleadoSeleccionado() {
        int fila = tablaEmpleados.getSelectedRow();
        return fila < 0 ? null : modeloEmpleados.getEmpleado(fila);
    }

    private Socio socioSeleccionado() {
        int fila = tablaSocios.getSelectedRow();
        return fila < 0 ? null : modeloSocios.getSocio(fila);
    }

    private void actualizarEstadoBotonesEmpleado() {
        boolean hay = empleadoSeleccionado() != null;
        botonEditarEmpleado.setEnabled(hay);
        botonEliminarEmpleado.setEnabled(hay);
        botonRetiroEmpleado.setEnabled(hay);
        botonCalcularCierre.setEnabled(hay);
    }

    private void actualizarEstadoBotonesSocio() {
        boolean hay = socioSeleccionado() != null;
        botonEditarSocio.setEnabled(hay);
        botonEliminarSocio.setEnabled(hay);
        botonRetiroSocio.setEnabled(hay);
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

    // ---------------------------------------------------------------- CRUD Empleado

    private void onNuevoEmpleado() {
        EmpleadoFormDialog dialogo = new EmpleadoFormDialog(ventana(), null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarEmpleado(dialogo.getEmpleado());
        }
    }

    private void onEditarEmpleado() {
        Empleado seleccionado = empleadoSeleccionado();
        if (seleccionado == null) {
            return;
        }
        EmpleadoFormDialog dialogo = new EmpleadoFormDialog(ventana(), seleccionado);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarEmpleado(dialogo.getEmpleado());
        }
    }

    private void guardarEmpleado(Empleado empleado) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    empleadoController.guardar(empleado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(EmpleadosSociosPanel.this,
                            error.getMessage(), "No fue posible guardar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarEmpleados();
            }
        }.execute();
    }

    private void onEliminarEmpleado() {
        Empleado seleccionado = empleadoSeleccionado();
        if (seleccionado == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Eliminar al empleado \"" + seleccionado.getNombre() + "\"?",
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
                    empleadoController.eliminar(seleccionado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(EmpleadosSociosPanel.this,
                            "No fue posible eliminar: el empleado tiene retiros u otros registros vinculados.",
                            "No fue posible eliminar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarEmpleados();
            }
        }.execute();
    }

    private void onRegistrarRetiroEmpleado() {
        Empleado seleccionado = empleadoSeleccionado();
        if (seleccionado == null) {
            return;
        }
        RetiroEmpleadoDialog dialogo = new RetiroEmpleadoDialog(ventana(), seleccionado);
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
                    // El propio Controller genera el MovimientoFinanciero de gasto.
                    retiroEmpleadoController.registrarRetirada(seleccionado, dialogo.getTipo(),
                            dialogo.getValor(), dialogo.getFecha(), dialogo.getObservacion());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(EmpleadosSociosPanel.this,
                            error.getMessage(), "No fue posible registrar el retiro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Refresca el cierre mensual mostrado, si ya habia uno calculado para este periodo.
                onCalcularCierre();
            }
        }.execute();
    }

    private void onCalcularCierre() {
        Empleado seleccionado = empleadoSeleccionado();
        if (seleccionado == null) {
            return;
        }

        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = LocalDate.parse(campoInicioEmpleado.getText().trim(), FORMATO_FECHA);
            fin = LocalDate.parse(campoFinEmpleado.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Las fechas del periodo deben tener el formato DD/MM/AAAA.",
                    "Periodo invalido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setHabilitado(false);
        new SwingWorker<EmpleadoController.ResultadoCierreMensual, Void>() {
            Exception error;

            @Override
            protected EmpleadoController.ResultadoCierreMensual doInBackground() {
                try {
                    return empleadoController.calcularCierreMensual(seleccionado, inicio, fin);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                EmpleadoController.ResultadoCierreMensual resultado = null;
                try {
                    resultado = get();
                } catch (Exception e) {
                    error = e;
                }
                if (error != null || resultado == null) {
                    mostrarErrorConexion();
                    return;
                }
                areaReporteEmpleado.setText(formatearCierre(resultado, inicio, fin));
            }
        }.execute();
    }

    private String formatearCierre(EmpleadoController.ResultadoCierreMensual resultado, LocalDate inicio, LocalDate fin) {
        StringBuilder texto = new StringBuilder();
        texto.append("Empleado: ").append(resultado.getEmpleado().getNombre()).append('\n');
        texto.append("Periodo:  ").append(inicio.format(FORMATO_FECHA))
                .append(" a ").append(fin.format(FORMATO_FECHA)).append("\n\n");

        texto.append("Retiros del periodo:\n");
        if (resultado.getRetiradas().isEmpty()) {
            texto.append("  (sin retiros en el periodo)\n");
        } else {
            for (RetiroEmpleado retiro : resultado.getRetiradas()) {
                texto.append("  ").append(retiro.getFecha().format(FORMATO_FECHA))
                        .append("  ").append(retiro.getTipo())
                        .append("  Gs. ").append(FORMATO_VALOR.format(retiro.getValor()));
                if (retiro.getObservacion() != null && !retiro.getObservacion().isBlank()) {
                    texto.append("  (").append(retiro.getObservacion()).append(')');
                }
                texto.append('\n');
            }
        }

        BigDecimal salarioBase = resultado.getEmpleado().getSalarioBase() != null
                ? resultado.getEmpleado().getSalarioBase() : BigDecimal.ZERO;
        texto.append("\nSalario base:      Gs. ").append(FORMATO_VALOR.format(salarioBase)).append('\n');
        texto.append("Total descontado:  Gs. ").append(FORMATO_VALOR.format(resultado.getTotalDescontado())).append('\n');
        texto.append("Valor liquido:     Gs. ").append(FORMATO_VALOR.format(resultado.getValorLiquido())).append('\n');
        return texto.toString();
    }

    // ---------------------------------------------------------------- CRUD Socio

    private void onNuevoSocio() {
        SocioFormDialog dialogo = new SocioFormDialog(ventana(), null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarSocio(dialogo.getSocio());
        }
    }

    private void onEditarSocio() {
        Socio seleccionado = socioSeleccionado();
        if (seleccionado == null) {
            return;
        }
        SocioFormDialog dialogo = new SocioFormDialog(ventana(), seleccionado);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            guardarSocio(dialogo.getSocio());
        }
    }

    private void guardarSocio(Socio socio) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    socioController.guardar(socio);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(EmpleadosSociosPanel.this,
                            error.getMessage(), "No fue posible guardar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarSocios();
            }
        }.execute();
    }

    private void onEliminarSocio() {
        Socio seleccionado = socioSeleccionado();
        if (seleccionado == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Eliminar al socio \"" + seleccionado.getNombre() + "\"?",
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
                    socioController.eliminar(seleccionado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(EmpleadosSociosPanel.this,
                            "No fue posible eliminar: el socio tiene retiros u otros registros vinculados.",
                            "No fue posible eliminar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarSocios();
            }
        }.execute();
    }

    private void onRegistrarRetiroSocio() {
        Socio seleccionado = socioSeleccionado();
        if (seleccionado == null) {
            return;
        }
        RetiroSocioDialog dialogo = new RetiroSocioDialog(ventana(), seleccionado);
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
                    // A proposito: NO se crea ningun MovimientoFinanciero aca ni dentro
                    // del Controller -- el retiro de socio solo se descuenta en la
                    // liquidacion (ver SocioController.calcularLiquidacion).
                    retiroSocioController.registrarRetirada(seleccionado, dialogo.getValor(),
                            dialogo.getFecha(), dialogo.getObservacion());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(EmpleadosSociosPanel.this,
                            error.getMessage(), "No fue posible registrar el retiro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void onCalcularLiquidacion() {
        BigDecimal gananciaTotal;
        String textoGanancia = campoGananciaTotal.getText().trim().replace(".", "").replace(",", ".");
        try {
            gananciaTotal = new BigDecimal(textoGanancia);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese la ganancia total del periodo (valor numerico valido).",
                    "Valor invalido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = LocalDate.parse(campoInicioSocio.getText().trim(), FORMATO_FECHA);
            fin = LocalDate.parse(campoFinSocio.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Las fechas del periodo deben tener el formato DD/MM/AAAA.",
                    "Periodo invalido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal gananciaFinal = gananciaTotal;
        setHabilitado(false);
        new SwingWorker<List<SocioController.ResultadoLiquidacion>, Void>() {
            Exception error;

            @Override
            protected List<SocioController.ResultadoLiquidacion> doInBackground() {
                try {
                    return socioController.calcularLiquidacion(gananciaFinal, inicio, fin);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                List<SocioController.ResultadoLiquidacion> resultado = List.of();
                try {
                    resultado = get();
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                    return;
                }
                areaReporteSocio.setText(formatearLiquidacion(resultado, gananciaFinal, inicio, fin));
            }
        }.execute();
    }

    private String formatearLiquidacion(List<SocioController.ResultadoLiquidacion> resultado,
                                         BigDecimal gananciaTotal, LocalDate inicio, LocalDate fin) {
        StringBuilder texto = new StringBuilder();
        texto.append("Periodo: ").append(inicio.format(FORMATO_FECHA))
                .append(" a ").append(fin.format(FORMATO_FECHA)).append('\n');
        texto.append("Ganancia total del periodo: Gs. ").append(FORMATO_VALOR.format(gananciaTotal)).append('\n');
        texto.append("Division entre socios: 50% / 50% (fija)\n\n");

        if (resultado.isEmpty()) {
            texto.append("(no hay socios activos)\n");
            return texto.toString();
        }

        for (SocioController.ResultadoLiquidacion linea : resultado) {
            texto.append(linea.getSocio().getNombre()).append(":\n");
            texto.append("  Parte de ganancia (50%):  Gs. ").append(FORMATO_VALOR.format(linea.getParteGanancia())).append('\n');
            texto.append("  Ya retirado en el periodo: Gs. ").append(FORMATO_VALOR.format(linea.getYaRetirado())).append('\n');
            texto.append("  Saldo a favor del socio:   Gs. ").append(FORMATO_VALOR.format(linea.getValorARecibir())).append("\n\n");
        }
        return texto.toString();
    }

    // ---------------------------------------------------------------- Modelos de tabla

    private static class TablaEmpleadosModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Nombre", "Documento", "Cargo", "Salario Base (Gs.)", "Activo"};
        private static final DecimalFormat FORMATO_SALARIO = new DecimalFormat("#,##0");

        private List<Empleado> empleados = List.of();

        void setDatos(List<Empleado> empleados) {
            this.empleados = empleados;
            fireTableDataChanged();
        }

        Empleado getEmpleado(int fila) {
            return empleados.get(fila);
        }

        @Override
        public int getRowCount() {
            return empleados.size();
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
            Empleado empleado = empleados.get(fila);
            switch (columna) {
                case 0:
                    return empleado.getNombre();
                case 1:
                    return empleado.getDocumento() == null ? "" : empleado.getDocumento();
                case 2:
                    return empleado.getCargo() == null ? "" : empleado.getCargo();
                case 3:
                    return FORMATO_SALARIO.format(empleado.getSalarioBase() == null ? BigDecimal.ZERO : empleado.getSalarioBase());
                case 4:
                    return empleado.isActivo() ? "Si" : "No";
                default:
                    return "";
            }
        }
    }

    private static class TablaSociosModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Nombre", "Documento", "Telefono", "Activo"};

        private List<Socio> socios = List.of();

        void setDatos(List<Socio> socios) {
            this.socios = socios;
            fireTableDataChanged();
        }

        Socio getSocio(int fila) {
            return socios.get(fila);
        }

        @Override
        public int getRowCount() {
            return socios.size();
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
            Socio socio = socios.get(fila);
            switch (columna) {
                case 0:
                    return socio.getNombre();
                case 1:
                    return socio.getDocumento() == null ? "" : socio.getDocumento();
                case 2:
                    return socio.getTelefono() == null ? "" : socio.getTelefono();
                case 3:
                    return socio.isActivo() ? "Si" : "No";
                default:
                    return "";
            }
        }
    }
}
