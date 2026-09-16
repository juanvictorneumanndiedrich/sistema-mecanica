package com.mecanica.controller;

import com.mecanica.dao.ClienteDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.Cliente;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de Cliente: recibe llamadas de la View, aplica las reglas de
 * negocio y delega al DAO. Las operaciones simples (CRUD) solo llaman al
 * ClienteDAO; las operaciones que tocan mas de una entidad a la vez
 * (como registrar un pago) abren su propia Session/Transaction aca,
 * para garantizar que todo pase junto o no pase nada.
 */
public class ClienteController {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    public Cliente guardar(Cliente cliente) {
        validar(cliente);
        return clienteDAO.guardar(cliente);
    }

    public Cliente buscarPorId(Long id) {
        return clienteDAO.buscarPorId(id);
    }

    public List<Cliente> listarTodos() {
        return clienteDAO.listarTodos();
    }

    public List<Cliente> buscarPorNombre(String nombre) {
        return clienteDAO.buscarPorNombre(nombre);
    }

    public void eliminar(Cliente cliente) {
        clienteDAO.eliminar(cliente);
    }

    /**
     * Registra un pago del cliente: descuenta el valor de su SALDO GENERAL
     * (no de una OS especifica) y genera el MovimientoFinanciero correspondiente
     * (ENTRADA / PAGO_CLIENTE). Las dos operaciones ocurren en la misma
     * transaccion, para nunca descontar el saldo sin registrar el movimiento (o
     * vice-versa).
     */
    public void registrarPagamento(Cliente cliente, BigDecimal valor, String descripcion) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del pago debe ser mayor que cero.");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Cliente clienteGerenciado = session.get(Cliente.class, cliente.getId());
            clienteGerenciado.setSaldo(clienteGerenciado.getSaldo().subtract(valor));
            session.merge(clienteGerenciado);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(LocalDate.now());
            movimiento.setTipo(TipoMovimientoFinanciero.ENTRADA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.PAGO_CLIENTE);
            movimiento.setValor(valor);
            movimiento.setDescripcion(descripcion);
            movimiento.setCliente(clienteGerenciado);
            session.persist(movimiento);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Retira en efectivo un credito a favor que el cliente ya tiene (saldo
     * NEGATIVO -- el cliente pago de mas en algun momento). Suma el valor al
     * saldo (lo acerca a cero) y genera el MovimientoFinanciero
     * correspondiente (SALIDA / DEVOLUCION_CLIENTE), ya que es dinero real
     * que sale de la caja de la mecanica. Misma transaccion atomica que
     * registrarPagamento. No deja retirar mas de lo que hay de credito
     * disponible.
     */
    public void retirarSaldo(Cliente cliente, BigDecimal valor, String descripcion) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor a retirar debe ser mayor que cero.");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Cliente clienteGerenciado = session.get(Cliente.class, cliente.getId());
            BigDecimal credito = clienteGerenciado.getSaldo().negate();
            if (credito.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El cliente no tiene credito a favor para retirar.");
            }
            if (valor.compareTo(credito) > 0) {
                throw new IllegalArgumentException(
                        "El valor a retirar no puede ser mayor que el credito disponible (Gs. " + credito + ").");
            }
            clienteGerenciado.setSaldo(clienteGerenciado.getSaldo().add(valor));
            session.merge(clienteGerenciado);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(LocalDate.now());
            movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.DEVOLUCION_CLIENTE);
            movimiento.setValor(valor);
            movimiento.setDescripcion(descripcion);
            movimiento.setCliente(clienteGerenciado);
            session.persist(movimiento);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    private void validar(Cliente cliente) {
        if (cliente.getNombre() == null || cliente.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }
    }
}
