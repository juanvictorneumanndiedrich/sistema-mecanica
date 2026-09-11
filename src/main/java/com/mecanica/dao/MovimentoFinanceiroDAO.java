package com.mecanica.dao;

import com.mecanica.enums.CategoriaMovimentoFinanceiro;
import com.mecanica.model.MovimentoFinanceiro;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

public class MovimentoFinanceiroDAO extends AbstractGenericDAO<MovimentoFinanceiro, Long> {

    public MovimentoFinanceiroDAO() {
        super(MovimentoFinanceiro.class);
    }

    /** Usado na tela Financeiro pra montar o extrato de um periodo. */
    public List<MovimentoFinanceiro> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM MovimentoFinanceiro m WHERE m.data BETWEEN :inicio AND :fim ORDER BY m.data";
            Query<MovimentoFinanceiro> query = session.createQuery(hql, MovimentoFinanceiro.class);
            query.setParameter("inicio", inicio);
            query.setParameter("fim", fim);
            return query.list();
        }
    }

    public List<MovimentoFinanceiro> listarPorCategoria(CategoriaMovimentoFinanceiro categoria) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM MovimentoFinanceiro m WHERE m.categoria = :categoria ORDER BY m.data DESC";
            Query<MovimentoFinanceiro> query = session.createQuery(hql, MovimentoFinanceiro.class);
            query.setParameter("categoria", categoria);
            return query.list();
        }
    }
}