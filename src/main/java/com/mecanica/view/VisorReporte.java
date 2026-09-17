package com.mecanica.view;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerController;
import net.sf.jasperreports.swing.JRViewerToolbar;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Abre un reporte de JasperReports en una ventana de vista previa (el visor
 * de JasperReports), con los botones de imprimir y de guardar en PDF.
 *
 * IMPRESION SIEMPRE EN BLANCO Y NEGRO (pedido del usuario, para ahorrar
 * tinta): los disenos .jrxml ya no usan colores ni bloques rellenos, y ademas
 * el boton de imprimir del visor fue reemplazado para mandar el trabajo a la
 * impresora como MONOCROMATICO (Chromaticity.MONOCHROME).
 *
 * La generacion corre en un SwingWorker -- la primera vez que se abre cada
 * reporte JasperReports tiene que compilar el diseno y tarda unos segundos;
 * las siguientes veces sale enseguida.
 */
public final class VisorReporte {

    private VisorReporte() {
        // clase utilitaria: no debe ser instanciada
    }

    /**
     * @param origen    componente desde el que se pidio (para el cursor y los mensajes)
     * @param titulo    titulo de la ventana del visor
     * @param generador arma el reporte (por ejemplo: () -> reporteController.listadoClientes())
     */
    public static void mostrar(Component origen, String titulo, Callable<JasperPrint> generador) {
        origen.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<JasperPrint, Void>() {
            @Override
            protected JasperPrint doInBackground() throws Exception {
                return generador.call();
            }

            @Override
            protected void done() {
                origen.setCursor(Cursor.getDefaultCursor());
                JasperPrint reporte;
                try {
                    reporte = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    causa.printStackTrace();
                    String mensaje = causa instanceof IllegalArgumentException || causa instanceof IllegalStateException
                            ? causa.getMessage()
                            : "No fue posible generar el reporte.\n\nDetalle: " + causa.getMessage();
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(origen), mensaje,
                            "Reporte", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JFrame ventana = new JFrame("Taller JB - " + titulo);
                // Cerrar el visor solo cierra esta ventana, nunca el sistema.
                ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                ventana.getContentPane().add(new VisorBlancoYNegro(reporte), BorderLayout.CENTER);
                ventana.setSize(1000, 750);
                ventana.setLocationRelativeTo(null);
                ventana.setExtendedState(JFrame.MAXIMIZED_BOTH);
                // Si se imprime desde un dialogo modal (ej. los items de la OS o los
                // recibos de salario), el visor tiene que poder usarse igual.
                ventana.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
                ventana.setVisible(true);
                ventana.toFront();
            }
        }.execute();
    }

    /**
     * Manda el reporte a la impresora pidiendo impresion monocromatica. Abre
     * el dialogo de impresion (con la impresora predeterminada ya elegida)
     * para poder cambiar de impresora o la cantidad de copias.
     */
    static void imprimirEnBlancoYNegro(Component origen, JasperPrint reporte) {
        Thread hilo = new Thread(() -> {
            try {
                PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
                atributos.add(Chromaticity.MONOCHROME);

                SimplePrintServiceExporterConfiguration configuracion = new SimplePrintServiceExporterConfiguration();
                configuracion.setPrintRequestAttributeSet(atributos);
                configuracion.setDisplayPrintDialog(Boolean.TRUE);
                configuracion.setDisplayPageDialog(Boolean.FALSE);
                // Sin esto JasperReports toma la PRIMERA impresora de la lista,
                // que no siempre es la predeterminada de Windows.
                PrintService predeterminada = PrintServiceLookup.lookupDefaultPrintService();
                if (predeterminada != null) {
                    configuracion.setPrintService(predeterminada);
                }

                JRPrintServiceExporter exportador = new JRPrintServiceExporter();
                exportador.setExporterInput(new SimpleExporterInput(reporte));
                exportador.setConfiguration(configuracion);
                exportador.exportReport();
            } catch (JRException | RuntimeException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(origen),
                        "No fue posible imprimir.\n\nDetalle: " + e.getMessage(),
                        "Imprimir", JOptionPane.ERROR_MESSAGE));
            }
        }, "impresion-reporte");
        hilo.start();
    }

    /** Visor de JasperReports con el boton de imprimir cambiado a blanco y negro. */
    private static class VisorBlancoYNegro extends JRViewer {
        VisorBlancoYNegro(JasperPrint reporte) {
            super(reporte);
        }

        @Override
        protected JRViewerToolbar createToolbar() {
            return new BarraBlancoYNegro(viewerContext, this);
        }
    }

    private static class BarraBlancoYNegro extends JRViewerToolbar {
        BarraBlancoYNegro(JRViewerController controlador, Component visor) {
            super(controlador);
            for (ActionListener original : btnPrint.getActionListeners()) {
                btnPrint.removeActionListener(original);
            }
            btnPrint.setToolTipText("Imprimir (en blanco y negro)");
            btnPrint.addActionListener(e -> imprimirEnBlancoYNegro(visor, controlador.getJasperPrint()));
        }
    }
}
