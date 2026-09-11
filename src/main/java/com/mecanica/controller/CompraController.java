package com.mecanica.controller;

import com.mecanica.dao.CompraDAO;
import com.mecanica.enums.CategoriaMovimentoFinanceiro;
import com.mecanica.enums.FormaPagamentoCompra;
import com.mecanica.enums.TipoMovimentoFinanceiro;
import com.mecanica.model.Compra;
import com.mecanica.model.Fornecedor;
import com.mecanica.model.MovimentoFinanceiro;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de Compra. So tem os 2 cenarios ja definidos na fase de
 * telas: PAGAMENTO_IMEDIATO (gera MovimentoFinanceiro na hora) ou
 * LANCADA_EM_CONTA_FORNECEDOR (fica pendente ate entrar num
 * FechamentoFornecedor, que so gera o MovimentoFinanceiro quando for
 * marcado como pago).
 */
public class CompraController {

    private final CompraDAO compraDAO = new CompraDAO();

    public Compra registrarCompra(Fornecedor fornecedor, LocalDate data, String descricao,
                                   BigDecimal valor, FormaPagamentoCompra formaPagamento) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da compra deve ser maior que zero.");
        }
        LocalDate dataFinal = data != null ? data : LocalDate.now();

        if (formaPagamento == FormaPagamentoCompra.PAGAMENTO_IMEDIATO) {
            return registrarComMovimentoImediato(fornecedor, dataFinal, descricao, valor, formaPagamento);
        }

        // LANCADA_EM_CONTA_FORNECEDOR: so grava a compra, sem MovimentoFinanceiro ainda.
        Compra compra = new Compra();
        compra.setFornecedor(fornecedor);
        compra.setData(dataFinal);
        compra.setDescricao(descricao);
        compra.setValor(valor);
        compra.setFormaPagamento(formaPagamento);
        return compraDAO.salvar(compra);
    }

    private Compra registrarComMovimentoImediato(Fornecedor fornecedor, LocalDate data, String descricao,
                                                  BigDecimal valor, FormaPagamentoCompra formaPagamento) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Compra compra = new Compra();
            compra.setFornecedor(fornecedor);
            compra.setData(data);
            compra.setDescricao(descricao);
            compra.setValor(valor);
            compra.setFormaPagamento(formaPagamento);
            session.persist(compra);

            MovimentoFinanceiro movimento = new MovimentoFinanceiro();
            movimento.setData(data);
            movimento.setTipo(TipoMovimentoFinanceiro.SAIDA);
            movimento.setCategoria(CategoriaMovimentoFinanceiro.COMPRA_FORNECEDOR);
            movimento.setValor(valor);
            movimento.setDescricao(descricao);
            movimento.setFornecedor(fornecedor);
            session.persist(movimento);

            tx.commit();
            return compra;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public List<Compra> listarPorFornecedor(Fornecedor fornecedor) {
        return compraDAO.listarPorFornecedor(fornecedor);
    }

    public List<Compra> listarPendentesDeFechamento(Fornecedor fornecedor) {
        return compraDAO.listarPendentesDeFechamento(fornecedor);
    }

    public void excluir(Compra compra) {
        compraDAO.excluir(compra);
    }
}