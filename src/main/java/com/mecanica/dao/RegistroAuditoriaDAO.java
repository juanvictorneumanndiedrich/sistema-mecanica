package com.mecanica.dao;

import com.mecanica.model.RegistroAuditoria;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

public class RegistroAuditoriaDAO extends AbstractGenericDAO<RegistroAuditoria, Long> {

    public RegistroAuditoriaDAO() {
        super(RegistroAuditoria.class);
    }

    /**
     * Registros entre las dos fechas (inclusive), del mas nuevo al mas viejo.
     * Con login null o vacio trae los de todos los usuarios.
     */
    public List<RegistroAuditoria> listar(LocalDate desde, LocalDate hasta, String login) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean filtrarUsuario = login != null && !login.isBlank();
            String hql = "FROM RegistroAuditoria r WHERE r.fechaHora >= :desde AND r.fechaHora < :hasta"
                    + (filtrarUsuario ? " AND r.usuarioLogin = :login" : "")
                    + " ORDER BY r.fechaHora DESC";
            Query<RegistroAuditoria> query = session.createQuery(hql, RegistroAuditoria.class);
            query.setParameter("desde", desde.atStartOfDay());
            query.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
            if (filtrarUsuario) {
                query.setParameter("login", login);
            }
            return query.list();
        }
    }
}
