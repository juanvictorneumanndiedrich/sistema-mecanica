package com.mecanica.controller;

import com.mecanica.dao.ItemOrdenServicioDAO;
import com.mecanica.enums.TipoItemOrdenServicio;
import com.mecanica.model.ItemOrdenServicio;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.util.Errores;
import com.mecanica.util.HibernateUtil;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller de ItemOrdenServicio: agregar/quitar item de una OS y
 * mantener el valorTotal de la OS siempre en sincronia con la suma de los items.
 */
public class ItemOrdenServicioController {

    private final ItemOrdenServicioDAO itemOrdemServicoDAO = new ItemOrdenServicioDAO();

    public ItemOrdenServicio agregar(OrdenDeServicio os, TipoItemOrdenServicio tipo, String descripcion,
                                       BigDecimal cantidad, BigDecimal valorUnitario) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor unitario inválido.");
        }

        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();

            // se traba la OS mientras se recalcula el total, para que dos altas de item
            // al mismo tiempo no dejen el total distinto de la suma de los items
            OrdenDeServicio osGestionada = session.get(OrdenDeServicio.class, os.getId(), LockMode.PESSIMISTIC_WRITE);
            if (osGestionada == null) {
                throw new IllegalStateException("Esa orden de servicio ya no existe.");
            }

            ItemOrdenServicio item = new ItemOrdenServicio();
            item.setOrdenDeServicio(osGestionada);
            item.setTipo(tipo);
            item.setDescripcion(descripcion);
            item.setCantidad(cantidad);
            item.setValorUnitario(valorUnitario);
            item.setValorTotal(cantidad.multiply(valorUnitario));
            session.persist(item);

            actualizarTotal(session, osGestionada);
            tx.commit();

            // el objeto que quedo en la pantalla tambien muestra el total nuevo
            os.setValorTotal(osGestionada.getValorTotal());
            return item;
        } catch (RuntimeException e) {
            Errores.revertir(tx);
            throw Errores.traducir(e);
        } finally {
            session.close();
        }
    }

    public void quitar(ItemOrdenServicio item) {
        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();

            // Se busca el item por id: con merge, Hibernate trae la OS con su lista
            // de items en cascada y termina cancelando el borrado sin avisar.
            ItemOrdenServicio gestionado = session.get(ItemOrdenServicio.class, item.getId());
            if (gestionado == null) {
                tx.commit();
                return;
            }
            OrdenDeServicio osGestionada = session.get(OrdenDeServicio.class,
                    gestionado.getOrdenDeServicio().getId(), LockMode.PESSIMISTIC_WRITE);
            session.remove(gestionado);

            actualizarTotal(session, osGestionada);
            tx.commit();

            if (item.getOrdenDeServicio() != null) {
                item.getOrdenDeServicio().setValorTotal(osGestionada.getValorTotal());
            }
        } catch (RuntimeException e) {
            Errores.revertir(tx);
            throw Errores.traducir(e);
        } finally {
            session.close();
        }
    }

    public List<ItemOrdenServicio> listarPorOrdemDeServico(OrdenDeServicio os) {
        return itemOrdemServicoDAO.listarPorOrdemDeServico(os);
    }

    /**
     * Suma los items de la OS dentro de la MISMA transaccion del agregar/quitar,
     * para que el total nunca quede desfasado de los items.
     */
    private void actualizarTotal(Session session, OrdenDeServicio os) {
        session.flush();
        BigDecimal total = session.createQuery(
                        "SELECT COALESCE(SUM(i.valorTotal), 0) FROM ItemOrdenServicio i WHERE i.ordenDeServicio = :os",
                        BigDecimal.class)
                .setParameter("os", os)
                .getSingleResult();
        os.setValorTotal(total);
    }
}
