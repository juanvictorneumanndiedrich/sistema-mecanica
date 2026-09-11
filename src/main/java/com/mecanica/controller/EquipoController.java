package com.mecanica.controller;

import com.mecanica.dao.EquipoDAO;
import com.mecanica.model.Cliente;
import com.mecanica.model.Equipo;

import java.util.List;

public class EquipoController {

    private final EquipoDAO equipamentoDAO = new EquipoDAO();

    public Equipo guardar(Equipo equipo) {
        validar(equipo);
        return equipamentoDAO.guardar(equipo);
    }

    public Equipo buscarPorId(Long id) {
        return equipamentoDAO.buscarPorId(id);
    }

    public List<Equipo> listarTodos() {
        return equipamentoDAO.listarTodos();
    }

    public List<Equipo> listarPorCliente(Cliente cliente) {
        return equipamentoDAO.listarPorCliente(cliente);
    }

    public void eliminar(Equipo equipo) {
        equipamentoDAO.eliminar(equipo);
    }

    private void validar(Equipo equipo) {
        if (equipo.getCliente() == null) {
            throw new IllegalArgumentException("El equipo debe estar vinculado a un cliente.");
        }
        if (equipo.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de equipo es obligatorio.");
        }
    }
}
