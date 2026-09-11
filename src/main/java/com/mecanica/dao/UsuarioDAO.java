package com.mecanica.dao;

import com.mecanica.model.Usuario;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class UsuarioDAO extends AbstractGenericDAO<Usuario, Long> {

    public UsuarioDAO() {
        super(Usuario.class);
    }

    /** Se usa en la pantalla de login. */
    public Usuario buscarPorLogin(String login) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Usuario u WHERE u.login = :login";
            Query<Usuario> query = session.createQuery(hql, Usuario.class);
            query.setParameter("login", login);
            List<Usuario> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }

    public List<Usuario> listarActivos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Usuario u WHERE u.activo = true ORDER BY u.nombre";
            return session.createQuery(hql, Usuario.class).list();
        }
    }
}
