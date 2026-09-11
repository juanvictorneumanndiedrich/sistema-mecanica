package com.mecanica.dao;

import com.mecanica.enums.FormaPagamentoCompra;
import com.mecanica.model.Compra;
import com.mecanica.model.Fornecedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class CompraDAO extends AbstractGenericDAO<Compra, Long> {

    public CompraDAO() {
        super(Compra.class);
    }

    public List<Compra> listarPorFornecedor(Fornecedor fornecedor) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Compra c WHERE c.fornecedor = :fornecedor ORDER BY c.data DESC";
            Query<Compra> query = session.createQuery(hql, Compra.class);
            query.setParameter("fornecedor", fornecedor);
            return query.list();
        }
    }

    /**
     * Compras lancadas na conta do fornecedor que ainda nao entraram em
     * nenhum FechamentoFornecedor -- e o que alimenta a tela de novo
     * fechamento.
     */
    public List<Compra> listarPendentesDeFechamento(Fornecedor fornecedor) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Compra c WHERE c.fornecedor = :fornecedor "
                    + "AND c.formaPagamento = :forma AND c.fechamentoFornecedor IS NULL "
                    + "ORDER BY c.data";
            Query<Compra> query = session.createQuery(hql, Compra.class);
            query.setParameter("fornecedor", fornecedor);
            query.setParameter("forma", FormaPagamentoCompra.LANCADA_EM_CONTA_FORNECEDOR);
            return query.list();
        }
    }
}