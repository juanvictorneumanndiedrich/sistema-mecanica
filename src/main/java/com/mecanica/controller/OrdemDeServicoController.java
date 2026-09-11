package com.mecanica.controller;

import com.mecanica.dao.OrdemDeServicoDAO;
import com.mecanica.enums.StatusOrdemServico;
import com.mecanica.model.Cliente;
import com.mecanica.model.Equipamento;
import com.mecanica.model.OrdemDeServico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de OrdemDeServico: abrir, consultar e fechar uma OS. A
 * inclusao/remocao de itens (e o recalculo do valor total) fica no
 * ItemOrdemServicoController, pra manter a responsabilidade num lugar so.
 */
public class OrdemDeServicoController {

    private final OrdemDeServicoDAO ordemDeServicoDAO = new OrdemDeServicoDAO();

    /** Abre uma nova OS, gerando o proximo numero sequencial automaticamente. */
    public OrdemDeServico abrir(Cliente cliente, Equipamento equipamento, String problemaRelatado) {
        if (cliente == null || equipamento == null) {
            throw new IllegalArgumentException("Cliente e equipamento sao obrigatorios pra abrir uma OS.");
        }
        Long maiorNumero = ordemDeServicoDAO.buscarMaiorNumero();

        OrdemDeServico os = new OrdemDeServico();
        os.setNumero(maiorNumero == null ? 1L : maiorNumero + 1);
        os.setCliente(cliente);
        os.setEquipamento(equipamento);
        os.setDataAbertura(LocalDate.now());
        os.setStatus(StatusOrdemServico.ABERTA);
        os.setProblemaRelatado(problemaRelatado);
        os.setValorTotal(BigDecimal.ZERO);
        return ordemDeServicoDAO.salvar(os);
    }

    /** Marca a OS como concluida e registra a data de fechamento (pra impressao/assinatura do cliente). */
    public OrdemDeServico fechar(OrdemDeServico os) {
        os.setStatus(StatusOrdemServico.CONCLUIDA);
        os.setDataFechamento(LocalDate.now());
        return ordemDeServicoDAO.salvar(os);
    }

    public OrdemDeServico cancelar(OrdemDeServico os) {
        os.setStatus(StatusOrdemServico.CANCELADA);
        os.setDataFechamento(LocalDate.now());
        return ordemDeServicoDAO.salvar(os);
    }

    public OrdemDeServico buscarPorId(Long id) {
        return ordemDeServicoDAO.buscarPorId(id);
    }

    public OrdemDeServico buscarPorNumero(Long numero) {
        return ordemDeServicoDAO.buscarPorNumero(numero);
    }

    public List<OrdemDeServico> listarTodos() {
        return ordemDeServicoDAO.listarTodos();
    }

    public List<OrdemDeServico> listarPorStatus(StatusOrdemServico status) {
        return ordemDeServicoDAO.listarPorStatus(status);
    }

    public List<OrdemDeServico> listarPorCliente(Cliente cliente) {
        return ordemDeServicoDAO.listarPorCliente(cliente);
    }

    public void excluir(OrdemDeServico os) {
        ordemDeServicoDAO.excluir(os);
    }
}