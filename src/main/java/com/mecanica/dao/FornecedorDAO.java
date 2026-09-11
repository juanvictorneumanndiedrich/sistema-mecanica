package com.mecanica.dao;

import com.mecanica.model.Fornecedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class FornecedorDAO extends AbstractGenericDAO<Fornecedor, Long> {

    public FornecedorDAO() {
        super(Fornecedor.class);
    }

    public List<Fornecedor> buscarPorNome(String nome) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Fornecedor f WHERE LOWER(f.nome) LIKE LOWER(:nome) ORDER BY f.nome";
            Query<Fornecedor> query = session.createQuery(hql, Fornecedor.class);
            query.setParameter("nome", "%" + nome + "%");
            return query.list();
        }
    }

    public Fornecedor buscarPorDocumento(String documento) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Fornecedor f WHERE f.documento = :documento";
            Query<Fornecedor> query = session.createQuery(hql, Fornecedor.class);
            query.setParameter("documento", documento);
            List<Fornecedor> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }
}