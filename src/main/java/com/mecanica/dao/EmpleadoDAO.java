package com.mecanica.dao;

import com.mecanica.model.Empleado;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class EmpleadoDAO extends AbstractGenericDAO<Empleado, Long> {

    public EmpleadoDAO() {
        super(Empleado.class);
    }

    public List<Empleado> listarActivos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Empleado f WHERE f.activo = true ORDER BY f.nombre";
            return session.createQuery(hql, Empleado.class).list();
        }
    }

    public List<Empleado> buscarPorNombre(String nombre) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Empleado f WHERE LOWER(f.nombre) LIKE LOWER(:nombre) ORDER BY f.nombre";
            Query<Empleado> query = session.createQuery(hql, Empleado.class);
            query.setParameter("nombre", "%" + nombre + "%");
            return query.list();
        }
    }
}
