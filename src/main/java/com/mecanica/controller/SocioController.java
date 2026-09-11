package com.mecanica.controller;

import com.mecanica.dao.SocioDAO;
import com.mecanica.dao.RetiradaSocioDAO;
import com.mecanica.model.RetiradaSocio;
import com.mecanica.model.Socio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller de Socio: CRUD basico e o calculo do ACERTO (divisao de
 * lucro). A divisao entre os 2 socios e sempre 50%/50% fixo -- essa regra
 * fica em codigo aqui, nao em dado gravado.
 */
public class SocioController {

    private final SocioDAO socioDAO = new SocioDAO();
    private final RetiradaSocioDAO retiradaSocioDAO = new RetiradaSocioDAO();

    public Socio salvar(Socio socio) {
        validar(socio);
        return socioDAO.salvar(socio);
    }

    public Socio buscarPorId(Long id) {
        return socioDAO.buscarPorId(id);
    }

    public List<Socio> listarTodos() {
        return socioDAO.listarTodos();
    }

    public List<Socio> listarAtivos() {
        return socioDAO.listarAtivos();
    }

    public void excluir(Socio socio) {
        socioDAO.excluir(socio);
    }

    /**
     * Calcula o acerto do periodo: divide o lucroTotal em partes iguais
     * entre os socios ativos (pensado pros 2 socios fixos, 50/50) e
     * desconta o que cada um ja retirou no periodo via RetiradaSocio.
     * O valor retornado por socio pode ser negativo se ele ja retirou
     * mais do que a parte dele.
     */
    public List<ResultadoAcerto> calcularAcerto(BigDecimal lucroTotal, LocalDate inicio, LocalDate fim) {
        List<Socio> socios = listarAtivos();
        if (socios.isEmpty()) {
            return List.of();
        }
        BigDecimal parte = lucroTotal.divide(BigDecimal.valueOf(socios.size()), 2, RoundingMode.HALF_UP);

        List<ResultadoAcerto> resultado = new ArrayList<>();
        for (Socio socio : socios) {
            List<RetiradaSocio> retiradas = retiradaSocioDAO.listarPorSocioEPeriodo(socio, inicio, fim);
            BigDecimal jaRetirado = BigDecimal.ZERO;
            for (RetiradaSocio r : retiradas) {
                jaRetirado = jaRetirado.add(r.getValor());
            }
            BigDecimal aReceber = parte.subtract(jaRetirado);
            resultado.add(new ResultadoAcerto(socio, parte, jaRetirado, aReceber));
        }
        return resultado;
    }

    private void validar(Socio socio) {
        if (socio.getNome() == null || socio.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do socio e obrigatorio.");
        }
    }

    /** Resultado do acerto de um socio num periodo. */
    public static class ResultadoAcerto {
        private final Socio socio;
        private final BigDecimal parteDoLucro;
        private final BigDecimal jaRetirado;
        private final BigDecimal valorAReceber;

        public ResultadoAcerto(Socio socio, BigDecimal parteDoLucro, BigDecimal jaRetirado, BigDecimal valorAReceber) {
            this.socio = socio;
            this.parteDoLucro = parteDoLucro;
            this.jaRetirado = jaRetirado;
            this.valorAReceber = valorAReceber;
        }

        public Socio getSocio() {
            return socio;
        }

        public BigDecimal getParteDoLucro() {
            return parteDoLucro;
        }

        public BigDecimal getJaRetirado() {
            return jaRetirado;
        }

        public BigDecimal getValorAReceber() {
            return valorAReceber;
        }
    }
}