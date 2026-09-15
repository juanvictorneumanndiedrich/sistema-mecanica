package com.mecanica.dao;

import com.mecanica.enums.EstadoCheque;
import com.mecanica.model.ChequePreDatado;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class ChequePreDatadoDAO extends AbstractGenericDAO<ChequePreDatado, Long> {

    public ChequePreDatadoDAO() {
        super(ChequePreDatado.class);
    }

    /** Se usa en la pestaña "Cheques Pendientes" de la pantalla Financiero. */
    public List<ChequePreDatado> listarPorEstado(EstadoCheque estado) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM ChequePreDatado c WHERE c.estado = :estado ORDER BY c.fechaVencimiento";
            Query<ChequePreDatado> query = session.createQuery(hql, ChequePreDatado.class);
            query.setParameter("estado", estado);
            return query.list();
        }
    }
}
