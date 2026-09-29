package com.mecanica.util;

import java.sql.SQLException;

/**
 * Ayuda para que un error de base de datos llegue a la pantalla como un
 * mensaje entendible: todo error que sube al usuario pasa por
 * {@link #traducir(RuntimeException)} (lo hace BD.consultar/BD.transaccion).
 */
public final class Errores {

    private Errores() {
    }

    /**
     * Convierte errores tecnicos de PostgreSQL en mensajes para el usuario.
     * Si no reconoce el error, devuelve el mismo que recibio (las validaciones
     * de negocio, que ya tienen su texto, pasan sin cambios).
     */
    public static RuntimeException traducir(RuntimeException error) {
        String estado = estadoSql(error);
        if (estado == null) {
            return error;
        }
        switch (estado) {
            case "22001":
                return new IllegalArgumentException("Uno de los textos es mas largo de lo que permite el sistema. "
                        + "Acorte el texto e intente de nuevo.");
            case "22003":
            case "22P02":
                return new IllegalArgumentException("El valor es demasiado grande para el sistema. "
                        + "Revise el monto e intente de nuevo.");
            case "23502":
                return new IllegalArgumentException("Falta completar un dato obligatorio.");
            case "23503":
                return new IllegalStateException("No se puede completar la operacion: el registro esta vinculado "
                        + "a otros registros (ordenes de servicio, compras o movimientos).");
            case "23505":
                return new IllegalArgumentException("Ya existe un registro con ese dato (no puede repetirse).");
            case "23514":
                return new IllegalStateException("La base de datos rechazo un valor de este registro. "
                        + "Avise al responsable del sistema: puede faltar actualizar la base.");
            case "08001":
            case "08003":
            case "08006":
            case "08004":
                return new IllegalStateException("Se perdio la conexion con la base de datos. "
                        + "Verifique que el PostgreSQL este encendido e intente de nuevo.");
            default:
                return error;
        }
    }

    /** Busca el SQLState en la cadena de causas del error. */
    private static String estadoSql(Throwable error) {
        Throwable actual = error;
        while (actual != null) {
            if (actual instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
            if (actual.getCause() == actual) {
                break;
            }
            actual = actual.getCause();
        }
        return null;
    }
}
