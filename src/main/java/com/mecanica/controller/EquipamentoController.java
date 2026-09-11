package com.mecanica.controller;

import com.mecanica.dao.EquipamentoDAO;
import com.mecanica.model.Cliente;
import com.mecanica.model.Equipamento;

import java.util.List;

public class EquipamentoController {

    private final EquipamentoDAO equipamentoDAO = new EquipamentoDAO();

    public Equipamento salvar(Equipamento equipamento) {
        validar(equipamento);
        return equipamentoDAO.salvar(equipamento);
    }

    public Equipamento buscarPorId(Long id) {
        return equipamentoDAO.buscarPorId(id);
    }

    public List<Equipamento> listarTodos() {
        return equipamentoDAO.listarTodos();
    }

    public List<Equipamento> listarPorCliente(Cliente cliente) {
        return equipamentoDAO.listarPorCliente(cliente);
    }

    public void excluir(Equipamento equipamento) {
        equipamentoDAO.excluir(equipamento);
    }

    private void validar(Equipamento equipamento) {
        if (equipamento.getCliente() == null) {
            throw new IllegalArgumentException("Equipamento precisa estar vinculado a um cliente.");
        }
        if (equipamento.getTipo() == null) {
            throw new IllegalArgumentException("Tipo do equipamento e obrigatorio.");
        }
    }
}