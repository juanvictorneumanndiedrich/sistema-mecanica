package com.mecanica.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

/**
 * Entrega las conexiones JDBC con la base PostgreSQL (reemplaza al
 * HibernateUtil de antes).
 *
 * <p>Los datos de conexion (url, usuario, clave) estan en el archivo
 * db.properties, en src/main/resources.
 *
 * <p>Abrir una conexion nueva con el PostgreSQL tarda un poco, por eso las
 * conexiones que ya se usaron se guardan (hasta {@link #MAXIMO_LIBRES}) y se
 * vuelven a entregar en la proxima operacion. Antes de entregar una conexion
 * guardada se prueba que siga viva (si el PostgreSQL se reinicio, se descarta
 * y se abre una nueva), asi el usuario no ve un error solo porque la base se
 * reinicio.
 *
 * <p>La primera vez que conecta, crea las tablas que falten (script
 * esquema.sql) -- lo mismo que antes hacia el Hibernate con
 * hbm2ddl.auto=update. En una base que ya tiene las tablas no cambia nada.
 */
public final class Conexion {

    private static final int MAXIMO_LIBRES = 4;
    private static final String MENSAJE_SIN_CONEXION = "No fue posible conectar a la base de datos. "
            + "Verifique que el PostgreSQL este encendido.";

    private static final Deque<Connection> libres = new ArrayDeque<>();
    private static Properties configuracion;
    private static volatile boolean esquemaListo;

    private Conexion() {
        // clase utilitaria: no debe ser instanciada
    }

    /** Devuelve una conexion lista para usar (en modo autocommit). Siempre devolverla con {@link #liberar}. */
    public static Connection obtener() {
        Connection conexion;
        while ((conexion = tomarLibre()) != null) {
            if (funciona(conexion)) {
                return conexion;
            }
            cerrarSinError(conexion);
        }
        conexion = abrirNueva();
        if (!esquemaListo) {
            try {
                prepararEsquema(conexion);
            } catch (RuntimeException e) {
                cerrarSinError(conexion);
                throw e;
            }
        }
        return conexion;
    }

    /** Devuelve la conexion para que se pueda reutilizar en la proxima operacion. */
    public static void liberar(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            if (conexion.isClosed()) {
                return;
            }
            if (!conexion.getAutoCommit()) {
                // una transaccion que quedo a medias nunca se reutiliza tal cual
                conexion.rollback();
                conexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            cerrarSinError(conexion);
            return;
        }
        synchronized (libres) {
            if (libres.size() < MAXIMO_LIBRES) {
                libres.push(conexion);
                return;
            }
        }
        cerrarSinError(conexion);
    }

    /** Cierra todas las conexiones guardadas (se usa al salir del sistema). */
    public static void cerrarTodas() {
        synchronized (libres) {
            while (!libres.isEmpty()) {
                cerrarSinError(libres.pop());
            }
        }
    }

    private static Connection tomarLibre() {
        synchronized (libres) {
            return libres.isEmpty() ? null : libres.pop();
        }
    }

    private static Connection abrirNueva() {
        Properties config = configuracion();
        try {
            return DriverManager.getConnection(config.getProperty("db.url"),
                    config.getProperty("db.usuario"), config.getProperty("db.clave"));
        } catch (SQLException e) {
            throw new IllegalStateException(MENSAJE_SIN_CONEXION, e);
        }
    }

    private static boolean funciona(Connection conexion) {
        try {
            return conexion.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void cerrarSinError(Connection conexion) {
        try {
            conexion.close();
        } catch (SQLException e) {
            // la conexion ya estaba muerta: no hay nada mas que hacer
        }
    }

    private static synchronized Properties configuracion() {
        if (configuracion == null) {
            Properties config = new Properties();
            try (InputStream entrada = Conexion.class.getResourceAsStream("/db.properties")) {
                if (entrada == null) {
                    throw new IllegalStateException("No se encontro el archivo db.properties.");
                }
                config.load(new java.io.InputStreamReader(entrada, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("No fue posible leer el archivo db.properties.", e);
            }
            configuracion = config;
        }
        return configuracion;
    }

    /** Crea las tablas que falten, una unica vez por ejecucion del sistema. */
    private static synchronized void prepararEsquema(Connection conexion) {
        if (esquemaListo) {
            return;
        }
        String script;
        try (InputStream entrada = Conexion.class.getResourceAsStream("/esquema.sql")) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontro el archivo esquema.sql.");
            }
            script = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible leer el archivo esquema.sql.", e);
        }
        try (Statement st = conexion.createStatement()) {
            st.execute(script);
        } catch (SQLException e) {
            throw new IllegalStateException("No fue posible preparar las tablas de la base de datos.", e);
        }
        esquemaListo = true;
    }
}
