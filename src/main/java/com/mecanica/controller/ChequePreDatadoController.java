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
import java.math.RoundingMode;
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

    /**
     * Cheque pre-datado recibido de un Cliente: ya descuenta su saldo
     * general. Sin descuento (uso comun).
     */
    public void registrarDeCliente(Cliente cliente, String numeroCheque, String banco,
            LocalDate fechaVencimiento, BigDecimal valor, String descripcion) {
        registrarDeCliente(cliente, numeroCheque, banco, fechaVencimiento, valor, BigDecimal.ZERO, descripcion);
    }

    /**
     * Igual que registrarDeCliente(...) de arriba, pero permite perdonar un
     * pedazo de la deuda (descuento). El valor del cheque es el valor REAL
     * del papel que el cliente entrega -- no se toca, y es el que va a
     * entrar entero en Financiero cuando el cheque se confirme (ver
     * confirmar()). El descuento se perdona aparte, asi que el saldo del
     * cliente baja por (valor del cheque + descuento).
     */
    public void registrarDeCliente(Cliente cliente, String numeroCheque, String banco,
            LocalDate fechaVencimiento, BigDecimal valorCheque, BigDecimal descuentoValor, String descripcion) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        validar(valorCheque, fechaVencimiento);
        BigDecimal descuento = validarDescuento(descuentoValor);

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Cliente clienteGerenciado = session.get(Cliente.class, cliente.getId());
            BigDecimal saldoAntes = clienteGerenciado.getSaldo();
            if (descuento.compareTo(saldoAntes) > 0) {
                throw new IllegalArgumentException(
                        "El descuento no puede ser mayor que el saldo del cliente (Gs. " + saldoAntes + ").");
            }
            clienteGerenciado.setSaldo(saldoAntes.subtract(valorCheque).subtract(descuento));
            session.merge(clienteGerenciado);

            ChequePreDatado cheque = new ChequePreDatado();
            cheque.setCliente(clienteGerenciado);
            cheque.setNumeroCheque(numeroCheque);
            cheque.setBanco(banco);
            cheque.setFechaRegistro(LocalDate.now());
            cheque.setFechaVencimiento(fechaVencimiento);
            cheque.setValor(valorCheque);
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                cheque.setDescuentoValor(descuento);
                cheque.setDescuentoPorcentaje(calcularPorcentaje(saldoAntes, descuento));
            }
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
     * Cheque pre-datado que vamos a entregar a un Proveedor: ya descuenta su
     * saldo general. Sin descuento (uso comun).
     */
    public void registrarDeProveedor(Proveedor proveedor, String numeroCheque, String banco,
            LocalDate fechaVencimiento, BigDecimal valor, String descripcion) {
        registrarDeProveedor(proveedor, numeroCheque, banco, fechaVencimiento, valor, BigDecimal.ZERO, descripcion);
    }

    /**
     * Igual que registrarDeProveedor(...) de arriba, pero permite perdonar
     * un pedazo de la deuda (descuento). Mismo esquema de
     * registrarDeCliente(...): el valor del cheque no se toca y el saldo
     * baja por (valor del cheque + descuento).
     */
    public void registrarDeProveedor(Proveedor proveedor, String numeroCheque, String banco,
            LocalDate fechaVencimiento, BigDecimal valorCheque, BigDecimal descuentoValor, String descripcion) {
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio.");
        }
        validar(valorCheque, fechaVencimiento);
        BigDecimal descuento = validarDescuento(descuentoValor);

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Proveedor proveedorGerenciado = session.get(Proveedor.class, proveedor.getId());
            BigDecimal saldoAntes = proveedorGerenciado.getSaldo();
            if (descuento.compareTo(saldoAntes) > 0) {
                throw new IllegalArgumentException(
                        "El descuento no puede ser mayor que el saldo con el proveedor (Gs. " + saldoAntes + ").");
            }
            proveedorGerenciado.setSaldo(saldoAntes.subtract(valorCheque).subtract(descuento));
            session.merge(proveedorGerenciado);

            ChequePreDatado cheque = new ChequePreDatado();
            cheque.setProveedor(proveedorGerenciado);
            cheque.setNumeroCheque(numeroCheque);
            cheque.setBanco(banco);
            cheque.setFechaRegistro(LocalDate.now());
            cheque.setFechaVencimiento(fechaVencimiento);
            cheque.setValor(valorCheque);
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                cheque.setDescuentoValor(descuento);
                cheque.setDescuentoPorcentaje(calcularPorcentaje(saldoAntes, descuento));
            }
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
            movimiento.setDescuentoValor(chequeGerenciado.getDescuentoValor());
            movimiento.setDescuentoPorcentaje(chequeGerenciado.getDescuentoPorcentaje());
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

    /** Valida el descuento y devuelve BigDecimal.ZERO cuando no se aplico ninguno. */
    private BigDecimal validarDescuento(BigDecimal descuentoValor) {
        BigDecimal descuento = descuentoValor == null ? BigDecimal.ZERO : descuentoValor;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo.");
        }
        return descuento;
    }

    /**
     * Porcentaje que el descuento representa sobre el saldo que habia antes
     * del pago -- solo para exhibicion (null cuando no hubo descuento o
     * cuando no habia deuda).
     */
    private BigDecimal calcularPorcentaje(BigDecimal saldoAntes, BigDecimal descuento) {
        if (descuento.compareTo(BigDecimal.ZERO) == 0 || saldoAntes.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return descuento.multiply(BigDecimal.valueOf(100)).divide(saldoAntes, 2, RoundingMode.HALF_UP);
    }
}
