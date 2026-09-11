package com.mecanica.dao;

import com.mecanica.model.ItemOrdemServico;
import com.mecanica.model.OrdemDeServico;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class ItemOrdemServicoDAO extends AbstractGenericDAO<ItemOrdemServico, Long> {

    public ItemOrdemServicoDAO() {
        super(ItemOrdemServico.class);
    }

    public List<ItemOrdemServico> listarPorOrdemDeServico(OrdemDeServico ordemDeServico) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM ItemOrdemServico i WHERE i.ordemDeServico = :os ORDER BY i.id";
            Query<ItemOrdemServico> query = session.createQuery(hql, ItemOrdemServico.class);
            query.setParameter("os", ordemDeServico);
            return query.list();
        }
    }
}