package com.mecanica.view;

import com.mecanica.controller.OrdenDeServicioController;
import com.mecanica.enums.EstadoOrdenServicio;
import com.mecanica.model.Maquinario;
import com.mecanica.model.OrdenDeServicio;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla real del area "Ordenes de Servicio": lista de OS con filtro por
 * estado y busqueda por nombre de cliente, y los botones para abrir una OS
 * nueva, ver/editar sus items, cerrarla o cancelarla. Cerrar y Cancelar son
 * acciones que se sienten irreversibles (una vez cerrada o cancelada la OS
 * no vuelve a quedar abierta) por eso ambas piden confirmacion antes de
 * ejecutarse -- ver EstadoOrdenServicio.
 *
 * El filtro por estado va a la base (OrdenDeServicioController.listarPorEstado);
 * el filtro por cliente se aplica en memoria sobre lo ya cargado, porque el
 * DAO de OS no tiene una busqueda de OS por nombre de cliente.
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en ClientesMaquinariosPanel.
 */
public class OrdenesServicioPanel extends JPanel {

    private final OrdenDeServicioController ordenDeServicioController = new OrdenDeServicioController();

    private final TablaOrdenesModel modeloOrdenes = new TablaOrdenesModel();
    private final JTable tablaOrdenes = new JTable(modeloOrdenes);

    private final JComboBox<FiltroEstado> comboFiltroEstado = new JComboBox<>();
    private final JTextField campoBusquedaCliente = new JTextField();

