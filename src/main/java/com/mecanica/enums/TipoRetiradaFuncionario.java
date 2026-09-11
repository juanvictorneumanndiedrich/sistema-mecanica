package com.mecanica.enums;

/**
 * Tipo de retirada lancada para um funcionario. VALE_SEMANAL corresponde
 * ao vale opcional de valor fixo (Gs. 200.000), lancado manualmente
 * quando o funcionario o solicita.
 */
public enum TipoRetiradaFuncionario {
    VALE_SEMANAL,
    ADIANTAMENTO,
    OUTRO
}