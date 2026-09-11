package com.mecanica.enums;

/**
 * Los dos unicos escenarios previstos en el formulario de Compra (ya definido
 * en la fase de pantallas): la compra se paga al instante, o entra en la cuenta del
 * proveedor para ser saldada despues en un CierreProveedor.
 *
 * El escenario "cuenta del propio cliente en el proveedor" fue explicitamente
 * definido como algo que el sistema NO registra.
 */
public enum FormaPagoCompra {
    PAGO_INMEDIATO,
    CARGADA_EN_CUENTA_PROVEEDOR
}
