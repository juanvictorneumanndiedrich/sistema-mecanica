package com.mecanica.view;

import com.mecanica.model.CierreProveedor;
import com.mecanica.model.Compra;
import com.mecanica.model.Proveedor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * ATENCION -- clase huerfana (2026-09-15): ninguna pantalla abre mas este
 * dialogo, el flujo de "cierre de cuenta" fue reemplazado por Compra
 * funcionando como notinha con boton Pagar directo (ver
 * CompraController.pagar). Se mantiene solo para no romper la compilacion
 * mientras el usuario no borra este archivo a mano.
 *
 * Dialogo modal para las dos etapas manuales del cierre de cuenta de un
 * Proveedor (ver CierreProveedorController):
 *
 * <ul>
 * <li>Modo ABRIR: se usa antes de CierreProveedorController.abrirCierre.
 * Muestra, a modo de previsualizacion, las compras pendientes (todas las
 * CARGADA_EN_CUENTA_PROVEEDOR sin cierre todavia) que van a entrar en el
 * cierre -- el metodo del Controller no permite elegir solo algunas, junta
 * siempre todas las pendientes del proveedor.</li>
 * <li>Modo PAGAR: se usa antes de CierreProveedorController.marcarComoPagado,
 * sobre un cierre CERRADO ya existente. Muestra su resumen y pide la fecha
 * de pago.</li>
 * </ul>
 */
public class CierreProveedorDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");

    private final JTextField campoFecha = new JTextField();
    private final JLabel labelError = new JLabel(" ");

    private LocalDate fecha;
    private boolean confirmado;

    /** Modo ABRIR: previsualiza las compras pendientes del proveedor antes de crear el cierre. */
    public CierreProveedorDialog(Window propietario, Proveedor proveedor, List<Compra> pendientes) {
        super(propietario, "Abrir Cierre - " + proveedor.getNombre(), ModalityType.APPLICATION_MODAL);
        armarPantalla("Se van a incluir estas compras pendientes de " + proveedor.getNombre() + ":",
                armarPanelPendientes(pendientes), "ABRIR CIERRE");
    }

    /** Modo PAGAR: muestra el resumen del cierre ya existente y pide la fecha de pago. */
    public CierreProveedorDialog(Window propietario, CierreProveedor cierreExistente) {
        super(propietario, "Marcar Como Pagado - " + cierreExistente.getProveedor().getNombre(),
                ModalityType.APPLICATION_MODAL);
        String resumen = "Cierre del " + cierreExistente.getFechaCierre().format(FORMATO_FECHA)
                + " -- Valor total: Gs. " + FORMATO_VALOR.format(cierreExistente.getValorTotal());
        armarPantalla(resumen, null, "MARCAR COMO PAGADO");
    }

    /** true si el usuario confirmo la accion (y no cerro/cancelo). */
    public boolean isConfirmado() {
        return confirmado;
    }

    /** Fecha de cierre (modo ABRIR) o fecha de pago (modo PAGAR), segun el constructor usado. */
    public LocalDate getFecha() {
        return fecha;
    }

    private void armarPantalla(String mensaje, JComponent panelPendientes, String textoBotonConfirmar) {
        boolean muestraTabla = panelPendientes != null;
        setSize(muestraTabla ? 480 : 400, muestraTabla ? 480 : 300);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel contenedor = new JPanel(new BorderLayout(0, 14));
        contenedor.setBackground(Paleta.GRIS_FONDO);
        contenedor.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel labelMensaje = new JLabel("<html>" + mensaje + "</html>");
        labelMensaje.setForeground(Paleta.AZUL_OSCURO);
        labelMensaje.setFont(new Font("Segoe UI", Font.BOLD, 13));
        contenedor.add(labelMensaje, BorderLayout.NORTH);

        if (muestraTabla) {
            contenedor.add(panelPendientes, BorderLayout.CENTER);
        }

        JPanel panelFecha = new JPanel(new GridBagLayout());
        panelFecha.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel labelFecha = new JLabel("FECHA (dd/mm/aaaa) *");
        labelFecha.setForeground(Paleta.GRIS_TEXTO);
        labelFecha.setFont(new Font("Segoe UI", Font.BOLD, 11));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        panelFecha.add(labelFecha, gbc);

        campoFecha.setText(LocalDate.now().format(FORMATO_FECHA));
        campoFecha.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campoFecha.setPreferredSize(new Dimension(0, 34));
        campoFecha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Paleta.GRIS_BORDE),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 0, 0);
        panelFecha.add(campoFecha, gbc);

        labelError.setForeground(Paleta.ROJO_ERROR);
        labelError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        panelFecha.add(labelError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano botonCancelar = new BotonPlano("CANCELAR", Paleta.GRIS_DESHABILITADO, Paleta.GRIS_TEXTO);
        botonCancelar.addActionListener(e -> dispose());
        BotonPlano botonConfirmar = new BotonPlano(textoBotonConfirmar);
        botonConfirmar.addActionListener(e -> onConfirmar());
        botones.add(botonCancelar);
        botones.add(botonConfirmar);
        gbc.gridy = 3;
        gbc.insets = new Insets(16, 0, 0, 0);
        panelFecha.add(botones, gbc);

        contenedor.add(panelFecha, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(botonConfirmar);
        add(contenedor, BorderLayout.CENTER);
        setLocationRelativeTo(getOwner());
    }

    /** Tabla de solo lectura con las compras pendientes y su total, para el modo ABRIR. */
    private JComponent armarPanelPendientes(List<Compra> pendientes) {
        JTable tabla = new JTable(new TablaPendientesModel(pendientes));
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(24);
        tabla.setEnabled(false);
        tabla.setGridColor(Paleta.GRIS_BORDE);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setForeground(Paleta.GRIS_TEXTO);

        BigDecimal total = BigDecimal.ZERO;
        for (Compra compra : pendientes) {
            total = total.add(compra.getValorTotal());
        }

        JLabel labelTotal = new JLabel("Total: Gs. " + FORMATO_VALOR.format(total));
        labelTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelTotal.setForeground(Paleta.AZUL_OSCURO);
        labelTotal.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(0, 200));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(labelTotal, BorderLayout.SOUTH);
        return panel;
    }

    private void onConfirmar() {
        try {
            fecha = LocalDate.parse(campoFecha.getText().trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            labelError.setText("Ingrese una fecha valida (dd/mm/aaaa).");
            return;
        }

        confirmado = true;
        dispose();
    }

    private static class TablaPendientesModel extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Descripcion", "Valor (Gs.)"};
        private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DecimalFormat FORMATO_VALOR = new DecimalFormat("#,##0");

        private final List<Compra> compras;

        TablaPendientesModel(List<Compra> compras) {
            this.compras = compras;
        }

        @Override
        public int getRowCount() {
            return compras.size();
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
            Compra compra = compras.get(fila);
            switch (columna) {
                case 0:
                    return compra.getFecha().format(FORMATO);
                case 1:
                    return compra.getNumero() == null ? "" : "Nota N° " + compra.getNumero();
                case 2:
                    return FORMATO_VALOR.format(compra.getValorTotal());
                default:
                    return "";
            }
        }
    }
}
