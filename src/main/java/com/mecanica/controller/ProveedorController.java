package com.mecanica.controller;

import com.mecanica.dao.ProveedorDAO;
import com.mecanica.model.Proveedor;

import java.util.List;

public class ProveedorController {

    private final ProveedorDAO fornecedorDAO = new ProveedorDAO();

    public Proveedor guardar(Proveedor proveedor) {
        validar(proveedor);
        return fornecedorDAO.guardar(proveedor);
    }

    public Proveedor buscarPorId(Long id) {
        return fornecedorDAO.buscarPorId(id);
    }

    public List<Proveedor> listarTodos() {
        return fornecedorDAO.listarTodos();
    }

    public List<Proveedor> buscarPorNombre(String nombre) {
        return fornecedorDAO.buscarPorNombre(nombre);
    }

    public Proveedor buscarPorDocumento(String documento) {
        return fornecedorDAO.buscarPorDocumento(documento);
    }

    public void eliminar(Proveedor proveedor) {
        fornecedorDAO.eliminar(proveedor);
    }

    private void validar(Proveedor proveedor) {
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio.");
        }
    }
}
