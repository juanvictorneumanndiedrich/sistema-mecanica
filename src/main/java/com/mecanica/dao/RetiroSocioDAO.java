package com.mecanica.dao;

import com.mecanica.model.RetiroSocio;
import com.mecanica.model.Socio;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

public class RetiroSocioDAO extends AbstractGenericDAO<RetiroSocio, Long> {

    public RetiroSocioDAO() {
        super(RetiroSocio.class);
    }

    /** Se usa en la liquidacion (division de ganancia 50/50): suma lo que el socio ya retiro en el periodo. */
    public List<RetiroSocio> listarPorSocioYPeriodo(Socio socio, LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM RetiroSocio r WHERE r.socio = :socio "
                    + "AND r.fecha BETWEEN :inicio AND :fin ORDER BY r.fecha";
            Query<RetiroSocio> query = session.createQuery(hql, RetiroSocio.class);
            query.setParameter("socio", socio);
            query.setParameter("inicio", inicio);
            query.setParameter("fin", fin);
            return query.list();
        }
    }
}
