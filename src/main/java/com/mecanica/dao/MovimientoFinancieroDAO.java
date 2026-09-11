package com.mecanica.dao;

import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

public class MovimientoFinancieroDAO extends AbstractGenericDAO<MovimientoFinanciero, Long> {

    public MovimientoFinancieroDAO() {
        super(MovimientoFinanciero.class);
    }

    /** Se usa en la pantalla Financiero para armar el extracto de un periodo. */
    public List<MovimientoFinanciero> listarPorPeriodo(LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM MovimientoFinanciero m WHERE m.fecha BETWEEN :inicio AND :fin ORDER BY m.fecha";
            Query<MovimientoFinanciero> query = session.createQuery(hql, MovimientoFinanciero.class);
            query.setParameter("inicio", inicio);
            query.setParameter("fin", fin);
            return query.list();
        }
    }

    public List<MovimientoFinanciero> listarPorCategoria(CategoriaMovimientoFinanciero categoria) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM MovimientoFinanciero m WHERE m.categoria = :categoria ORDER BY m.fecha DESC";
            Query<MovimientoFinanciero> query = session.createQuery(hql, MovimientoFinanciero.class);
            query.setParameter("categoria", categoria);
            return query.list();
        }
    }
}
