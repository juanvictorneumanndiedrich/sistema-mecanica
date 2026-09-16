package com.mecanica.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Retiro de ganancia hecho por un socio. La division entre los 2 socios es
 * siempre 50%/50% -- esa regla se aplica en el Controller al calcular cuanto
 * puede retirar cada socio, y no es un dato guardado por retiro.
 *
 * El campo cierre marca a que CierreMensual entro este retiro (null =
 * todavia pendiente, se descuenta en el proximo cierre que se haga). Se
 * marca en CierreMensualController.cerrar(), tomando SIEMPRE todos los
 * retiros todavia no vinculados a un cierre (sin usar un rango de fechas),
 * para evitar el mismo problema que tenia el retiro de empleado antes de la
 * correccion: un mismo retiro descontado dos veces en dos cierres distintos.
 */
@Entity
@Table(name = "retiro_socio")
public class RetiroSocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(length = 200)
    private String observacion;

    @ManyToOne
    @JoinColumn(name = "cierre_id")
    private CierreMensual cierre;

    public RetiroSocio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public CierreMensual getCierre() {
        return cierre;
    }

    public void setCierre(CierreMensual cierre) {
        this.cierre = cierre;
    }
}
