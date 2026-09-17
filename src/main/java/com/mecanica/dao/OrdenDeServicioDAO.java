package com.mecanica.dao;

import com.mecanica.enums.EstadoOrdenServicio;
import com.mecanica.model.Cliente;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class OrdenDeServicioDAO extends AbstractGenericDAO<OrdenDeServicio, Long> {

    public OrdenDeServicioDAO() {
        super(OrdenDeServicio.class);
    }

    /**
     * Trae TODAS las OS con su cliente y maquinario ya cargados en la misma
     * consulta (LEFT JOIN FETCH), en vez de dejar que Hibernate los traiga
     * uno por uno al mostrarlos en la tabla (con miles de OS, eso son miles
     * de consultas extra -- N+1 -- solo para pintar la pantalla).
     */
    @Override
    public List<OrdenDeServicio> listarTodos() {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "SELECT DISTINCT o FROM OrdenDeServicio o "
                    + "LEFT JOIN FETCH o.cliente LEFT JOIN FETCH o.maquinario "
                    + "ORDER BY o.fechaApertura DESC";
            return session.createQuery(hql, OrdenDeServicio.class).list();
        }
    }

    public List<OrdenDeServicio> listarPorEstado(EstadoOrdenServicio estado) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "SELECT DISTINCT o FROM OrdenDeServicio o "
                    + "LEFT JOIN FETCH o.cliente LEFT JOIN FETCH o.maquinario "
                    + "WHERE o.estado = :estado ORDER BY o.fechaApertura DESC";
            Query<OrdenDeServicio> query = session.createQuery(hql, OrdenDeServicio.class);
            query.setParameter("estado", estado);
            return query.list();
        }
    }

    public List<OrdenDeServicio> listarPorCliente(Cliente cliente) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "SELECT DISTINCT o FROM OrdenDeServicio o "
                    + "LEFT JOIN FETCH o.cliente LEFT JOIN FETCH o.maquinario "
                    + "WHERE o.cliente = :cliente ORDER BY o.fechaApertura DESC";
            Query<OrdenDeServicio> query = session.createQuery(hql, OrdenDeServicio.class);
            query.setParameter("cliente", cliente);
            return query.list();
        }
    }

    /** Se usa al reimprimir/consultar una OS por el numero mostrado en la via impresa. */
    public OrdenDeServicio buscarPorNumero(Long numero) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM OrdenDeServicio o WHERE o.numero = :numero";
            Query<OrdenDeServicio> query = session.createQuery(hql, OrdenDeServicio.class);
            query.setParameter("numero", numero);
            List<OrdenDeServicio> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }

    /**
     * Lo usa el Controller para generar el proximo numero secuencial de OS
     * (numero actual + 1). Devuelve null si todavia no existe ninguna OS.
     */
    public Long buscarMayorNumero() {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "SELECT MAX(o.numero) FROM OrdenDeServicio o";
            Query<Long> query = session.createQuery(hql, Long.class);
            List<Long> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }
}
