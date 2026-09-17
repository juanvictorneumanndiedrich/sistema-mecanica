package com.mecanica.dao;

import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class MaquinarioDAO extends AbstractGenericDAO<Maquinario, Long> {

    public MaquinarioDAO() {
        super(Maquinario.class);
    }

    /** Se usa en la pantalla de Clientes y Maquinarios, para listar los maquinarios de un cliente. */
    public List<Maquinario> listarPorCliente(Cliente cliente) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM Maquinario e WHERE e.cliente = :cliente ORDER BY e.identificacion";
            Query<Maquinario> query = session.createQuery(hql, Maquinario.class);
            query.setParameter("cliente", cliente);
            return query.list();
        }
    }
}
