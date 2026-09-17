package com.mecanica.dao;

import com.mecanica.model.Proveedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class ProveedorDAO extends AbstractGenericDAO<Proveedor, Long> {

    public ProveedorDAO() {
        super(Proveedor.class);
    }

    public List<Proveedor> buscarPorNombre(String nombre) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM Proveedor f WHERE LOWER(f.nombre) LIKE LOWER(:nombre) ORDER BY f.nombre";
            Query<Proveedor> query = session.createQuery(hql, Proveedor.class);
            query.setParameter("nombre", "%" + nombre + "%");
            return query.list();
        }
    }

    public Proveedor buscarPorDocumento(String documento) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM Proveedor f WHERE f.documento = :documento";
            Query<Proveedor> query = session.createQuery(hql, Proveedor.class);
            query.setParameter("documento", documento);
            List<Proveedor> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }
}
