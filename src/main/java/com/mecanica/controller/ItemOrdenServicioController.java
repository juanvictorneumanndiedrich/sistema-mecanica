package com.mecanica.controller;

import com.mecanica.dao.ItemOrdenServicioDAO;
import com.mecanica.dao.OrdenDeServicioDAO;
import com.mecanica.enums.TipoItemOrdenServicio;
import com.mecanica.model.ItemOrdenServicio;
import com.mecanica.model.OrdenDeServicio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller de ItemOrdenServicio: agregar/quitar item de una OS y
 * mantener el valorTotal de la OS siempre en sincronia con la suma de los items.
 */
public class ItemOrdenServicioController {

    private final ItemOrdenServicioDAO itemOrdemServicoDAO = new ItemOrdenServicioDAO();
    private final OrdenDeServicioDAO ordemDeServicoDAO = new OrdenDeServicioDAO();

    public ItemOrdenServicio agregar(OrdenDeServicio os, TipoItemOrdenServicio tipo, String descripcion,
                                       BigDecimal cantidad, BigDecimal valorUnitario) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor unitario inválido.");
        }

        ItemOrdenServicio item = new ItemOrdenServicio();
        item.setOrdenDeServicio(os);
        item.setTipo(tipo);
        item.setDescripcion(descripcion);
        item.setCantidad(cantidad);
        item.setValorUnitario(valorUnitario);
        item.setValorTotal(cantidad.multiply(valorUnitario));
        ItemOrdenServicio salvo = itemOrdemServicoDAO.guardar(item);

        recalcularValorTotal(os);
        return salvo;
    }

    public void quitar(ItemOrdenServicio item) {
        OrdenDeServicio os = item.getOrdenDeServicio();
        itemOrdemServicoDAO.eliminar(item);
        recalcularValorTotal(os);
    }

    public List<ItemOrdenServicio> listarPorOrdemDeServico(OrdenDeServicio os) {
        return itemOrdemServicoDAO.listarPorOrdemDeServico(os);
    }

    /** Suma los items actuales de la OS y actualiza su valorTotal. */
    private void recalcularValorTotal(OrdenDeServicio os) {
        List<ItemOrdenServicio> items = itemOrdemServicoDAO.listarPorOrdemDeServico(os);
        BigDecimal total = BigDecimal.ZERO;
        for (ItemOrdenServicio i : items) {
            total = total.add(i.getValorTotal());
        }
        os.setValorTotal(total);
        ordemDeServicoDAO.guardar(os);
    }
}
