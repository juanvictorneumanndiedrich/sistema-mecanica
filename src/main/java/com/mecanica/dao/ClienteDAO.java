package com.mecanica.dao;

import com.mecanica.model.Cliente;
import com.mecanica.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

/**
 * DAO especifico de Cliente. Exemplo de como estender AbstractGenericDAO
 * e acrescentar buscas proprias da entidade -- os outros 12 DAOs
 * (EquipamentoDAO, OrdemDeServicoDAO, UsuarioDAO, FornecedorDAO,
 * CompraDAO, FechamentoFornecedorDAO, FuncionarioDAO,
 * RetiradaFuncionarioDAO, SocioDAO, RetiradaSocioDAO,
 * MovimentoFinanceiroDAO, ItemOrdemServicoDAO) seguem exatamente o mesmo
 * padrao.
 */
public class ClienteDAO extends AbstractGenericDAO<Cliente, Long> {

    public ClienteDAO() {
        super(Cliente.class);
    }

    /** Busca por nome (contendo o texto), usada na tela de Clientes e Equipamentos. */
    public List<Cliente> buscarPorNome(String nome) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Cliente c WHERE LOWER(c.nome) LIKE LOWER(:nome) ORDER BY c.nome";
            Query<Cliente> query = session.createQuery(hql, Cliente.class);
            query.setParameter("nome", "%" + nome + "%");
            return query.list();
        }
    }

    /** Busca por documento (CI/RUC) exato. */
    public Cliente buscarPorDocumento(String documento) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Cliente c WHERE c.documento = :documento";
            Query<Cliente> query = session.createQuery(hql, Cliente.class);
            query.setParameter("documento", documento);
            List<Cliente> resultado = query.list();
            return resultado.isEmpty() ? null : resultado.get(0);
        }
    }
}