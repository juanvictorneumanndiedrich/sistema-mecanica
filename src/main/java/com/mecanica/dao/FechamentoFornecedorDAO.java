package com.mecanica.dao;

import com.mecanica.enums.StatusFechamentoFornecedor;
import com.mecanica.model.FechamentoFornecedor;
import com.mecanica.model.Fornecedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class FechamentoFornecedorDAO extends AbstractGenericDAO<FechamentoFornecedor, Long> {

    public FechamentoFornecedorDAO() {
        super(FechamentoFornecedor.class);
    }

    public List<FechamentoFornecedor> listarPorFornecedor(Fornecedor fornecedor) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM FechamentoFornecedor f WHERE f.fornecedor = :fornecedor ORDER BY f.dataFechamento DESC";
            Query<FechamentoFornecedor> query = session.createQuery(hql, FechamentoFornecedor.class);
            query.setParameter("fornecedor", fornecedor);
            return query.list();
        }
    }

    /** Usado pra achar os fechamentos ja FECHADOs esperando pagamento (2a etapa manual). */
    public List<FechamentoFornecedor> listarPorStatus(StatusFechamentoFornecedor status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM FechamentoFornecedor f WHERE f.status = :status ORDER BY f.dataFechamento DESC";
            Query<FechamentoFornecedor> query = session.createQuery(hql, FechamentoFornecedor.class);
            query.setParameter("status", status);
            return query.list();
        }
    }
}