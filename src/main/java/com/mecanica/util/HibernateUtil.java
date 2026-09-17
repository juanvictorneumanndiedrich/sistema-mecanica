package com.mecanica.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Clase utilitaria responsable de crear y proveer la SessionFactory
 * (y, a partir de ella, las Session) usadas por toda la aplicacion para hablar
 * con la base PostgreSQL via Hibernate.
 *
 * La SessionFactory se crea una unica vez a partir del hibernate.cfg.xml
 * (que esta en src/main/resources, en la raiz del classpath) y se reutiliza
 * durante toda la ejecucion del sistema. Cada operacion del DAO debe abrir su
 * propia Session con getSessionFactory().openSession().
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
}
