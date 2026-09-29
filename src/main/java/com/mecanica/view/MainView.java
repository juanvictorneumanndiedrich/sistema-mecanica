package com.mecanica.view;

import com.mecanica.controller.AuditoriaController;
import com.mecanica.model.Usuario;
import com.mecanica.util.Sesion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana principal, abierta despues del login. Barra superior azul,
 * menu lateral con las 6 areas de navegacion (solo aparecen las que el
 * usuario logueado tiene permiso) y un panel central con CardLayout que
 * cambia de contenido segun el area seleccionada. Las 6 areas (Clientes y
 * Maquinarios, Ordenes de Servicio, Compras y Proveedores, Financiero,
 * Empleados y Socios, Usuarios y Permisos) ya tienen su pantalla real.
 */
public class MainView extends JFrame {

    static final String CARD_CLIENTES_MAQUINARIOS = "clientesMaquinarios";
    static final String CARD_ORDENES_SERVICIO = "ordenesServicio";
    static final String CARD_COMPRAS_PROVEEDORES = "comprasProveedores";
    static final String CARD_FINANCIERO = "financiero";
    static final String CARD_EMPLEADOS_SOCIOS = "empleadosSocios";
    static final String CARD_USUARIOS = "usuarios";
    private static final String CARD_SIN_PERMISOS = "sinPermisos";

    private final Usuario usuarioLogueado;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelContenido = new JPanel(cardLayout);
    private final List<BotonMenu> botonesMenu = new ArrayList<>();

    /**
     * Paneles que saben recargar sus propios datos (ver PanelActualizable),
     * indexados por el nombre de card. Se usa en seleccionarArea() para que
     * cualquier cambio hecho en otra pantalla (un pago, el cierre de una OS,
     * etc.) ya aparezca actualizado en cuanto el usuario entra de nuevo en
     * esa area, sin tener que cerrar y abrir el sistema.
     */
    private final Map<String, PanelActualizable> panelesActualizables = new LinkedHashMap<>();

    /** Paneles de cada area, por nombre de card (para saber que pestana esta abierta al pedir ayuda). */
    private final Map<String, JComponent> paneles = new LinkedHashMap<>();

    /** Areas del menu en el orden en que aparecen: {etiqueta, nombre de card}. Las usa el recorrido de bienvenida. */
    private final List<String[]> areasDelMenu = new ArrayList<>();

    /** Card que se esta mostrando ahora. */
    private String areaActual;

    private final BotonCircular botonAyuda = new BotonCircular(BotonCircular.SIGNO_AYUDA);

    public MainView(Usuario usuarioLogueado) {
        super("Taller JB");
        this.usuarioLogueado = usuarioLogueado;
        armarPantalla();
    }

    private void armarPantalla() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Tamano de respaldo para cuando el usuario restaura la ventana
        // (deja de estar maximizada); el arranque real siempre es maximizado.
        setSize(1040, 660);
        setMinimumSize(new Dimension(900, 560));
        setLayout(new BorderLayout());

        add(armarSuperior(), BorderLayout.NORTH);
        add(armarMenuLateral(), BorderLayout.WEST);
        add(armarContenido(), BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Deja la primera area ya abierta, para que el sistema no arranque
        // con el panel central vacio.
        if (!botonesMenu.isEmpty()) {
            botonesMenu.get(0).doClick();
        } else {
            cardLayout.show(panelContenido, CARD_SIN_PERMISOS);
        }
    }

