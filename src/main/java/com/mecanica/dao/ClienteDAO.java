package com.mecanica.dao;

import com.mecanica.model.Cliente;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

/**
 * DAO especifico de Cliente. Ejemplo de como extender AbstractGenericDAO
 * y agregar busquedas propias de la entidad -- los otros 12 DAOs
 * (MaquinarioDAO, OrdenDeServicioDAO, UsuarioDAO, ProveedorDAO,
 * CompraDAO, CierreProveedorDAO, EmpleadoDAO,
 * RetiroEmpleadoDAO, SocioDAO, RetiroSocioDAO,
 * MovimientoFinancieroDAO, ItemOrdenServicioDAO) siguen exactamente el mismo
 * patron.
 */
public class ClienteDAO extends AbstractGenericDAO<Cliente, Long> {

    public ClienteDAO() {
        super(Cliente.class);
    }

    /** Busqueda por nombre (que contenga el texto), usada en la pantalla de Clientes y Maquinarios. */
    public List<Cliente> buscarPorNombre(String nombre) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM Cliente c WHERE LOWER(c.nombre) LIKE LOWER(:nombre) ORDER BY c.nombre";
            Query<Cliente> query = session.createQuery(hql, Cliente.class);
            query.setParameter("nombre", "%" + nombre + "%");
            return query.list();
        }
    }

    /** Busca por documento (CI/RUC) exato. */
    public Cliente buscarPorDocumento(String documento) {
        try (Session session = HibernateUtil.abrirSesion()) {
            String hql = "FROM Cliente c WHERE c.documento = :documento";
            Query<Cliente> query = session.createQuery(hql, Cliente.class);
            query.setParameter("documento", documento);
            List<Cliente> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }
}
