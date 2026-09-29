package com.mecanica.controller;

import com.mecanica.dao.EmpleadoDAO;
import com.mecanica.dao.MovimientoFinancieroDAO;
import com.mecanica.dao.RetiroEmpleadoDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.Permiso;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.Empleado;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.RetiroEmpleado;
import com.mecanica.util.BD;
import com.mecanica.util.Sesion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller de Empleado: CRUD, la vista previa del cierre mensual
 * (recibo con vales, adelantos, total descontado y valor liquido) y el
 * pago de salario en si. El registro de los retiros en si queda en el
 * RetiroEmpleadoController.
 */
public class EmpleadoController {

    private final EmpleadoDAO funcionarioDAO = new EmpleadoDAO();
    private final AuditoriaController auditoria = new AuditoriaController();
    private final RetiroEmpleadoDAO retiradaFuncionarioDAO = new RetiroEmpleadoDAO();
    private final MovimientoFinancieroDAO movimientoDAO = new MovimientoFinancieroDAO();

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Empleado guardar(Empleado empleado) {
        Sesion.exigir(Permiso.EDITAR_EMPLEADOS);
        validar(empleado);
        boolean nuevo = empleado.getId() == null;
        Empleado guardado = funcionarioDAO.guardar(empleado);
        auditoria.registrar(nuevo ? "EMPLEADO CREADO" : "EMPLEADO EDITADO", guardado.getNombre()
                + " - salario base " + AuditoriaController.gs(guardado.getSalarioBase()));
        return guardado;
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
        Sesion.exigir(Permiso.EDITAR_EMPLEADOS);
        funcionarioDAO.eliminar(empleado);
        auditoria.registrar("EMPLEADO ELIMINADO", empleado.getNombre());
    }

    /**
     * Vista previa del cierre mensual del empleado: suma los vales y
     * adelantos del periodo (todavia no liquidados) y calcula el valor
     * liquido informativo (salario base - total descontado). NO genera
     * nada en Financiero ni marca ningun retiro -- es solo el "borrador"
     * que se muestra antes de pagar. El pago real es pagarSalario().
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

    /**
     * Pago real del salario mensual del empleado. Hace, todo en una sola
     * transaccion atomica:
     * 1) Busca los retiros (vales/adelantos) del periodo que todavia NO
     *    fueron liquidados y los marca como liquidados (fechaLiquidacion
     *    = hoy), para que no se cuenten de nuevo en un pago futuro.
     * 2) Genera UN SOLO MovimientoFinanciero (SALIDA / SALARIO_EMPLEADO)
     *    con el SALARIO BASE COMPLETO -- sin descontar los vales/adelantos,
     *    que son solo informativos para el recibo impreso.
     * El resultado devuelto (con los retiros consumidos, el total
     * descontado y el valor liquido) es lo que se imprime como recibo.
     */
    public ResultadoCierreMensual pagarSalario(Empleado empleado, LocalDate inicio, LocalDate fin) {
        Sesion.exigir(Permiso.PAGAR_SALARIO);
        if (empleado == null) {
            throw new IllegalArgumentException("Debe seleccionar un empleado.");
        }
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new IllegalArgumentException("El periodo del pago es invalido.");
        }
        LocalDate fechaPago = LocalDate.now();

        ResultadoCierreMensual resultado = BD.transaccion(conexion -> {
            List<RetiroEmpleado> retiradas =
                    retiradaFuncionarioDAO.listarPorEmpleadoYPeriodo(conexion, empleado, inicio, fin);
            BigDecimal salarioBase = empleado.getSalarioBase() != null ? empleado.getSalarioBase() : BigDecimal.ZERO;

            // El movimiento se graba ANTES de marcar los retiros, para que cada
            // retiro quede vinculado a este pago (pagoSalario) -- asi el recibo
            // se puede reimprimir despues con los descuentos exactos.
            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(fechaPago);
            movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.SALARIO_EMPLEADO);
            movimiento.setValor(salarioBase);
            movimiento.setDescripcion("Salario de " + empleado.getNombre() + " ("
                    + inicio.format(FORMATO_FECHA) + " a " + fin.format(FORMATO_FECHA) + ")");
            movimiento.setEmpleado(empleado);
            movimientoDAO.guardar(conexion, movimiento);

            BigDecimal totalDescontado = BigDecimal.ZERO;
            for (RetiroEmpleado r : retiradas) {
                totalDescontado = totalDescontado.add(r.getValor());
                r.setLiquidado(true);
                r.setFechaLiquidacion(fechaPago);
                r.setPagoSalario(movimiento);
                retiradaFuncionarioDAO.guardar(conexion, r);
            }
            BigDecimal valorLiquido = salarioBase.subtract(totalDescontado);
            ResultadoCierreMensual pago =
                    new ResultadoCierreMensual(empleado, new ArrayList<>(retiradas), totalDescontado, valorLiquido);
            pago.pagoSalario = movimiento;
            return pago;
        });
        auditoria.registrar("SALARIO PAGADO", resultado.getPagoSalario().getDescripcion() + " - salario "
                + AuditoriaController.gs(resultado.getPagoSalario().getValor()) + ", descuentos "
                + AuditoriaController.gs(resultado.getTotalDescontado())
                + ", liquido " + AuditoriaController.gs(resultado.getValorLiquido()));
        return resultado;
    }

    /** Pagos de salario ya hechos al empleado (el mas reciente primero), para reimprimir el recibo. */
    public List<MovimientoFinanciero> listarPagosSalario(Empleado empleado) {
        return movimientoDAO.listarSalariosPorEmpleado(empleado);
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
        /** Movimiento del pago real -- null en la vista previa (calcularCierreMensual). */
        private MovimientoFinanciero pagoSalario;

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

        public MovimientoFinanciero getPagoSalario() {
            return pagoSalario;
        }
    }
}
