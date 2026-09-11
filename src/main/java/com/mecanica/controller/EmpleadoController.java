package com.mecanica.controller;

import com.mecanica.dao.EmpleadoDAO;
import com.mecanica.dao.RetiroEmpleadoDAO;
import com.mecanica.model.Empleado;
import com.mecanica.model.RetiroEmpleado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller de Empleado: CRUD y el calculo del cierre mensual
 * (recibo con vales, adelantos, total descontado y valor liquido).
 * El registro de los retiros en si queda en el RetiroEmpleadoController.
 */
public class EmpleadoController {

    private final EmpleadoDAO funcionarioDAO = new EmpleadoDAO();
    private final RetiroEmpleadoDAO retiradaFuncionarioDAO = new RetiroEmpleadoDAO();

    public Empleado guardar(Empleado empleado) {
        validar(empleado);
        return funcionarioDAO.guardar(empleado);
    }

    public Empleado buscarPorId(Long id) {
        return funcionarioDAO.buscarPorId(id);
    }

    public List<Empleado> listarTodos() {
        return funcionarioDAO.listarTodos();
    }

    public List<Empleado> listarActivos() {
        return funcionarioDAO.listarActivos();
    }

    public List<Empleado> buscarPorNombre(String nombre) {
        return funcionarioDAO.buscarPorNombre(nombre);
    }

    public void eliminar(Empleado empleado) {
        funcionarioDAO.eliminar(empleado);
    }

    /**
     * Arma el cierre mensual del empleado: suma los vales y
     * adelantos del periodo y calcula el valor liquido a pagar
     * (salario base - total descontado). Se usa para generar el recibo
     * imprimible (JasperReports).
     */
    public ResultadoCierreMensual calcularCierreMensual(Empleado empleado, LocalDate inicio, LocalDate fin) {
        List<RetiroEmpleado> retiradas = retiradaFuncionarioDAO.listarPorEmpleadoYPeriodo(empleado, inicio, fin);
        BigDecimal totalDescontado = BigDecimal.ZERO;
        for (RetiroEmpleado r : retiradas) {
            totalDescontado = totalDescontado.add(r.getValor());
        }
        BigDecimal salarioBase = empleado.getSalarioBase() != null ? empleado.getSalarioBase() : BigDecimal.ZERO;
        BigDecimal valorLiquido = salarioBase.subtract(totalDescontado);
        return new ResultadoCierreMensual(empleado, new ArrayList<>(retiradas), totalDescontado, valorLiquido);
    }

    private void validar(Empleado empleado) {
        if (empleado.getNombre() == null || empleado.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del empleado es obligatorio.");
        }
    }

    /** Resultado del cierre mensual de un empleado, listo para el recibo. */
    public static class ResultadoCierreMensual {
        private final Empleado empleado;
        private final List<RetiroEmpleado> retiradas;
        private final BigDecimal totalDescontado;
        private final BigDecimal valorLiquido;

        public ResultadoCierreMensual(Empleado empleado, List<RetiroEmpleado> retiradas,
                                          BigDecimal totalDescontado, BigDecimal valorLiquido) {
            this.empleado = empleado;
            this.retiradas = retiradas;
            this.totalDescontado = totalDescontado;
            this.valorLiquido = valorLiquido;
        }

        public Empleado getEmpleado() {
            return empleado;
        }

        public List<RetiroEmpleado> getRetiradas() {
            return retiradas;
        }

        public BigDecimal getTotalDescontado() {
            return totalDescontado;
        }

        public BigDecimal getValorLiquido() {
            return valorLiquido;
        }
    }
}
