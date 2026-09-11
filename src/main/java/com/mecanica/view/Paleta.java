package com.mecanica.view;

import java.awt.Color;

/**
 * Paleta de colores del sistema: azul, blanco y gris. Todas las pantallas
 * usan estas constantes, asi el sistema entero queda consistente y un
 * cambio de color se hace en un solo lugar.
 */
public final class Paleta {

    public static final Color AZUL_OSCURO = new Color(21, 47, 77);
    public static final Color AZUL_MEDIO = new Color(33, 74, 119);
    public static final Color AZUL = new Color(37, 109, 179);
    public static final Color AZUL_CLARO = new Color(52, 131, 208);
    public static final Color AZUL_TENUE = new Color(158, 185, 214);

    public static final Color BLANCO = new Color(255, 255, 255);

    public static final Color GRIS_FONDO = new Color(246, 247, 249);
    public static final Color GRIS_BORDE = new Color(214, 219, 226);
    public static final Color GRIS_TEXTO = new Color(105, 114, 127);
    public static final Color GRIS_DESHABILITADO = new Color(176, 182, 190);

    /** Fuera de la paleta: se usa solo para mensajes de error. */
    public static final Color ROJO_ERROR = new Color(192, 57, 43);

    private Paleta() {
        // clase utilitaria: no debe ser instanciada
    }
}
