package com.mecanica.controller;

import com.mecanica.enums.TipoRetiroEmpleado;
import com.mecanica.model.Empleado;
import com.mecanica.model.RetiroEmpleado;
import com.mecanica.dao.RetiroEmpleadoDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de RetiroEmpleado. El retiro de empleado (vale semanal o
 * adelanto) NO genera gasto en el momento -- es solo un registro que
 * despues se descuenta del salario base, en el pago mensual real
 * (ver EmpleadoController.pagarSalario). El unico gasto que entra en
 * Financiero es el salario completo, generado una vez al mes.
 */
public class RetiroEmpleadoController {

    private final RetiroEmpleadoDAO retiradaFuncionarioDAO = new RetiroEmpleadoDAO();

    public RetiroEmpleado registrarRetirada(Empleado empleado, TipoRetiroEmpleado tipo,
                                                  BigDecimal valor, LocalDate fecha, String observacion) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del retiro debe ser mayor que cero.");
        }
        LocalDate fechaFinal = fecha != null ? fecha : LocalDate.now();

        RetiroEmpleado retiro = new RetiroEmpleado();
        retiro.setEmpleado(empleado);
        retiro.setTipo(tipo);
        retiro.setValor(valor);
        retiro.setFecha(fechaFinal);
        retiro.setObservacion(observacion);
        return retiradaFuncionarioDAO.guardar(retiro);
    }

    public List<RetiroEmpleado> listarPorEmpleadoYPeriodo(Empleado empleado, LocalDate inicio, LocalDate fin) {
        return retiradaFuncionarioDAO.listarPorEmpleadoYPeriodo(empleado, inicio, fin);
    }

    public void eliminar(RetiroEmpleado retiro) {
        retiradaFuncionarioDAO.eliminar(retiro);
    }
}
