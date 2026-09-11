package com.mecanica.model;

import com.mecanica.enums.CategoriaMovimentoFinanceiro;
import com.mecanica.enums.TipoMovimentoFinanceiro;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lancamento no fluxo de caixa geral (tela Financeiro). Pagamento de
 * cliente, compra de fornecedor e retirada de funcionario geram um
 * registro aqui, para dar visao consolidada de entradas e saidas -- as
 * referencias abaixo sao opcionais e apontam para a origem do lancamento,
 * quando houver.
 *
 * IMPORTANTE: retirada de socio NAO gera MovimentoFinanceiro. Ela nao e
 * tratada como gasto -- e apenas descontada da parte daquele socio no
 * acerto (divisao de lucro 50/50), controlada só pela entidade
 * RetiradaSocio. Por isso nao ha aqui uma referencia para Socio.
 */
@Entity
@Table(name = "movimento_financeiro")
public class MovimentoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentoFinanceiro tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaMovimentoFinanceiro categoria;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(length = 200)
    private String descricao;

    // Referencias opcionais para a origem do lancamento (apenas uma delas
    // fica preenchida, de acordo com a categoria).
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @ManyToOne
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    public MovimentoFinanceiro() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public TipoMovimentoFinanceiro getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentoFinanceiro tipo) {
        this.tipo = tipo;
    }

    public CategoriaMovimentoFinanceiro getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaMovimentoFinanceiro categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }
}