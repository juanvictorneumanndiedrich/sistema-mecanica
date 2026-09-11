package com.mecanica.dao;

import com.mecanica.model.RetiradaSocio;
import com.mecanica.model.Socio;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

public class RetiradaSocioDAO extends AbstractGenericDAO<RetiradaSocio, Long> {

    public RetiradaSocioDAO() {
        super(RetiradaSocio.class);
    }

    /** Usado no acerto (divisao de lucro 50/50): soma o que o socio ja retirou no periodo. */
    public List<RetiradaSocio> listarPorSocioEPeriodo(Socio socio, LocalDate inicio, LocalDate fim) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM RetiradaSocio r WHERE r.socio = :socio "
                    + "AND r.data BETWEEN :inicio AND :fim ORDER BY r.data";
            Query<RetiradaSocio> query = session.createQuery(hql, RetiradaSocio.class);
            query.setParameter("socio", socio);
            query.setParameter("inicio", inicio);
            query.setParameter("fim", fim);
            return query.list();
        }
    }
}