package com.mecanica.controller;

import com.mecanica.dao.MovimientoFinancieroDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.MovimientoFinanciero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de MovimientoFinanciero: se usa principalmente para consulta
 * (extracto/resumen de la pantalla Financiero), ya que los movimientos en si son
 * generados por otros Controllers (Cliente, Compra, CierreProveedor,
 * RetiroEmpleado). El unico movimiento creado directamente aca es el
 * manual (categoria OTRO).
 */
public class MovimientoFinancieroController {

    private final MovimientoFinancieroDAO movimentoFinanceiroDAO = new MovimientoFinancieroDAO();

    /** Movimiento manual, fuera de los flujos automaticos (categoria OTRO). */
    public MovimientoFinanciero registrarMovimientoManual(TipoMovimientoFinanciero tipo, BigDecimal valor,
                                                          LocalDate fecha, String descripcion) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del movimiento debe ser mayor que cero.");
        }
        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setFecha(fecha != null ? fecha : LocalDate.now());
        movimiento.setTipo(tipo);
        movimiento.setCategoria(CategoriaMovimientoFinanciero.OTRO);
        movimiento.setValor(valor);
        movimiento.setDescripcion(descripcion);
        return movimentoFinanceiroDAO.guardar(movimiento);
    }

    public List<MovimientoFinanciero> listarPorPeriodo(LocalDate inicio, LocalDate fin) {
        return movimentoFinanceiroDAO.listarPorPeriodo(inicio, fin);
    }

    public List<MovimientoFinanciero> listarPorCategoria(CategoriaMovimientoFinanciero categoria) {
        return movimentoFinanceiroDAO.listarPorCategoria(categoria);
    }

    /** Saldo del periodo: total de ENTRADA menos total de SALIDA. */
    public BigDecimal calcularSaldoPeriodo(LocalDate inicio, LocalDate fin) {
        BigDecimal entradas = BigDecimal.ZERO;
        BigDecimal saidas = BigDecimal.ZERO;
        for (MovimientoFinanciero m : listarPorPeriodo(inicio, fin)) {
            if (m.getTipo() == TipoMovimientoFinanciero.ENTRADA) {
                entradas = entradas.add(m.getValor());
            } else {
                saidas = saidas.add(m.getValor());
            }
        }
        return entradas.subtract(saidas);
    }
}
