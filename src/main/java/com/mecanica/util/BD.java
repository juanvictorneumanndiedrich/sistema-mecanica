package com.mecanica.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Ayudas para ejecutar SQL con JDBC sin repetir en cada DAO/Controller el
 * mismo codigo de abrir conexion, commit, rollback y cierre.
 *
 * <ul>
 * <li>{@link #consultar}: una o mas lecturas, sin transaccion.</li>
 * <li>{@link #transaccion} / {@link #ejecutarEnTransaccion}: varias escrituras que tienen que pasar todas
 * juntas o ninguna (por ejemplo: bajar el saldo del cliente Y registrar el
 * movimiento en Financiero). Si algo falla en el medio, se deshace todo.</li>
 * </ul>
 *
 * En los dos casos los errores de la base llegan a la pantalla ya traducidos
 * a un mensaje entendible ({@link Errores#traducir}).
 */
public final class BD {

    private BD() {
        // clase utilitaria: no debe ser instanciada
    }

    /** Trabajo que se hace con una conexion abierta. */
    @FunctionalInterface
    public interface Trabajo<R> {
        R ejecutar(Connection conexion) throws SQLException;
    }

    /** Igual que {@link Trabajo}, para cuando no hay nada que devolver. */
    @FunctionalInterface
    public interface TrabajoSinRetorno {
        void ejecutar(Connection conexion) throws SQLException;
    }

    /** Ejecuta lecturas con una conexion en modo autocommit. */
    public static <R> R consultar(Trabajo<R> trabajo) {
        Connection conexion = Conexion.obtener();
        try {
            return trabajo.ejecutar(conexion);
        } catch (SQLException e) {
            throw Errores.traducir(new ErrorBaseDatos(e));
        } catch (RuntimeException e) {
            throw Errores.traducir(e);
        } finally {
            Conexion.liberar(conexion);
        }
    }

    /** Ejecuta el trabajo dentro de una transaccion: commit si todo salio bien, rollback si algo fallo. */
    public static <R> R transaccion(Trabajo<R> trabajo) {
        Connection conexion = Conexion.obtener();
        try {
            conexion.setAutoCommit(false);
            R resultado = trabajo.ejecutar(conexion);
            conexion.commit();
            conexion.setAutoCommit(true);
            return resultado;
        } catch (SQLException e) {
            revertir(conexion);
            throw Errores.traducir(new ErrorBaseDatos(e));
        } catch (RuntimeException e) {
            revertir(conexion);
            throw Errores.traducir(e);
        } finally {
            Conexion.liberar(conexion);
        }
    }

    /** {@link #transaccion(Trabajo)} para trabajos que no devuelven nada. */
    public static void ejecutarEnTransaccion(TrabajoSinRetorno trabajo) {
        transaccion(conexion -> {
            trabajo.ejecutar(conexion);
            return null;
        });
    }

    /** Deshace la transaccion sin dejar que un fallo del propio rollback tape el error real. */
    private static void revertir(Connection conexion) {
        try {
            if (!conexion.getAutoCommit()) {
                conexion.rollback();
            }
        } catch (SQLException e) {
            // el rollback puede fallar si la conexion ya se cayo: no debe ocultar el error original
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------ ejecucion de SQL

    /** Ejecuta un INSERT/UPDATE/DELETE y devuelve cuantas filas cambio. */
    public static int actualizar(Connection conexion, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            asignar(ps, parametros);
            return ps.executeUpdate();
        }
    }

    /** Ejecuta una consulta que devuelve un unico valor (COUNT, MAX, SUM...). Null si no hay fila. */
    public static <V> V valor(Connection conexion, Class<V> tipo, String sql, Object... parametros)
            throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, tipo) : null;
            }
        }
    }

    /** Pone los parametros (?) en el orden en que aparecen en el SQL. */
    public static void asignar(PreparedStatement ps, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            asignar(ps, i + 1, parametros[i]);
        }
    }

    private static void asignar(PreparedStatement ps, int indice, Object valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.NULL);
        } else if (valor instanceof Enum<?> enumerado) {
            // los enums se guardan por nombre, igual que antes (EnumType.STRING)
            ps.setString(indice, enumerado.name());
        } else if (valor instanceof LocalDate fecha) {
            ps.setObject(indice, fecha);
        } else if (valor instanceof LocalDateTime fechaHora) {
            ps.setTimestamp(indice, Timestamp.valueOf(fechaHora));
        } else if (valor instanceof BigDecimal numero) {
            ps.setBigDecimal(indice, numero);
        } else if (valor instanceof Long numero) {
            ps.setLong(indice, numero);
        } else if (valor instanceof Integer numero) {
            ps.setInt(indice, numero);
        } else if (valor instanceof Boolean logico) {
            ps.setBoolean(indice, logico);
        } else if (valor instanceof String texto) {
            ps.setString(indice, texto);
        } else if (valor instanceof Long[] ids) {
            ps.setArray(indice, ps.getConnection().createArrayOf("bigint", ids));
        } else {
            ps.setObject(indice, valor);
        }
    }

    // ------------------------------------------------------------ lectura de columnas

    public static Long largo(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }

    public static LocalDate fecha(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, LocalDate.class);
    }

    public static LocalDateTime fechaHora(ResultSet rs, String columna) throws SQLException {
        Timestamp valor = rs.getTimestamp(columna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    public static <E extends Enum<E>> E enumerado(ResultSet rs, String columna, Class<E> tipo) throws SQLException {
        String valor = rs.getString(columna);
        return valor == null ? null : Enum.valueOf(tipo, valor);
    }

    /**
     * Crea un objeto "de referencia" (solo con el id) para una columna de
     * clave foranea, por ejemplo el Cliente de una OS a partir de cliente_id.
     * Despues el DAO reemplaza esa referencia por el objeto completo, cargado
     * de la base (ver AbstractGenericDAO.completarReferencias). Null si la
     * columna esta vacia.
     */
    public static <E> E referencia(ResultSet rs, String columna, Supplier<E> crear, BiConsumer<E, Long> ponerId)
            throws SQLException {
        Long id = largo(rs, columna);
        if (id == null) {
            return null;
        }
        E objeto = crear.get();
        ponerId.accept(objeto, id);
        return objeto;
    }

    /** Error de la base con el SQLException original como causa (para que Errores lo pueda traducir). */
    public static class ErrorBaseDatos extends RuntimeException {
        public ErrorBaseDatos(SQLException causa) {
            super(causa.getMessage(), causa);
        }
    }
}
