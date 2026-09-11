package com.mecanica.dao;

import com.mecanica.enums.EstadoCierreProveedor;
import com.mecanica.model.CierreProveedor;
import com.mecanica.model.Proveedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class CierreProveedorDAO extends AbstractGenericDAO<CierreProveedor, Long> {

    public CierreProveedorDAO() {
        super(CierreProveedor.class);
    }

    public List<CierreProveedor> listarPorFornecedor(Proveedor proveedor) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM CierreProveedor f WHERE f.proveedor = :proveedor ORDER BY f.fechaCierre DESC";
            Query<CierreProveedor> query = session.createQuery(hql, CierreProveedor.class);
            query.setParameter("proveedor", proveedor);
            return query.list();
        }
    }

    /** Usado pra achar os fechamentos ja FECHADOs esperando pagamento (2a etapa manual). */
    public List<CierreProveedor> listarPorEstado(EstadoCierreProveedor estado) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM CierreProveedor f WHERE f.estado = :estado ORDER BY f.fechaCierre DESC";
            Query<CierreProveedor> query = session.createQuery(hql, CierreProveedor.class);
            query.setParameter("estado", estado);
            return query.list();
        }
    }
}
