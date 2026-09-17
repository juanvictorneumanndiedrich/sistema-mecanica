package com.mecanica.dao;

import com.mecanica.model.Compra;
import com.mecanica.model.ItemCompra;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class ItemCompraDAO extends AbstractGenericDAO<ItemCompra, Long> {

    public ItemCompraDAO() {
        super(ItemCompra.class);
    }

    public List<ItemCompra> listarPorCompra(Compra compra) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM ItemCompra i WHERE i.compra = :compra ORDER BY i.id";
            Query<ItemCompra> query = session.createQuery(hql, ItemCompra.class);
            query.setParameter("compra", compra);
            return query.list();
        }
    }
}
