package com.mecanica.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Retirada de lucro feita por um socio. A divisao entre os 2 socios e
 * sempre 50%/50% -- essa regra e' aplicada no Service ao calcular quanto
 * cada socio pode retirar, e nao e' um dado gravado por retirada.
 */
@Entity
@Table(name = "retirada_socio")
public class RetiradaSocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(length = 200)
    private String observacao;

    public RetiradaSocio() {
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

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}