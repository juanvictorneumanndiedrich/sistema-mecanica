package com.mecanica.dao;

import com.mecanica.model.Cliente;
import com.mecanica.model.Equipamento;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class EquipamentoDAO extends AbstractGenericDAO<Equipamento, Long> {

    public EquipamentoDAO() {
        super(Equipamento.class);
    }

    /** Usado na tela de Clientes e Equipamentos, pra listar os equipamentos de um cliente. */
    public List<Equipamento> listarPorCliente(Cliente cliente) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Equipamento e WHERE e.cliente = :cliente ORDER BY e.identificacao";
            Query<Equipamento> query = session.createQuery(hql, Equipamento.class);
            query.setParameter("cliente", cliente);
            return query.list();
        }
    }
}