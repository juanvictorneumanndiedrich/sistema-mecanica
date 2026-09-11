package com.mecanica.dao;

import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Implementacao base de GenericDAO usando a API nativa do Hibernate
 * (Session/Transaction), configurada via hibernate.cfg.xml. Cada DAO
 * especifico so precisa estender esta classe e informar a classe da
 * entidade, por exemplo:
 *
 * <pre>
 * public class ClienteDAO extends AbstractGenericDAO&lt;Cliente, Long&gt; {
 *     public ClienteDAO() {
 *         super(Cliente.class);
 *     }
 *
 *     // metodos de busca especificos do Cliente entram aqui
 * }
 * </pre>
 *
 * Cada metodo publico abre e fecha sua propria Session (try-with-resources),
 * o que e simples e suficiente para uma aplicacao desktop Swing de uso
 * interno.
 */
public abstract class AbstractGenericDAO<T, ID> implements GenericDAO<T, ID> {

    private final Class<T> classeEntidade;

    protected AbstractGenericDAO(Class<T> classeEntidade) {
        this.classeEntidade = classeEntidade;
    }

    @Override
    public T salvar(T entidade) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            // merge cobre tanto insercao (entidade nova, id nulo) quanto
            // atualizacao (entidade ja existente) num unico metodo.
            T entidadeSalva = session.merge(entidade);
            tx.commit();
            return entidadeSalva;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Override
    public T buscarPorId(ID id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(classeEntidade, id);
        }
    }

    @Override
    public List<T> listarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM " + classeEntidade.getSimpleName();
            Query<T> query = session.createQuery(hql, classeEntidade);
            return query.list();
        }
    }

    @Override
    public void excluir(T entidade) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            T gerenciada = session.contains(entidade) ? entidade : session.merge(entidade);
            session.remove(gerenciada);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}