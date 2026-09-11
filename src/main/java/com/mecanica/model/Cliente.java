package com.mecanica.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente da mecanica/tornearia. O pagamento do cliente abate o SALDO
 * GERAL dele (nao uma Ordem de Servico especifica) -- por isso o saldo
 * fica aqui no Cliente, e nao em OrdemDeServico.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** CI (pessoa fisica) ou RUC (empresa), documento paraguaio. */
    @Column(name = "documento", length = 30)
    private String documento;

    @Column(length = 30)
    private String telefone;

    @Column(length = 200)
    private String endereco;

    /**
     * Saldo geral do cliente: positivo = cliente deve para a mecanica.
     * E abatido diretamente pelos pagamentos, sem vincular a uma OS.
     */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Equipamento> equipamentos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente")
    private List<OrdemDeServico> ordensDeServico = new ArrayList<>();

    public Cliente() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public List<Equipamento> getEquipamentos() {
        return equipamentos;
    }

    public void setEquipamentos(List<Equipamento> equipamentos) {
        this.equipamentos = equipamentos;
    }

    public List<OrdemDeServico> getOrdensDeServico() {
        return ordensDeServico;
    }

    public void setOrdensDeServico(List<OrdemDeServico> ordensDeServico) {
        this.ordensDeServico = ordensDeServico;
    }
}