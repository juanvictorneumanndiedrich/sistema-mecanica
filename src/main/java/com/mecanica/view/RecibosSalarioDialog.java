package com.mecanica.view;

import com.mecanica.controller.EmpleadoController;
import com.mecanica.controller.ReporteController;
import com.mecanica.model.Empleado;
import com.mecanica.model.MovimientoFinanciero;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lista los pagos de salario ya hechos a un empleado, para reimprimir el
 * recibo de cualquiera de ellos (con los vales/adelantos que se descontaron
 * en ese pago).
 */
public class RecibosSalarioDialog extends JDialog {

    private final EmpleadoController empleadoController = new EmpleadoController();
    private final ReporteController reporteController = new ReporteController();

    private final TablaPagosModel modelo = new TablaPagosModel();
    private final JTable tabla = new JTable(modelo);
    private final BotonPlano botonImprimir = new BotonPlano("IMPRIMIR RECIBO");
    private final JLabel labelMensaje = new JLabel(" ");

    public RecibosSalarioDialog(Window padre, Empleado empleado) {
        super(padre, "Recibos de salario - " + empleado.getNombre(), ModalityType.APPLICATION_MODAL);

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setBackground(Paleta.GRIS_FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel titulo = new JLabel("Pagos de salario de " + empleado.getNombre());
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(Paleta.AZUL_OSCURO);
        contenido.add(titulo, BorderLayout.NORTH);

        tabla.setRowHeight(26);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getColumnModel().getColumn(0).setMaxWidth(110);
        tabla.getColumnModel().getColumn(2).setMaxWidth(140);
        tabla.getSelectionModel().addListSelectionListener(e -> botonImprimir.setEnabled(tabla.getSelectedRow() >= 0));
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    onImprimir();
                }
            }
        });
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        labelMensaje.setForeground(Paleta.GRIS_TEXTO);
        labelMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pie.add(labelMensaje, BorderLayout.WEST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCerrar = new BotonPlano("CERRAR", Paleta.GRIS_TEXTO, Paleta.GRIS_TEXTO.brighter());
        botonCerrar.addActionListener(e -> dispose());
        botonImprimir.addActionListener(e -> onImprimir());
        botonImprimir.setEnabled(false);
        botones.add(botonCerrar);
        botones.add(botonImprimir);
        pie.add(botones, BorderLayout.EAST);
        contenido.add(pie, BorderLayout.SOUTH);

        setContentPane(contenido);
        setSize(new Dimension(640, 420));
        setLocationRelativeTo(padre);

        cargar(empleado);
    }

    private void cargar(Empleado empleado) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<MovimientoFinanciero>, Void>() {
            @Override
            protected List<MovimientoFinanciero> doInBackground() {
                return empleadoController.listarPagosSalario(empleado);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    List<MovimientoFinanciero> pagos = get();
                    modelo.setDatos(pagos);
                    labelMensaje.setText(pagos.isEmpty()
                            ? "Este empleado todavia no tiene pagos de salario registrados."
                            : "Seleccione un pago (o doble clic) para imprimir el recibo.");
                    if (!pagos.isEmpty()) {
                        tabla.setRowSelectionInterval(0, 0);
                    }
                } catch (Exception e) {
                    labelMensaje.setForeground(Paleta.ROJO_ERROR);
                    labelMensaje.setText("No fue posible conectar a la base de datos.");
                }
            }
        }.execute();
    }

    private void onImprimir() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        MovimientoFinanciero pago = modelo.getPago(tabla.convertRowIndexToModel(fila));
        VisorReporte.mostrar(this, "Recibo de Salario", () -> reporteController.reciboSalario(pago));
    }

    private static class TablaPagosModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Concepto", "Salario (Gs.)"};
        private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");
        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private List<MovimientoFinanciero> pagos = List.of();

        void setDatos(List<MovimientoFinanciero> pagos) {
            this.pagos = pagos;
            fireTableDataChanged();
        }

        MovimientoFinanciero getPago(int fila) {
            return pagos.get(fila);
        }

        @Override
        public int getRowCount() {
            return pagos.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            MovimientoFinanciero pago = pagos.get(fila);
            switch (columna) {
                case 0:
                    return pago.getFecha() == null ? "" : pago.getFecha().format(FORMATO_FECHA);
                case 1:
                    return pago.getDescripcion() == null ? "" : pago.getDescripcion();
                case 2:
                    return FORMATO_VALOR.format(pago.getValor() == null ? BigDecimal.ZERO : pago.getValor());
                default:
                    return "";
            }
        }
    }
}
