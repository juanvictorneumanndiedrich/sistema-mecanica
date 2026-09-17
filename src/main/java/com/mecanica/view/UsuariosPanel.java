package com.mecanica.view;

import com.mecanica.controller.UsuarioController;
import com.mecanica.enums.Permiso;
import com.mecanica.model.Usuario;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Pantalla real del area "Usuarios y Permisos": lista de los usuarios del
 * sistema con su estado (activo/inactivo) y un resumen de las areas que
 * tiene habilitadas. Los permisos son booleanos individuales por usuario
 * -- no hay roles ni grupos -- y se editan uno por uno en
 * UsuarioFormDialog, marcando exactamente las mismas 6 areas que
 * MainView lee para armar el menu lateral.
 *
 * El cambio de contrasena es una accion aparte y deliberada
 * (CambiarClaveDialog): el formulario de edicion normal (UsuarioFormDialog)
 * nunca toca la clave, para no pisarla sin querer.
 *
 * Las llamadas al Controller (que abren Session de Hibernate) corren en
 * SwingWorker para no trabar la interfaz, siguiendo el mismo patron ya
 * usado en ClientesMaquinariosPanel.
 */
public class UsuariosPanel extends JPanel implements PanelActualizable {

    private final UsuarioController usuarioController = new UsuarioController();

    private final TablaUsuariosModel modeloUsuarios = new TablaUsuariosModel();
    private final JTable tablaUsuarios = new JTable(modeloUsuarios);

    private final BotonPlano botonEditar = new BotonPlano("EDITAR", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonCambiarClave = new BotonPlano("CAMBIAR CONTRASENA", Paleta.AZUL, Paleta.AZUL_CLARO);
    private final BotonPlano botonEliminar = new BotonPlano("ELIMINAR", Paleta.ROJO_ERROR, Paleta.ROJO_ERROR.brighter());

    public UsuariosPanel() {
        super(new BorderLayout());
        setBackground(Paleta.GRIS_FONDO);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(armarEncabezado(), BorderLayout.NORTH);
        add(armarCentro(), BorderLayout.CENTER);
        add(armarBotones(), BorderLayout.SOUTH);

        actualizarEstadoBotones();
        cargarUsuarios();
    }

    /** Recarga la lista de usuarios al entrar en esta area. */
    @Override
    public void actualizar() {
        cargarUsuarios();
    }

    private JComponent armarEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout(8, 0));
        encabezado.setOpaque(false);
        encabezado.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel titulo = new JLabel("Usuarios y Permisos");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        encabezado.add(titulo, BorderLayout.WEST);

        BotonPlano botonNuevo = new BotonPlano("NUEVO USUARIO");
        botonNuevo.addActionListener(e -> onNuevoUsuario());
        BotonPlano botonActividad = new BotonPlano("REGISTRO DE ACTIVIDAD", Paleta.GRIS_TEXTO, Paleta.GRIS_TEXTO.brighter());
        botonActividad.addActionListener(e -> new RegistroActividadDialog(ventana()).setVisible(true));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(botonActividad);
        acciones.add(botonNuevo);
        encabezado.add(acciones, BorderLayout.EAST);

