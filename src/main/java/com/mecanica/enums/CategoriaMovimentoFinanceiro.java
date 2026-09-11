package com.mecanica.enums;

/**
 * Origem/categoria de um MovimentoFinanceiro, usada para filtrar e
 * totalizar o Financeiro por tipo de lancamento.
 *
 * RETIRADA_SOCIO nao existe aqui de proposito: a retirada de um socio nao
 * entra como gasto no fluxo de caixa -- ela e' apenas descontada da parte
 * daquele socio na hora do acerto (divisao de lucro). Isso e controlado
 * so pela entidade RetiradaSocio, sem gerar MovimentoFinanceiro.
 */
public enum CategoriaMovimentoFinanceiro {
    PAGAMENTO_CLIENTE,
    COMPRA_FORNECEDOR,
    RETIRADA_FUNCIONARIO,
    OUTRO
}