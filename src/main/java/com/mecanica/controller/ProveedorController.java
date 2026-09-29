package com.mecanica.controller;

import com.mecanica.dao.MovimientoFinancieroDAO;
import com.mecanica.dao.ProveedorDAO;
import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.Permiso;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.Proveedor;
import com.mecanica.util.BD;
import com.mecanica.util.Sesion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de Proveedor. Las operaciones simples (CRUD) solo llaman al
 * ProveedorDAO; el pago al proveedor toca mas de una entidad a la vez
 * (saldo + MovimientoFinanciero) y por eso abre su propia
 * transaccion aca -- mismo patron de ClienteController.
 */
public class ProveedorController {

    private final ProveedorDAO fornecedorDAO = new ProveedorDAO();
    private final MovimientoFinancieroDAO movimientoDAO = new MovimientoFinancieroDAO();
    private final AuditoriaController auditoria = new AuditoriaController();

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
        Sesion.exigir(Permiso.ELIMINAR_REGISTROS);
        fornecedorDAO.eliminar(proveedor);
        auditoria.registrar("PROVEEDOR ELIMINADO", proveedor.getNombre());
    }

    /**
     * Registra un pago hecho al proveedor: descuenta el valor de su SALDO
     * GENERAL (no de una Compra especifica -- no se paga notinha por
     * notinha) y genera el MovimientoFinanciero correspondiente (SALIDA /
     * COMPRA_PROVEEDOR). Las dos operaciones ocurren en la misma
     * transaccion, para nunca descontar el saldo sin registrar el gasto (o
     * vice-versa). Mismo esquema de ClienteController.registrarPagamento.
     * Sin descuento (uso comun).
     */
    public void registrarPagamento(Proveedor proveedor, BigDecimal valor, String descripcion) {
        registrarPagamento(proveedor, valor, BigDecimal.ZERO, descripcion);
    }

    /**
     * Igual que registrarPagamento(Proveedor, BigDecimal, String), pero
     * permite perdonar un pedazo de la DEUDA con el proveedor (el proveedor
     * nos hace un descuento). Misma semantica de
     * ClienteController.registrarPagamento(Cliente, BigDecimal, BigDecimal,
     * String): valorPagado es la plata que sale de verdad de la caja y entra
     * ENTERA en Financiero; el descuento se perdona aparte, asi que el saldo
     * baja por (valorPagado + descuento).
     */
    public void registrarPagamento(Proveedor proveedor, BigDecimal valorPagado, BigDecimal descuentoValor,
            String descripcion) {
        if (valorPagado == null || valorPagado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del pago debe ser mayor que cero.");
        }
        BigDecimal descuento = descuentoValor == null ? BigDecimal.ZERO : descuentoValor;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo.");
        }

        Proveedor proveedorGerenciado = BD.transaccion(conexion -> {
            Proveedor gerenciado = fornecedorDAO.buscarPorId(conexion, proveedor.getId());
            BigDecimal saldoAntes = gerenciado.getSaldo();
            BigDecimal deuda = saldoAntes.max(BigDecimal.ZERO);
            if (descuento.compareTo(deuda) > 0) {
                throw new IllegalArgumentException(
                        "El descuento no puede ser mayor que la deuda con el proveedor (Gs. " + deuda + ").");
            }
            gerenciado.setSaldo(saldoAntes.subtract(valorPagado).subtract(descuento));
            fornecedorDAO.guardar(conexion, gerenciado);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(LocalDate.now());
            movimiento.setTipo(TipoMovimientoFinanciero.SALIDA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.COMPRA_PROVEEDOR);
            movimiento.setValor(valorPagado);
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                movimiento.setDescuentoValor(descuento);
                if (saldoAntes.compareTo(BigDecimal.ZERO) > 0) {
                    movimiento.setDescuentoPorcentaje(descuento.multiply(BigDecimal.valueOf(100))
                            .divide(saldoAntes, 2, RoundingMode.HALF_UP));
                }
            }
            movimiento.setDescripcion(descripcion);
            movimiento.setProveedor(gerenciado);
            movimientoDAO.guardar(conexion, movimiento);
            return gerenciado;
        });
        auditoria.registrar("PAGO A PROVEEDOR", proveedorGerenciado.getNombre() + " - pago "
                + AuditoriaController.gs(valorPagado)
                + (descuento.compareTo(BigDecimal.ZERO) > 0 ? " + descuento " + AuditoriaController.gs(descuento) : ""));
    }

    /**
     * Registra que el proveedor devuelve en efectivo un credito a favor que
     * la mecanica ya tiene con el (saldo del proveedor NEGATIVO -- la
     * mecanica le pago de mas en algun momento). Suma el valor al saldo (lo
     * acerca a cero) y genera el MovimientoFinanciero correspondiente
     * (ENTRADA / DEVOLUCION_PROVEEDOR), ya que es dinero real que entra a la
     * caja de la mecanica. Misma transaccion atomica que registrarPagamento.
     * No deja retirar mas de lo que hay de credito disponible.
     */
    public void retirarSaldo(Proveedor proveedor, BigDecimal valor, String descripcion) {
        Sesion.exigir(Permiso.RETIRAR_SALDO);
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor a retirar debe ser mayor que cero.");
        }

        Proveedor proveedorGerenciado = BD.transaccion(conexion -> {
            Proveedor gerenciado = fornecedorDAO.buscarPorId(conexion, proveedor.getId());
            BigDecimal credito = gerenciado.getSaldo().negate();
            if (credito.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La mecanica no tiene credito a favor con este proveedor.");
            }
            if (valor.compareTo(credito) > 0) {
                throw new IllegalArgumentException(
                        "El valor a retirar no puede ser mayor que el credito disponible (Gs. " + credito + ").");
            }
            gerenciado.setSaldo(gerenciado.getSaldo().add(valor));
            fornecedorDAO.guardar(conexion, gerenciado);

            MovimientoFinanciero movimiento = new MovimientoFinanciero();
            movimiento.setFecha(LocalDate.now());
            movimiento.setTipo(TipoMovimientoFinanciero.ENTRADA);
            movimiento.setCategoria(CategoriaMovimientoFinanciero.DEVOLUCION_PROVEEDOR);
            movimiento.setValor(valor);
            movimiento.setDescripcion(descripcion);
            movimiento.setProveedor(gerenciado);
            movimientoDAO.guardar(conexion, movimiento);
            return gerenciado;
        });
        auditoria.registrar("SALDO RETIRADO (PROVEEDOR)",
                proveedorGerenciado.getNombre() + " - " + AuditoriaController.gs(valor));
    }

    private void validar(Proveedor proveedor) {
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio.");
        }
    }
}
