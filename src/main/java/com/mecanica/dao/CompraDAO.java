package com.mecanica.dao;

import com.mecanica.enums.FormaPagoCompra;
import com.mecanica.model.Compra;
import com.mecanica.model.Proveedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class CompraDAO extends AbstractGenericDAO<Compra, Long> {

    public CompraDAO() {
        super(Compra.class);
    }

    public List<Compra> listarPorFornecedor(Proveedor proveedor) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Compra c WHERE c.proveedor = :proveedor ORDER BY c.fecha DESC";
            Query<Compra> query = session.createQuery(hql, Compra.class);
            query.setParameter("proveedor", proveedor);
            return query.list();
        }
    }

    /**
     * Compras cargadas en la cuenta del proveedor que todavia no entraron en
     * ningun CierreProveedor -- es lo que alimenta la pantalla de nuevo
     * cierre.
     */
    public List<Compra> listarPendientesDeCierre(Proveedor proveedor) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Compra c WHERE c.proveedor = :proveedor "
                    + "AND c.formaPago = :forma AND c.cierreProveedor IS NULL "
                    + "ORDER BY c.fecha";
            Query<Compra> query = session.createQuery(hql, Compra.class);
            query.setParameter("proveedor", proveedor);
            query.setParameter("forma", FormaPagoCompra.CARGADA_EN_CUENTA_PROVEEDOR);
            return query.list();
        }
    }
}
