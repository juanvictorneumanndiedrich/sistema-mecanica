package com.mecanica.dao;

import com.mecanica.util.Errores;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Implementacion base de GenericDAO usando la API nativa de Hibernate
 * (Session/Transaction), configurada via hibernate.cfg.xml. Cada DAO
 * especifico solo necesita extender esta clase e informar la clase de la
 * entidad, por ejemplo:
 *
 * <pre>
 * public class ClienteDAO extends AbstractGenericDAO&lt;Cliente, Long&gt; {
 *     public ClienteDAO() {
 *         super(Cliente.class);
 *     }
 *
 * // metodos de busqueda especificos del Cliente van aca
 * }
 * </pre>
 *
 * Cada metodo publico abre y cierra su propia Session (try-with-resources),
 * lo que es simple y suficiente para una aplicacion desktop Swing de uso
 * interno.
 */
public abstract class AbstractGenericDAO<T, ID> implements GenericDAO<T, ID> {

    private final Class<T> claseEntidad;

    protected AbstractGenericDAO(Class<T> claseEntidad) {
        this.claseEntidad = claseEntidad;
    }

    @Override
    public T guardar(T entidad) {
        Transaction tx = null;
        Session session = HibernateUtil.abrirSesion();
        try {
            tx = session.beginTransaction();
            // merge cubre tanto la insercion (entidad nueva, id nulo) como
            // la actualizacion (entidad ya existente) en un solo metodo.
            T entidadGuardada = session.merge(entidad);
            tx.commit();
            return entidadGuardada;
        } catch (RuntimeException e) {
            Errores.revertir(tx);
            throw Errores.traducir(e);
        } finally {
            session.close();
        }
    }

    @Override
    public T buscarPorId(ID id) {
        try (Session session = HibernateUtil.abrirSesion()) {
            return session.get(claseEntidad, id);
        }
    }

    @Override
    public List<T> listarTodos() {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM " + claseEntidad.getSimpleName();
            Query<T> query = session.createQuery(hql, claseEntidad);
            return query.list();
        }
    }

    @Override
    public void eliminar(T entidad) {
        Transaction tx = null;
        Session session = HibernateUtil.abrirSesion();
        try {
            tx = session.beginTransaction();
            // Importante: se busca la entidad por id en vez de hacer merge. Con merge,
            // Hibernate trae de arrastre al "padre" (por ejemplo el Cliente del Maquinario)
            // con su lista en cascada, y termina cancelando el borrado sin avisar.
            T gestionada = session.contains(entidad) ? entidad : porId(session, entidad);
            if (gestionada == null) {
                tx.commit();
                return;
            }
            session.remove(gestionada);
            tx.commit();
        } catch (RuntimeException e) {
            Errores.revertir(tx);
            throw Errores.traducir(e);
        } finally {
            session.close();
        }
    }

    /** Recarga la entidad dentro de la sesion a partir de su id (null si ya no existe). */
    private T porId(Session session, T entidad) {
        Object id = HibernateUtil.getSessionFactory().getPersistenceUnitUtil().getIdentifier(entidad);
        return id == null ? null : session.get(claseEntidad, id);
    }
}
