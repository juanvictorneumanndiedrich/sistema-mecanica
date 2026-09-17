package com.mecanica.controller;

import com.mecanica.dao.ClienteDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.Permiso;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.Cliente;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.util.HibernateUtil;
import com.mecanica.util.Sesion;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final AuditoriaController auditoria = new AuditoriaController();

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
        Sesion.exigir(Permiso.ELIMINAR_REGISTROS);
        clienteDAO.eliminar(cliente);
        auditoria.registrar("CLIENTE ELIMINADO", cliente.getNombre());
    }

    /**
     * Registra un pago del cliente: descuenta el valor de su SALDO GENERAL
     * (no de una OS especifica) y genera el MovimientoFinanciero correspondiente
     * (ENTRADA / PAGO_CLIENTE). Las dos operaciones ocurren en la misma
     * transaccion, para nunca descontar el saldo sin registrar el movimiento (o
     * vice-versa). Sin descuento (uso comun).
     */
    public void registrarPagamento(Cliente cliente, BigDecimal valor, String descripcion) {
        registrarPagamento(cliente, valor, BigDecimal.ZERO, descripcion);
    }

    /**
     * Igual que registrarPagamento(Cliente, BigDecimal, String), pero permite
     * perdonar un pedazo de la DEUDA del cliente (descuento).
     *
     * Ojo con la semantica, que es la parte facil de confundir: valorPagado
     * es la plata que el cliente entrega AHORA -- entra ENTERA en
     * Financiero, porque es plata de verdad que llego a la caja. El
     * descuento es aparte: se perdona de la cuenta sin que nadie pague nada
     * por el. Por eso el saldo baja por (valorPagado + descuento): con un
     * saldo de 150.000 y 10% de descuento (15.000), el cliente paga 135.000
     * y la cuenta queda en cero.
     *
     * El porcentaje que ese descuento representa sobre el saldo que habia
     * antes del pago se calcula y se guarda junto, solo para mostrarlo en la
     * tabla de Movimientos.
     *
     * @param descuentoValor monto perdonado en Gs. (BigDecimal.ZERO o null = sin descuento)
     */
    public void registrarPagamento(Cliente cliente, BigDecimal valorPagado, BigDecimal descuentoValor,
            String descripcion) {
        if (valorPagado == null || valorPagado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del pago debe ser mayor que cero.");
        }
        BigDecimal descuento = descuentoValor == null ? BigDecimal.ZERO : descuentoValor;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo.");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Cliente clienteGerenciado = session.get(Cliente.class, cliente.getId());
            BigDecimal saldoAntes = clienteGerenciado.getSaldo();
            if (descuento.compareTo(saldoAntes) > 0) {
                throw new IllegalArgumentException(
                        "El descuento no puede ser mayor que el saldo del cliente (Gs. " + saldoAntes + ").");
            }
            clienteGerenciado.setSaldo(saldoAntes.subtract(valorPagado).subtract(descuento));
            session.merge(clienteGerenciado);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(LocalDate.now());
            movimiento.setTipo(TipoMovimientoFinanciero.ENTRADA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.PAGO_CLIENTE);
            movimiento.setValor(valorPagado);
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                movimiento.setDescuentoValor(descuento);
                if (saldoAntes.compareTo(BigDecimal.ZERO) > 0) {
                    movimiento.setDescuentoPorcentaje(descuento.multiply(BigDecimal.valueOf(100))
                            .divide(saldoAntes, 2, RoundingMode.HALF_UP));
                }
            }
            movimiento.setDescripcion(descripcion);
            movimiento.setCliente(clienteGerenciado);
            session.persist(movimiento);

            tx.commit();
            auditoria.registrar("PAGO DE CLIENTE", clienteGerenciado.getNombre() + " - pago "
                    + AuditoriaController.gs(valorPagado)
                    + (descuento.compareTo(BigDecimal.ZERO) > 0 ? " + descuento " + AuditoriaController.gs(descuento) : ""));
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
        Sesion.exigir(Permiso.RETIRAR_SALDO);
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
            auditoria.registrar("SALDO RETIRADO (CLIENTE)",
                    clienteGerenciado.getNombre() + " - " + AuditoriaController.gs(valor));
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
