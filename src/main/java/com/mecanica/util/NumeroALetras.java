package com.mecanica.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte un valor en guaranies a letras, para el texto del recibo
 * ("la suma de Gs. 2.098.309 (dos millones noventa y ocho mil trescientos
 * nueve guaranies)"). El guarani no usa centavos, asi que se redondea a entero.
 */
public final class NumeroALetras {

    private static final String[] UNIDADES = {"", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete",
            "ocho", "nueve", "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete",
            "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidos", "veintitres", "veinticuatro",
            "veinticinco", "veintiseis", "veintisiete", "veintiocho", "veintinueve"};
    private static final String[] DECENAS = {"", "", "", "treinta", "cuarenta", "cincuenta", "sesenta",
            "setenta", "ochenta", "noventa"};
    private static final String[] CENTENAS = {"", "ciento", "doscientos", "trescientos", "cuatrocientos",
            "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"};

    private NumeroALetras() {
        // clase utilitaria: no debe ser instanciada
    }

    public static String guaranies(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        long numero = valor.setScale(0, RoundingMode.HALF_UP).longValue();
        String prefijo = numero < 0 ? "menos " : "";
        numero = Math.abs(numero);
        String letras = numero == 0 ? "cero" : apocopar(convertir(numero)); // "veintiun guaranies"
        return prefijo + letras + " guaranies";
    }

    private static String convertir(long n) {
        if (n >= 1_000_000_000_000L) {
            return String.valueOf(n);
        }
        StringBuilder texto = new StringBuilder();
        long millones = n / 1_000_000;
        long miles = (n % 1_000_000) / 1000;
        long resto = n % 1000;
        if (millones > 0) {
            texto.append(millones == 1 ? "un millon" : apocopar(hastaMil(millones)) + " millones");
        }
        if (miles > 0) {
            separar(texto);
            texto.append(miles == 1 ? "mil" : apocopar(hastaMil(miles)) + " mil");
        }
        if (resto > 0) {
            separar(texto);
            texto.append(hastaMil(resto));
        }
        return texto.toString();
    }

    /** 1..999 (hasta 999.999 millones con la division de arriba). */
    private static String hastaMil(long n) {
        if (n >= 1000) {
            return convertir(n);
        }
        if (n == 100) {
            return "cien";
        }
        StringBuilder texto = new StringBuilder(CENTENAS[(int) (n / 100)]);
        int dos = (int) (n % 100);
        if (dos > 0) {
            separar(texto);
            if (dos < 30) {
                texto.append(UNIDADES[dos]);
            } else {
                texto.append(DECENAS[dos / 10]);
                if (dos % 10 > 0) {
                    texto.append(" y ").append(UNIDADES[dos % 10]);
                }
            }
        }
        return texto.toString();
    }

    /** "veintiuno mil" -> "veintiun mil", "treinta y uno millones" -> "treinta y un millones". */
    private static String apocopar(String texto) {
        if (texto.endsWith("veintiuno")) {
            return texto.substring(0, texto.length() - 1);
        }
        if (texto.endsWith("uno")) {
            return texto.substring(0, texto.length() - 1);
        }
        return texto;
    }

    private static void separar(StringBuilder texto) {
        if (texto.length() > 0) {
            texto.append(' ');
        }
    }
}
