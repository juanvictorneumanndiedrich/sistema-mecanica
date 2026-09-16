package com.mecanica.controller;

import com.mecanica.dao.SocioDAO;
import com.mecanica.model.Socio;

import java.util.List;

/**
 * Controller de Socio: CRUD basico. El calculo de la LIQUIDACION (division de
 * ganancia 50%/50%, siempre fija entre los 2 socios) NO vive aca -- a partir
 * del 2026-09-16 vive solo en CierreMensualController, para no repetir la
 * misma cuenta de dos formas distintas (esta clase llego a tener un
 * calcularLiquidacion() con ganancia tipeada a mano y descuento por rango de
 * fechas -- removido a pedido del usuario, para que la pestana "Socios" sea
 * solo cadastro + registro de retiro, y la division de ganancia quede solo
 * en la pestana "Cierre Mensual" de Financiero).
 */
public class SocioController {

    private final SocioDAO socioDAO = new SocioDAO();

    public Socio guardar(Socio socio) {
        validar(socio);
        return socioDAO.guardar(socio);
    }

    public Socio buscarPorId(Long id) {
        return socioDAO.buscarPorId(id);
    }

    public List<Socio> listarTodos() {
        return socioDAO.listarTodos();
    }

    public List<Socio> listarActivos() {
        return socioDAO.listarActivos();
    }

    public void eliminar(Socio socio) {
        socioDAO.eliminar(socio);
    }

    private void validar(Socio socio) {
        if (socio.getNombre() == null || socio.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del socio es obligatorio.");
        }
    }
}
