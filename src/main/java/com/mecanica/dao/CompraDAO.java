package com.mecanica.dao;

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
            String hql = "FROM Compra c WHERE c.proveedor = :proveedor ORDER BY c.numero DESC";
            Query<Compra> query = session.createQuery(hql, Compra.class);
            query.setParameter("proveedor", proveedor);
            return query.list();
        }
    }

    /**
     * Lo usa el Controller para generar el proximo numero secuencial de la
     * notinha (numero actual + 1). Devuelve null si todavia no existe
     * ninguna Compra -- mismo patron de OrdenDeServicioDAO.buscarMayorNumero.
     */
    public Long buscarMayorNumero() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT MAX(c.numero) FROM Compra c";
            Query<Long> query = session.createQuery(hql, Long.class);
            List<Long> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }
}
