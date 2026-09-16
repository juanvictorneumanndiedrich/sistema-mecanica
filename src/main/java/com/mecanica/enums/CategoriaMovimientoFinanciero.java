package com.mecanica.enums;

/**
 * Origen/categoria de un MovimientoFinanciero, se usa para filtrar y
 * totalizar el Financiero por tipo de movimiento.
 *
 * RETIRO_SOCIO no existe aca a proposito: el retiro de un socio no
 * entra como gasto en el flujo de caja -- solo se descuenta de la parte
 * de ese socio al momento de la liquidacion (division de ganancia). Eso se controla
 * solo con la entidad RetiroSocio, sin generar MovimientoFinanciero.
 *
 * RETIRO_EMPLEADO existio hasta el 2026-09-16, cuando fue eliminada del enum:
 * el vale/adelanto de un empleado nunca llego a generar ningun movimiento
 * real con esa categoria en el banco del usuario, asi que no quedo ningun
 * historico que preservar. El gasto real del empleado es SALARIO_EMPLEADO,
 * generado una sola vez por EmpleadoController.pagarSalario() con el
 * salario base COMPLETO (sin descontar vales/adelantos).
 *
 * DEVOLUCION_CLIENTE / DEVOLUCION_PROVEEDOR (agregadas 2026-09-16): retiro en
 * efectivo de un credito a favor ya existente (saldo negativo del Cliente o
 * del Proveedor). DEVOLUCION_CLIENTE es SALIDA (la mecanica le devuelve
 * dinero al cliente); DEVOLUCION_PROVEEDOR es ENTRADA (el proveedor le
 * devuelve dinero a la mecanica). Ver ClienteController.retirarSaldo /
 * ProveedorController.retirarSaldo.
 */
public enum CategoriaMovimientoFinanciero {
    PAGO_CLIENTE,
    COMPRA_PROVEEDOR,
    SALARIO_EMPLEADO,
    DEVOLUCION_CLIENTE,
    DEVOLUCION_PROVEEDOR,
    OTRO
}
