package com.mecanica.controller;

import com.mecanica.dao.MaquinarioDAO;
import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;

import java.util.List;

public class MaquinarioController {

    private final MaquinarioDAO maquinarioDAO = new MaquinarioDAO();

    public Maquinario guardar(Maquinario maquinario) {
        validar(maquinario);
        return maquinarioDAO.guardar(maquinario);
    }

    public Maquinario buscarPorId(Long id) {
        return maquinarioDAO.buscarPorId(id);
    }

    public List<Maquinario> listarTodos() {
        return maquinarioDAO.listarTodos();
    }

    public List<Maquinario> listarPorCliente(Cliente cliente) {
        return maquinarioDAO.listarPorCliente(cliente);
    }

    public void eliminar(Maquinario maquinario) {
        maquinarioDAO.eliminar(maquinario);
    }

    private void validar(Maquinario maquinario) {
        if (maquinario.getCliente() == null) {
            throw new IllegalArgumentException("El maquinario debe estar vinculado a un cliente.");
        }
        if (maquinario.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de maquinario es obligatorio.");
        }
    }
}
