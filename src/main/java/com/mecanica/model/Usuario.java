package com.mecanica.model;

import jakarta.persistence.*;

/**
 * Usuario do sistema (login/senha). Ha 3 usuarios previstos: os 2 socios
 * e o(a) secretario(a), mas o cadastro de Usuario e independente das
 * entidades de negocio Socio/Funcionario -- e apenas controle de acesso.
 *
 * As permissoes sao individuais por usuario (nao fixas por "cargo"),
 * conforme definido na fase de telas: um booleano por area do sistema.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 60)
    private String login;

    /** Guardar sempre um hash (nunca a senha em texto puro). */
    @Column(nullable = false, length = 255)
    private String senha;

    @Column(nullable = false)
    private boolean ativo = true;

    // Permissoes individuais, uma por area da navegacao principal (6 areas)
    @Column(name = "permissao_clientes_equipamentos", nullable = false)
    private boolean permissaoClientesEquipamentos;

    @Column(name = "permissao_ordens_servico", nullable = false)
    private boolean permissaoOrdensServico;

    @Column(name = "permissao_compras_fornecedores", nullable = false)
    private boolean permissaoComprasFornecedores;

    @Column(name = "permissao_financeiro", nullable = false)
    private boolean permissaoFinanceiro;

    @Column(name = "permissao_funcionarios_socios", nullable = false)
    private boolean permissaoFuncionariosSocios;

    @Column(name = "permissao_usuarios", nullable = false)
    private boolean permissaoUsuarios;

    public Usuario() {
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

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public boolean isPermissaoClientesEquipamentos() {
        return permissaoClientesEquipamentos;
    }

    public void setPermissaoClientesEquipamentos(boolean permissaoClientesEquipamentos) {
        this.permissaoClientesEquipamentos = permissaoClientesEquipamentos;
    }

    public boolean isPermissaoOrdensServico() {
        return permissaoOrdensServico;
    }

    public void setPermissaoOrdensServico(boolean permissaoOrdensServico) {
        this.permissaoOrdensServico = permissaoOrdensServico;
    }

    public boolean isPermissaoComprasFornecedores() {
        return permissaoComprasFornecedores;
    }

    public void setPermissaoComprasFornecedores(boolean permissaoComprasFornecedores) {
        this.permissaoComprasFornecedores = permissaoComprasFornecedores;
    }

    public boolean isPermissaoFinanceiro() {
        return permissaoFinanceiro;
    }

    public void setPermissaoFinanceiro(boolean permissaoFinanceiro) {
        this.permissaoFinanceiro = permissaoFinanceiro;
    }

    public boolean isPermissaoFuncionariosSocios() {
        return permissaoFuncionariosSocios;
    }

    public void setPermissaoFuncionariosSocios(boolean permissaoFuncionariosSocios) {
        this.permissaoFuncionariosSocios = permissaoFuncionariosSocios;
    }

    public boolean isPermissaoUsuarios() {
        return permissaoUsuarios;
    }

    public void setPermissaoUsuarios(boolean permissaoUsuarios) {
        this.permissaoUsuarios = permissaoUsuarios;
    }
}