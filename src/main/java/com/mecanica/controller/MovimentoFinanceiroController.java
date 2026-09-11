package com.mecanica.controller;

import com.mecanica.dao.MovimentoFinanceiroDAO;
import com.mecanica.enums.CategoriaMovimentoFinanceiro;
import com.mecanica.enums.TipoMovimentoFinanceiro;
import com.mecanica.model.MovimentoFinanceiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de MovimentoFinanceiro: usado principalmente pra consulta
 * (extrato/resumo da tela Financeiro), ja que os lancamentos em si sao
 * gerados por outros Controllers (Cliente, Compra, FechamentoFornecedor,
 * RetiradaFuncionario). O unico lancamento criado diretamente aqui e o
 * manual (categoria OUTRO).
 */
public class MovimentoFinanceiroController {

    private final MovimentoFinanceiroDAO movimentoFinanceiroDAO = new MovimentoFinanceiroDAO();

    /** Lancamento manual, fora dos fluxos automaticos (categoria OUTRO). */
    public MovimentoFinanceiro registrarLancamentoManual(TipoMovimentoFinanceiro tipo, BigDecimal valor,
                                                          LocalDate data, String descricao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do lancamento deve ser maior que zero.");
        }
        MovimentoFinanceiro movimento = new MovimentoFinanceiro();
        movimento.setData(data != null ? data : LocalDate.now());
        movimento.setTipo(tipo);
        movimento.setCategoria(CategoriaMovimentoFinanceiro.OUTRO);
        movimento.setValor(valor);
        movimento.setDescricao(descricao);
        return movimentoFinanceiroDAO.salvar(movimento);
    }

    public List<MovimentoFinanceiro> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return movimentoFinanceiroDAO.listarPorPeriodo(inicio, fim);
    }

    public List<MovimentoFinanceiro> listarPorCategoria(CategoriaMovimentoFinanceiro categoria) {
        return movimentoFinanceiroDAO.listarPorCategoria(categoria);
    }

    /** Saldo do periodo: total de ENTRADA menos total de SAIDA. */
    public BigDecimal calcularSaldoPeriodo(LocalDate inicio, LocalDate fim) {
        BigDecimal entradas = BigDecimal.ZERO;
        BigDecimal saidas = BigDecimal.ZERO;
        for (MovimentoFinanceiro m : listarPorPeriodo(inicio, fim)) {
            if (m.getTipo() == TipoMovimentoFinanceiro.ENTRADA) {
                entradas = entradas.add(m.getValor());
            } else {
                saidas = saidas.add(m.getValor());
            }
        }
        return entradas.subtract(saidas);
    }
}