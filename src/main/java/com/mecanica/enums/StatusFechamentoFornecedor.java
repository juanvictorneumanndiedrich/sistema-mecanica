package com.mecanica.enums;

/**
 * O fechamento com fornecedor acontece em duas etapas manuais: primeiro
 * fecha-se a conta (FECHADO), depois, quando o valor e efetivamente
 * pago, marca-se como PAGO.
 */
public enum StatusFechamentoFornecedor {
    FECHADO,
    PAGO
}