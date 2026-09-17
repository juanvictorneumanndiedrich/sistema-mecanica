package com.mecanica.view;

import com.mecanica.controller.AuditoriaController;
import com.mecanica.controller.UsuarioController;
import com.mecanica.model.RegistroAuditoria;
import com.mecanica.model.Usuario;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta del registro de actividad (quien hizo que y cuando): pagos,
 * eliminaciones, cierre del mes, pago de salario, cambios de usuarios,
 * inicios y cierres de sesion. Solo lectura -- se abre desde Usuarios y
 * Permisos, asi que solo lo ve quien tiene ese permiso.
 */
public class RegistroActividadDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TODOS = "(todos)";

    private final AuditoriaController auditoriaController = new AuditoriaController();
    private final UsuarioController usuarioController = new UsuarioController();

    private final JTextField campoDesde = new JTextField(9);
    private final JTextField campoHasta = new JTextField(9);
    private final JComboBox<String> comboUsuario = new JComboBox<>();
    private final TablaRegistrosModel modelo = new TablaRegistrosModel();
    private final JTable tabla = new JTable(modelo);
    private final JLabel labelMensaje = new JLabel(" ");
    private BotonPlano botonFiltrar;

    public RegistroActividadDialog(Window padre) {
        super(padre, "Registro de actividad", ModalityType.APPLICATION_MODAL);

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setBackground(Paleta.GRIS_FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(etiqueta("DESDE"));
        filtros.add(campoDesde);
        filtros.add(etiqueta("HASTA"));
        filtros.add(campoHasta);
        filtros.add(etiqueta("USUARIO"));
        comboUsuario.addItem(TODOS);
        filtros.add(comboUsuario);
        BotonPlano botonFiltrar = new BotonPlano("FILTRAR");
        botonFiltrar.addActionListener(e -> buscar());
        filtros.add(botonFiltrar);
        contenido.add(filtros, BorderLayout.NORTH);
        this.botonFiltrar = botonFiltrar;

        LocalDate hoy = LocalDate.now();
        campoDesde.setText(hoy.minusDays(7).format(FORMATO_FECHA));
        campoHasta.setText(hoy.format(FORMATO_FECHA));

        tabla.setRowHeight(24);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getColumnModel().getColumn(0).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(0).setMaxWidth(140);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(1).setMaxWidth(220);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(2).setMaxWidth(260);
        tabla.setFillsViewportHeight(true);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        labelMensaje.setForeground(Paleta.GRIS_TEXTO);
        labelMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pie.add(labelMensaje, BorderLayout.WEST);
        BotonPlano botonCerrar = new BotonPlano("CERRAR", Paleta.GRIS_TEXTO, Paleta.GRIS_TEXTO.brighter());
        botonCerrar.addActionListener(e -> dispose());
        pie.add(botonCerrar, BorderLayout.EAST);
        contenido.add(pie, BorderLayout.SOUTH);

        setContentPane(contenido);
        setSize(new Dimension(1000, 600));
        // Enter en los campos de fecha/usuario dispara el filtro.
        getRootPane().setDefaultButton(botonFiltrar);
        setLocationRelativeTo(padre);

        cargarUsuarios();
        buscar();
    }

    private JLabel etiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(Paleta.GRIS_TEXTO);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return label;
    }

    private void cargarUsuarios() {
        new SwingWorker<List<Usuario>, Void>() {
            @Override
            protected List<Usuario> doInBackground() {
                return usuarioController.listarTodos();
            }

            @Override
            protected void done() {
                try {
                    for (Usuario usuario : get()) {
                        comboUsuario.addItem(usuario.getLogin());
                    }
                } catch (Exception e) {
                    // sin la lista, el filtro queda solo en (todos)
                }
            }
        }.execute();
    }

    private void buscar() {
        LocalDate desde;
        LocalDate hasta;
        try {
            desde = LocalDate.parse(campoDesde.getText().trim(), FORMATO_FECHA);
            hasta = LocalDate.parse(campoHasta.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            mostrarError("Fecha invalida. Use el formato dd/mm/aaaa.");
            return;
        }
        if (hasta.isBefore(desde)) {
            mostrarError("La fecha HASTA no puede ser anterior a DESDE.");
            return;
        }
        Object elegido = comboUsuario.getSelectedItem();
        String login = elegido == null || TODOS.equals(elegido) ? null : elegido.toString();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<RegistroAuditoria>, Void>() {
            @Override
            protected List<RegistroAuditoria> doInBackground() {
                return auditoriaController.listar(desde, hasta, login);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    List<RegistroAuditoria> registros = get();
                    modelo.setDatos(registros);
                    labelMensaje.setForeground(Paleta.GRIS_TEXTO);
                    labelMensaje.setText(registros.size() + " registro(s) en el periodo.");
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarError("No fue posible conectar a la base de datos.");
                }
            }
        }.execute();
    }

    private void mostrarError(String mensaje) {
        labelMensaje.setForeground(Paleta.ROJO_ERROR);
        labelMensaje.setText(mensaje);
    }

    private static class TablaRegistrosModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha y hora", "Usuario", "Accion", "Detalle"};

        private List<RegistroAuditoria> registros = new ArrayList<>();

        void setDatos(List<RegistroAuditoria> registros) {
            this.registros = registros;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return registros.size();
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
            RegistroAuditoria registro = registros.get(fila);
            switch (columna) {
                case 0:
                    return registro.getFechaHora() == null ? "" : registro.getFechaHora().format(FORMATO_FECHA_HORA);
                case 1:
                    return registro.getUsuarioNombre() == null
                            ? ""
                            : registro.getUsuarioNombre() + " (" + registro.getUsuarioLogin() + ")";
                case 2:
                    return registro.getAccion();
                case 3:
                    return registro.getDetalle() == null ? "" : registro.getDetalle();
                default:
                    return "";
            }
        }
    }
}
