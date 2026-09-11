package com.mecanica.view;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Engranaje dibujado con Java2D. Se dibuja por codigo para no depender de
 * un archivo de imagen dentro del proyecto, y porque asi se adapta a
 * cualquier tamano sin perder calidad.
 */
public class IconoEngranaje extends JComponent {

    private final int tamano;
    private final Color color;

    public IconoEngranaje(int tamano, Color color) {
        this.tamano = tamano;
        this.color = color;
        setPreferredSize(new Dimension(tamano, tamano));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double centro = tamano / 2.0;
        double radio = tamano * 0.34;
        double radioAgujero = tamano * 0.14;
        double anchoDiente = tamano * 0.14;
        double altoDiente = tamano * 0.18;
        int cantidadDientes = 8;

        Area engranaje = new Area(new Ellipse2D.Double(
                centro - radio, centro - radio, radio * 2, radio * 2));

        for (int i = 0; i < cantidadDientes; i++) {
            Rectangle2D.Double diente = new Rectangle2D.Double(
                    centro - anchoDiente / 2,
                    centro - radio - altoDiente * 0.5,
                    anchoDiente,
                    altoDiente);
            AffineTransform rotacion = AffineTransform.getRotateInstance(
                    Math.PI * 2 * i / cantidadDientes, centro, centro);
            engranaje.add(new Area(rotacion.createTransformedShape(diente)));
        }

        engranaje.subtract(new Area(new Ellipse2D.Double(
                centro - radioAgujero, centro - radioAgujero,
                radioAgujero * 2, radioAgujero * 2)));

        g2.setColor(color);
        g2.fill(engranaje);
        g2.dispose();
    }
}
