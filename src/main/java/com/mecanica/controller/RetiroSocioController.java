package com.mecanica.controller;

import com.mecanica.dao.RetiroSocioDAO;
import com.mecanica.enums.Permiso;
import com.mecanica.model.RetiroSocio;
import com.mecanica.model.Socio;
import com.mecanica.util.Sesion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de RetiroSocio. IMPORTANTE: registrar un retiro de
 * socio NO genera MovimientoFinanciero (no se trata como gasto) -- solo
 * queda registrado aca, para ser descontado de la parte del socio en el
 * proximo Cierre Mensual (ver CierreMensualController).
 */
public class RetiroSocioController {

    private final RetiroSocioDAO retiradaSocioDAO = new RetiroSocioDAO();
    private final AuditoriaController auditoria = new AuditoriaController();

    public RetiroSocio registrarRetirada(Socio socio, BigDecimal valor, LocalDate fecha, String observacion) {
        Sesion.exigir(Permiso.SOCIOS);
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del retiro debe ser mayor que cero.");
        }
        RetiroSocio retiro = new RetiroSocio();
        retiro.setSocio(socio);
        retiro.setValor(valor);
        retiro.setFecha(fecha != null ? fecha : LocalDate.now());
        retiro.setObservacion(observacion);
        RetiroSocio guardado = retiradaSocioDAO.guardar(retiro);
        auditoria.registrar("RETIRO DE SOCIO", socio.getNombre() + " - " + AuditoriaController.gs(valor)
                + (observacion == null || observacion.isBlank() ? "" : " - " + observacion));
        return guardado;
    }

    public List<RetiroSocio> listarPorSocioYPeriodo(Socio socio, LocalDate inicio, LocalDate fin) {
        return retiradaSocioDAO.listarPorSocioYPeriodo(socio, inicio, fin);
    }

    public void eliminar(RetiroSocio retiro) {
        retiradaSocioDAO.eliminar(retiro);
    }
}
