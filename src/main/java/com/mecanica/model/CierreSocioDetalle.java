package com.mecanica.model;

import java.math.BigDecimal;

/**
 * Una linea del CierreMensual: cuanto le toco de ganancia a un socio en
 * ese cierre, cuanto ya habia retirado (retiros todavia no vinculados a
 * ningun cierre anterior, ver RetiroSocio.cierre) y cuanto le queda por
 * recibir. Puede quedar negativo si el socio ya retiro mas que su parte.
 */
public class CierreSocioDetalle {

    private Long id;

    private CierreMensual cierre;

    private Socio socio;

    private BigDecimal parteGanancia;

    private BigDecimal yaRetirado;

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
