package com.mecanica.util;

import java.util.regex.Pattern;

/**
 * Validaciones de formato reutilizadas en los formularios Swing del sistema.
 * Reglas generales: no se puede guardar un campo obligatorio vacio, no se
 * permiten letras donde va un numero, no se permiten numeros donde va una
 * letra, y no se permiten simbolos fuera de los casos explicitamente
 * aceptados (por ejemplo el guion del RUC). Los patrones usan \p{L} para
 * aceptar letras con acentos y la enie.
 */
public final class Validaciones {

    private static final Pattern SOLO_LETRAS = Pattern.compile("[\\p{L} ]+");
    private static final Pattern SOLO_NUMEROS = Pattern.compile("[0-9]+");
    private static final Pattern DOCUMENTO = Pattern.compile("[0-9]+(-[0-9]+)?");
    private static final Pattern ALFANUMERICO_ESPACIO_GUION = Pattern.compile("[\\p{L}0-9 \\-]+");
    private static final Pattern ALFANUMERICO = Pattern.compile("[\\p{L}0-9]+");
    // nombre de persona o razon social: letras, espacios, punto, guion y apostrofe
    // (permite "Transportes Díaz S.R.L.", "Agro-Sur", "O'Higgins")
    private static final Pattern NOMBRE_O_RAZON_SOCIAL = Pattern.compile("[\\p{L}0-9 .\\-']+");

    private Validaciones() {
    }

    /** true si el texto es null, o queda vacio despues de recortar espacios. */
    public static boolean esVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    /** Solo letras (con acentos/enie) y espacios -- para nombres de persona. */
    public static boolean soloLetras(String texto) {
        return texto != null && SOLO_LETRAS.matcher(texto.trim()).matches();
    }

    /**
     * Nombre de persona o razon social -- letras, numeros, espacios, punto,
     * guion y apostrofe. Para clientes y proveedores, donde el nombre puede
     * ser una empresa ("Transportes Díaz S.R.L.", "Agro-Sur", "O'Higgins").
     */
    public static boolean nombreORazonSocial(String texto) {
        return texto != null && NOMBRE_O_RAZON_SOCIAL.matcher(texto.trim()).matches();
    }

    /** Solo digitos, sin letras ni simbolos -- para telefonos, cantidades, etc. */
    public static boolean soloNumeros(String texto) {
        return texto != null && SOLO_NUMEROS.matcher(texto.trim()).matches();
    }

    /** Digitos con un guion opcional seguido de mas digitos (CI o RUC con digito verificador). */
    public static boolean documentoValido(String texto) {
        return texto != null && DOCUMENTO.matcher(texto.trim()).matches();
    }

    /** Letras, numeros, espacios y guiones -- para identificaciones/placas y campos similares. */
    public static boolean alfanumericoConEspacioGuion(String texto) {
        return texto != null && ALFANUMERICO_ESPACIO_GUION.matcher(texto.trim()).matches();
    }

    /** Solo letras y numeros, sin espacios ni simbolos -- para logins/usuarios. */
    public static boolean soloLetrasYNumeros(String texto) {
        return texto != null && ALFANUMERICO.matcher(texto.trim()).matches();
    }

    /** true si el texto tiene al menos "minimo" caracteres (sin recortar espacios -- para claves). */
    public static boolean longitudMinima(String texto, int minimo) {
        return texto != null && texto.length() >= minimo;
    }
}
