package com.mecanica.view;

import com.mecanica.controller.UsuarioController;
import com.mecanica.model.Usuario;
import com.mecanica.util.Sesion;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Ayuda en pantalla para quien recien empieza a usar el sistema:
 *
 * <ul>
 * <li>El boton "?" de la barra superior (ver MainView) abre {@link #mostrarGuia}
 * con los pasos de la pantalla/pestana que esta abierta.</li>
 * <li>{@link #tooltip} deja una explicacion corta que aparece al dejar el
 * mouse sobre un boton o campo.</li>
 * <li>El recorrido de bienvenida (RecorridoBienvenida) usa
 * {@link #resumenArea} para presentar cada area.</li>
 * </ul>
 *
 * Cada usuario puede apagar la ayuda (engranaje de la barra superior ->
 * "Mostrar ayuda"). La opcion se guarda en la base, en el propio usuario
 * (columna mostrar_ayuda), asi vale en cualquier ingreso. Con la ayuda
 * apagada desaparecen el boton "?" y las explicaciones al pasar el mouse.
 */
public final class Ayuda {

    private static final int ANCHO_TOOLTIP = 260;

    /** Componentes con explicacion registrada. Debil: no impide que se liberen los dialogos cerrados. */
    private static final Map<JComponent, String> explicaciones = new WeakHashMap<>();

    /** Aviso a la ventana principal cuando la ayuda se prende o se apaga (para mostrar/ocultar el "?"). */
    private static Runnable alCambiar;

    private static final Map<String, Guia> guias = new LinkedHashMap<>();
    private static final Map<String, String> resumenes = new LinkedHashMap<>();

    private Ayuda() {
        // clase utilitaria: no debe ser instanciada
    }

    // ------------------------------------------------------------ prender / apagar

    /** true si el usuario logueado tiene la ayuda prendida. */
    public static boolean activa() {
        Usuario usuario = Sesion.getUsuario();
        return usuario == null || usuario.isMostrarAyuda();
    }

    /** Prende o apaga la ayuda del usuario logueado y la guarda en la base. */
    public static void setActiva(Component padre, boolean activa) {
        Usuario usuario = Sesion.getUsuario();
        if (usuario == null || usuario.isMostrarAyuda() == activa) {
            return;
        }
        usuario.setMostrarAyuda(activa);
        aplicarATodos();
        guardarOpciones(padre, usuario);
    }

    /** Guarda en la base las opciones de ayuda del usuario (en segundo plano, sin trabar la pantalla). */
    static void guardarOpciones(Component padre, Usuario usuario) {
        new SwingWorker<Void, Void>() {
            RuntimeException error;

            @Override
            protected Void doInBackground() {
                try {
                    new UsuarioController().guardarOpcionesAyuda(usuario);
                } catch (RuntimeException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    error.printStackTrace();
                    JOptionPane.showMessageDialog(padre,
                            "No fue posible guardar la opcion de ayuda.\n\n" + error.getMessage(),
                            "Ayuda", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    static void setAlCambiar(Runnable accion) {
        alCambiar = accion;
    }

    // ------------------------------------------------------------ explicaciones al pasar el mouse

    /**
     * Deja una explicacion corta que aparece al dejar el mouse sobre el
     * componente. Si la ayuda esta apagada no aparece (y vuelve a aparecer
     * si se prende de nuevo).
     */
    public static void tooltip(JComponent componente, String texto) {
        explicaciones.put(componente, texto);
        aplicar(componente, texto);
    }

    private static void aplicarATodos() {
        for (Map.Entry<JComponent, String> entrada : new ArrayList<>(explicaciones.entrySet())) {
            aplicar(entrada.getKey(), entrada.getValue());
        }
        if (alCambiar != null) {
            alCambiar.run();
        }
    }

    private static void aplicar(JComponent componente, String texto) {
        if (componente == null) {
            return;
        }
        componente.setToolTipText(activa() ? formatear(texto) : null);
    }

    /** Textos largos en varias lineas, para que la explicacion no cruce toda la pantalla. */
    private static String formatear(String texto) {
        if (texto.length() <= 45) {
            return texto;
        }
        return "<html><div style='width:" + ANCHO_TOOLTIP + "px'>" + escapar(texto) + "</div></html>";
    }

    // ------------------------------------------------------------ guia de cada pantalla

    /**
     * Muestra los pasos de la pantalla indicada. area = nombre de la card de
     * MainView; pestana = titulo de la pestana abierta (o null si el area no
     * tiene pestanas).
     */
    static void mostrarGuia(Component padre, String area, String pestana) {
        Guia guia = pestana != null ? guias.get(area + "/" + pestana) : null;
        if (guia == null) {
            guia = guias.get(area);
        }
        if (guia == null) {
            guia = new Guia("Ayuda", "Use el menu de la izquierda para elegir un area del sistema.");
        }

        Window ventana = padre instanceof Window w ? w : SwingUtilities.getWindowAncestor(padre);
        JDialog dialogo = new JDialog(ventana, "Ayuda - " + guia.titulo, Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBackground(Paleta.BLANCO);

        JLabel encabezado = new JLabel(guia.titulo);
        encabezado.setOpaque(true);
        encabezado.setBackground(Paleta.AZUL_OSCURO);
        encabezado.setForeground(Paleta.BLANCO);
        encabezado.setFont(new Font("Segoe UI", Font.BOLD, 15));
        encabezado.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        contenido.add(encabezado, BorderLayout.NORTH);

        StringBuilder html = new StringBuilder("<html><body style='width:430px'>");
        html.append("<ol style='margin-left:18px'>");
        for (String paso : guia.pasos) {
            html.append("<li style='margin-bottom:7px'>").append(paso).append("</li>");
        }
        html.append("</ol></body></html>");
        JLabel pasos = new JLabel(html.toString());
        pasos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pasos.setForeground(Paleta.AZUL_OSCURO);
        pasos.setBorder(BorderFactory.createEmptyBorder(14, 10, 6, 18));
        contenido.add(pasos, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(12, 0));
        pie.setBackground(Paleta.GRIS_FONDO);
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        JLabel nota = new JLabel("<html>Para apagar la ayuda: engranaje (arriba a la derecha) &gt; Mostrar ayuda.</html>");
        nota.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        nota.setForeground(Paleta.GRIS_TEXTO);
        pie.add(nota, BorderLayout.CENTER);
        BotonPlano entendido = new BotonPlano("ENTENDIDO");
        entendido.addActionListener(e -> dialogo.dispose());
        pie.add(entendido, BorderLayout.EAST);
        contenido.add(pie, BorderLayout.SOUTH);

        dialogo.setContentPane(contenido);
        dialogo.getRootPane().setDefaultButton(entendido);
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialogo.pack();
        dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(ventana);
        dialogo.setVisible(true);
    }

    /** Una o dos frases que presentan el area en el recorrido de bienvenida. */
    static String resumenArea(String area) {
        return resumenes.getOrDefault(area, "");
    }

    private static final class Guia {
        final String titulo;
        final List<String> pasos;

        Guia(String titulo, String... pasos) {
            this.titulo = titulo;
            this.pasos = List.of(pasos);
        }
    }

    private static void guia(String clave, String titulo, String... pasos) {
        guias.put(clave, new Guia(titulo, pasos));
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------ textos

    static {
        // Explicaciones al pasar el mouse: un poco mas de tiempo en pantalla para poder leerlas.
        ToolTipManager.sharedInstance().setDismissDelay(20000);

        resumenes.put(MainView.CARD_CLIENTES_MAQUINARIOS,
                "Aqui se cargan los clientes y sus maquinarios (camiones, tractores, etc.). "
                        + "Tambien se ve cuanto debe cada cliente y se registran sus pagos.");
        resumenes.put(MainView.CARD_ORDENES_SERVICIO,
                "Cada trabajo del taller es una Orden de Servicio (OS): se abre, se le cargan "
                        + "servicios y repuestos, y al terminar se cierra e imprime.");
        resumenes.put(MainView.CARD_COMPRAS_PROVEEDORES,
                "Aqui se cargan los proveedores y las compras que el taller les hace, y se "
                        + "registran los pagos a cada proveedor.");
        resumenes.put(MainView.CARD_FINANCIERO,
                "Todo el dinero que entra y sale del taller. Tambien los cheques pre-datados "
                        + "pendientes y el cierre del mes con el reparto entre los socios.");
        resumenes.put(MainView.CARD_EMPLEADOS_SOCIOS,
                "Datos de los empleados, sus vales y adelantos, y el pago del salario. "
                        + "En la pestana Socios, los retiros de cada socio.");
        resumenes.put(MainView.CARD_USUARIOS,
                "Quien puede entrar al sistema y que puede hacer cada uno. "
                        + "Tambien el registro de todo lo que se hizo en el sistema.");

        guia(MainView.CARD_CLIENTES_MAQUINARIOS, "Clientes y Maquinarios",
                "Para cargar un cliente nuevo, haga clic en <b>NUEVO CLIENTE</b> y complete sus datos.",
                "Para encontrar un cliente, escriba parte del nombre en <b>BUSCAR</b> y presione <b>ENTER</b>.",
                "Al seleccionar un cliente, a la derecha aparecen sus maquinarios. Con <b>NUEVO</b> "
                        + "se agrega un camion, tractor, etc. de ese cliente.",
                "La columna <b>Saldo</b> muestra en <font color='#c0392b'>rojo</font> lo que el cliente "
                        + "le debe al taller y en <font color='#278a5b'>verde</font> el credito a su favor. "
                        + "El saldo sube cuando se cierra una OS del cliente.",
                "Cuando el cliente paga, seleccionelo y haga clic en <b>REGISTRAR PAGO</b> "
                        + "(en efectivo o con cheque pre-datado; tambien se puede hacer un descuento). "
                        + "El pago baja su saldo y entra en Financiero.",
                "<b>RETIRAR SALDO</b> sirve solo cuando el cliente tiene credito a favor y se le "
                        + "devuelve ese dinero.");

        guia(MainView.CARD_ORDENES_SERVICIO, "Ordenes de Servicio",
                "Haga clic en <b>NUEVA OS</b>, elija el cliente y, si corresponde, el maquinario "
                        + "(tambien puede ser un servicio general, sin maquinaria). Describa el problema.",
                "Al crearla se abre la pantalla de items: agregue los <b>servicios</b> (mano de obra) "
                        + "y los <b>repuestos</b> con cantidad y precio. El total se calcula solo.",
                "Para volver a una OS, seleccionela en la lista y haga clic en <b>VER / EDITAR ITEMS</b>. "
                        + "Use <b>ESTADO</b> y <b>BUSCAR POR CLIENTE</b> para encontrarla.",
                "Cuando el trabajo esta terminado, haga clic en <b>CERRAR OS</b>: el total se suma a "
                        + "la cuenta (saldo) del cliente. Una OS cerrada no vuelve a quedar abierta.",
                "<b>IMPRIMIR OS</b> genera la hoja para imprimir y entregar.",
                "<b>CANCELAR OS</b> es para un trabajo que no se va a hacer: no se le cobra nada al cliente.");

        guia(MainView.CARD_COMPRAS_PROVEEDORES, "Compras y Proveedores",
                "Para cargar un proveedor nuevo, haga clic en <b>NUEVO PROVEEDOR</b>.",
                "Al seleccionar un proveedor, a la derecha aparecen sus compras (notas).",
                "<b>NUEVA COMPRA</b> abre una nota con numero automatico. Agregue cada cosa comprada "
                        + "con cantidad y precio. El valor se suma a lo que el taller le debe al proveedor.",
                "Cuando el taller le paga, seleccione el proveedor y haga clic en <b>REGISTRAR PAGO</b> "
                        + "(efectivo o cheque pre-datado). Baja la deuda y queda como gasto en Financiero.",
                "Lo pagado va cubriendo las notas de la mas vieja a la mas nueva. Una nota "
                        + "<b>PAGADA</b> ya no se puede editar ni eliminar.",
                "<b>RETIRAR SALDO</b> sirve cuando el taller pago de mas y el proveedor devuelve ese dinero.");

        guia(MainView.CARD_FINANCIERO + "/Movimientos", "Financiero - Movimientos",
                "Aqui aparece todo el dinero que entro y salio. Casi todo se registra solo: pagos de "
                        + "clientes, pagos a proveedores, salarios y cheques confirmados.",
                "Elija las fechas <b>DESDE</b> y <b>HASTA</b> (dd/mm/aaaa) y el <b>TIPO</b>, y haga clic "
                        + "en <b>FILTRAR</b>. Abajo aparece el saldo del periodo.",
                "<b>NUEVO MOVIMIENTO</b> es para registrar a mano algo que no entra por otra pantalla "
                        + "(por ejemplo: luz, alquiler, una venta suelta).",
                "<b>IMPRIMIR</b> genera el reporte de los movimientos filtrados.");

        guia(MainView.CARD_FINANCIERO + "/Cheques Pendientes", "Financiero - Cheques Pendientes",
                "Aqui estan los cheques pre-datados (de clientes o a proveedores) que todavia no se cobraron.",
                "El saldo del cliente o del proveedor ya se desconto cuando se registro el cheque.",
                "Una fecha de vencimiento en <font color='#c0392b'>rojo</font> quiere decir que el cheque ya vencio.",
                "Cuando el cheque se cobra, seleccionelo y haga clic en <b>CONFIRMAR CHEQUE</b>: "
                        + "recien ahi el dinero entra (o sale) en Financiero.");

        guia(MainView.CARD_FINANCIERO + "/Cierre Mensual", "Financiero - Cierre Mensual",
                "En la lista de pendientes estan los movimientos que todavia no entraron en ningun "
                        + "cierre, sin importar la fecha.",
                "Marque la casilla de cada movimiento que entra en este cierre.",
                "Si quiere, escriba una descripcion (por ejemplo: \"Septiembre 2026\").",
                "<b>CERRAR MES</b> calcula la ganancia (entradas menos salidas) y la reparte 50/50 "
                        + "entre los socios, descontando lo que cada socio ya retiro.",
                "Los movimientos cerrados quedan bloqueados. El cierre queda guardado en el "
                        + "<b>Historico de cierres</b>, y se imprime con <b>IMPRIMIR ACERTO</b>.");

        guia(MainView.CARD_EMPLEADOS_SOCIOS + "/Empleados", "Empleados",
                "Para cargar un empleado, haga clic en <b>NUEVO EMPLEADO</b> (con su salario base).",
                "<b>REGISTRAR RETIRO</b> es para los vales y adelantos. No salen de Financiero en ese "
                        + "momento: se descuentan cuando se paga el salario.",
                "Para pagar el mes: seleccione el empleado, ponga el periodo (desde / hasta) y haga "
                        + "clic en <b>CALCULAR</b> para ver el salario, los vales y lo que queda por pagar.",
                "<b>PAGAR SALARIO</b> registra el salario como gasto en Financiero y marca los vales "
                        + "del periodo como ya descontados.",
                "<b>RECIBOS DE SALARIO</b> muestra los pagos ya hechos para imprimir el recibo.");

        guia(MainView.CARD_EMPLEADOS_SOCIOS + "/Socios", "Socios",
                "Con <b>NUEVO SOCIO</b> y <b>EDITAR</b> se cargan los datos de los socios.",
                "<b>REGISTRAR RETIRO</b> es para cuando un socio saca dinero a cuenta de su ganancia.",
                "El retiro del socio no aparece en Financiero: se le descuenta de su parte en el "
                        + "proximo cierre (Financiero &gt; Cierre Mensual).");

        guia(MainView.CARD_USUARIOS, "Usuarios y Permisos",
                "<b>NUEVO USUARIO</b>: nombre, login y contrasena, y marque que areas y acciones puede usar.",
                "<b>EDITAR</b> sirve para cambiar los permisos o desactivar un usuario "
                        + "(un usuario inactivo no puede entrar).",
                "<b>CAMBIAR CONTRASENA</b> pone una contrasena nueva al usuario seleccionado.",
                "<b>REGISTRO DE ACTIVIDAD</b> muestra quien hizo que y cuando.",
                "Siempre tiene que quedar al menos un usuario activo con permiso de Usuarios y Permisos.");
    }
}
