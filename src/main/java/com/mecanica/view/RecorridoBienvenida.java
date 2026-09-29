package com.mecanica.view;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Recorrido de bienvenida: una ventanita que va paso a paso por las areas
 * que el usuario tiene habilitadas. En cada paso la ventana principal
 * cambia a esa area (queda marcada en el menu de la izquierda), asi el
 * usuario ve la pantalla de la que se esta hablando.
 *
 * Se ofrece solo en el primer ingreso de cada usuario (ver
 * MainView.ofrecerRecorridoSiCorresponde) y se puede volver a ver cuando
 * se quiera desde el engranaje de la barra superior.
 */
class RecorridoBienvenida extends JDialog {

    /** Un paso: titulo, texto y el area que se muestra detras (null = ninguna). */
    record Paso(String titulo, String texto, String area) {
    }

    private final List<Paso> pasos;
    private final Consumer<String> mostrarArea;
    private int actual;

    private final JLabel labelContador = new JLabel();
    private final JLabel labelTitulo = new JLabel();
    private final JLabel labelTexto = new JLabel();
    private final BotonPlano botonAnterior = new BotonPlano("ANTERIOR", Paleta.GRIS_TEXTO, Paleta.GRIS_TEXTO.brighter());
    private final BotonPlano botonSiguiente = new BotonPlano("SIGUIENTE");

    /**
     * @param areas       areas habilitadas del usuario, en el orden del menu: etiqueta del menu -> nombre de la card
     * @param mostrarArea cambia la ventana principal a esa area
     */
    RecorridoBienvenida(JFrame principal, String nombreUsuario, List<String[]> areas, Consumer<String> mostrarArea) {
        super(principal, "Recorrido de bienvenida", Dialog.ModalityType.APPLICATION_MODAL);
        this.mostrarArea = mostrarArea;
        this.pasos = armarPasos(nombreUsuario, areas);
        armarPantalla();
        mostrarPaso(0);
        ubicar(principal);
    }

    private static List<Paso> armarPasos(String nombreUsuario, List<String[]> areas) {
        List<Paso> lista = new ArrayList<>();
        lista.add(new Paso("Bienvenido/a, " + nombreUsuario,
                "Este recorrido rapido le muestra donde esta cada cosa en el sistema. "
                        + "Use SIGUIENTE para avanzar; puede salir cuando quiera.",
                null));
        lista.add(new Paso("El menu de la izquierda",
                "Cada boton del menu abre un area del sistema. Solo aparecen las areas que su "
                        + "usuario tiene habilitadas.",
                null));
        for (String[] area : areas) {
            lista.add(new Paso(area[0], Ayuda.resumenArea(area[1]), area[1]));
        }
        lista.add(new Paso("Si tiene dudas",
                "Haga clic en el boton <b>?</b> (arriba a la derecha) para ver los pasos de la pantalla "
                        + "en la que esta. Deje el mouse quieto sobre un boton para ver que hace.<br><br>"
                        + "Para apagar la ayuda o ver de nuevo este recorrido, use el <b>engranaje</b> "
                        + "(arriba a la derecha).",
                null));
        return lista;
    }

    private void armarPantalla() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBackground(Paleta.BLANCO);
        contenido.setBorder(BorderFactory.createLineBorder(Paleta.AZUL, 2));

        JPanel cuerpo = new JPanel(new BorderLayout(0, 8));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        labelContador.setFont(new Font("Segoe UI", Font.BOLD, 10));
        labelContador.setForeground(Paleta.GRIS_TEXTO);
        labelTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        labelTitulo.setForeground(Paleta.AZUL_OSCURO);
        JPanel cabecera = new JPanel(new BorderLayout(0, 2));
        cabecera.setOpaque(false);
        cabecera.add(labelContador, BorderLayout.NORTH);
        cabecera.add(labelTitulo, BorderLayout.CENTER);
        cuerpo.add(cabecera, BorderLayout.NORTH);

        labelTexto.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelTexto.setForeground(Paleta.AZUL_OSCURO);
        labelTexto.setVerticalAlignment(SwingConstants.TOP);
        labelTexto.setPreferredSize(new Dimension(390, 140));
        cuerpo.add(labelTexto, BorderLayout.CENTER);
        contenido.add(cuerpo, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(Paleta.GRIS_FONDO);
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JButton botonSalir = new JButton("Salir del recorrido");
        botonSalir.setContentAreaFilled(false);
        botonSalir.setBorderPainted(false);
        botonSalir.setFocusPainted(false);
        botonSalir.setForeground(Paleta.GRIS_TEXTO);
        botonSalir.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        botonSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botonSalir.addActionListener(e -> dispose());
        pie.add(botonSalir, BorderLayout.WEST);

        JPanel navegacion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navegacion.setOpaque(false);
        botonAnterior.addActionListener(e -> mostrarPaso(actual - 1));
        botonSiguiente.addActionListener(e -> {
            if (actual == pasos.size() - 1) {
                dispose();
            } else {
                mostrarPaso(actual + 1);
            }
        });
        navegacion.add(botonAnterior);
        navegacion.add(botonSiguiente);
        pie.add(navegacion, BorderLayout.EAST);
        contenido.add(pie, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(botonSiguiente);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
    }

    private void mostrarPaso(int indice) {
        actual = Math.max(0, Math.min(indice, pasos.size() - 1));
        Paso paso = pasos.get(actual);
        labelContador.setText("PASO " + (actual + 1) + " DE " + pasos.size());
        labelTitulo.setText(paso.titulo());
        labelTexto.setText("<html><div style='width:300px'>" + paso.texto() + "</div></html>");
        botonAnterior.setEnabled(actual > 0);
        botonSiguiente.setText(actual == pasos.size() - 1 ? "TERMINAR" : "SIGUIENTE");
        if (paso.area() != null) {
            mostrarArea.accept(paso.area());
        }
        botonSiguiente.requestFocusInWindow();
    }

    /** Abajo a la derecha de la ventana principal, para no tapar el menu ni el contenido de arriba. */
    private void ubicar(JFrame principal) {
        Rectangle marco = principal.getBounds();
        int x = marco.x + marco.width - getWidth() - 40;
        int y = marco.y + marco.height - getHeight() - 40;
        setLocation(Math.max(marco.x, x), Math.max(marco.y, y));
    }
}
