package com.mecanica.controller;

import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.enums.TipoRetiroEmpleado;
import com.mecanica.model.Empleado;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.RetiroEmpleado;
import com.mecanica.dao.RetiroEmpleadoDAO;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de RetiroEmpleado. A diferencia del retiro de socio,
 * el retiro de empleado (vale semanal o adelanto) SI entra como gasto
 * -- genera un MovimientoFinanciero (SALIDA / RETIRO_EMPLEADO) en la misma
 * transaccion.
 */
public class RetiroEmpleadoController {

    private final RetiroEmpleadoDAO retiradaFuncionarioDAO = new RetiroEmpleadoDAO();

    public RetiroEmpleado registrarRetirada(Empleado empleado, TipoRetiroEmpleado tipo,
                                                  BigDecimal valor, LocalDate fecha, String observacion) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del retiro debe ser mayor que cero.");
        }
        LocalDate fechaFinal = fecha != null ? fecha : LocalDate.now();

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            RetiroEmpleado retiro = new RetiroEmpleado();
            retiro.setEmpleado(empleado);
            retiro.setTipo(tipo);
            retiro.setValor(valor);
            retiro.setFecha(fechaFinal);
            retiro.setObservacion(observacion);
            session.persist(retiro);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(fechaFinal);
            movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.RETIRO_EMPLEADO);
            movimiento.setValor(valor);
            movimiento.setDescripcion(observacion);
            movimiento.setEmpleado(empleado);
            session.persist(movimiento);

            tx.commit();
            return retiro;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public List<RetiroEmpleado> listarPorEmpleadoYPeriodo(Empleado empleado, LocalDate inicio, LocalDate fin) {
        return retiradaFuncionarioDAO.listarPorEmpleadoYPeriodo(empleado, inicio, fin);
    }

    public void eliminar(RetiroEmpleado retiro) {
        retiradaFuncionarioDAO.eliminar(retiro);
    }
}
