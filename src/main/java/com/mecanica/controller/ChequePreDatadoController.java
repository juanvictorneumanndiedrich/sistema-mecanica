package com.mecanica.controller;

import com.mecanica.dao.ChequePreDatadoDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.EstadoCheque;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.Cliente;
import com.mecanica.model.ChequePreDatado;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.Proveedor;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de ChequePreDatado. Un cheque pre-datado (recibido de un
 * Cliente, o entregado por nosotros a un Proveedor) ya descuenta el SALDO
 * GENERAL en el momento en que se registra -- mismo efecto inmediato que un
 * pago normal (ver ClienteController.registrarPagamento /
 * ProveedorController.registrarPagamento). La diferencia es que el
 * MovimientoFinanciero (la entrada o el gasto real en el flujo de caja de
 * Financiero) NO se genera todavia: queda pendiente hasta que el cheque
 * venza de verdad y alguien lo confirme (ver confirmar()), porque hasta esa
 * fecha la plata no esta disponible de verdad.
 */
public class ChequePreDatadoController {

    private final ChequePreDatadoDAO chequeDAO = new ChequePreDatadoDAO();

    /** Cheque pre-datado recibido de un Cliente: ya descuenta su saldo general. */
    public void registrarDeCliente(Cliente cliente, String numeroCheque, String banco,
            LocalDate fechaVencimiento, BigDecimal valor, String descripcion) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        validar(valor, fechaVencimiento);

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Cliente clienteGerenciado = session.get(Cliente.class, cliente.getId());
            clienteGerenciado.setSaldo(clienteGerenciado.getSaldo().subtract(valor));
            session.merge(clienteGerenciado);

            ChequePreDatado cheque = new ChequePreDatado();
            cheque.setCliente(clienteGerenciado);
            cheque.setNumeroCheque(numeroCheque);
            cheque.setBanco(banco);
            cheque.setFechaRegistro(LocalDate.now());
            cheque.setFechaVencimiento(fechaVencimiento);
            cheque.setValor(valor);
            cheque.setDescripcion(descripcion);
            cheque.setEstado(EstadoCheque.PENDIENTE);
            session.persist(cheque);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /** Cheque pre-datado que vamos a entregar a un Proveedor: ya descuenta su saldo general. */
    public void registrarDeProveedor(Proveedor proveedor, String numeroCheque, String banco,
            LocalDate fechaVencimiento, BigDecimal valor, String descripcion) {
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio.");
        }
        validar(valor, fechaVencimiento);

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Proveedor proveedorGerenciado = session.get(Proveedor.class, proveedor.getId());
            proveedorGerenciado.setSaldo(proveedorGerenciado.getSaldo().subtract(valor));
            session.merge(proveedorGerenciado);

            ChequePreDatado cheque = new ChequePreDatado();
            cheque.setProveedor(proveedorGerenciado);
            cheque.setNumeroCheque(numeroCheque);
            cheque.setBanco(banco);
            cheque.setFechaRegistro(LocalDate.now());
            cheque.setFechaVencimiento(fechaVencimiento);
            cheque.setValor(valor);
            cheque.setDescripcion(descripcion);
            cheque.setEstado(EstadoCheque.PENDIENTE);
            session.persist(cheque);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Confirma que el cheque vencio y se compenso de verdad: recien aca se
     * genera el MovimientoFinanciero (ENTRADA/PAGO_CLIENTE si era de un
     * Cliente, SALIDA/COMPRA_PROVEEDOR si era para un Proveedor). El saldo
     * del Cliente/Proveedor NO se toca de nuevo aca -- ya fue descontado
     * cuando el cheque se registro.
     */
    public void confirmar(ChequePreDatado cheque) {
        if (cheque == null) {
            throw new IllegalArgumentException("El cheque es obligatorio.");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            ChequePreDatado chequeGerenciado = session.get(ChequePreDatado.class, cheque.getId());
            if (chequeGerenciado == null) {
                tx.commit();
                return;
            }
            if (chequeGerenciado.getEstado() == EstadoCheque.CONFIRMADO) {
                throw new IllegalStateException("Ese cheque ya fue confirmado.");
            }
            chequeGerenciado.setEstado(EstadoCheque.CONFIRMADO);
            chequeGerenciado.setFechaConfirmacion(LocalDate.now());
            session.merge(chequeGerenciado);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(LocalDate.now());
            String origen;
            if (chequeGerenciado.getCliente() != null) {
                movimiento.setTipo(TipoMovimientoFinanciero.ENTRADA);
                movimiento.setCategoria(CategoriaMovimientoFinanciero.PAGO_CLIENTE);
                movimiento.setCliente(chequeGerenciado.getCliente());
                origen = chequeGerenciado.getCliente().getNombre();
            } else {
                movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
                movimiento.setCategoria(CategoriaMovimientoFinanciero.COMPRA_PROVEEDOR);
                movimiento.setProveedor(chequeGerenciado.getProveedor());
                origen = chequeGerenciado.getProveedor().getNombre();
            }
            movimiento.setValor(chequeGerenciado.getValor());
            String numero = chequeGerenciado.getNumeroCheque();
            String descripcionBase = "Cheque pre-datado"
                    + (numero == null || numero.isBlank() ? "" : " Nº " + numero)
                    + " (" + origen + ")";
            String descripcionExtra = chequeGerenciado.getDescripcion();
            movimiento.setDescripcion(descripcionExtra == null || descripcionExtra.isBlank()
                    ? descripcionBase
                    : descripcionBase + " - " + descripcionExtra);
            session.persist(movimiento);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /** Lista de cheques pendientes (de clientes y de proveedores), ordenados por fecha de vencimiento. */
    public List<ChequePreDatado> listarPendientes() {
        return chequeDAO.listarPorEstado(EstadoCheque.PENDIENTE);
    }

    private void validar(BigDecimal valor, LocalDate fechaVencimiento) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del cheque debe ser mayor que cero.");
        }
        if (fechaVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento del cheque es obligatoria.");
        }
    }
}
