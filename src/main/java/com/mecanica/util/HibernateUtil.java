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

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private HibernateUtil() {
        // clase utilitaria: no debe ser instanciada
    }

    private static SessionFactory buildSessionFactory() {
        try {
            // configure() busca el hibernate.cfg.xml en el classpath
            return new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError("Error al crear la SessionFactory: " + ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
