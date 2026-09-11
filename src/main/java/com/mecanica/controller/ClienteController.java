package com.mecanica.controller;

import com.mecanica.dao.ClienteDAO;
import com.mecanica.enums.CategoriaMovimentoFinanceiro;
import com.mecanica.enums.TipoMovimentoFinanceiro;
import com.mecanica.model.Cliente;
import com.mecanica.model.MovimentoFinanceiro;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de Cliente: recebe chamadas da View, aplica as regras de
 * negocio e repassa pro DAO. Operacoes simples (CRUD) so delegam pro
 * ClienteDAO; operacoes que mexem em mais de uma entidade ao mesmo tempo
 * (como registrar pagamento) abrem sua propria Session/Transaction aqui,
 * pra garantir que tudo aconteca junto ou nada aconteca.
 */
public class ClienteController {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    public Cliente salvar(Cliente cliente) {
        validar(cliente);
        return clienteDAO.salvar(cliente);
    }

    public Cliente buscarPorId(Long id) {
        return clienteDAO.buscarPorId(id);
    }

    public List<Cliente> listarTodos() {
        return clienteDAO.listarTodos();
    }

    public List<Cliente> buscarPorNome(String nome) {
        return clienteDAO.buscarPorNome(nome);
    }

    public void excluir(Cliente cliente) {
        clienteDAO.excluir(cliente);
    }

    /**
     * Registra um pagamento do cliente: abate o valor do SALDO GERAL dele
     * (nao de uma OS especifica) e gera o MovimentoFinanceiro correspondente
     * (ENTRADA / PAGAMENTO_CLIENTE). As duas operacoes acontecem na mesma
     * transacao, pra nunca abater o saldo sem registrar o movimento (ou
     * vice-versa).
     */
    public void registrarPagamento(Cliente cliente, BigDecimal valor, String descricao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero.");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Cliente clienteGerenciado = session.get(Cliente.class, cliente.getId());
            clienteGerenciado.setSaldo(clienteGerenciado.getSaldo().subtract(valor));
            session.merge(clienteGerenciado);

            MovimentoFinanceiro movimento = new MovimentoFinanceiro();
            movimento.setData(LocalDate.now());
            movimento.setTipo(TipoMovimentoFinanceiro.ENTRADA);
            movimento.setCategoria(CategoriaMovimentoFinanceiro.PAGAMENTO_CLIENTE);
            movimento.setValor(valor);
            movimento.setDescricao(descricao);
            movimento.setCliente(clienteGerenciado);
            session.persist(movimento);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    private void validar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do cliente e obrigatorio.");
        }
    }
}