package com.mecanica.dao;

import java.util.List;

/**
 * Contrato basico que todo DAO especifico (ClienteDAO, OrdemDeServicoDAO,
 * etc.) deve seguir. Segue o fluxo de camadas ja definido na arquitetura:
 * View -> Service -> DAO -> Model.
 *
 * @param <T>  tipo da entidade (ex: Cliente)
 * @param <ID> tipo da chave primaria da entidade (ex: Long)
 */
public interface GenericDAO<T, ID> {

    T salvar(T entidade);

    T buscarPorId(ID id);

    List<T> listarTodos();

    void excluir(T entidade);
}