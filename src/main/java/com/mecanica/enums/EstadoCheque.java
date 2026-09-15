package com.mecanica.enums;

/**
 * Situacion de un ChequePreDatado: nace PENDIENTE y solo pasa a CONFIRMADO
 * cuando el cheque vence de verdad y alguien lo confirma en la pantalla
 * Financiero (ver ChequePreDatadoController.confirmar) -- recien ahi se
 * genera el MovimientoFinanciero (entrada o gasto real en el flujo de caja).
 */
public enum EstadoCheque {
    PENDIENTE,
    CONFIRMADO
}
