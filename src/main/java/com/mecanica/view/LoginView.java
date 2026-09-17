package com.mecanica.view;

import com.mecanica.controller.UsuarioController;
import com.mecanica.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Pantalla de acceso al sistema. Es un JDialog modal: se muestra antes de
 * la ventana principal y bloquea hasta que el usuario se autentica o
 * cierra la ventana. Quien orquesta eso es Main.iniciarSesion(), que
 * despues lee el resultado con getUsuarioAutenticado().
 *
 * La autenticacion corre en un SwingWorker y no en el hilo de la
 * interfaz, porque la primera consulta dispara la creacion de la
 * SessionFactory de Hibernate y puede tardar varios segundos -- si
 * corriera en el hilo de la interfaz, la ventana quedaria congelada y
 * pareceria trabada.
 */
public class LoginView extends JDialog {

    private final UsuarioController usuarioController = new UsuarioController();

    private JTextField campoUsuario;
    private JPasswordField campoClave;
    private JLabel labelError;
    private BotonPlano botonIngresar;

    /** Queda en null si el usuario cierra la ventana sin autenticarse. */
    private Usuario usuarioAutenticado;

    public LoginView() {
        super((Frame) null, "Taller JB - Acceso", true);
        armarPantalla();
    }

    /** Resultado del login: el usuario autenticado, o null si se cancelo. */
    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    private void armarPantalla() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(400, 470);
        setResizable(false);
        setLayout(new BorderLayout());

        add(armarEncabezado(), BorderLayout.NORTH);
        add(armarFormulario(), BorderLayout.CENTER);

        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                campoUsuario.requestFocusInWindow();
                verificarAccesoInicial();
            }
        });
    }

    /**
     * Apenas se abre la pantalla, conecta con la base en segundo plano y
     * crea el usuario "admin" si la tabla de usuarios todavia esta vacia.
     * Sirve para dos cosas: permitir el primer acceso a una base recien
     * creada, y adelantar el arranque de Hibernate (que tarda unos
     * segundos) mientras el usuario todavia esta escribiendo.
     */
    private void verificarAccesoInicial() {
        setCargando(true, "CONECTANDO...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                if (usuarioController.asegurarUsuarioInicial()) {
                    // base recien creada: el admin ya nace con todos los permisos,
                    // no hay nada que migrar.
                    return "Primer acceso -- usuario: admin / contrasena: admin";
                }
                // base que ya tenia usuarios: si es la primera vez que corre esta
                // version (con los 9 permisos de accion nuevos), les da a los
                // usuarios existentes los permisos de su area, para que no pierdan
                // de golpe accesos que ya tenian antes de la actualizacion.
                int afectados = usuarioController.migrarPermisosDeAccionSiHaceFalta();
                if (afectados > 0) {
                    return "El sistema se actualizo con permisos mas detallados.\n"
                            + afectados + " usuario(s) recibieron automaticamente los permisos de su area "
                            + "(revise Usuarios y Permisos para ajustarlos si hace falta).";
                }
                return null;
            }

            @Override
            protected void done() {
                setCargando(false, null);
                try {
                    String aviso = get();
                    if (aviso != null) {
                        mostrarAviso(aviso);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarError("No fue posible conectar a la base de datos.");
                }
            }
        }.execute();
    }

    /** Franja superior azul con degradado, el engranaje y el nombre del taller. */
    private JComponent armarEncabezado() {
        JPanel encabezado = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Paleta.AZUL_OSCURO,
                        getWidth(), getHeight(), Paleta.AZUL_MEDIO));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        encabezado.setPreferredSize(new Dimension(0, 160));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;

        encabezado.add(new IconoEngranaje(52, Paleta.BLANCO), gbc);

        JLabel titulo = new JLabel("TALLER JB");
        titulo.setForeground(Paleta.BLANCO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 0, 0, 0);
        encabezado.add(titulo, gbc);

        JLabel subtitulo = new JLabel("Sistema de Gestion");
        subtitulo.setForeground(Paleta.AZUL_TENUE);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = 2;
        gbc.insets = new Insets(2, 0, 0, 0);
        encabezado.add(subtitulo, gbc);

        return encabezado;
    }

    private JComponent armarFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Paleta.GRIS_FONDO);
        formulario.setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        formulario.add(crearEtiqueta("USUARIO"), gbc);

        campoUsuario = new JTextField();
        estilizarCampo(campoUsuario);
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoUsuario, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(crearEtiqueta("CONTRASENA"), gbc);

        campoClave = new JPasswordField();
        estilizarCampo(campoClave);
        gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 0, 0);
        formulario.add(campoClave, gbc);

        // Espacio reservado para el mensaje de error, asi el formulario no
        // "salta" cuando aparece o desaparece el texto.
        labelError = new JLabel(" ");
        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 0, 0, 0);
        formulario.add(labelError, gbc);

        botonIngresar = new BotonPlano("INGRESAR");
        botonIngresar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        botonIngresar.setPreferredSize(new Dimension(0, 42));
        botonIngresar.addActionListener(e -> onIngresar());
        gbc.gridy = 5;
        gbc.insets = new Insets(12, 0, 0, 0);
        formulario.add(botonIngresar, gbc);

        getRootPane().setDefaultButton(botonIngresar);
        return formulario;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(Paleta.GRIS_TEXTO);
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return etiqueta;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setPreferredSize(new Dimension(0, 36));
        campo.setBackground(Paleta.BLANCO);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
    }

    private void onIngresar() {
        String usuario = campoUsuario.getText().trim();
        String clave = new String(campoClave.getPassword());

        if (usuario.isEmpty() || clave.isEmpty()) {
            mostrarError("Ingrese usuario y contrasena.");
            return;
        }

        mostrarError(null);
        setCargando(true, "VERIFICANDO...");

        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() {
                return usuarioController.autenticar(usuario, clave);
            }

            @Override
            protected void done() {
                setCargando(false, null);
                try {
                    Usuario encontrado = get();
                    if (encontrado == null) {
                        mostrarError("Usuario o contrasena invalidos.");
                        campoClave.setText("");
                        campoClave.requestFocusInWindow();
                        return;
                    }
                    usuarioAutenticado = encontrado;
                    dispose();
                } catch (Exception e) {
                    e.printStackTrace();
                    mostrarError("No fue posible conectar a la base de datos.");
                }
            }
        }.execute();
    }

    /**
     * Bloquea la pantalla mientras corre una consulta y muestra en que
     * esta (conectando, verificando). Con cargando=false vuelve al estado
     * normal y el texto se ignora.
     */
    private void setCargando(boolean cargando, String textoCargando) {
        botonIngresar.setEnabled(!cargando);
        botonIngresar.setText(cargando ? textoCargando : "INGRESAR");
        campoUsuario.setEnabled(!cargando);
        campoClave.setEnabled(!cargando);
    }

    /** Mensaje de error, en rojo. */
    private void mostrarError(String mensaje) {
        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setText(mensaje == null || mensaje.isBlank() ? " " : mensaje);
    }

    /** Mensaje informativo, en azul (no es un error). */
    private void mostrarAviso(String mensaje) {
        labelError.setForeground(Paleta.AZUL);
        labelError.setText(mensaje == null || mensaje.isBlank() ? " " : mensaje);
    }
}
