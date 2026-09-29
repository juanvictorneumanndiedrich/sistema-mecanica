package com.mecanica.dao;

import com.mecanica.enums.EstadoCheque;
import com.mecanica.model.ChequePreDatado;
import com.mecanica.model.Cliente;
import com.mecanica.model.Proveedor;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ChequePreDatadoDAO extends AbstractGenericDAO<ChequePreDatado, Long> {

    @Override
    protected String tabla() {
        return "cheque_predatado";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"cliente_id", "proveedor_id", "numero_cheque", "banco", "fecha_registro",
                "fecha_vencimiento", "valor", "descripcion", "descuento_valor", "descuento_porcentaje", "estado",
                "fecha_confirmacion"};
    }

    @Override
    protected Object[] valoresColumnas(ChequePreDatado c) {
        return new Object[] {c.getCliente() == null ? null : c.getCliente().getId(),
                c.getProveedor() == null ? null : c.getProveedor().getId(), c.getNumeroCheque(), c.getBanco(),
                c.getFechaRegistro(), c.getFechaVencimiento(), c.getValor(), c.getDescripcion(),
                c.getDescuentoValor(), c.getDescuentoPorcentaje(), c.getEstado(), c.getFechaConfirmacion()};
    }

    @Override
    protected ChequePreDatado mapear(ResultSet rs) throws SQLException {
        ChequePreDatado c = new ChequePreDatado();
        c.setCliente(BD.referencia(rs, "cliente_id", Cliente::new, Cliente::setId));
        c.setProveedor(BD.referencia(rs, "proveedor_id", Proveedor::new, Proveedor::setId));
        c.setNumeroCheque(rs.getString("numero_cheque"));
        c.setBanco(rs.getString("banco"));
        c.setFechaRegistro(BD.fecha(rs, "fecha_registro"));
        c.setFechaVencimiento(BD.fecha(rs, "fecha_vencimiento"));
        c.setValor(rs.getBigDecimal("valor"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setDescuentoValor(rs.getBigDecimal("descuento_valor"));
        c.setDescuentoPorcentaje(rs.getBigDecimal("descuento_porcentaje"));
        c.setEstado(BD.enumerado(rs, "estado", EstadoCheque.class));
        c.setFechaConfirmacion(BD.fecha(rs, "fecha_confirmacion"));
        return c;
    }

    @Override
    protected void completarReferencias(Connection conexion, List<ChequePreDatado> lista) throws SQLException {
        completar(conexion, lista, ChequePreDatado::getCliente, ChequePreDatado::setCliente, new ClienteDAO());
        completar(conexion, lista, ChequePreDatado::getProveedor, ChequePreDatado::setProveedor, new ProveedorDAO());
    }

    @Override
    protected Long idDe(ChequePreDatado c) {
        return c.getId();
    }

    @Override
    protected void ponerId(ChequePreDatado c, Long id) {
        c.setId(id);
    }

    /** Se usa en la pestaña "Cheques Pendientes" de la pantalla Financiero. */
    public List<ChequePreDatado> listarPorEstado(EstadoCheque estado) {
        return listar("WHERE estado = ? ORDER BY fecha_vencimiento", estado);
    }
}
