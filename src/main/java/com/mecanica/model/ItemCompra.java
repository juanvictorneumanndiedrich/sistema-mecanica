package com.mecanica.model;

import java.math.BigDecimal;

/**
 * Item de una Compra: cada cosa comprada al proveedor en esa "notinha",
 * con su cantidad y precio. Mismo patron de ItemOrdenServicio.
 */
public class ItemCompra {

    private Long id;

    private Compra compra;

    private String descripcion;

    private BigDecimal cantidad = BigDecimal.ONE;

    private BigDecimal valorUnitario;

    private BigDecimal valorTotal;

    public ItemCompra() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }
}
