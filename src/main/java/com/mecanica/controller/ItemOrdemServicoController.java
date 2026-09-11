package com.mecanica.controller;

import com.mecanica.dao.ItemOrdemServicoDAO;
import com.mecanica.dao.OrdemDeServicoDAO;
import com.mecanica.enums.TipoItemOrdemServico;
import com.mecanica.model.ItemOrdemServico;
import com.mecanica.model.OrdemDeServico;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller de ItemOrdemServico: adicionar/remover item de uma OS e
 * manter o valorTotal da OS sempre em sincronia com a soma dos itens.
 */
public class ItemOrdemServicoController {

    private final ItemOrdemServicoDAO itemOrdemServicoDAO = new ItemOrdemServicoDAO();
    private final OrdemDeServicoDAO ordemDeServicoDAO = new OrdemDeServicoDAO();

    public ItemOrdemServico adicionar(OrdemDeServico os, TipoItemOrdemServico tipo, String descricao,
                                       BigDecimal quantidade, BigDecimal valorUnitario) {
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor unitario invalido.");
        }

        ItemOrdemServico item = new ItemOrdemServico();
        item.setOrdemDeServico(os);
        item.setTipo(tipo);
        item.setDescricao(descricao);
        item.setQuantidade(quantidade);
        item.setValorUnitario(valorUnitario);
        item.setValorTotal(quantidade.multiply(valorUnitario));
        ItemOrdemServico salvo = itemOrdemServicoDAO.salvar(item);

        recalcularValorTotal(os);
        return salvo;
    }

    public void remover(ItemOrdemServico item) {
        OrdemDeServico os = item.getOrdemDeServico();
        itemOrdemServicoDAO.excluir(item);
        recalcularValorTotal(os);
    }

    public List<ItemOrdemServico> listarPorOrdemDeServico(OrdemDeServico os) {
        return itemOrdemServicoDAO.listarPorOrdemDeServico(os);
    }

    /** Soma os itens atuais da OS e atualiza o valorTotal dela. */
    private void recalcularValorTotal(OrdemDeServico os) {
        List<ItemOrdemServico> itens = itemOrdemServicoDAO.listarPorOrdemDeServico(os);
        BigDecimal total = BigDecimal.ZERO;
        for (ItemOrdemServico i : itens) {
            total = total.add(i.getValorTotal());
        }
        os.setValorTotal(total);
        ordemDeServicoDAO.salvar(os);
    }
}