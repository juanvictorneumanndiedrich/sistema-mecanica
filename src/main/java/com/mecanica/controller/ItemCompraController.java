package com.mecanica.controller;

import com.mecanica.dao.ItemCompraDAO;
import com.mecanica.model.Compra;
import com.mecanica.model.ItemCompra;
import com.mecanica.model.Proveedor;
import com.mecanica.util.Errores;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller de ItemCompra: agregar/quitar item de una Compra, manteniendo
 * en sincronia el valorTotal de la nota Y el saldo general del proveedor --
 * las dos cosas en la misma transaccion, porque el valor de la nota es
 * justamente lo que la mecanica le debe al proveedor.
 *
 * Una nota PENDIENTE se puede editar siempre (si se agrega un item la
 * cuenta del proveedor sube; si se quita, baja). Una nota ya PAGADA queda
 * bloqueada -- ver CompraController.verificarEditable.
 */
public class ItemCompraController {

    private final ItemCompraDAO itemCompraDAO = new ItemCompraDAO();
    private final CompraController compraController = new CompraController();

    public ItemCompra agregar(Compra compra, String descripcion, BigDecimal cantidad, BigDecimal valorUnitario) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor unitario inválido.");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del item es obligatoria.");
        }
        compraController.verificarEditable(compra);
        BigDecimal valorItem = cantidad.multiply(valorUnitario);

        Transaction tx = null;
        Session session = HibernateUtil.abrirSesion();
        try {
            tx = session.beginTransaction();

            Compra compraGerenciada = session.get(Compra.class, compra.getId());

            ItemCompra item = new ItemCompra();
            item.setCompra(compraGerenciada);
            item.setDescripcion(descripcion);
            item.setCantidad(cantidad);
            item.setValorUnitario(valorUnitario);
            item.setValorTotal(valorItem);
            session.persist(item);

            ajustarCuenta(session, compraGerenciada, valorItem);

            tx.commit();
            return item;
        } catch (RuntimeException e) {
            Errores.revertir(tx);
            throw Errores.traducir(e);
        } finally {
            session.close();
        }
    }

    public void quitar(ItemCompra item) {
        compraController.verificarEditable(item.getCompra());

        Transaction tx = null;
        Session session = HibernateUtil.abrirSesion();
        try {
            tx = session.beginTransaction();

            ItemCompra itemGerenciado = session.get(ItemCompra.class, item.getId());
            if (itemGerenciado == null) {
                tx.commit();
                return;
            }
            Compra compraGerenciada = itemGerenciado.getCompra();
            BigDecimal valorItem = itemGerenciado.getValorTotal() == null
                    ? BigDecimal.ZERO
                    : itemGerenciado.getValorTotal();

            session.remove(itemGerenciado);
            ajustarCuenta(session, compraGerenciada, valorItem.negate());

            tx.commit();
        } catch (RuntimeException e) {
            Errores.revertir(tx);
            throw Errores.traducir(e);
        } finally {
            session.close();
        }
    }

    public List<ItemCompra> listarPorCompra(Compra compra) {
        return itemCompraDAO.listarPorCompra(compra);
    }

    /**
     * Suma (o resta, si la diferencia es negativa) el valor al total de la
     * nota y al saldo del proveedor, dentro de la transaccion que ya esta
     * abierta.
     */
    private void ajustarCuenta(Session session, Compra compra, BigDecimal diferencia) {
        BigDecimal totalActual = compra.getValorTotal() == null ? BigDecimal.ZERO : compra.getValorTotal();
        compra.setValorTotal(totalActual.add(diferencia));
        session.merge(compra);

        Proveedor proveedorGerenciado = session.get(Proveedor.class, compra.getProveedor().getId());
        proveedorGerenciado.setSaldo(proveedorGerenciado.getSaldo().add(diferencia));
        session.merge(proveedorGerenciado);
    }
}
