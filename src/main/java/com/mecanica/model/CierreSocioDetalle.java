package com.mecanica.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Una linea del CierreMensual: cuanto le toco de ganancia a un socio en
 * ese cierre, cuanto ya habia retirado (retiros todavia no vinculados a
 * ningun cierre anterior, ver RetiroSocio.cierre) y cuanto le queda por
 * recibir. Puede quedar negativo si el socio ya retiro mas que su parte.
 */
@Entity
@Table(name = "cierre_socio_detalle")
public class CierreSocioDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cierre_id", nullable = false)
    private CierreMensual cierre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal parteGanancia;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal yaRetirado;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorARecibir;

    public CierreSocioDetalle() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CierreMensual getCierre() {
        return cierre;
    }

    public void setCierre(CierreMensual cierre) {
        this.cierre = cierre;
    }

    public Socio getSocio() {
        return socio;
    }

    public void setSocio(Socio socio) {
        this.socio = socio;
    }

    public BigDecimal getParteGanancia() {
        return parteGanancia;
    }

    public void setParteGanancia(BigDecimal parteGanancia) {
        this.parteGanancia = parteGanancia;
    }

    public BigDecimal getYaRetirado() {
        return yaRetirado;
    }

    public void setYaRetirado(BigDecimal yaRetirado) {
        this.yaRetirado = yaRetirado;
    }

    public BigDecimal getValorARecibir() {
        return valorARecibir;
    }

    public void setValorARecibir(BigDecimal valorARecibir) {
        this.valorARecibir = valorARecibir;
    }
}
