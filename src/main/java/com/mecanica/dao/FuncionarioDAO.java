package com.mecanica.dao;

import com.mecanica.model.Funcionario;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class FuncionarioDAO extends AbstractGenericDAO<Funcionario, Long> {

    public FuncionarioDAO() {
        super(Funcionario.class);
    }

    public List<Funcionario> listarAtivos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Funcionario f WHERE f.ativo = true ORDER BY f.nome";
            return session.createQuery(hql, Funcionario.class).list();
        }
    }

    public List<Funcionario> buscarPorNome(String nome) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Funcionario f WHERE LOWER(f.nome) LIKE LOWER(:nome) ORDER BY f.nome";
            Query<Funcionario> query = session.createQuery(hql, Funcionario.class);
            query.setParameter("nome", "%" + nome + "%");
            return query.list();
        }
    }
}