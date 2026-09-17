package com.mecanica.util;

/**
 * Datos fijos del taller que aparecen en el encabezado de todos los reportes
 * impresos (listados, recibos, orden de servicio, cierre mensual). Para
 * cambiar lo que sale impreso, basta con editar las constantes de abajo --
 * un campo vacio simplemente no se imprime.
 */
public final class DatosTaller {

    public static final String NOMBRE = "Taller JB";
    public static final String RUC = "7697192-9";
    public static final String DIRECCION = "Barrio María Auxiliadora, Katueté - Canindeyú, Paraguay";
    public static final String TELEFONO = "0983 567 154 / 0983 565 890";

    private DatosTaller() {
        // clase utilitaria: no debe ser instanciada
    }

    /** Linea con RUC, direccion y telefono, omitiendo los que esten vacios. */
    public static String lineaDatos() {
        StringBuilder texto = new StringBuilder();
        agregar(texto, RUC.isBlank() ? "" : "RUC: " + RUC);
        agregar(texto, DIRECCION);
        agregar(texto, TELEFONO.isBlank() ? "" : "Tel: " + TELEFONO);
        return texto.toString();
    }

    private static void agregar(StringBuilder texto, String parte) {
        if (parte == null || parte.isBlank()) {
            return;
        }
        if (texto.length() > 0) {
            texto.append("   ·   ");
        }
        texto.append(parte);
    }
}
