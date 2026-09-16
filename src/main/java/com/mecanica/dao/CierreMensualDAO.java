package com.mecanica.dao;

import com.mecanica.model.CierreMensual;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class CierreMensualDAO extends AbstractGenericDAO<CierreMensual, Long> {

    public CierreMensualDAO() {
        super(CierreMensual.class);
    }

    /**
     * Historico de cierres, del mas reciente al mas antiguo, con los
     * detalles por socio ya cargados (join fetch) para poder mostrar el
     * reporte sin necesitar una segunda consulta con la sesion ya cerrada.
     */
    public List<CierreMensual> listarOrdenados() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT DISTINCT c FROM CierreMensual c "
                    + "LEFT JOIN FETCH c.detalles d LEFT JOIN FETCH d.socio "
                    + "ORDER BY c.fechaCierre DESC, c.id DESC";
            Query<CierreMensual> query = session.createQuery(hql, CierreMensual.class);
            return query.list();
        }
    }
}
