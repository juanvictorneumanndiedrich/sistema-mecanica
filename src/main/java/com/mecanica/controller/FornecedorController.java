package com.mecanica.controller;

import com.mecanica.dao.FornecedorDAO;
import com.mecanica.model.Fornecedor;

import java.util.List;

public class FornecedorController {

    private final FornecedorDAO fornecedorDAO = new FornecedorDAO();

    public Fornecedor salvar(Fornecedor fornecedor) {
        validar(fornecedor);
        return fornecedorDAO.salvar(fornecedor);
    }

    public Fornecedor buscarPorId(Long id) {
        return fornecedorDAO.buscarPorId(id);
    }

    public List<Fornecedor> listarTodos() {
        return fornecedorDAO.listarTodos();
    }

    public List<Fornecedor> buscarPorNome(String nome) {
        return fornecedorDAO.buscarPorNome(nome);
    }

    public Fornecedor buscarPorDocumento(String documento) {
        return fornecedorDAO.buscarPorDocumento(documento);
    }

    public void excluir(Fornecedor fornecedor) {
        fornecedorDAO.excluir(fornecedor);
    }

    private void validar(Fornecedor fornecedor) {
        if (fornecedor.getNome() == null || fornecedor.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do fornecedor e obrigatorio.");
        }
    }
}