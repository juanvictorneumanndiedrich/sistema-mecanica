package com.mecanica.dao;

import com.mecanica.enums.StatusOrdemServico;
import com.mecanica.model.Cliente;
import com.mecanica.model.OrdemDeServico;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class OrdemDeServicoDAO extends AbstractGenericDAO<OrdemDeServico, Long> {

    public OrdemDeServicoDAO() {
        super(OrdemDeServico.class);
    }

    public List<OrdemDeServico> listarPorStatus(StatusOrdemServico status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM OrdemDeServico o WHERE o.status = :status ORDER BY o.dataAbertura DESC";
            Query<OrdemDeServico> query = session.createQuery(hql, OrdemDeServico.class);
            query.setParameter("status", status);
            return query.list();
        }
    }

    public List<OrdemDeServico> listarPorCliente(Cliente cliente) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM OrdemDeServico o WHERE o.cliente = :cliente ORDER BY o.dataAbertura DESC";
            Query<OrdemDeServico> query = session.createQuery(hql, OrdemDeServico.class);
            query.setParameter("cliente", cliente);
            return query.list();
        }
    }

    /** Usado ao reimprimir/consultar uma OS pelo numero mostrado na via impressa. */
    public OrdemDeServico buscarPorNumero(Long numero) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM OrdemDeServico o WHERE o.numero = :numero";
            Query<OrdemDeServico> query = session.createQuery(hql, OrdemDeServico.class);
            query.setParameter("numero", numero);
            List<OrdemDeServico> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }
}