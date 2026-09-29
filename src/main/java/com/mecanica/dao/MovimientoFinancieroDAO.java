package com.mecanica.dao;

import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.CierreMensual;
import com.mecanica.model.Cliente;
import com.mecanica.model.Empleado;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.Proveedor;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MovimientoFinancieroDAO extends AbstractGenericDAO<MovimientoFinanciero, Long> {

    @Override
    protected String tabla() {
        return "movimiento_financiero";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"fecha", "tipo", "categoria", "valor", "descripcion", "descuento_valor",
                "descuento_porcentaje", "cliente_id", "proveedor_id", "empleado_id", "cierre_id"};
    }

    @Override
    protected Object[] valoresColumnas(MovimientoFinanciero m) {
        return new Object[] {m.getFecha(), m.getTipo(), m.getCategoria(), m.getValor(), m.getDescripcion(),
                m.getDescuentoValor(), m.getDescuentoPorcentaje(),
                m.getCliente() == null ? null : m.getCliente().getId(),
                m.getProveedor() == null ? null : m.getProveedor().getId(),
                m.getEmpleado() == null ? null : m.getEmpleado().getId(),
                m.getCierre() == null ? null : m.getCierre().getId()};
    }

    @Override
    protected MovimientoFinanciero mapear(ResultSet rs) throws SQLException {
        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setFecha(BD.fecha(rs, "fecha"));
        m.setTipo(BD.enumerado(rs, "tipo", TipoMovimientoFinanciero.class));
        m.setCategoria(BD.enumerado(rs, "categoria", CategoriaMovimientoFinanciero.class));
        m.setValor(rs.getBigDecimal("valor"));
        m.setDescripcion(rs.getString("descripcion"));
        m.setDescuentoValor(rs.getBigDecimal("descuento_valor"));
        m.setDescuentoPorcentaje(rs.getBigDecimal("descuento_porcentaje"));
        m.setCliente(BD.referencia(rs, "cliente_id", Cliente::new, Cliente::setId));
        m.setProveedor(BD.referencia(rs, "proveedor_id", Proveedor::new, Proveedor::setId));
        m.setEmpleado(BD.referencia(rs, "empleado_id", Empleado::new, Empleado::setId));
        m.setCierre(BD.referencia(rs, "cierre_id", CierreMensual::new, CierreMensual::setId));
        return m;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<MovimientoFinanciero> lista) throws SQLException {
        completar(conexion, lista, MovimientoFinanciero::getCliente, MovimientoFinanciero::setCliente,
                new ClienteDAO());
        completar(conexion, lista, MovimientoFinanciero::getProveedor, MovimientoFinanciero::setProveedor,
                new ProveedorDAO());
        completar(conexion, lista, MovimientoFinanciero::getEmpleado, MovimientoFinanciero::setEmpleado,
                new EmpleadoDAO());
        completar(conexion, lista, MovimientoFinanciero::getCierre, MovimientoFinanciero::setCierre,
                new CierreMensualDAO());
    }

    @Override
    protected Long idDe(MovimientoFinanciero m) {
        return m.getId();
    }

    @Override
    protected void ponerId(MovimientoFinanciero m, Long id) {
        m.setId(id);
    }

    /** Se usa en la pantalla Financiero para armar el extracto de un periodo. */
    public List<MovimientoFinanciero> listarPorPeriodo(LocalDate inicio, LocalDate fin) {
        return listar("WHERE fecha BETWEEN ? AND ? ORDER BY fecha", inicio, fin);
    }

    public List<MovimientoFinanciero> listarPorCategoria(CategoriaMovimientoFinanciero categoria) {
        return listar("WHERE categoria = ? ORDER BY fecha DESC", categoria);
    }

    /**
     * Movimientos que todavia no entraron en ningun CierreMensual (cierre
     * IS NULL). Se usa en la pestaña "Cierre Mensual" de Financiero, donde
     * el usuario elige con checkbox cuales entran en el cierre de ahora.
     */
    public List<MovimientoFinanciero> listarPendientesDeCierre() {
        return listar("WHERE cierre_id IS NULL ORDER BY fecha");
    }

    /** Pagos de salario (SALARIO_EMPLEADO) de un empleado, del mas reciente al mas antiguo. */
    public List<MovimientoFinanciero> listarSalariosPorEmpleado(Empleado empleado) {
        return listar("WHERE empleado_id = ? AND categoria = ? ORDER BY fecha DESC, id DESC",
                empleado.getId(), CategoriaMovimientoFinanciero.SALARIO_EMPLEADO);
    }

    /** Cantidad de movimientos que entraron en un cierre (se muestra en el reporte del cierre). */
    public long contarPorCierre(Long cierreId) {
        Long total = BD.consultar(conexion -> BD.valor(conexion, Long.class,
                "SELECT COUNT(*) FROM movimiento_financiero WHERE cierre_id = ?", cierreId));
        return total == null ? 0 : total;
    }

    /** Marca el movimiento como incluido en un cierre (dentro de la transaccion del cierre). */
    public void marcarCierre(Connection conexion, Long movimientoId, Long cierreId) throws SQLException {
        BD.actualizar(conexion, "UPDATE movimiento_financiero SET cierre_id = ? WHERE id = ?", cierreId,
                movimientoId);
    }
}
