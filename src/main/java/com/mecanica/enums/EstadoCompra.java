package com.mecanica.enums;

/**
 * Situacion de una Compra ("notinha" del proveedor) frente a lo que ya se
 * le pago. NO se guarda en la base: se calcula en pantalla (ver
 * ComprasProveedoresPanel), porque el pago al proveedor no es por nota --
 * es un valor que descuenta el saldo general. Se considera que la plata ya
 * pagada va cubriendo las notas de la mas vieja a la mas nueva.
 */
public enum EstadoCompra {
    PENDIENTE,
    PAGADA
}
