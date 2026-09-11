package com.mecanica.model;

import jakarta.persistence.*;

/**
 * Um dos 2 socios da mecanica. A divisao de lucro entre os socios e
 * sempre fixa em 50%/50%, por isso NAO existe um campo de percentual
 * configuravel aqui -- a regra e aplicada em codigo (Service), nao em
 * dado editavel.
 */
@Entity
@Table(name = "socio")
public class Socio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Cedula de identidad (CI), documento paraguaio. */
    @Column(name = "documento", length = 30)
    private String documento;

    @Column(length = 30)
    private String telefone;

    @Column(nullable = false)
    private boolean ativo = true;

    public Socio() {
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}