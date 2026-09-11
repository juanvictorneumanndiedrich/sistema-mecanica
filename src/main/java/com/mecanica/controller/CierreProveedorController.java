package com.mecanica.controller;

import com.mecanica.dao.CompraDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.EstadoCierreProveedor;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.Compra;
import com.mecanica.model.CierreProveedor;
import com.mecanica.model.Proveedor;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de CierreProveedor. El cierre ocurre en dos
 * etapas manuales: abrirCierre (junta las compras pendientes en una cuenta
 * cerrada, sin generar MovimientoFinanciero todavia) y marcarComoPagado (recien
 * ahi genera el MovimientoFinanciero de salida). No esta atado a un mes
 * calendario fijo.
 */
public class CierreProveedorController {

    private final CompraDAO compraDAO = new CompraDAO();

    /**
     * 1a etapa: suma las compras pendientes (CARGADA_EN_CUENTA_PROVEEDOR y
     * sin cierre todavia) del proveedor y crea el CierreProveedor
     * con estado CERRADO, vinculando esas compras a el.
     */
    public CierreProveedor abrirCierre(Proveedor proveedor, LocalDate fechaCierre) {
        List<Compra> pendientes = compraDAO.listarPendientesDeCierre(proveedor);
        if (pendientes.isEmpty()) {
            throw new IllegalStateException("No hay compras pendientes para cerrar con ese proveedor.");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Compra c : pendientes) {
            total = total.add(c.getValor());
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            CierreProveedor cierre = new CierreProveedor();
            cierre.setProveedor(proveedor);
            cierre.setFechaCierre(fechaCierre != null ? fechaCierre : LocalDate.now());
            cierre.setValorTotal(total);
            cierre.setEstado(EstadoCierreProveedor.CERRADO);
            session.persist(cierre);

            for (Compra c : pendientes) {
                c.setCierreProveedor(cierre);
                session.merge(c);
            }

            tx.commit();
            return cierre;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * 2a etapa: marca el cierre como pagado y recien ahi genera el
     * MovimientoFinanciero (SALIDA / COMPRA_PROVEEDOR) del valor total.
     */
    public CierreProveedor marcarComoPagado(CierreProveedor cierre, LocalDate fechaPago) {
        if (cierre.getEstado() == EstadoCierreProveedor.PAGADO) {
            throw new IllegalStateException("Ese cierre ya está pagado.");
        }
        LocalDate fechaFinal = fechaPago != null ? fechaPago : LocalDate.now();

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            cierre.setEstado(EstadoCierreProveedor.PAGADO);
            cierre.setFechaPago(fechaFinal);
            session.merge(cierre);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(fechaFinal);
            movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.COMPRA_PROVEEDOR);
            movimiento.setValor(cierre.getValorTotal());
            movimiento.setDescripcion("Pago del cierre de " + cierre.getProveedor().getNombre());
            movimiento.setProveedor(cierre.getProveedor());
            session.persist(movimiento);

            tx.commit();
            return cierre;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
