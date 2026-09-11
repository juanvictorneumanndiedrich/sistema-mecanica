package com.mecanica.model;

import com.mecanica.enums.FormaPagamentoCompra;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Compra feita a um fornecedor. O formulario de compra tem apenas os
 * 2 cenarios previstos em FormaPagamentoCompra -- o cenario de "conta do
 * proprio cliente no fornecedor" nao existe aqui, por decisao de negocio.
 */
@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(nullable = false)
    private LocalDate data;

    @Column(length = 200)
    private String descricao;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 30)
    private FormaPagamentoCompra formaPagamento;

    /**
     * Preenchido quando a compra e do tipo LANCADA_EM_CONTA_FORNECEDOR e
     * ja foi incluida em um fechamento. Fica nulo enquanto pendente.
     */
    @ManyToOne
    @JoinColumn(name = "fechamento_fornecedor_id")
    private FechamentoFornecedor fechamentoFornecedor;

    public Compra() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public FormaPagamentoCompra getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamentoCompra formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public FechamentoFornecedor getFechamentoFornecedor() {
        return fechamentoFornecedor;
    }

    public void setFechamentoFornecedor(FechamentoFornecedor fechamentoFornecedor) {
        this.fechamentoFornecedor = fechamentoFornecedor;
    }
}