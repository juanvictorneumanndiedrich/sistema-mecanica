package com.mecanica.controller;

import com.mecanica.dao.ProveedorDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.Proveedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de Proveedor. Las operaciones simples (CRUD) solo llaman al
 * ProveedorDAO; el pago al proveedor toca mas de una entidad a la vez
 * (saldo + MovimientoFinanciero) y por eso abre su propia
 * Session/Transaction aca -- mismo patron de ClienteController.
 */
public class ProveedorController {

    private final ProveedorDAO fornecedorDAO = new ProveedorDAO();

    public Proveedor guardar(Proveedor proveedor) {
        validar(proveedor);
        return fornecedorDAO.guardar(proveedor);
    }

    public Proveedor buscarPorId(Long id) {
        return fornecedorDAO.buscarPorId(id);
    }

    public List<Proveedor> listarTodos() {
        return fornecedorDAO.listarTodos();
    }

    public List<Proveedor> buscarPorNombre(String nombre) {
        return fornecedorDAO.buscarPorNombre(nombre);
    }

    public Proveedor buscarPorDocumento(String documento) {
        return fornecedorDAO.buscarPorDocumento(documento);
    }

    public void eliminar(Proveedor proveedor) {
        fornecedorDAO.eliminar(proveedor);
    }

    /**
     * Registra un pago hecho al proveedor: descuenta el valor de su SALDO
     * GENERAL (no de una Compra especifica -- no se paga notinha por
     * notinha) y genera el MovimientoFinanciero correspondiente (SALIDA /
     * COMPRA_PROVEEDOR). Las dos operaciones ocurren en la misma
     * transaccion, para nunca descontar el saldo sin registrar el gasto (o
     * vice-versa). Mismo esquema de ClienteController.registrarPagamento.
     */
    public void registrarPagamento(Proveedor proveedor, BigDecimal valor, String descripcion) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del pago debe ser mayor que cero.");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Proveedor proveedorGerenciado = session.get(Proveedor.class, proveedor.getId());
            proveedorGerenciado.setSaldo(proveedorGerenciado.getSaldo().subtract(valor));
            session.merge(proveedorGerenciado);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(LocalDate.now());
            movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.COMPRA_PROVEEDOR);
            movimiento.setValor(valor);
            movimiento.setDescripcion(descripcion);
            movimiento.setProveedor(proveedorGerenciado);
            session.persist(movimiento);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    private void validar(Proveedor proveedor) {
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio.");
        }
    }
}
