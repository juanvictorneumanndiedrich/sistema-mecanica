package com.mecanica.controller;

import com.mecanica.dao.ItemOrdenServicioDAO;
import com.mecanica.dao.OrdenDeServicioDAO;
import com.mecanica.enums.EstadoOrdenServicio;
import com.mecanica.enums.TipoItemOrdenServicio;
import com.mecanica.model.ItemOrdenServicio;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.util.BD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller de ItemOrdenServicio: agregar/quitar item de una OS y
 * mantener el valorTotal de la OS siempre en sincronia con la suma de los items.
 */
public class ItemOrdenServicioController {

    private final ItemOrdenServicioDAO itemOrdemServicoDAO = new ItemOrdenServicioDAO();
    private final OrdenDeServicioDAO ordenDAO = new OrdenDeServicioDAO();

    /**
     * Agrega un item a la OS. El valor unitario puede quedar en cero (item
     * "sin valor" todavia): se guarda igual, pero la OS no se puede cerrar
     * hasta que ese item tenga valor o sea quitado (ver
     * OrdenDeServicioController.cerrar).
     */
    public ItemOrdenServicio agregar(OrdenDeServicio os, TipoItemOrdenServicio tipo, String descripcion,
                                       BigDecimal cantidad, BigDecimal valorUnitarioInformado) {
        BigDecimal valorUnitario = valorUnitarioInformado == null ? BigDecimal.ZERO : valorUnitarioInformado;
        validar(descripcion, cantidad, valorUnitario);

        ItemOrdenServicio item = BD.transaccion(conexion -> {
            // se traba la OS mientras se recalcula el total, para que dos altas de item
            // al mismo tiempo no dejen el total distinto de la suma de los items
            OrdenDeServicio osGestionada = ordenDAO.buscarPorIdParaActualizar(conexion, os.getId());
            if (osGestionada == null) {
                throw new IllegalStateException("Esa orden de servicio ya no existe.");
            }
            ItemOrdenServicio nuevo = new ItemOrdenServicio();
            nuevo.setOrdenDeServicio(osGestionada);
            nuevo.setTipo(tipo);
            nuevo.setDescripcion(descripcion);
            nuevo.setCantidad(cantidad);
            nuevo.setValorUnitario(valorUnitario);
            nuevo.setValorTotal(cantidad.multiply(valorUnitario));
            itemOrdemServicoDAO.guardar(conexion, nuevo);
            actualizarTotal(conexion, osGestionada);
            return nuevo;
        });
        // el objeto que quedo en la pantalla tambien muestra el total nuevo
        os.setValorTotal(item.getOrdenDeServicio().getValorTotal());
        return item;
    }

    /**
     * Cambia tipo, descripcion, cantidad y valor de un item ya agregado. Solo
     * mientras la OS siga abierta (ni CONCLUIDA ni CANCELADA). El total de la
     * OS se recalcula en la misma transaccion.
     */
    public void editar(ItemOrdenServicio item, TipoItemOrdenServicio tipo, String descripcion,
                       BigDecimal cantidad, BigDecimal valorUnitarioInformado) {
        BigDecimal valorUnitario = valorUnitarioInformado == null ? BigDecimal.ZERO : valorUnitarioInformado;
        validar(descripcion, cantidad, valorUnitario);
        BigDecimal totalNuevo = BD.transaccion(conexion -> {
            ItemOrdenServicio gestionado = itemOrdemServicoDAO.buscarPorId(conexion, item.getId());
            if (gestionado == null) {
                throw new IllegalStateException("Ese item ya no existe.");
            }
            OrdenDeServicio osGestionada = ordenDAO.buscarPorIdParaActualizar(conexion,
                    gestionado.getOrdenDeServicio().getId());
            if (osGestionada.getEstado() == EstadoOrdenServicio.CONCLUIDA
                    || osGestionada.getEstado() == EstadoOrdenServicio.CANCELADA) {
                throw new IllegalStateException("La OS ya esta cerrada: sus items no se pueden editar.");
            }
            gestionado.setTipo(tipo);
            gestionado.setDescripcion(descripcion.trim());
            gestionado.setCantidad(cantidad);
            gestionado.setValorUnitario(valorUnitario);
            gestionado.setValorTotal(cantidad.multiply(valorUnitario));
            itemOrdemServicoDAO.guardar(conexion, gestionado);
            actualizarTotal(conexion, osGestionada);
            return osGestionada.getValorTotal();
        });
        if (item.getOrdenDeServicio() != null) {
            item.getOrdenDeServicio().setValorTotal(totalNuevo);
        }
    }

    private void validar(String descripcion, BigDecimal cantidad, BigDecimal valorUnitario) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        if (valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor unitario inválido.");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del item es obligatoria.");
        }
    }

    public void quitar(ItemOrdenServicio item) {
        BigDecimal totalNuevo = BD.transaccion(conexion -> {
            ItemOrdenServicio gestionado = itemOrdemServicoDAO.buscarPorId(conexion, item.getId());
            if (gestionado == null) {
                return null;
            }
            OrdenDeServicio osGestionada = ordenDAO.buscarPorIdParaActualizar(conexion,
                    gestionado.getOrdenDeServicio().getId());
            itemOrdemServicoDAO.eliminar(conexion, gestionado);
            actualizarTotal(conexion, osGestionada);
            return osGestionada.getValorTotal();
        });
        if (totalNuevo != null && item.getOrdenDeServicio() != null) {
            item.getOrdenDeServicio().setValorTotal(totalNuevo);
        }
    }

    public List<ItemOrdenServicio> listarPorOrdemDeServico(OrdenDeServicio os) {
        return itemOrdemServicoDAO.listarPorOrdemDeServico(os);
    }

    /**
     * Suma los items de la OS dentro de la MISMA transaccion del agregar/quitar,
     * para que el total nunca quede desfasado de los items.
     */
    private void actualizarTotal(Connection conexion, OrdenDeServicio os) throws SQLException {
        BigDecimal total = itemOrdemServicoDAO.sumarPorOrden(conexion, os.getId());
        os.setValorTotal(total);
        BD.actualizar(conexion, "UPDATE orden_de_servicio SET valor_total = ? WHERE id = ?", total, os.getId());
    }
}
