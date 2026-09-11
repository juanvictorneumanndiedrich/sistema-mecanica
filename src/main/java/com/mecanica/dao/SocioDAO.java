package com.mecanica.dao;

import com.mecanica.model.Socio;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class SocioDAO extends AbstractGenericDAO<Socio, Long> {

    public SocioDAO() {
        super(Socio.class);
    }

    public List<Socio> listarActivos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Socio s WHERE s.activo = true ORDER BY s.nombre";
            return session.createQuery(hql, Socio.class).list();
        }
    }
}
