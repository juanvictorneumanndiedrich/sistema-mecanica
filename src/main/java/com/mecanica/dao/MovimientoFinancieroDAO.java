package com.mecanica.dao;

import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.model.Empleado;
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

    /**
     * Movimientos que todavia no entraron en ningun CierreMensual (cierre
     * IS NULL). Se usa en la pestaña "Cierre Mensual" de Financiero, donde
     * el usuario elige con checkbox cuales entran en el cierre de ahora.
     */
    public List<MovimientoFinanciero> listarPendientesDeCierre() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM MovimientoFinanciero m WHERE m.cierre IS NULL ORDER BY m.fecha";
            Query<MovimientoFinanciero> query = session.createQuery(hql, MovimientoFinanciero.class);
            return query.list();
        }
    }

    /** Pagos de salario (SALARIO_EMPLEADO) de un empleado, del mas reciente al mas antiguo. */
    public List<MovimientoFinanciero> listarSalariosPorEmpleado(Empleado empleado) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM MovimientoFinanciero m WHERE m.empleado = :empleado "
                    + "AND m.categoria = :categoria ORDER BY m.fecha DESC, m.id DESC";
            Query<MovimientoFinanciero> query = session.createQuery(hql, MovimientoFinanciero.class);
            query.setParameter("empleado", empleado);
            query.setParameter("categoria", CategoriaMovimientoFinanciero.SALARIO_EMPLEADO);
            return query.list();
        }
    }

    /** Cantidad de movimientos que entraron en un cierre (se muestra en el reporte del cierre). */
    public long contarPorCierre(Long cierreId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(m) FROM MovimientoFinanciero m WHERE m.cierre.id = :id", Long.class);
            query.setParameter("id", cierreId);
            Long total = query.uniqueResult();
            return total == null ? 0 : total;
        }
    }
}
