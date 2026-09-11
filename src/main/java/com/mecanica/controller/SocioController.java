package com.mecanica.controller;

import com.mecanica.dao.RetiroSocioDAO;
import com.mecanica.dao.SocioDAO;
import com.mecanica.model.RetiroSocio;
import com.mecanica.model.Socio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller de Socio: CRUD basico y el calculo de la LIQUIDACION (division de
 * ganancia). La division entre los 2 socios es siempre 50%/50% fija -- esa regla
 * esta en codigo aca, no en un dato guardado.
 */
public class SocioController {

    private final SocioDAO socioDAO = new SocioDAO();
    private final RetiroSocioDAO retiradaSocioDAO = new RetiroSocioDAO();

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

    /**
     * Calcula la liquidacion del periodo: divide la gananciaTotal en partes iguales
     * entre los socios activos (pensado para los 2 socios fijos, 50/50) y
     * descuenta lo que cada uno ya retiro en el periodo via RetiroSocio.
     * El valor devuelto por socio puede ser negativo si ya retiro
     * mas que su parte.
     */
    public List<ResultadoLiquidacion> calcularLiquidacion(BigDecimal gananciaTotal, LocalDate inicio, LocalDate fin) {
        List<Socio> socios = listarActivos();
        if (socios.isEmpty()) {
            return List.of();
        }
        BigDecimal parte = gananciaTotal.divide(BigDecimal.valueOf(socios.size()), 2, RoundingMode.HALF_UP);

        List<ResultadoLiquidacion> resultado = new ArrayList<>();
        for (Socio socio : socios) {
            List<RetiroSocio> retiradas = retiradaSocioDAO.listarPorSocioYPeriodo(socio, inicio, fin);
            BigDecimal yaRetirado = BigDecimal.ZERO;
            for (RetiroSocio r : retiradas) {
                yaRetirado = yaRetirado.add(r.getValor());
            }
            BigDecimal aReceber = parte.subtract(yaRetirado);
            resultado.add(new ResultadoLiquidacion(socio, parte, yaRetirado, aReceber));
        }
        return resultado;
    }

    private void validar(Socio socio) {
        if (socio.getNombre() == null || socio.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del socio es obligatorio.");
        }
    }

    /** Resultado de la liquidacion de un socio en un periodo. */
    public static class ResultadoLiquidacion {
        private final Socio socio;
        private final BigDecimal parteGanancia;
        private final BigDecimal yaRetirado;
        private final BigDecimal valorARecibir;

        public ResultadoLiquidacion(Socio socio, BigDecimal parteGanancia, BigDecimal yaRetirado, BigDecimal valorARecibir) {
            this.socio = socio;
            this.parteGanancia = parteGanancia;
            this.yaRetirado = yaRetirado;
            this.valorARecibir = valorARecibir;
        }

        public Socio getSocio() {
            return socio;
        }

        public BigDecimal getParteGanancia() {
            return parteGanancia;
        }

        public BigDecimal getYaRetirado() {
            return yaRetirado;
        }

        public BigDecimal getValorARecibir() {
            return valorARecibir;
        }
    }
}