        return encabezado;
    }

    private JComponent armarCentro() {
        estilizarTabla(tablaUsuarios);
        tablaUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotones();
            }
        });
        return new JScrollPane(tablaUsuarios);
    }

    private JComponent armarBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        botonEditar.addActionListener(e -> onEditarUsuario());
        botonCambiarClave.addActionListener(e -> onCambiarClave());
        botonEliminar.addActionListener(e -> onEliminarUsuario());
        botones.add(botonEditar);
        botones.add(botonCambiarClave);
        botones.add(botonEliminar);
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

    // ---------------------------------------------------------------- Carga de datos

    private void cargarUsuarios() {
        setHabilitado(false);
        new SwingWorker<List<Usuario>, Void>() {
            Exception error;

            @Override
            protected List<Usuario> doInBackground() {
                try {
                    return usuarioController.listarTodos();
                } catch (Exception e) {
                    error = e;
                    return List.of();
                }
            }

            @Override
            protected void done() {
                setHabilitado(true);
                try {
                    modeloUsuarios.setDatos(get());
                } catch (Exception e) {
                    error = e;
                }
                if (error != null) {
                    mostrarErrorConexion();
                }
                actualizarEstadoBotones();
            }
        }.execute();
    }

    private Usuario usuarioSeleccionado() {
        int fila = tablaUsuarios.getSelectedRow();
        return fila < 0 ? null : modeloUsuarios.getUsuario(fila);
    }

    private void actualizarEstadoBotones() {
        boolean hay = usuarioSeleccionado() != null;
        botonEditar.setEnabled(hay);
        botonCambiarClave.setEnabled(hay);
        botonEliminar.setEnabled(hay);
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

    // ---------------------------------------------------------------- CRUD Usuario

    private void onNuevoUsuario() {
        UsuarioFormDialog dialogo = new UsuarioFormDialog(ventana(), null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            crearUsuario(dialogo.getUsuario(), dialogo.getClaveInicial());
        }
    }

    private void onEditarUsuario() {
        Usuario seleccionado = usuarioSeleccionado();
        if (seleccionado == null) {
            return;
        }
        UsuarioFormDialog dialogo = new UsuarioFormDialog(ventana(), seleccionado);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            actualizarUsuario(dialogo.getUsuario());
        }
    }

    private void crearUsuario(Usuario usuario, String claveInicial) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    usuarioController.registrar(usuario, claveInicial);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            error.getMessage(), "No fue posible guardar", JOptionPane.ERROR_MESSAGE);
                }
                // recarga siempre: si no se guardo, la tabla vuelve a mostrar lo que hay en la base
                cargarUsuarios();
            }
        }.execute();
    }

    private void actualizarUsuario(Usuario usuario) {
        setHabilitado(false);
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    usuarioController.actualizarDatos(usuario);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            error.getMessage(), "No fue posible guardar", JOptionPane.ERROR_MESSAGE);
                }
                // recarga siempre: si no se guardo, la tabla vuelve a mostrar lo que hay en la base
                cargarUsuarios();
            }
        }.execute();
    }

    private void onEliminarUsuario() {
        Usuario seleccionado = usuarioSeleccionado();
        if (seleccionado == null) {
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Eliminar el usuario \"" + seleccionado.getNombre() + "\"?",
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
                    usuarioController.eliminar(seleccionado);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    String mensaje = error instanceof IllegalArgumentException || error instanceof IllegalStateException
                            ? error.getMessage()
                            : "No fue posible eliminar: el usuario tiene registros vinculados.";
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            mensaje, "No fue posible eliminar", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarUsuarios();
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Cambiar contrasena

    private void onCambiarClave() {
        Usuario seleccionado = usuarioSeleccionado();
        if (seleccionado == null) {
            return;
        }
        CambiarClaveDialog dialogo = new CambiarClaveDialog(ventana(), seleccionado.getNombre());
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
                    usuarioController.cambiarClave(seleccionado, dialogo.getNuevaClave());
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                setHabilitado(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(UsuariosPanel.this,
                            error.getMessage(), "No fue posible cambiar la contrasena", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(UsuariosPanel.this,
                        "Contrasena actualizada.", "Listo", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    // ---------------------------------------------------------------- Modelo de tabla

    private static class TablaUsuariosModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Nombre", "Login", "Estado", "Permisos habilitados"};

        private List<Usuario> usuarios = List.of();

        void setDatos(List<Usuario> usuarios) {
            this.usuarios = usuarios;
            fireTableDataChanged();
        }

        Usuario getUsuario(int fila) {
            return usuarios.get(fila);
        }

        @Override
        public int getRowCount() {
            return usuarios.size();
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
            Usuario usuario = usuarios.get(fila);
            switch (columna) {
                case 0:
                    return usuario.getNombre();
                case 1:
                    return usuario.getLogin();
                case 2:
                    return usuario.isActivo() ? "Activo" : "Inactivo";
                case 3:
                    return resumenPermisos(usuario);
                default:
                    return "";
            }
        }

        private String resumenPermisos(Usuario usuario) {
            StringBuilder resumen = new StringBuilder();
            int acciones = 0;
            for (Permiso permiso : Permiso.values()) {
                if (!usuario.tiene(permiso)) {
                    continue;
                }
                if (!permiso.esArea()) {
                    acciones++;
                    continue;
                }
                if (resumen.length() > 0) {
                    resumen.append(", ");
                }
                resumen.append(permiso.getEtiqueta().replace(" (incluye el registro de actividad)", ""));
            }
            if (resumen.length() == 0) {
                return "Sin permisos";
            }
            return acciones == 0 ? resumen.toString() : resumen + "  (+" + acciones + " de accion)";
        }
    }
}
