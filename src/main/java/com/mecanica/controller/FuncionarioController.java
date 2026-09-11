package com.mecanica.controller;

import com.mecanica.dao.FuncionarioDAO;
import com.mecanica.dao.RetiradaFuncionarioDAO;
import com.mecanica.model.Funcionario;
import com.mecanica.model.RetiradaFuncionario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller de Funcionario: CRUD e o calculo do fechamento mensal
 * (recibo com vales, adiantamentos, total descontado e valor liquido).
 * O registro das retiradas em si fica no RetiradaFuncionarioController.
 */
public class FuncionarioController {

    private final FuncionarioDAO funcionarioDAO = new FuncionarioDAO();
    private final RetiradaFuncionarioDAO retiradaFuncionarioDAO = new RetiradaFuncionarioDAO();

    public Funcionario salvar(Funcionario funcionario) {
        validar(funcionario);
        return funcionarioDAO.salvar(funcionario);
    }

    public Funcionario buscarPorId(Long id) {
        return funcionarioDAO.buscarPorId(id);
    }

    public List<Funcionario> listarTodos() {
        return funcionarioDAO.listarTodos();
    }

    public List<Funcionario> listarAtivos() {
        return funcionarioDAO.listarAtivos();
    }

    public List<Funcionario> buscarPorNome(String nome) {
        return funcionarioDAO.buscarPorNome(nome);
    }

    public void excluir(Funcionario funcionario) {
        funcionarioDAO.excluir(funcionario);
    }

    /**
     * Monta o fechamento mensal do funcionario: soma os vales e
     * adiantamentos do periodo e calcula o valor liquido a pagar
     * (salario base - total descontado). Usado pra gerar o recibo
     * imprimivel (JasperReports).
     */
    public ResultadoFechamentoMensal calcularFechamentoMensal(Funcionario funcionario, LocalDate inicio, LocalDate fim) {
        List<RetiradaFuncionario> retiradas = retiradaFuncionarioDAO.listarPorFuncionarioEPeriodo(funcionario, inicio, fim);
        BigDecimal totalDescontado = BigDecimal.ZERO;
        for (RetiradaFuncionario r : retiradas) {
            totalDescontado = totalDescontado.add(r.getValor());
        }
        BigDecimal salarioBase = funcionario.getSalarioBase() != null ? funcionario.getSalarioBase() : BigDecimal.ZERO;
        BigDecimal valorLiquido = salarioBase.subtract(totalDescontado);
        return new ResultadoFechamentoMensal(funcionario, new ArrayList<>(retiradas), totalDescontado, valorLiquido);
    }

    private void validar(Funcionario funcionario) {
        if (funcionario.getNome() == null || funcionario.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do funcionario e obrigatorio.");
        }
    }

    /** Resultado do fechamento mensal de um funcionario, pronto pro recibo. */
    public static class ResultadoFechamentoMensal {
        private final Funcionario funcionario;
        private final List<RetiradaFuncionario> retiradas;
        private final BigDecimal totalDescontado;
        private final BigDecimal valorLiquido;

        public ResultadoFechamentoMensal(Funcionario funcionario, List<RetiradaFuncionario> retiradas,
                                          BigDecimal totalDescontado, BigDecimal valorLiquido) {
            this.funcionario = funcionario;
            this.retiradas = retiradas;
            this.totalDescontado = totalDescontado;
            this.valorLiquido = valorLiquido;
        }

        public Funcionario getFuncionario() {
            return funcionario;
        }

        public List<RetiradaFuncionario> getRetiradas() {
            return retiradas;
        }

        public BigDecimal getTotalDescontado() {
            return totalDescontado;
        }

        public BigDecimal getValorLiquido() {
            return valorLiquido;
        }
    }
}