package com.mecanica.controller;

import com.mecanica.dao.RetiradaSocioDAO;
import com.mecanica.model.RetiradaSocio;
import com.mecanica.model.Socio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de RetiradaSocio. IMPORTANTE: registrar uma retirada de
 * socio NAO gera MovimentoFinanceiro (nao e tratada como gasto) -- so
 * fica registrada aqui, pra ser descontada da parte do socio no acerto
 * (ver SocioController.calcularAcerto).
 */
public class RetiradaSocioController {

    private final RetiradaSocioDAO retiradaSocioDAO = new RetiradaSocioDAO();

    public RetiradaSocio registrarRetirada(Socio socio, BigDecimal valor, LocalDate data, String observacao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da retirada deve ser maior que zero.");
        }
        RetiradaSocio retirada = new RetiradaSocio();
        retirada.setSocio(socio);
        retirada.setValor(valor);
        retirada.setData(data != null ? data : LocalDate.now());
        retirada.setObservacao(observacao);
        return retiradaSocioDAO.salvar(retirada);
    }

    public List<RetiradaSocio> listarPorSocioEPeriodo(Socio socio, LocalDate inicio, LocalDate fim) {
        return retiradaSocioDAO.listarPorSocioEPeriodo(socio, inicio, fim);
    }

    public void excluir(RetiradaSocio retirada) {
        retiradaSocioDAO.excluir(retirada);
    }
}