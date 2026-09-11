package com.mecanica.controller;

import com.mecanica.dao.RetiradaFuncionarioDAO;
import com.mecanica.enums.CategoriaMovimentoFinanceiro;
import com.mecanica.enums.TipoMovimentoFinanceiro;
import com.mecanica.enums.TipoRetiradaFuncionario;
import com.mecanica.model.Funcionario;
import com.mecanica.model.MovimentoFinanceiro;
import com.mecanica.model.RetiradaFuncionario;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de RetiradaFuncionario. Diferente da retirada de socio,
 * retirada de funcionario (vale semanal ou adiantamento) ENTRA como gasto
 * -- gera um MovimentoFinanceiro (SAIDA / RETIRADA_FUNCIONARIO) na mesma
 * transacao.
 */
public class RetiradaFuncionarioController {

    private final RetiradaFuncionarioDAO retiradaFuncionarioDAO = new RetiradaFuncionarioDAO();

    public RetiradaFuncionario registrarRetirada(Funcionario funcionario, TipoRetiradaFuncionario tipo,
                                                  BigDecimal valor, LocalDate data, String observacao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da retirada deve ser maior que zero.");
        }
        LocalDate dataFinal = data != null ? data : LocalDate.now();

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            RetiradaFuncionario retirada = new RetiradaFuncionario();
            retirada.setFuncionario(funcionario);
            retirada.setTipo(tipo);
            retirada.setValor(valor);
            retirada.setData(dataFinal);
            retirada.setObservacao(observacao);
            session.persist(retirada);

            MovimentoFinanceiro movimento = new MovimentoFinanceiro();
            movimento.setData(dataFinal);
            movimento.setTipo(TipoMovimentoFinanceiro.SAIDA);
            movimento.setCategoria(CategoriaMovimentoFinanceiro.RETIRADA_FUNCIONARIO);
            movimento.setValor(valor);
            movimento.setDescricao(observacao);
            movimento.setFuncionario(funcionario);
            session.persist(movimento);

            tx.commit();
            return retirada;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public List<RetiradaFuncionario> listarPorFuncionarioEPeriodo(Funcionario funcionario, LocalDate inicio, LocalDate fim) {
        return retiradaFuncionarioDAO.listarPorFuncionarioEPeriodo(funcionario, inicio, fim);
    }

    public void excluir(RetiradaFuncionario retirada) {
        retiradaFuncionarioDAO.excluir(retirada);
    }
}