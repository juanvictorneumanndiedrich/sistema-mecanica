package com.mecanica.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Clase utilitaria responsable de crear y proveer la SessionFactory
 * (y, a partir de ella, las Session) usadas por toda la aplicacion para hablar
 * con la base PostgreSQL via Hibernate.
 *
 * La SessionFactory se crea una unica vez a partir del hibernate.cfg.xml
 * (que esta en src/main/resources, en la raiz del classpath) y se reutiliza
 * durante toda la ejecucion del sistema. Cada operacion del DAO/Controller
 * debe abrir su propia Session con {@link #abrirSesion()} (no con
 * getSessionFactory().openSession() directamente -- ver el porque abajo).
 */
public final class HibernateUtil {

    private static volatile SessionFactory sessionFactory;

    private HibernateUtil() {
        // clase utilitaria: no debe ser instanciada
    }

    /**
     * Devuelve la SessionFactory, creandola en el primer uso.
     *
     * <p>Se crea aca (y no en un campo estatico final) a proposito: si el sistema
     * abre antes de que el PostgreSQL termine de arrancar, el primer intento falla
     * pero el siguiente vuelve a intentar. Con el campo estatico, la clase quedaba
     * "quemada" en la JVM y el sistema solo conectaba cerrando y abriendo el programa.
     */
    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || !sessionFactory.isOpen()) {
            try {
                // configure() busca el hibernate.cfg.xml en el classpath
                sessionFactory = new Configuration().configure().buildSessionFactory();
            } catch (Throwable ex) {
                sessionFactory = null;
                throw new IllegalStateException("No fue posible conectar a la base de datos. "
                        + "Verifique que el PostgreSQL este encendido.", ex);
            }
        }
        return sessionFactory;
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }

    /**
     * Abre una Session lista para usar, probando primero que la conexion
     * realmente funciona.
     *
     * <p>El pool de conexiones que trae Hibernate (el que se configura con
     * connection.pool_size en el hibernate.cfg.xml) es, segun la propia
     * documentacion de Hibernate, "unicamente para pruebas" -- no valida las
     * conexiones que ya tiene abiertas. Si el PostgreSQL se reinicia, esas
     * conexiones quedan muertas pero el pool las sigue entregando igual, y
     * recien la PRIMERA operacion despues del reinicio falla (las siguientes
     * ya andan bien, porque el pool va reemplazando de a una las conexiones
     * muertas). Para no mostrarle ese error de una vez al usuario, aca se
     * prueba la conexion con un "SELECT 1" liviano antes de devolverla; si
     * falla, se descarta toda la SessionFactory (para que abra conexiones
     * nuevas) y se intenta una unica vez mas.
     */
    public static Session abrirSesion() {
        Session session = getSessionFactory().openSession();
        if (conexionFunciona(session)) {
            return session;
        }
        session.close();
        shutdown();
        session = getSessionFactory().openSession();
        if (!conexionFunciona(session)) {
            session.close();
            throw new IllegalStateException("No fue posible conectar a la base de datos. "
                    + "Verifique que el PostgreSQL este encendido.");
        }
        return session;
    }

    private static boolean conexionFunciona(Session session) {
        try {
            session.createNativeQuery("SELECT 1", Integer.class).getSingleResult();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
