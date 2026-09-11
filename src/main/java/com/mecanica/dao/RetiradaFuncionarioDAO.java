package com.mecanica.dao;

import com.mecanica.model.Funcionario;
import com.mecanica.model.RetiradaFuncionario;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

public class RetiradaFuncionarioDAO extends AbstractGenericDAO<RetiradaFuncionario, Long> {

    public RetiradaFuncionarioDAO() {
        super(RetiradaFuncionario.class);
    }

    /** Usado no fechamento mensal do funcionario (soma vales + adiantamentos do periodo). */
    public List<RetiradaFuncionario> listarPorFuncionarioEPeriodo(Funcionario funcionario, LocalDate inicio, LocalDate fim) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM RetiradaFuncionario r WHERE r.funcionario = :funcionario "
                    + "AND r.data BETWEEN :inicio AND :fim ORDER BY r.data";
            Query<RetiradaFuncionario> query = session.createQuery(hql, RetiradaFuncionario.class);
            query.setParameter("funcionario", funcionario);
            query.setParameter("inicio", inicio);
            query.setParameter("fim", fim);
            return query.list();
        }
    }
}