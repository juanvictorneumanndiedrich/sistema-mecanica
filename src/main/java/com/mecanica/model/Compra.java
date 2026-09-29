package com.mecanica.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Compra hecha a un proveedor. Funciona como una "notinha": se abre una
 * Compra por cada visita/pedido al proveedor (CompraController.abrir) y se
 * le van agregando los items (cada cosa comprada, con cantidad y precio --
 * ver ItemCompra), en vez de registrar una Compra por cada producto.
 *
 * La nota nunca se "cierra": se puede seguir editando siempre. Su valor
 * entra en la cuenta del proveedor a medida que se cargan los items -- cada
 * item agregado/quitado ajusta el saldo del proveedor en la misma
 * transaccion (ver ItemCompraController), y borrar la nota entera descuenta
 * su valor de esa cuenta (CompraController.eliminar).
 *
 * No hay campo de estado: PENDIENTE/PAGADA se calcula en pantalla a partir
 * de lo que ya se le pago al proveedor, porque el pago no es por nota
 * especifica (ver ProveedorController.registrarPagamento).
 */
public class Compra {

    private Long id;

    /**
     * Numero secuencial de la notinha, generado automaticamente por el
     * Controller (ver CompraController.abrir). No se edita a mano y no se
     * repite -- sirve para identificar la nota de compra.
     */
    private Long numero;

    private Proveedor proveedor;

    private LocalDate fecha;

    private BigDecimal valorTotal = BigDecimal.ZERO;

    private List<ItemCompra> items = new ArrayList<>();

    public Compra() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public List<ItemCompra> getItems() {
        return items;
    }

    public void setItems(List<ItemCompra> items) {
        this.items = items;
    }
}
