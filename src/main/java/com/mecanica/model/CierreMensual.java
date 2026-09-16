package com.mecanica.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro permanente de un cierre mensual del negocio. Junta los
 * MovimientoFinanciero que el usuario eligio con checkbox (no
 * necesariamente los de un mes calendario exacto -- ver
 * FinancieroPanel, pestaña "Cierre Mensual", y CierreMensualController),
 * calcula la ganancia total (entradas - salidas) y guarda como quedo
 * repartida esa ganancia entre los socios activos en ese momento (ver
 * CierreSocioDetalle). Una vez creado, los movimientos y los retiros de
 * socio que entraron quedan vinculados a este cierre (ver
 * MovimientoFinanciero.cierre / RetiroSocio.cierre) y no vuelven a
 * aparecer como pendientes en un cierre futuro.
 */
@Entity
@Table(name = "cierre_mensual")
public class CierreMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaCierre;

    @Column(length = 100)
    private String descripcion;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEntradas;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalSalidas;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal gananciaTotal;

    @OneToMany(mappedBy = "cierre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CierreSocioDetalle> detalles = new ArrayList<>();

    public CierreMensual() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getTotalEntradas() {
        return totalEntradas;
    }

    public void setTotalEntradas(BigDecimal totalEntradas) {
        this.totalEntradas = totalEntradas;
    }

    public BigDecimal getTotalSalidas() {
        return totalSalidas;
    }

    public void setTotalSalidas(BigDecimal totalSalidas) {
        this.totalSalidas = totalSalidas;
    }

    public BigDecimal getGananciaTotal() {
        return gananciaTotal;
    }

    public void setGananciaTotal(BigDecimal gananciaTotal) {
        this.gananciaTotal = gananciaTotal;
    }

    public List<CierreSocioDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<CierreSocioDetalle> detalles) {
        this.detalles = detalles;
    }
}
