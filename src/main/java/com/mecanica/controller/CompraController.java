package com.mecanica.controller;

import com.mecanica.dao.CompraDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.FormaPagoCompra;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.Compra;
import com.mecanica.model.Proveedor;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de Compra. Solo tiene los 2 escenarios ya definidos en la fase de
 * pantallas: PAGO_INMEDIATO (genera MovimientoFinanciero al instante) o
 * CARGADA_EN_CUENTA_PROVEEDOR (queda pendiente hasta entrar en un
 * CierreProveedor, que recien genera el MovimientoFinanciero cuando sea
 * marcado como pagado).
 */
public class CompraController {

    private final CompraDAO compraDAO = new CompraDAO();

    public Compra registrarCompra(Proveedor proveedor, LocalDate fecha, String descripcion,
                                   BigDecimal valor, FormaPagoCompra formaPago) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor de la compra debe ser mayor que cero.");
        }
        LocalDate fechaFinal = fecha != null ? fecha : LocalDate.now();

        if (formaPago == FormaPagoCompra.PAGO_INMEDIATO) {
            return registrarComMovimentoImediato(proveedor, fechaFinal, descripcion, valor, formaPago);
        }

        // CARGADA_EN_CUENTA_PROVEEDOR: solo graba la compra, sin MovimientoFinanciero todavia.
        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setFecha(fechaFinal);
        compra.setDescripcion(descripcion);
        compra.setValor(valor);
        compra.setFormaPago(formaPago);
        return compraDAO.guardar(compra);
    }

    private Compra registrarComMovimentoImediato(Proveedor proveedor, LocalDate fecha, String descripcion,
                                                  BigDecimal valor, FormaPagoCompra formaPago) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Compra compra = new Compra();
            compra.setProveedor(proveedor);
            compra.setFecha(fecha);
            compra.setDescripcion(descripcion);
            compra.setValor(valor);
            compra.setFormaPago(formaPago);
            session.persist(compra);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(fecha);
            movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.COMPRA_PROVEEDOR);
            movimiento.setValor(valor);
            movimiento.setDescripcion(descripcion);
            movimiento.setProveedor(proveedor);
            session.persist(movimiento);

            tx.commit();
            return compra;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public List<Compra> listarPorFornecedor(Proveedor proveedor) {
        return compraDAO.listarPorFornecedor(proveedor);
    }

    public List<Compra> listarPendientesDeCierre(Proveedor proveedor) {
        return compraDAO.listarPendientesDeCierre(proveedor);
    }

    public void eliminar(Compra compra) {
        compraDAO.eliminar(compra);
    }
}
