package com.mecanica.model;

import com.mecanica.enums.TipoItemOrdenServicio;
import java.math.BigDecimal;

/**
 * Item de una Orden de Servicio: puede ser un servicio (mano de obra) o
 * un repuesto aplicado.
 */
public class ItemOrdenServicio {

    private Long id;

    private OrdenDeServicio ordenDeServicio;

    private TipoItemOrdenServicio tipo;

    private String descripcion;

    private BigDecimal cantidad = BigDecimal.ONE;

    private BigDecimal valorUnitario;

    private BigDecimal valorTotal;

    public ItemOrdenServicio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrdenDeServicio getOrdenDeServicio() {
        return ordenDeServicio;
    }

    public void setOrdenDeServicio(OrdenDeServicio ordenDeServicio) {
        this.ordenDeServicio = ordenDeServicio;
    }

    public TipoItemOrdenServicio getTipo() {
        return tipo;
    }

    public void setTipo(TipoItemOrdenServicio tipo) {
        this.tipo = tipo;
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
