package com.mecanica.model;

import com.mecanica.enums.EstadoCierreProveedor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cierre de cuenta con un proveedor. Ocurre en dos etapas
 * manuales (CERRADO -> PAGADO) y no esta atado a un mes calendario
 * fijo -- es el usuario el que decide cuando cerrar y cuando pagar.
 */
@Entity
@Table(name = "cierre_proveedor")
public class CierreProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCierreProveedor estado = EstadoCierreProveedor.CERRADO;

    @OneToMany(mappedBy = "cierreProveedor")
    private List<Compra> compras = new ArrayList<>();

    public CierreProveedor() {
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

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public EstadoCierreProveedor getEstado() {
        return estado;
    }

    public void setEstado(EstadoCierreProveedor estado) {
        this.estado = estado;
    }

    public List<Compra> getCompras() {
        return compras;
    }

    public void setCompras(List<Compra> compras) {
        this.compras = compras;
    }
}
