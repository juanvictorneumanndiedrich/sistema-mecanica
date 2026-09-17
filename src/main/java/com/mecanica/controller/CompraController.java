package com.mecanica.controller;

import com.mecanica.dao.CompraDAO;
import com.mecanica.dao.ProveedorDAO;
import com.mecanica.enums.Permiso;
import com.mecanica.model.Compra;
import com.mecanica.model.Proveedor;
import com.mecanica.util.Errores;
import com.mecanica.util.HibernateUtil;
import com.mecanica.util.Sesion;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller de Compra: funciona como una "notinha" del proveedor -- se
 * abre con abrir() (numero secuencial automatico) y se le van agregando
 * items (ver ItemCompraController), que van sumando en la cuenta del
 * proveedor a medida que se cargan.
 *
 * Mientras la nota esta PENDIENTE se puede editar y borrar libremente;
 * borrarla descuenta su valor de la cuenta del proveedor, en la misma
 * transaccion. Una vez que quedo PAGADA ya no se puede tocar mas.
 *
 * PENDIENTE/PAGADA no se guarda en la base: se calcula con
 * calcularPagadas(), porque el pago al proveedor no es por nota -- es un
 * valor que descuenta el saldo general (ProveedorController.registrarPagamento).
 */
public class CompraController {

    private final CompraDAO compraDAO = new CompraDAO();
    private final AuditoriaController auditoria = new AuditoriaController();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    /**
     * Abre una Compra nueva, sin items todavia (valor cero), generando el
     * proximo numero secuencial automaticamente.
     */
    public Compra abrir(Proveedor proveedor, LocalDate fecha) {
        if (proveedor == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio para abrir una compra.");
        }
        Long mayorNumero = compraDAO.buscarMayorNumero();

        Compra compra = new Compra();
        compra.setNumero(mayorNumero == null ? 1L : mayorNumero + 1);
        compra.setProveedor(proveedor);
        compra.setFecha(fecha != null ? fecha : LocalDate.now());
        compra.setValorTotal(BigDecimal.ZERO);
        return compraDAO.guardar(compra);
    }

    /**
     * Borra la nota entera (con todos sus items, por el cascade) y DESCUENTA
     * su valor de la cuenta del proveedor -- las dos cosas en la misma
     * transaccion, para que la cuenta nunca quede inflada por una nota que
     * ya no existe. Una nota ya pagada no se puede borrar.
     */
    public void eliminar(Compra compra) {
        Sesion.exigir(Permiso.ELIMINAR_REGISTROS);
        verificarEditable(compra);

        Transaction tx = null;
        Session session = HibernateUtil.abrirSesion();
        try {
            tx = session.beginTransaction();

            Compra compraGerenciada = session.get(Compra.class, compra.getId());
            if (compraGerenciada == null) {
                tx.commit();
                return;
            }

            BigDecimal valorNota = compraGerenciada.getValorTotal() == null
                    ? BigDecimal.ZERO
                    : compraGerenciada.getValorTotal();

            Proveedor proveedorGerenciado = session.get(Proveedor.class, compraGerenciada.getProveedor().getId());
            proveedorGerenciado.setSaldo(proveedorGerenciado.getSaldo().subtract(valorNota));
            session.merge(proveedorGerenciado);

            session.remove(compraGerenciada);

            tx.commit();
            auditoria.registrar("NOTA DE COMPRA ELIMINADA", "Nota Nº " + compraGerenciada.getNumero() + " de "
                    + proveedorGerenciado.getNombre() + " - " + AuditoriaController.gs(valorNota));
        } catch (RuntimeException e) {
            Errores.revertir(tx);
            throw Errores.traducir(e);
        } finally {
            session.close();
        }
    }

    /**
     * Corta cualquier intento de modificar una nota ya pagada -- lo usan
     * eliminar() y tambien el ItemCompraController al agregar/quitar items.
     */
    public void verificarEditable(Compra compra) {
        if (estaPagada(compra)) {
            throw new IllegalStateException("Esa nota ya fue pagada y no se puede modificar ni borrar.");
        }
    }

    /** Relee el proveedor y sus notas de la base para saber si esta ya quedo cubierta por los pagos. */
    public boolean estaPagada(Compra compra) {
        Proveedor proveedor = proveedorDAO.buscarPorId(compra.getProveedor().getId());
        if (proveedor == null) {
            return false;
        }
        return calcularPagadas(compraDAO.listarPorFornecedor(proveedor), proveedor.getSaldo())
                .contains(compra.getId());
    }

    /**
     * Ids de las notas que ya quedan cubiertas por lo que se le pago al
     * proveedor. Como el pago no es por nota especifica, se considera que la
     * plata pagada va cubriendo las notas de la mas vieja a la mas nueva
     * (por numero): lo pagado es la suma de las notas menos el saldo que
     * todavia se le debe.
     *
     * Una nota sin items (valor cero) nunca cuenta como pagada -- no hay
     * nada que pagar en ella, y ademas recien se esta cargando.
     */
    public static Set<Long> calcularPagadas(List<Compra> compras, BigDecimal saldoProveedor) {
        BigDecimal totalNotas = BigDecimal.ZERO;
        for (Compra compra : compras) {
            totalNotas = totalNotas.add(valorDe(compra));
        }

        BigDecimal totalPagado = totalNotas.subtract(saldoProveedor == null ? BigDecimal.ZERO : saldoProveedor);
        if (totalPagado.compareTo(BigDecimal.ZERO) < 0) {
            totalPagado = BigDecimal.ZERO;
        }

        List<Compra> ordenadas = new ArrayList<>(compras);
        ordenadas.sort(Comparator.comparing(Compra::getNumero));

        Set<Long> pagadas = new HashSet<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (Compra compra : ordenadas) {
            BigDecimal valor = valorDe(compra);
            acumulado = acumulado.add(valor);
            if (valor.compareTo(BigDecimal.ZERO) > 0 && acumulado.compareTo(totalPagado) <= 0) {
                pagadas.add(compra.getId());
            }
        }
        return pagadas;
    }

    private static BigDecimal valorDe(Compra compra) {
        return compra.getValorTotal() == null ? BigDecimal.ZERO : compra.getValorTotal();
    }

    public List<Compra> listarPorFornecedor(Proveedor proveedor) {
        return compraDAO.listarPorFornecedor(proveedor);
    }

    public Compra buscarPorId(Long id) {
        return compraDAO.buscarPorId(id);
    }
}
