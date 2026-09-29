package com.mecanica.dao;

import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Implementacion base de GenericDAO con SQL directo (JDBC). Cada DAO
 * especifico informa su tabla, sus columnas y como pasar una fila a objeto
 * (y el objeto a los parametros del INSERT/UPDATE); el SQL comun (guardar,
 * buscar por id, listar, eliminar) queda aca.
 *
 * <p>Cada metodo publico sin {@code Connection} abre y cierra su propia
 * conexion. Los que reciben una {@code Connection} son para usar dentro de
 * una transaccion de un Controller (BD.transaccion), cuando varias
 * operaciones tienen que pasar todas juntas.
 *
 * <p>Claves foraneas: al leer una fila, las columnas tipo cliente_id se
 * convierten en un objeto "de referencia" (solo con el id). Despues
 * {@link #completarReferencias} carga de una vez todos los objetos
 * referenciados por la lista (una consulta por tabla, no una por fila), asi
 * las pantallas reciben el Cliente/Proveedor/etc. completo, igual que antes.
 */
public abstract class AbstractGenericDAO<T, ID> implements GenericDAO<T, ID> {

    /** Nombre de la tabla en la base. */
    protected abstract String tabla();

    /** Columnas (sin el id), en el mismo orden en que {@link #asignarColumnas} pone los valores. */
    protected abstract String[] columnas();

    /** Arma el objeto a partir de la fila actual (claves foraneas como referencia con solo el id). */
    protected abstract T mapear(ResultSet rs) throws SQLException;

    /** Valores de {@link #columnas()} para el INSERT/UPDATE, en el mismo orden. */
    protected abstract Object[] valoresColumnas(T entidad);

    protected abstract Long idDe(T entidad);

    protected abstract void ponerId(T entidad, Long id);

    /**
     * Reemplaza las referencias (objetos con solo el id) por los objetos
     * completos. Los DAOs de entidades con claves foraneas lo sobreescriben.
     */
    protected void completarReferencias(Connection conexion, List<T> lista) throws SQLException {
        // sin claves foraneas: no hay nada que completar
    }

    /**
     * Borra los registros "hijos" que antes se borraban en cascada junto con
     * esta entidad (por ejemplo los items de una Compra). Se llama dentro de
     * la misma transaccion, justo antes de borrar la entidad.
     */
    protected void eliminarDependientes(Connection conexion, Long id) throws SQLException {
        // sin dependientes: no hay nada que borrar antes
    }

    // ------------------------------------------------------------ operaciones con conexion propia

    @Override
    public T guardar(T entidad) {
        return BD.transaccion(conexion -> guardar(conexion, entidad));
    }

    @Override
    public T buscarPorId(ID id) {
        return BD.consultar(conexion -> buscarPorId(conexion, (Long) id));
    }

    @Override
    public List<T> listarTodos() {
        return BD.consultar(conexion -> listar(conexion, ""));
    }

    @Override
    public void eliminar(T entidad) {
        BD.ejecutarEnTransaccion(conexion -> eliminar(conexion, entidad));
    }

    // ------------------------------------------------------------ operaciones dentro de una transaccion

    /** INSERT si la entidad es nueva (id nulo), UPDATE si ya existe. Devuelve la misma entidad, con el id. */
    public T guardar(Connection conexion, T entidad) throws SQLException {
        String[] columnas = columnas();
        Object[] valores = valoresColumnas(entidad);
        Long id = idDe(entidad);
        if (id == null) {
            String sql = "INSERT INTO " + tabla() + " (" + String.join(", ", columnas) + ") VALUES ("
                    + String.join(", ", java.util.Collections.nCopies(columnas.length, "?")) + ") RETURNING id";
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                BD.asignar(ps, valores);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ponerId(entidad, rs.getLong(1));
                }
            }
        } else {
            StringBuilder sql = new StringBuilder("UPDATE ").append(tabla()).append(" SET ");
            for (int i = 0; i < columnas.length; i++) {
                sql.append(i == 0 ? "" : ", ").append(columnas[i]).append(" = ?");
            }
            sql.append(" WHERE id = ?");
            Object[] parametros = java.util.Arrays.copyOf(valores, valores.length + 1);
            parametros[valores.length] = id;
            int filas = BD.actualizar(conexion, sql.toString(), parametros);
            if (filas == 0) {
                throw new IllegalStateException("Ese registro ya no existe (fue eliminado). "
                        + "Actualice la pantalla e intente de nuevo.");
            }
        }
        return entidad;
    }

    public T buscarPorId(Connection conexion, Long id) throws SQLException {
        if (id == null) {
            return null;
        }
        List<T> resultado = listar(conexion, "WHERE id = ?", id);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    /** Igual que buscarPorId, pero traba la fila hasta el fin de la transaccion (SELECT ... FOR UPDATE). */
    public T buscarPorIdParaActualizar(Connection conexion, Long id) throws SQLException {
        List<T> resultado = listar(conexion, "WHERE id = ? FOR UPDATE", id);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public void eliminar(Connection conexion, T entidad) throws SQLException {
        Long id = idDe(entidad);
        if (id == null) {
            return;
        }
        eliminarDependientes(conexion, id);
        BD.actualizar(conexion, "DELETE FROM " + tabla() + " WHERE id = ?", id);
    }

    // ------------------------------------------------------------ consultas

    /**
     * SELECT de todas las columnas de la tabla, con el resto del SQL que se
     * pase (WHERE / ORDER BY), ya con las referencias completas.
     */
    protected List<T> listar(Connection conexion, String restoSql, Object... parametros) throws SQLException {
        String sql = "SELECT id, " + String.join(", ", columnas()) + " FROM " + tabla() + " " + restoSql;
        List<T> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            BD.asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    T entidad = mapear(rs);
                    ponerId(entidad, rs.getLong("id"));
                    lista.add(entidad);
                }
            }
        }
        if (!lista.isEmpty()) {
            completarReferencias(conexion, lista);
        }
        return lista;
    }

    /** Version de {@link #listar(Connection, String, Object...)} que abre su propia conexion. */
    protected List<T> listar(String restoSql, Object... parametros) {
        return BD.consultar(conexion -> listar(conexion, restoSql, parametros));
    }

    /** Primer resultado de la consulta, o null si no hay ninguno. */
    protected T primero(String restoSql, Object... parametros) {
        List<T> resultado = listar(restoSql, parametros);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    /** Carga de una vez todos los registros con esos ids (id -> objeto completo). */
    public Map<Long, T> cargarPorIds(Connection conexion, Collection<Long> ids) throws SQLException {
        Map<Long, T> porId = new HashMap<>();
        if (ids.isEmpty()) {
            return porId;
        }
        for (T entidad : listar(conexion, "WHERE id = ANY(?)", (Object) ids.toArray(new Long[0]))) {
            porId.put(idDe(entidad), entidad);
        }
        return porId;
    }

    /**
     * Cambia, en cada elemento de la lista, la referencia (objeto con solo el
     * id) por el objeto completo cargado con el otro DAO. Ej.: el Cliente de
     * cada OS de la lista.
     */
    protected <R> void completar(Connection conexion, List<T> lista, Function<T, R> obtener,
            BiConsumer<T, R> poner, AbstractGenericDAO<R, ?> daoReferencia) throws SQLException {
        Set<Long> ids = new LinkedHashSet<>();
        for (T entidad : lista) {
            R referencia = obtener.apply(entidad);
            if (referencia != null) {
                ids.add(daoReferencia.idDe(referencia));
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, R> completos = daoReferencia.cargarPorIds(conexion, ids);
        for (T entidad : lista) {
            R referencia = obtener.apply(entidad);
            if (referencia != null) {
                poner.accept(entidad, completos.get(daoReferencia.idDe(referencia)));
            }
        }
    }
}