    /** Barra superior azul: identidad del sistema y el usuario conectado. */
    private JComponent armarSuperior() {
        JPanel superior = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Paleta.AZUL_OSCURO,
                        getWidth(), 0, Paleta.AZUL_MEDIO));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        superior.setPreferredSize(new Dimension(0, 58));
        superior.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        JPanel identidad = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        identidad.setOpaque(false);
        identidad.add(new IconoEngranaje(26, Paleta.BLANCO));

        JLabel marca = new JLabel("TALLER JB");
        marca.setForeground(Paleta.BLANCO);
        marca.setFont(new Font("Segoe UI", Font.BOLD, 17));
        identidad.add(marca);
        superior.add(identidad, BorderLayout.WEST);

        JPanel sesion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        sesion.setOpaque(false);

        // Ayuda en pantalla: "?" con los pasos de la pantalla abierta (solo con la ayuda
        // prendida) y el engranaje con las opciones de la ayuda (siempre visible).
        botonAyuda.setToolTipText("Ayuda de esta pantalla");
        botonAyuda.addActionListener(e -> mostrarAyudaDePantalla());
        botonAyuda.setVisible(Ayuda.activa());
        Ayuda.setAlCambiar(() -> botonAyuda.setVisible(Ayuda.activa()));
        sesion.add(botonAyuda);

        BotonCircular botonOpciones = new BotonCircular(BotonCircular.ENGRANAJE);
        botonOpciones.setToolTipText("Configuracion");
        botonOpciones.addActionListener(e -> mostrarMenuOpciones(botonOpciones));
        sesion.add(botonOpciones);

        JLabel bienvenido = new JLabel(usuarioLogueado.getNombre());
        bienvenido.setForeground(Paleta.AZUL_TENUE);
        bienvenido.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sesion.add(bienvenido);

        BotonPlano botonSalir = new BotonPlano("SALIR", Paleta.AZUL, Paleta.AZUL_CLARO);
        botonSalir.addActionListener(e -> salir());
        sesion.add(botonSalir);
        superior.add(sesion, BorderLayout.EAST);

        return superior;
    }

    /** Menu lateral blanco con un boton por area permitida. */
    private JComponent armarMenuLateral() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Paleta.BLANCO);
        menu.setPreferredSize(new Dimension(232, 0));
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(14, 0, 14, 0)));

        if (usuarioLogueado.isPermisoClientesMaquinarios()) {
            menu.add(crearBotonMenu("Clientes y Maquinarios", CARD_CLIENTES_MAQUINARIOS));
        }
        if (usuarioLogueado.isPermisoOrdenesServicio()) {
            menu.add(crearBotonMenu("Ordenes de Servicio", CARD_ORDENES_SERVICIO));
        }
        if (usuarioLogueado.isPermisoComprasProveedores()) {
            menu.add(crearBotonMenu("Compras y Proveedores", CARD_COMPRAS_PROVEEDORES));
        }
        if (usuarioLogueado.isPermisoFinanciero()) {
            menu.add(crearBotonMenu("Financiero", CARD_FINANCIERO));
        }
        if (usuarioLogueado.isPermisoEmpleadosSocios()) {
            menu.add(crearBotonMenu("Empleados y Socios", CARD_EMPLEADOS_SOCIOS));
        }
        if (usuarioLogueado.isPermisoUsuarios()) {
            menu.add(crearBotonMenu("Usuarios y Permisos", CARD_USUARIOS));
        }

        menu.add(Box.createVerticalGlue());
        return menu;
    }

    private BotonMenu crearBotonMenu(String etiqueta, String nombreCard) {
        BotonMenu boton = new BotonMenu(etiqueta);
        boton.addActionListener(e -> seleccionarArea(boton, nombreCard));
        botonesMenu.add(boton);
        areasDelMenu.add(new String[] {etiqueta, nombreCard});
        return boton;
    }

    /**
     * Marca el boton elegido, muestra el area correspondiente y le pide al
     * panel que recargue sus datos -- asi cualquier cambio hecho en otra
     * pantalla ya aparece actualizado.
     */
    private void seleccionarArea(BotonMenu elegido, String nombreCard) {
        for (BotonMenu boton : botonesMenu) {
            boton.setSeleccionado(boton == elegido);
        }
        cardLayout.show(panelContenido, nombreCard);
        areaActual = nombreCard;
        PanelActualizable panel = panelesActualizables.get(nombreCard);
        if (panel != null) {
            panel.actualizar();
        }
    }

    private JComponent armarContenido() {
        panelContenido.setBackground(Paleta.GRIS_FONDO);
        agregarArea(CARD_CLIENTES_MAQUINARIOS, new ClientesMaquinariosPanel());
        agregarArea(CARD_ORDENES_SERVICIO, new OrdenesServicioPanel());
        agregarArea(CARD_COMPRAS_PROVEEDORES, new ComprasProveedoresPanel());
        agregarArea(CARD_FINANCIERO, new FinancieroPanel());
        agregarArea(CARD_EMPLEADOS_SOCIOS, new EmpleadosSociosPanel());
        agregarArea(CARD_USUARIOS, new UsuariosPanel());
        panelContenido.add(crearPanelSinPermisos(), CARD_SIN_PERMISOS);
        return panelContenido;
    }

    /** Agrega el panel al CardLayout y lo guarda para poder actualizarlo despues. */
    private void agregarArea(String nombreCard, JComponent panel) {
        panelContenido.add(panel, nombreCard);
        paneles.put(nombreCard, panel);
        if (panel instanceof PanelActualizable actualizable) {
            panelesActualizables.put(nombreCard, actualizable);
        }
    }

    private JComponent crearPanelSinPermisos() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Paleta.GRIS_FONDO);

        JLabel aviso = new JLabel("Su usuario no tiene ningun area habilitada.");
        aviso.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        aviso.setForeground(Paleta.GRIS_TEXTO);
        panel.add(aviso);

        return panel;
    }

    // ---------------------------------------------------------------- ayuda en pantalla

    /** Boton "?": pasos de la pantalla (y pestana) que esta abierta ahora. */
    private void mostrarAyudaDePantalla() {
        String pestana = null;
        JTabbedPane pestanas = buscarPestanas(paneles.get(areaActual));
        if (pestanas != null && pestanas.getSelectedIndex() >= 0) {
            pestana = pestanas.getTitleAt(pestanas.getSelectedIndex());
        }
        Ayuda.mostrarGuia(this, areaActual, pestana);
    }

    /** Primer grupo de pestanas dentro del panel del area (null si el area no tiene pestanas). */
    private static JTabbedPane buscarPestanas(Component componente) {
        if (componente instanceof JTabbedPane pestanas) {
            return pestanas;
        }
        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) {
                JTabbedPane encontrado = buscarPestanas(hijo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    /** Engranaje: prender/apagar la ayuda y volver a ver el recorrido de bienvenida. */
    private void mostrarMenuOpciones(JComponent origen) {
        JPopupMenu menu = new JPopupMenu();
        JCheckBoxMenuItem mostrarAyuda = new JCheckBoxMenuItem("Mostrar ayuda", Ayuda.activa());
        mostrarAyuda.addActionListener(e -> Ayuda.setActiva(this, mostrarAyuda.isSelected()));
        menu.add(mostrarAyuda);
        JMenuItem recorrido = new JMenuItem("Ver recorrido de bienvenida");
        recorrido.setEnabled(!areasDelMenu.isEmpty());
        recorrido.addActionListener(e -> mostrarRecorrido());
        menu.add(recorrido);
        menu.show(origen, origen.getWidth() - menu.getPreferredSize().width, origen.getHeight() + 4);
    }

    /**
     * Primer ingreso de un usuario con la ayuda prendida: ofrece el recorrido
     * de bienvenida. "Mas tarde" lo vuelve a ofrecer en el proximo ingreso;
     * las otras dos opciones no lo ofrecen mas (se puede ver igual desde el
     * engranaje).
     */
    void ofrecerRecorridoSiCorresponde() {
        Usuario usuario = Sesion.getUsuario();
        if (usuario == null || !usuario.isMostrarAyuda() || usuario.isTourVisto() || areasDelMenu.isEmpty()) {
            return;
        }
        String[] opciones = {"Ver recorrido", "Mas tarde", "No mostrar mas"};
        int eleccion = JOptionPane.showOptionDialog(this,
                "Bienvenido/a al sistema del Taller JB.\n\n"
                        + "Quiere ver un recorrido rapido que muestra donde esta cada cosa?",
                "Bienvenido/a", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opciones, opciones[0]);
        if (eleccion == 1 || eleccion == JOptionPane.CLOSED_OPTION) {
            return;
        }
        usuario.setTourVisto(true);
        Ayuda.guardarOpciones(this, usuario);
        if (eleccion == 0) {
            mostrarRecorrido();
        }
    }

    private void mostrarRecorrido() {
        String areaAntes = areaActual;
        new RecorridoBienvenida(this, usuarioLogueado.getNombre(), areasDelMenu, this::mostrarArea).setVisible(true);
        // al terminar, vuelve al area en la que estaba el usuario
        mostrarArea(areaAntes);
    }

    /** Muestra el area como si el usuario hubiera hecho clic en el menu. */
    private void mostrarArea(String nombreCard) {
        for (int i = 0; i < areasDelMenu.size(); i++) {
            if (areasDelMenu.get(i)[1].equals(nombreCard)) {
                seleccionarArea(botonesMenu.get(i), nombreCard);
                return;
            }
        }
    }

    /** Cierra esta ventana y vuelve al login. */
    private void salir() {
        ControlInactividad.detener();
        new AuditoriaController().registrar("CIERRE DE SESION", null);
        dispose();
        Main.iniciarSesion();
    }

    /**
     * Llamado por ControlInactividad despues de ControlInactividad.MINUTOS
     * minutos sin uso: cierra TODAS las ventanas del sistema (incluidos
     * dialogos abiertos y visores de reportes) y vuelve al login.
     */
    void cerrarPorInactividad() {
        new AuditoriaController().registrar("SESION CERRADA POR INACTIVIDAD",
                "Sin uso durante " + ControlInactividad.MINUTOS + " minutos");
        for (Window ventana : Window.getWindows()) {
            ventana.dispose();
        }
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    "La sesion se cerro por " + ControlInactividad.MINUTOS + " minutos sin uso.\n"
                            + "Ingrese de nuevo para continuar.",
                    "Sesion cerrada", JOptionPane.INFORMATION_MESSAGE);
            Main.iniciarSesion();
        });
    }

    /**
     * Boton redondo y chico de la barra superior: el "?" de la ayuda o el
     * engranaje de las opciones. Transparente, con un circulo claro al pasar
     * el mouse.
     */
    private static class BotonCircular extends JButton {

        static final int SIGNO_AYUDA = 0;
        static final int ENGRANAJE = 1;
        private static final int TAMANO = 30;

        private final int tipo;
        private boolean mouseEncima;

        BotonCircular(int tipo) {
            this.tipo = tipo;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            // sin borde ni margen: si no, el espacio interno del boton no deja lugar al engranaje
            setBorder(BorderFactory.createEmptyBorder());
            setMargin(new Insets(0, 0, 0, 0));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(TAMANO, TAMANO));
            setLayout(new GridBagLayout());
            if (tipo == ENGRANAJE) {
                add(new IconoEngranaje(18, Paleta.AZUL_TENUE));
            }
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    mouseEncima = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mouseEncima = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int lado = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth() - lado) / 2;
            int y = (getHeight() - lado) / 2;
            if (mouseEncima) {
                g2.setColor(Paleta.AZUL);
                g2.fillOval(x, y, lado, lado);
            }
            if (tipo == SIGNO_AYUDA) {
                g2.setColor(Paleta.AZUL_TENUE);
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawOval(x + 3, y + 3, lado - 6, lado - 6);
                g2.setColor(Paleta.BLANCO);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String texto = "?";
                g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Boton del menu lateral: sin relieve, alineado a la izquierda, y
     * pintado en azul cuando es el area seleccionada.
     */
    private static class BotonMenu extends JButton {

        private boolean seleccionado;
        private boolean mouseEncima;

        BotonMenu(String texto) {
            super(texto);
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 14));
            setForeground(Paleta.GRIS_TEXTO);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(200, 44));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    mouseEncima = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mouseEncima = false;
                    repaint();
                }
            });
        }

        void setSeleccionado(boolean seleccionado) {
            this.seleccionado = seleccionado;
            setForeground(seleccionado ? Paleta.BLANCO : Paleta.GRIS_TEXTO);
            setFont(new Font("Segoe UI", seleccionado ? Font.BOLD : Font.PLAIN, 14));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (seleccionado || mouseEncima) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(seleccionado ? Paleta.AZUL : Paleta.GRIS_FONDO);
                g2.fill(new RoundRectangle2D.Float(8, 3, getWidth() - 16f, getHeight() - 6f, 8, 8));
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
