package com.mecanica.enums;

/**
 * Un item de Orden de Servicio puede ser un servicio (mano de obra: soldadura,
 * torneria, etc.), un repuesto/material aplicado o un VIAJE. El viaje se cobra
 * al cliente junto con el resto de la OS (suma en su saldo), pero esa plata
 * NO entra en Financiero: va a la pestaña "Viajes" -- ver ViajeController.
 */
public enum TipoItemOrdenServicio {
    SERVICIO,
    REPUESTO,
    VIAJE
}
