package com.mecanica.dao;

import com.mecanica.enums.EstadoOrdenServicio;
import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.util.BD;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OrdenDeServicioDAO extends AbstractGenericDAO<OrdenDeServicio, Long> {

    @Override
    protected String tabla() {
        return "orden_de_servicio";
    }

    @Override
    protected String[] columnas() {
        return new String[] {"numero", "cliente_id", "maquinario_id", "fecha_apertura", "fecha_cierre", "estado",
                "problema_reportado", "valor_total"};
    }

    @Override
    protected Object[] valoresColumnas(OrdenDeServicio o) {
        return new Object[] {o.getNumero(), o.getCliente() == null ? null : o.getCliente().getId(),
                o.getMaquinario() == null ? null : o.getMaquinario().getId(), o.getFechaApertura(),
                o.getFechaCierre(), o.getEstado(), o.getProblemaReportado(), o.getValorTotal()};
    }

    @Override
    protected OrdenDeServicio mapear(ResultSet rs) throws SQLException {
        OrdenDeServicio o = new OrdenDeServicio();
        o.setNumero(BD.largo(rs, "numero"));
        o.setCliente(BD.referencia(rs, "cliente_id", Cliente::new, Cliente::setId));
        o.setMaquinario(BD.referencia(rs, "maquinario_id", Maquinario::new, Maquinario::setId));
        o.setFechaApertura(BD.fecha(rs, "fecha_apertura"));
        o.setFechaCierre(BD.fecha(rs, "fecha_cierre"));
        o.setEstado(BD.enumerado(rs, "estado", EstadoOrdenServicio.class));
        o.setProblemaReportado(rs.getString("problema_reportado"));
        o.setValorTotal(rs.getBigDecimal("valor_total"));
        return o;
    }

    /**
     * Carga el cliente y el maquinario de TODAS las OS de la lista con una
     * consulta por tabla (no una por OS), asi la pantalla de OS abre rapido
     * aunque haya miles de ordenes.
     */
    @Override
    protected void completarReferencias(Connection conexion, List<OrdenDeServicio> lista) throws SQLException {
        completar(conexion, lista, OrdenDeServicio::getCliente, OrdenDeServicio::setCliente, new ClienteDAO());
        completar(conexion, lista, OrdenDeServicio::getMaquinario, OrdenDeServicio::setMaquinario,
                new MaquinarioDAO());
    }

    @Override
    protected Long idDe(OrdenDeServicio o) {
        return o.getId();
    }

    @Override
    protected void ponerId(OrdenDeServicio o, Long id) {
        o.setId(id);
    }

    /** Los items de la OS se borraban en cascada junto con la OS. */
    @Override
    protected void eliminarDependientes(Connection conexion, Long id) throws SQLException {
        BD.actualizar(conexion, "DELETE FROM item_orden_servicio WHERE orden_de_servicio_id = ?", id);
    }

    @Override
    public List<OrdenDeServicio> listarTodos() {
        return listar("ORDER BY fecha_apertura DESC");
    }

    public List<OrdenDeServicio> listarPorEstado(EstadoOrdenServicio estado) {
        return listar("WHERE estado = ? ORDER BY fecha_apertura DESC", estado);
    }

    public List<OrdenDeServicio> listarPorCliente(Cliente cliente) {
        return listar("WHERE cliente_id = ? ORDER BY fecha_apertura DESC", cliente.getId());
    }

    /** Se usa al reimprimir/consultar una OS por el numero mostrado en la via impresa. */
    public OrdenDeServicio buscarPorNumero(Long numero) {
        return primero("WHERE numero = ?", numero);
    }

    /**
     * Lo usa el Controller para generar el proximo numero secuencial de OS
     * (numero actual + 1). Devuelve null si todavia no existe ninguna OS.
     */
    public Long buscarMayorNumero() {
        return BD.consultar(conexion -> BD.valor(conexion, Long.class, "SELECT MAX(numero) FROM orden_de_servicio"));
    }
}
