package com.mecanica.controller;

import com.mecanica.dao.CierreProveedorDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.EstadoCierreProveedor;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.CierreProveedor;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.Proveedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.List;

/**
 * ATENCION -- clase huerfana (2026-09-15): el flujo de "cierre de cuenta"
 * en 2 etapas fue reemplazado por Compra funcionando como notinha con
 * boton Pagar directo (ver CompraController.pagar). Ninguna pantalla llama
 * mas a este Controller ni a CierreProveedorDialog -- se mantiene solo
 * para no romper la compilacion mientras el usuario no borra a mano este
 * archivo junto con CierreProveedor.java (model), CierreProveedorDAO.java
 * y CierreProveedorDialog.java (view). La tabla cierre_proveedor tampoco
 * sigue mapeada en hibernate.cfg.xml.
 */
public class CierreProveedorController {

    private final CierreProveedorDAO cierreProveedorDAO = new CierreProveedorDAO();

    public List<CierreProveedor> listarPorProveedor(Proveedor proveedor) {
        return cierreProveedorDAO.listarPorFornecedor(proveedor);
    }

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
