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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        SalarioDelPeriodo salario = calcularSalarioDelPeriodo(empleado, inicio, fin);
        BigDecimal valorLiquido = salario.getValor().subtract(totalDescontado);
        ResultadoCierreMensual resultado =
                new ResultadoCierreMensual(empleado, new ArrayList<>(retiradas), totalDescontado, valorLiquido);
        resultado.salario = salario;
        return resultado;
    }

    /**
     * Salario que corresponde al empleado en el periodo. Regla del taller
     * (mes comercial de 30 dias, valor del dia = salario base / 30):
     * - Empleado antiguo (no entra ni sale dentro del periodo): salario base
     *   COMPLETO, sea cual sea el periodo elegido.
     * - Mes de ENTRADA (la fecha de admision cae en el periodo): se paga
     *   30 - dia de entrada (entro el 19 -> 11 dias; el 13 -> 17 dias).
     *   Entrar el dia 1 es mes completo.
     * - Mes de SALIDA (la fecha de salida cae en el periodo): se pagan los
     *   dias hasta la salida, contandola (salio el 10 -> 10 dias).
     * El dia 31 cuenta como 30 y el fin de febrero tambien como 30.
     */
    public SalarioDelPeriodo calcularSalarioDelPeriodo(Empleado empleado, LocalDate inicio, LocalDate fin) {
        BigDecimal salarioBase = empleado.getSalarioBase() != null ? empleado.getSalarioBase() : BigDecimal.ZERO;
        LocalDate admision = empleado.getFechaAdmision();
        LocalDate salida = empleado.getFechaSalida();

        if ((admision != null && admision.isAfter(fin)) || (salida != null && salida.isBefore(inicio))) {
            // Todavia no habia entrado, o ya habia salido antes del periodo.
            return new SalarioDelPeriodo(salarioBase, BigDecimal.ZERO, 0, inicio, fin);
        }
        boolean entraEnElPeriodo = admision != null && !admision.isBefore(inicio)
                && admision.getDayOfMonth() != 1;
        boolean saleEnElPeriodo = salida != null && !salida.isAfter(fin);
        if (!entraEnElPeriodo && !saleEnElPeriodo) {
            // Empleado antiguo: mes completo.
            return new SalarioDelPeriodo(salarioBase, salarioBase, null, inicio, fin);
        }

        LocalDate desde = entraEnElPeriodo ? admision : inicio;
        LocalDate hasta = saleEnElPeriodo ? salida : desde.withDayOfMonth(desde.lengthOfMonth());
        int dias = diasComerciales(desde, hasta);
        if (entraEnElPeriodo) {
            dias = dias - 1; // el dia de entrada no se cuenta: 30 - dia de entrada
        }
        dias = Math.max(0, dias);
        if (dias >= DIAS_DEL_MES) {
            return new SalarioDelPeriodo(salarioBase, salarioBase, null, desde, hasta);
        }
        BigDecimal valor = salarioBase.multiply(BigDecimal.valueOf(dias))
                .divide(BigDecimal.valueOf(DIAS_DEL_MES), 0, RoundingMode.HALF_UP);
        return new SalarioDelPeriodo(salarioBase, valor, dias, desde, hasta);
    }

    /** Mes comercial usado para el salario proporcional. */
    public static final int DIAS_DEL_MES = 30;

    /**
     * Dias entre dos fechas (contando las dos) en meses de 30 dias: el dia
     * 31 cuenta como 30, y el ultimo dia de febrero (28 o 29) tambien cuenta
     * como 30 (el 31 nunca se paga aparte).
     */
    static int diasComerciales(LocalDate desde, LocalDate hasta) {
        int d1 = diaComercial(desde);
        int d2 = diaComercial(hasta);
        return (hasta.getYear() - desde.getYear()) * 360
                + (hasta.getMonthValue() - desde.getMonthValue()) * 30
                + (d2 - d1) + 1;
    }

    private static int diaComercial(LocalDate fecha) {
        if (fecha.getDayOfMonth() == fecha.lengthOfMonth()) {
            return 30;
        }
        return Math.min(fecha.getDayOfMonth(), 30);
    }

    /**
     * Pago real del salario mensual del empleado. Hace, todo en una sola
     * transaccion atomica:
     * 1) Busca los retiros (vales/adelantos) del periodo que todavia NO
     *    fueron liquidados y los marca como liquidados (fechaLiquidacion
     *    = hoy), para que no se cuenten de nuevo en un pago futuro.
     * 2) Genera UN SOLO MovimientoFinanciero (SALIDA / SALARIO_EMPLEADO)
     *    con el salario del periodo -- el salario base completo, o el
     *    proporcional a los dias trabajados si el empleado entro o salio
     *    dentro del periodo (ver calcularSalarioDelPeriodo) -- sin descontar
     *    los vales/adelantos, que son solo informativos para el recibo impreso.
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
        SalarioDelPeriodo salario = calcularSalarioDelPeriodo(empleado, inicio, fin);
        if (salario.getDiasTrabajados() != null && salario.getDiasTrabajados() == 0) {
            throw new IllegalArgumentException("El empleado no trabajo ningun dia en este periodo "
                    + "(revise la fecha de admision y la fecha de salida).");
        }

        ResultadoCierreMensual resultado = BD.transaccion(conexion -> {
            List<RetiroEmpleado> retiradas =
                    retiradaFuncionarioDAO.listarPorEmpleadoYPeriodo(conexion, empleado, inicio, fin);
            BigDecimal salarioBase = salario.getValor();

            // El movimiento se graba ANTES de marcar los retiros, para que cada
            // retiro quede vinculado a este pago (pagoSalario) -- asi el recibo
            // se puede reimprimir despues con los descuentos exactos.
            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(fechaPago);
            movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.SALARIO_EMPLEADO);
            movimiento.setValor(salarioBase);
            movimiento.setDescripcion("Salario de " + empleado.getNombre() + " ("
                    + inicio.format(FORMATO_FECHA) + " a " + fin.format(FORMATO_FECHA) + ")"
                    + (salario.esProporcional()
                            ? " - proporcional " + salario.getDiasTrabajados() + " de " + DIAS_DEL_MES + " dias"
                            : ""));
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
            pago.salario = salario;
            return pago;
        });
        auditoria.registrar("SALARIO PAGADO", resultado.getPagoSalario().getDescripcion() + " - salario "
                + AuditoriaController.gs(resultado.getPagoSalario().getValor()) + ", descuentos "
                + AuditoriaController.gs(resultado.getTotalDescontado())
                + ", liquido " + AuditoriaController.gs(resultado.getValorLiquido()));
        return resultado;
    }

    /**
     * Proximo periodo de salario que falta pagar al empleado, para que la
     * pantalla complete DESDE/HASTA sola:
     * - si nunca se le pago: desde la fecha de admision hasta el fin de ese mes
     *   (ej.: entro el 13/10 -> 13/10 a 31/10);
     * - si ya se le pago: el mes siguiente completo al del ultimo pago
     *   (ej.: pagado 13/10 a 31/10 -> 01/11 a 30/11; pagado 01/11 a 02/11 ->
     *   01/12 a 31/12 -- cada pago cubre su mes);
     * - si tiene fecha de salida en ese mes, el periodo termina en la salida.
     * Devuelve null si no se puede saber (sin admision ni pagos) o si ya no
     * queda nada por pagar (ya se pago hasta la salida).
     */
    public LocalDate[] proximoPeriodoAPagar(Empleado empleado) {
        LocalDate desde = null;
        List<MovimientoFinanciero> pagos = movimientoDAO.listarSalariosPorEmpleado(empleado);
        if (!pagos.isEmpty()) {
            LocalDate ultimoPagado = finDelPeriodoPagado(pagos.get(0).getDescripcion());
            if (ultimoPagado != null) {
                desde = ultimoPagado.withDayOfMonth(1).plusMonths(1);
            }
        }
        if (desde == null) {
            if (!pagos.isEmpty() || empleado.getFechaAdmision() == null) {
                return null;
            }
            desde = empleado.getFechaAdmision();
        }
        LocalDate hasta = desde.withDayOfMonth(desde.lengthOfMonth());
        if (empleado.getFechaSalida() != null) {
            if (empleado.getFechaSalida().isBefore(desde)) {
                return null;
            }
            if (empleado.getFechaSalida().isBefore(hasta)) {
                hasta = empleado.getFechaSalida();
            }
        }
        return new LocalDate[] {desde, hasta};
    }

    private static final Pattern PERIODO_EN_DESCRIPCION = Pattern.compile("\\((\\S+) a (\\S+)\\)");

    /** Lee el fin del periodo de la descripcion del pago: "Salario de X (01/10/2026 a 31/10/2026)". */
    static LocalDate finDelPeriodoPagado(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        Matcher m = PERIODO_EN_DESCRIPCION.matcher(descripcion);
        if (!m.find()) {
            return null;
        }
        String texto = m.group(2);
        try {
            return LocalDate.parse(texto, FORMATO_FECHA);
        } catch (RuntimeException e) {
            try {
                // Pagos antiguos guardaron la fecha como 2026-09-30.
                return LocalDate.parse(texto);
            } catch (RuntimeException e2) {
                return null;
            }
        }
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
        /** Salario que corresponde al periodo (completo o proporcional). */
        private SalarioDelPeriodo salario;

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

        public SalarioDelPeriodo getSalario() {
            return salario;
        }
    }

    /** Salario del periodo: completo, o proporcional a los dias trabajados. */
    public static class SalarioDelPeriodo {
        private final BigDecimal salarioBase;
        private final BigDecimal valor;
        /** null cuando trabajo el periodo completo (no hay proporcional). */
        private final Integer diasTrabajados;
        private final LocalDate desde;
        private final LocalDate hasta;

        public SalarioDelPeriodo(BigDecimal salarioBase, BigDecimal valor, Integer diasTrabajados,
                                 LocalDate desde, LocalDate hasta) {
            this.salarioBase = salarioBase;
            this.valor = valor;
            this.diasTrabajados = diasTrabajados;
            this.desde = desde;
            this.hasta = hasta;
        }

        public BigDecimal getSalarioBase() {
            return salarioBase;
        }

        /** Lo que se paga en este periodo. */
        public BigDecimal getValor() {
            return valor;
        }

        public Integer getDiasTrabajados() {
            return diasTrabajados;
        }

        public boolean esProporcional() {
            return diasTrabajados != null;
        }

        public LocalDate getDesde() {
            return desde;
        }

        public LocalDate getHasta() {
            return hasta;
        }
    }
}
