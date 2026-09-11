package com.mecanica.model;

import com.mecanica.enums.FormaPagoCompra;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Compra hecha a un proveedor. El formulario de compra tiene solo los
 * 2 escenarios previstos en FormaPagoCompra -- el escenario de "cuenta del
 * propio cliente en el proveedor" no existe aca, por decision de negocio.
 */
@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 30)
    private FormaPagoCompra formaPago;

    /**
     * Se completa cuando la compra es del tipo CARGADA_EN_CUENTA_PROVEEDOR y
     * ya fue incluida en un cierre. Queda nulo mientras esta pendiente.
     */
    @ManyToOne
    @JoinColumn(name = "cierre_proveedor_id")
    private CierreProveedor cierreProveedor;

    public Compra() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public FormaPagoCompra getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(FormaPagoCompra formaPago) {
        this.formaPago = formaPago;
    }

    public CierreProveedor getCierreProveedor() {
        return cierreProveedor;
    }

    public void setCierreProveedor(CierreProveedor cierreProveedor) {
        this.cierreProveedor = cierreProveedor;
    }
}
