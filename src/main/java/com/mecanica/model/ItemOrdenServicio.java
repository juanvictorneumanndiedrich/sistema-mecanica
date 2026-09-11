package com.mecanica.model;

import com.mecanica.enums.TipoItemOrdenServicio;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Item de una Orden de Servicio: puede ser un servicio (mano de obra) o
 * un repuesto aplicado.
 */
@Entity
@Table(name = "item_orden_servicio")
public class ItemOrdenServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_de_servicio_id", nullable = false)
    private OrdenDeServicio ordenDeServicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoItemOrdenServicio tipo;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad = BigDecimal.ONE;

    @Column(name = "valor_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
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