    private final BotonPlano botonVerEditar = new BotonPlano("VER / EDITAR ITEMS", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonCerrar = new BotonPlano("CERRAR OS", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonCancelar = new BotonPlano("CANCELAR OS", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());

    /** Ultimo resultado traido de la base para el filtro de estado actual; la busqueda por cliente filtra sobre esta lista. */
    private List<OrdenDeServicio> ordenesCargadas = List.of();

    public OrdenesServicioPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(armarEncabezadoYFiltros(), BorderLayout.NORTH);

        estilizarTabla(tablaOrdenes);
        tablaOrdenes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotones();
            }
        });
        add(new JScrollPane(tablaOrdenes), BorderLayout.CENTER);

        add(armarBotones(), BorderLayout.SOUTH);

        actualizarEstadoBotones();
        cargarOrdenes();
    }

    private JComponent armarEncabezadoYFiltros() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Ordenes de Servicio");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        encabezado.add(titulo, BorderLayout.WEST);

        BotonPlano botonNuevaOS = new BotonPlano("NUEVA OS");
        botonNuevaOS.addActionListener(e -> onNuevaOS());
        encabezado.add(botonNuevaOS, BorderLayout.EAST);
        panel.add(encabezado, BorderLayout.NORTH);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        filtros.setOpaque(false);

        JPanel filtroEstadoPanel = new JPanel(new BorderLayout(0, 4));
        filtroEstadoPanel.setOpaque(false);
        JLabel labelEstado = new JLabel("ESTADO");
        labelEstado.setForeground(Paleta.GRIS_TEXTO);
        labelEstado.setFont(new Font("Segoe UI", Font.BOLD, 10));
        comboFiltroEstado.addItem(new FiltroEstado(null));
        for (EstadoOrdenServicio estado : EstadoOrdenServicio.values()) {
            comboFiltroEstado.addItem(new FiltroEstado(estado));
        }
        comboFiltroEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboFiltroEstado.addActionListener(e -> cargarOrdenes());
        filtroEstadoPanel.add(labelEstado, BorderLayout.NORTH);
        filtroEstadoPanel.add(comboFiltroEstado, BorderLayout.CENTER);
        filtros.add(filtroEstadoPanel);

        JPanel filtroClientePanel = new JPanel(new BorderLayout(0, 4));
        filtroClientePanel.setOpaque(false);
        JLabel labelCliente = new JLabel("BUSCAR POR CLIENTE (ENTER PARA BUSCAR)");
        labelCliente.setForeground(Paleta.GRIS_TEXTO);
        labelCliente.setFont(new Font("Segoe UI", Font.BOLD, 10));
        campoBusquedaCliente.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoBusquedaCliente.setPreferredSize(new Dimension(260, 30));
        campoBusquedaCliente.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        campoBusquedaCliente.addActionListener(e -> aplicarFiltroCliente());
        filtroClientePanel.add(labelCliente, BorderLayout.NORTH);
        filtroClientePanel.add(campoBusquedaCliente, BorderLayout.CENTER);
        filtros.add(filtroClientePanel);

        panel.add(filtros, BorderLayout.CENTER);
        return panel;
    }

    private JComponent armarBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        botones.setOpaque(false);
        botonVerEditar.addActionListener(e -> onVerEditar());
        botonCerrar.addActionListener(e -> onCerrarOS());
        botonCancelar.addActionListener(e -> onCancelarOS());
        botones.add(botonVerEditar);
        botones.add(botonCerrar);
        botones.add(botonCancelar);
        return botones;
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

    // ---------------------------------------------------------------- Carga / filtro

    private EstadoOrdenServicio estadoFiltradoSeleccionado() {
        FiltroEstado filtro = (FiltroEstado) comboFiltroEstado.getSelectedItem();
        return filtro == null ? null : filtro.estado;
    }

    private void cargarOrdenes() {
        EstadoOrdenServicio estado = estadoFiltradoSeleccionado();
        setHabilitado(false);
        new SwingWorker<List<OrdenDeServicio>, Void>() {
            Exception error;

            @Override
            protected List<OrdenDeServicio> doInBackground() {
                try {
                    return estado == null
                            ? ordenDeServicioController.listarTodos()
                            : ordenDeServicioController.listarPorEstado(estado);
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    ordenesCargadas = get();
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                    ordenesCargadas = List.of();
                }
                aplicarFiltroCliente();
            }
        }.execute();
    }

    /** Filtra en memoria, sobre ordenesCargadas, por nombre de cliente. */
    private void aplicarFiltroCliente() {
        String texto = campoBusquedaCliente.getText().trim().toLowerCase();
        List<OrdenDeServicio> filtradas;
        if (texto.isEmpty()) {
            filtradas = ordenesCargadas;
        } else {
            filtradas = new ArrayList<>();
            for (OrdenDeServicio os : ordenesCargadas) {
                String nombreCliente = os.getCliente() == null ? "" : os.getCliente().getNombre();
                if (nombreCliente != null && nombreCliente.toLowerCase().contains(texto)) {
                    filtradas.add(os);
                }
            }
        }
        modeloOrdenes.setDatos(filtradas);
        actualizarEstadoBotones();
    }

    private OrdenDeServicio ordenSeleccionada() {
        int fila = tablaOrdenes.getSelectedRow();
        return fila < 0 ? null : modeloOrdenes.getOrden(fila);
    }

    private void actualizarEstadoBotones() {
        OrdenDeServicio seleccionada = ordenSeleccionada();
        boolean hay = seleccionada != null;
        botonVerEditar.setEnabled(hay);
        boolean puedeCerrarOCancelar = hay
                && seleccionada.getEstado() != EstadoOrdenServicio.CONCLUIDA
                && seleccionada.getEstado() != EstadoOrdenServicio.CANCELADA;
        botonCerrar.setEnabled(puedeCerrarOCancelar);
        botonCancelar.setEnabled(puedeCerrarOCancelar);
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

    // ---------------------------------------------------------------- Acciones

    private void onNuevaOS() {
        OrdenServicioFormDialog dialogo = new OrdenServicioFormDialog(ventana());
        dialogo.setVisible(true);
        if (!dialogo.isConfirmado()) {
            return;
        }
        OrdenDeServicio creada = dialogo.getOrdenCreada();
        cargarOrdenes();
        if (creada != null) {
            // Recien abierta la OS, se abre de una el detalle para cargar los items.
            new ItemOrdenServicioDialog(ventana(), creada).setVisible(true);
            cargarOrdenes();
        }
    }

    private void onVerEditar() {
        OrdenDeServicio seleccionada = ordenSeleccionada();
        if (seleccionada == null) {
            return;
        }
        new ItemOrdenServicioDialog(ventana(), seleccionada).setVisible(true);
        cargarOrdenes();
    }

    private void onCerrarOS() {
        OrdenDeServicio seleccionada = ordenSeleccionada();
        if (seleccionada == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Cerrar la OS N° " + seleccionada.getNumero() + "? Esta accion no se puede deshacer.",
                "Confirmar cierre", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    ordenDeServicioController.cerrar(seleccionada);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(OrdenesServicioPanel.this,
                            error.getMessage(), "No fue posible cerrar la OS", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarOrdenes();
            }
        }.execute();
    }

    private void onCancelarOS() {
        OrdenDeServicio seleccionada = ordenSeleccionada();
        if (seleccionada == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Cancelar la OS N° " + seleccionada.getNumero() + "? Esta accion no se puede deshacer.",
                "Confirmar cancelacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    ordenDeServicioController.cancelar(seleccionada);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(OrdenesServicioPanel.this,
                            error.getMessage(), "No fue posible cancelar la OS", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarOrdenes();
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Filtro de estado (combo)

    /** Item del combo de filtro: estado == null representa la opcion "TODOS". */
    private static class FiltroEstado {
        final EstadoOrdenServicio estado;

        FiltroEstado(EstadoOrdenServicio estado) {
            this.estado = estado;
        }

        @Override
        public String toString() {
            return estado == null ? "TODOS" : estado.toString();
        }
    }

    // ---------------------------------------------------------------- Modelo de tabla

    private static class TablaOrdenesModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"N°", "Cliente", "Maquinario", "Fecha Apertura", "Estado", "Valor Total (Gs.)"};
        private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");
        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private List<OrdenDeServicio> ordenes = List.of();

        void setDatos(List<OrdenDeServicio> ordenes) {
            this.ordenes = ordenes;
            fireTableDataChanged();
        }

        OrdenDeServicio getOrden(int fila) {
            return ordenes.get(fila);
        }

        @Override
        public int getRowCount() {
            return ordenes.size();
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
            OrdenDeServicio os = ordenes.get(fila);
            switch (columna) {
                case 0:
                    return os.getNumero();
                case 1:
                    return os.getCliente() == null ? "" : os.getCliente().getNombre();
                case 2:
                    return formatoMaquinario(os.getMaquinario());
                case 3:
                    return os.getFechaApertura() == null ? "" : os.getFechaApertura().format(FORMATO_FECHA);
                case 4:
                    return os.getEstado();
                case 5:
                    return FORMATO_VALOR.format(os.getValorTotal());
                default:
                    return "";
            }
        }

        private String formatoMaquinario(Maquinario maquinario) {
            if (maquinario == null) {
                return "";
            }
            if (maquinario.getIdentificacion() != null && !maquinario.getIdentificacion().isBlank()) {
                return maquinario.getTipo() + " - " + maquinario.getIdentificacion();
            }
            return maquinario.getTipo().toString();
        }
    }
}
