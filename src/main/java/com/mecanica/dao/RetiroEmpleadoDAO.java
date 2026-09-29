package com.mecanica.dao;

import com.mecanica.enums.TipoRetiroEmpleado;
import com.mecanica.model.Empleado;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.RetiroEmpleado;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class RetiroEmpleadoDAO extends AbstractGenericDAO<RetiroEmpleado, Long> {

    @Override
    protected String tabla() {
        return "retiro_empleado";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"empleado_id", "fecha", "tipo", "valor", "observacion", "liquidado", "fechaliquidacion",
                "pago_salario_id"};
    }

    @Override
    protected Object[] valoresColumnas(RetiroEmpleado r) {
        return new Object[] {r.getEmpleado() == null ? null : r.getEmpleado().getId(), r.getFecha(), r.getTipo(),
                r.getValor(), r.getObservacion(), r.isLiquidado(), r.getFechaLiquidacion(),
                r.getPagoSalario() == null ? null : r.getPagoSalario().getId()};
    }

    @Override
    protected RetiroEmpleado mapear(ResultSet rs) throws SQLException {
        RetiroEmpleado r = new RetiroEmpleado();
        r.setEmpleado(BD.referencia(rs, "empleado_id", Empleado::new, Empleado::setId));
        r.setFecha(BD.fecha(rs, "fecha"));
        r.setTipo(BD.enumerado(rs, "tipo", TipoRetiroEmpleado.class));
        r.setValor(rs.getBigDecimal("valor"));
        r.setObservacion(rs.getString("observacion"));
        r.setLiquidado(rs.getBoolean("liquidado"));
        r.setFechaLiquidacion(BD.fecha(rs, "fechaliquidacion"));
        r.setPagoSalario(BD.referencia(rs, "pago_salario_id", MovimientoFinanciero::new, MovimientoFinanciero::setId));
        return r;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<RetiroEmpleado> lista) throws SQLException {
        completar(conexion, lista, RetiroEmpleado::getEmpleado, RetiroEmpleado::setEmpleado, new EmpleadoDAO());
        completar(conexion, lista, RetiroEmpleado::getPagoSalario, RetiroEmpleado::setPagoSalario,
                new MovimientoFinancieroDAO());
    }

    @Override
    protected Long idDe(RetiroEmpleado r) {
        return r.getId();
    }

    @Override
    protected void ponerId(RetiroEmpleado r, Long id) {
        r.setId(id);
    }

    /**
     * Usado en el cierre/pago mensual del empleado (suma vales + adelantos
     * del periodo). Solo trae los retiros NO liquidados todavia -- uno ya
     * usado en un pago de salario anterior no debe contarse de nuevo.
     */
    public List<RetiroEmpleado> listarPorEmpleadoYPeriodo(Empleado empleado, LocalDate inicio, LocalDate fin) {
        return BD.consultar(conexion -> listarPorEmpleadoYPeriodo(conexion, empleado, inicio, fin));
    }

    /** Version para usar dentro de una transaccion (pago de salario). */
    public List<RetiroEmpleado> listarPorEmpleadoYPeriodo(Connection conexion, Empleado empleado, LocalDate inicio,
            LocalDate fin) throws SQLException {
        return listar(conexion, "WHERE empleado_id = ? AND fecha BETWEEN ? AND ? AND liquidado = false ORDER BY fecha",
                empleado.getId(), inicio, fin);
    }

    /**
     * Retiros descontados en un pago de salario, para reimprimir el recibo.
     * Busca por el vinculo pagoSalario; para los pagos hechos antes de que
     * existiera ese vinculo (2026-09-17), cae en los retiros del mismo
     * empleado liquidados en la misma fecha del pago.
     */
    public List<RetiroEmpleado> listarPorPagoSalario(MovimientoFinanciero pago) {
        return BD.consultar(conexion -> {
            List<RetiroEmpleado> vinculados = listar(conexion, "WHERE pago_salario_id = ? ORDER BY fecha",
                    pago.getId());
            if (!vinculados.isEmpty() || pago.getEmpleado() == null) {
                return vinculados;
            }
            return listar(conexion, "WHERE empleado_id = ? AND liquidado = true AND pago_salario_id IS NULL "
                    + "AND fechaliquidacion = ? ORDER BY fecha", pago.getEmpleado().getId(), pago.getFecha());
        });
    }
}
