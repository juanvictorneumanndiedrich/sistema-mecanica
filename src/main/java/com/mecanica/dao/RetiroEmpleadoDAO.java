package com.mecanica.dao;

import com.mecanica.model.Empleado;
import com.mecanica.model.RetiroEmpleado;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

public class RetiroEmpleadoDAO extends AbstractGenericDAO<RetiroEmpleado, Long> {

    public RetiroEmpleadoDAO() {
        super(RetiroEmpleado.class);
    }

    /** Usado no cierre mensal do empleado (soma vales + adiantamentos do periodo). */
    public List<RetiroEmpleado> listarPorEmpleadoYPeriodo(Empleado empleado, LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM RetiroEmpleado r WHERE r.empleado = :empleado "
                    + "AND r.fecha BETWEEN :inicio AND :fin ORDER BY r.fecha";
            Query<RetiroEmpleado> query = session.createQuery(hql, RetiroEmpleado.class);
            query.setParameter("empleado", empleado);
            query.setParameter("inicio", inicio);
            query.setParameter("fin", fin);
            return query.list();
        }
    }
}
