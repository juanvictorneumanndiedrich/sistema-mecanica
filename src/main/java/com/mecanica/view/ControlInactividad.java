package com.mecanica.view;

import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;

/**
 * Cierre de sesion por inactividad: si nadie mueve el mouse ni toca el
 * teclado en ninguna ventana del sistema durante MINUTOS minutos, se ejecuta
 * la accion recibida en iniciar(...) -- en la practica, MainView cierra
 * todas las ventanas y vuelve a la pantalla de login.
 *
 * Para cambiar el tiempo, basta con editar la constante MINUTOS.
 */
final class ControlInactividad {

    static final int MINUTOS = 15;

    private static final long EVENTOS = AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
            | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK;

    private static Timer timer;
    private static AWTEventListener oyente;

    private ControlInactividad() {
        // clase utilitaria: no debe ser instanciada
    }

    /** Empieza a contar; cualquier actividad del usuario vuelve el contador a cero. */
    static void iniciar(Runnable alVencer) {
        detener();
        Timer nuevo = new Timer(MINUTOS * 60_000, e -> {
            detener();
            alVencer.run();
        });
        nuevo.setRepeats(false);
        timer = nuevo;
        oyente = evento -> nuevo.restart();
        Toolkit.getDefaultToolkit().addAWTEventListener(oyente, EVENTOS);
        nuevo.start();
    }

    static void detener() {
        if (oyente != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(oyente);
            oyente = null;
        }
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
}
