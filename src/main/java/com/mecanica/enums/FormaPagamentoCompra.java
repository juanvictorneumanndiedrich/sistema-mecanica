package com.mecanica.enums;

/**
 * Os dois unicos cenarios previstos no formulario de Compra: a compra e
 * paga de imediato, ou entra na conta do fornecedor para ser acertada
 * depois em um FechamentoFornecedor. O cenario "conta do proprio cliente
 * no fornecedor" nao existe aqui -- o sistema nao registra isso.
 */
public enum FormaPagamentoCompra {
    PAGAMENTO_IMEDIATO,
    LANCADA_EM_CONTA_FORNECEDOR
}