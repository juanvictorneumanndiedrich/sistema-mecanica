package com.mecanica.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Boton de color plano con esquinas redondeadas, en los colores de la
 * Paleta. Se pinta a mano porque varios Look and Feel ignoran
 * setBackground() en un JButton comun, y el color terminaria distinto en
 * cada maquina.
 */
public class BotonPlano extends JButton {

    private final Color colorBase;
    private final Color colorEncima;
    private boolean mouseEncima;

    public BotonPlano(String texto) {
        this(texto, Paleta.AZUL, Paleta.AZUL_CLARO);
    }

    public BotonPlano(String texto, Color colorBase, Color colorEncima) {
        super(texto);
        this.colorBase = colorBase;
        this.colorEncima = colorEncima;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        setForeground(Paleta.BLANCO);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
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

        Color color;
        if (!isEnabled()) {
            color = Paleta.GRIS_DESHABILITADO;
        } else if (getModel().isPressed()) {
            color = colorBase.darker();
        } else if (mouseEncima) {
            color = colorEncima;
        } else {
            color = colorBase;
        }

        g2.setColor(color);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
        g2.dispose();
        super.paintComponent(g);
    }
}
