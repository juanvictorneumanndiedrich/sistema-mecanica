package com.mecanica.dao;

import com.mecanica.model.ItemOrdenServicio;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class ItemOrdenServicioDAO extends AbstractGenericDAO<ItemOrdenServicio, Long> {

    public ItemOrdenServicioDAO() {
        super(ItemOrdenServicio.class);
    }

    public List<ItemOrdenServicio> listarPorOrdemDeServico(OrdenDeServicio ordenDeServicio) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM ItemOrdenServicio i WHERE i.ordenDeServicio = :os ORDER BY i.id";
            Query<ItemOrdenServicio> query = session.createQuery(hql, ItemOrdenServicio.class);
            query.setParameter("os", ordenDeServicio);
            return query.list();
        }
    }
}
