package com.mecanica.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Formato de numeros y montos en guaranies, siempre igual sin importar el
 * idioma/region configurado en Windows.
 *
 * <p>Antes cada pantalla creaba su propio {@code new DecimalFormat("#,##0")}
 * con el {@link DecimalFormatSymbols} del locale por defecto de la maquina:
 * en una Windows configurada en ingles el separador de miles sale como coma
 * ("1,500,000") en vez del punto que se usa en Paraguay ("1.500.000"). Aca
 * los simbolos se fijan a mano (no dependen del locale del sistema) y se
 * comparten en un solo lugar.
 */
public final class Moneda {

    private static final DecimalFormatSymbols SIMBOLOS = new DecimalFormatSymbols();
    static {
        SIMBOLOS.setGroupingSeparator('.');
        SIMBOLOS.setDecimalSeparator(',');
    }

    /** Monto sin decimales (guaranies no tienen centavos), con separador de miles fijo. */
    private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0", SIMBOLOS);

    /** Cantidad con hasta 3 decimales (horas, litros, etc.), con separador de miles fijo. */
    private static final DecimalFormat FORMATO_CANTIDAD = new DecimalFormat("#,##0.###", SIMBOLOS);

    private Moneda() {
    }

    public static String formatear(BigDecimal valor) {
        return FORMATO_VALOR.format(valor == null ? BigDecimal.ZERO : valor);
    }

    public static String formatearConGs(BigDecimal valor) {
        return "Gs. " + formatear(valor);
    }

    public static String formatearCantidad(BigDecimal valor) {
        return FORMATO_CANTIDAD.format(valor == null ? BigDecimal.ZERO : valor);
    }

    /**
     * Una instancia propia de DecimalFormat para pantallas que la guardan en
     * un campo estatico y la usan muchas veces (mismos simbolos fijos, sin
     * depender del locale de Windows).
     */
    public static DecimalFormat nuevoFormatoValor() {
        return new DecimalFormat("#,##0", SIMBOLOS);
    }

    public static DecimalFormat nuevoFormatoCantidad() {
        return new DecimalFormat("#,##0.###", SIMBOLOS);
    }
}
