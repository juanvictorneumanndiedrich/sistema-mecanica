package com.mecanica.view;

/**
 * Implementada por los paneles de las 6 areas de navegacion. Permite que el
 * MainView les pida recargar sus datos cada vez que el usuario entra en esa
 * area del menu -- asi cualquier cambio hecho en otra pantalla (un pago, el
 * cierre de una OS, etc.) ya aparece actualizado sin tener que cerrar y abrir
 * el sistema de nuevo.
 */
public interface PanelActualizable {

    /** Recarga los datos que este panel muestra, consultando el banco de nuevo. */
    void actualizar();
}
