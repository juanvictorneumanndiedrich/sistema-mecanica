package com.mecanica.dao;

import com.mecanica.model.Empleado;
import com.mecanica.model.MovimientoFinanciero;
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

    /**
     * Usado en el cierre/pago mensual del empleado (suma vales + adelantos
     * del periodo). Solo trae los retiros NO liquidados todavia -- uno ya
     * usado en un pago de salario anterior no debe contarse de nuevo.
     */
    public List<RetiroEmpleado> listarPorEmpleadoYPeriodo(Empleado empleado, LocalDate inicio, LocalDate fin) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM RetiroEmpleado r WHERE r.empleado = :empleado "
                    + "AND r.fecha BETWEEN :inicio AND :fin AND r.liquidado = false ORDER BY r.fecha";
            Query<RetiroEmpleado> query = session.createQuery(hql, RetiroEmpleado.class);
            query.setParameter("empleado", empleado);
            query.setParameter("inicio", inicio);
            query.setParameter("fin", fin);
            return query.list();
        }
    }

    /**
     * Retiros descontados en un pago de salario, para reimprimir el recibo.
     * Busca por el vinculo pagoSalario; para los pagos hechos antes de que
     * existiera ese vinculo (2026-09-17), cae en los retiros del mismo
     * empleado liquidados en la misma fecha del pago.
     */
    public List<RetiroEmpleado> listarPorPagoSalario(MovimientoFinanciero pago) {
        try (Session session = HibernateUtil.abrirSesion()) {
            Query<RetiroEmpleado> query = session.createQuery(
                    "FROM RetiroEmpleado r WHERE r.pagoSalario = :pago ORDER BY r.fecha", RetiroEmpleado.class);
            query.setParameter("pago", pago);
            List<RetiroEmpleado> vinculados = query.list();
            if (!vinculados.isEmpty() || pago.getEmpleado() == null) {
                return vinculados;
            }
            Query<RetiroEmpleado> antiguos = session.createQuery(
                    "FROM RetiroEmpleado r WHERE r.empleado = :empleado AND r.liquidado = true "
                            + "AND r.pagoSalario IS NULL AND r.fechaLiquidacion = :fecha ORDER BY r.fecha",
                    RetiroEmpleado.class);
            antiguos.setParameter("empleado", pago.getEmpleado());
            antiguos.setParameter("fecha", pago.getFecha());
            return antiguos.list();
        }
    }
}
