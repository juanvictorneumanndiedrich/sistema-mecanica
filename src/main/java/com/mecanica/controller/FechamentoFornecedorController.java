package com.mecanica.controller;

import com.mecanica.dao.CompraDAO;
import com.mecanica.enums.CategoriaMovimentoFinanceiro;
import com.mecanica.enums.StatusFechamentoFornecedor;
import com.mecanica.enums.TipoMovimentoFinanceiro;
import com.mecanica.model.Compra;
import com.mecanica.model.FechamentoFornecedor;
import com.mecanica.model.Fornecedor;
import com.mecanica.model.MovimentoFinanceiro;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de FechamentoFornecedor. O fechamento acontece em duas
 * etapas manuais: abrirFechamento (junta as compras pendentes numa conta
 * fechada, sem gerar MovimentoFinanceiro ainda) e marcarComoPago (so ai
 * gera o MovimentoFinanceiro de saida). Nao esta vinculado a mes
 * calendario fixo.
 */
public class FechamentoFornecedorController {

    private final CompraDAO compraDAO = new CompraDAO();

    /**
     * 1a etapa: soma as compras pendentes (LANCADA_EM_CONTA_FORNECEDOR e
     * sem fechamento ainda) do fornecedor e cria o FechamentoFornecedor
     * com status FECHADO, vinculando essas compras a ele.
     */
    public FechamentoFornecedor abrirFechamento(Fornecedor fornecedor, LocalDate dataFechamento) {
        List<Compra> pendentes = compraDAO.listarPendentesDeFechamento(fornecedor);
        if (pendentes.isEmpty()) {
            throw new IllegalStateException("Nao ha compras pendentes pra fechar com esse fornecedor.");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Compra c : pendentes) {
            total = total.add(c.getValor());
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            FechamentoFornecedor fechamento = new FechamentoFornecedor();
            fechamento.setFornecedor(fornecedor);
            fechamento.setDataFechamento(dataFechamento != null ? dataFechamento : LocalDate.now());
            fechamento.setValorTotal(total);
            fechamento.setStatus(StatusFechamentoFornecedor.FECHADO);
            session.persist(fechamento);

            for (Compra c : pendentes) {
                c.setFechamentoFornecedor(fechamento);
                session.merge(c);
            }

            tx.commit();
            return fechamento;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * 2a etapa: marca o fechamento como pago e so ai gera o
     * MovimentoFinanceiro (SAIDA / COMPRA_FORNECEDOR) do valor total.
     */
    public FechamentoFornecedor marcarComoPago(FechamentoFornecedor fechamento, LocalDate dataPagamento) {
        if (fechamento.getStatus() == StatusFechamentoFornecedor.PAGO) {
            throw new IllegalStateException("Esse fechamento ja esta pago.");
        }
        LocalDate dataFinal = dataPagamento != null ? dataPagamento : LocalDate.now();

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            fechamento.setStatus(StatusFechamentoFornecedor.PAGO);
            fechamento.setDataPagamento(dataFinal);
            session.merge(fechamento);

            MovimentoFinanceiro movimento = new MovimentoFinanceiro();
            movimento.setData(dataFinal);
            movimento.setTipo(TipoMovimentoFinanceiro.SAIDA);
            movimento.setCategoria(CategoriaMovimentoFinanceiro.COMPRA_FORNECEDOR);
            movimento.setValor(fechamento.getValorTotal());
            movimento.setDescricao("Pagamento do fechamento de " + fechamento.getFornecedor().getNome());
            movimento.setFornecedor(fechamento.getFornecedor());
            session.persist(movimento);

            tx.commit();
            return fechamento;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}