package com.mecanica.controller;

import com.mecanica.dao.CierreMensualDAO;
import com.mecanica.dao.CierreSocioDetalleDAO;
import com.mecanica.dao.MovimientoFinancieroDAO;
import com.mecanica.dao.RetiroSocioDAO;
import com.mecanica.dao.SocioDAO;
import com.mecanica.enums.Permiso;
import com.mecanica.enums.TipoMovimientoFinanciero;
import com.mecanica.model.CierreMensual;
import com.mecanica.model.CierreSocioDetalle;
import com.mecanica.model.MovimientoFinanciero;
import com.mecanica.model.RetiroSocio;
import com.mecanica.model.Socio;
import com.mecanica.util.BD;
import com.mecanica.util.Sesion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller del cierre mensual del negocio. Junta los MovimientoFinanciero
 * que el usuario eligio con checkbox (ver FinancieroPanel, pestaña "Cierre
 * Mensual") -- no necesariamente los de un mes calendario exacto, para
 * poder dejar algo para el cierre siguiente o traer algo de uno anterior --,
 * calcula la ganancia (entradas - salidas) y reparte 50/50 entre los socios
 * activos, descontando TODOS los retiros de socio que todavia no entraron
 * en un cierre anterior (no se usa un rango de fechas para el retiro, para
 * no repetir el mismo problema que tenia el retiro de empleado: un retiro
 * contado dos veces en dos cierres distintos).
 *
 * Todo queda grabado como un registro permanente (CierreMensual +
 * CierreSocioDetalle por socio), y los movimientos/retiros usados quedan
 * vinculados a este cierre para no volver a aparecer como pendientes.
 */
public class CierreMensualController {

    private final CierreMensualDAO cierreDAO = new CierreMensualDAO();
    private final SocioDAO socioDAO = new SocioDAO();
    private final CierreSocioDetalleDAO detalleDAO = new CierreSocioDetalleDAO();
    private final RetiroSocioDAO retiroSocioDAO = new RetiroSocioDAO();
    private final MovimientoFinancieroDAO movimientoDAO = new MovimientoFinancieroDAO();
    private final AuditoriaController auditoria = new AuditoriaController();

    /** Historico de cierres ya hechos, del mas reciente al mas antiguo. */
    public List<CierreMensual> listarHistorico() {
        return cierreDAO.listarOrdenados();
    }

    public CierreMensual cerrar(List<MovimientoFinanciero> seleccionados, String descripcion) {
        Sesion.exigir(Permiso.CIERRE_MENSUAL);
        if (seleccionados == null || seleccionados.isEmpty()) {
            throw new IllegalArgumentException("Seleccione al menos un movimiento para el cierre.");
        }
        List<Socio> socios = socioDAO.listarActivos();
        if (socios.isEmpty()) {
            throw new IllegalArgumentException("No hay socios activos para repartir la ganancia.");
        }

        BigDecimal totalEntradas = BigDecimal.ZERO;
        BigDecimal totalSalidas = BigDecimal.ZERO;
        for (MovimientoFinanciero m : seleccionados) {
            if (m.getTipo() == TipoMovimientoFinanciero.ENTRADA) {
                totalEntradas = totalEntradas.add(m.getValor());
            } else {
                totalSalidas = totalSalidas.add(m.getValor());
            }
        }
        BigDecimal gananciaTotal = totalEntradas.subtract(totalSalidas);
        BigDecimal parte = gananciaTotal.divide(BigDecimal.valueOf(socios.size()), 2, RoundingMode.HALF_UP);

        BigDecimal entradasDelCierre = totalEntradas;
        BigDecimal salidasDelCierre = totalSalidas;
        CierreMensual cierre = BD.transaccion(conexion -> {
            CierreMensual nuevo = new CierreMensual();
            nuevo.setFechaCierre(LocalDate.now());
            nuevo.setDescripcion(descripcion);
            nuevo.setTotalEntradas(entradasDelCierre);
            nuevo.setTotalSalidas(salidasDelCierre);
            nuevo.setGananciaTotal(gananciaTotal);
            cierreDAO.guardar(conexion, nuevo);

            for (Socio socio : socios) {
                List<RetiroSocio> retiros = retiroSocioDAO.listarSinCierre(conexion, socio);
                BigDecimal yaRetirado = BigDecimal.ZERO;
                for (RetiroSocio r : retiros) {
                    yaRetirado = yaRetirado.add(r.getValor());
                    r.setCierre(nuevo);
                    retiroSocioDAO.marcarCierre(conexion, r.getId(), nuevo.getId());
                }
                BigDecimal aRecibir = parte.subtract(yaRetirado);

                CierreSocioDetalle detalle = new CierreSocioDetalle();
                detalle.setCierre(nuevo);
                detalle.setSocio(socio);
                detalle.setParteGanancia(parte);
                detalle.setYaRetirado(yaRetirado);
                detalle.setValorARecibir(aRecibir);
                detalleDAO.guardar(conexion, detalle);
                nuevo.getDetalles().add(detalle);
            }

            for (MovimientoFinanciero m : seleccionados) {
                m.setCierre(nuevo);
                movimientoDAO.marcarCierre(conexion, m.getId(), nuevo.getId());
            }
            return nuevo;
        });
        auditoria.registrar("CIERRE MENSUAL", (descripcion == null || descripcion.isBlank() ? "" : descripcion + " - ")
                + seleccionados.size() + " movimientos - ganancia " + AuditoriaController.gs(gananciaTotal));
        return cierre;
    }
}
