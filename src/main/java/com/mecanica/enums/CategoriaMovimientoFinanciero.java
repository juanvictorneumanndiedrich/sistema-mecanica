package com.mecanica.enums;

/**
 * Origen/categoria de un MovimientoFinanciero, se usa para filtrar y
 * totalizar el Financiero por tipo de movimiento.
 *
 * RETIRO_SOCIO no existe aca a proposito: el retiro de un socio no
 * entra como gasto en el flujo de caja -- solo se descuenta de la parte
 * de ese socio al momento de la liquidacion (division de ganancia). Eso se controla
 * solo con la entidad RetiroSocio, sin generar MovimientoFinanciero.
 */
public enum CategoriaMovimientoFinanciero {
    PAGO_CLIENTE,
    COMPRA_PROVEEDOR,
    RETIRO_EMPLEADO,
    OTRO
}
