package com.mecanica.dao;

import com.mecanica.model.Cliente;
import com.mecanica.model.Equipo;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class EquipoDAO extends AbstractGenericDAO<Equipo, Long> {

    public EquipoDAO() {
        super(Equipo.class);
    }

    /** Se usa en la pantalla de Clientes y Equipos, para listar los equipos de un cliente. */
    public List<Equipo> listarPorCliente(Cliente cliente) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Equipo e WHERE e.cliente = :cliente ORDER BY e.identificacion";
            Query<Equipo> query = session.createQuery(hql, Equipo.class);
            query.setParameter("cliente", cliente);
            return query.list();
        }
    }
}
