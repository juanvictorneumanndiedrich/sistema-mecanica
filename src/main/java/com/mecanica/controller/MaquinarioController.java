package com.mecanica.controller;

import com.mecanica.dao.MaquinarioDAO;
import com.mecanica.enums.Permiso;
import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;
import com.mecanica.util.Sesion;

import java.util.List;

public class MaquinarioController {

    private final MaquinarioDAO maquinarioDAO = new MaquinarioDAO();
    private final AuditoriaController auditoria = new AuditoriaController();

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
        Sesion.exigir(Permiso.ELIMINAR_REGISTROS);
        maquinarioDAO.eliminar(maquinario);
        auditoria.registrar("MAQUINARIO ELIMINADO", maquinario.getTipo() + " "
                + (maquinario.getMarca() == null ? "" : maquinario.getMarca()) + " "
                + (maquinario.getModelo() == null ? "" : maquinario.getModelo())
                + (maquinario.getCliente() == null ? "" : " - cliente " + maquinario.getCliente().getNombre()));
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
