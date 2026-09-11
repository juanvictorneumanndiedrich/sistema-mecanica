package com.mecanica.enums;

/**
 * El cierre con el proveedor ocurre en dos etapas manuales:
 * primero se cierra la cuenta (CERRADO), despues, cuando el valor es
 * efectivamente pagado, se marca como PAGADO.
 */
public enum EstadoCierreProveedor {
    CERRADO,
    PAGADO
}
