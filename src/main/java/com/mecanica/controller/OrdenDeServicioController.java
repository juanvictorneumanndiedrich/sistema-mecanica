package com.mecanica.controller;

import com.mecanica.dao.OrdenDeServicioDAO;
import com.mecanica.enums.EstadoOrdenServicio;
import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;
import com.mecanica.model.OrdenDeServicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller de OrdenDeServicio: abrir, consultar y cerrar una OS. La
 * inclusion/eliminacion de items (y el recalculo del valor total) queda en el
 * ItemOrdenServicioController, para mantener la responsabilidad en un solo lugar.
 */
public class OrdenDeServicioController {

    private final OrdenDeServicioDAO ordemDeServicoDAO = new OrdenDeServicioDAO();

    /** Abre una nueva OS, generando el proximo numero secuencial automaticamente. */
    public OrdenDeServicio abrir(Cliente cliente, Maquinario maquinario, String problemaReportado) {
        if (cliente == null || maquinario == null) {
            throw new IllegalArgumentException("El cliente y el maquinario son obligatorios para abrir una OS.");
        }
        Long mayorNumero = ordemDeServicoDAO.buscarMayorNumero();

        OrdenDeServicio os = new OrdenDeServicio();
        os.setNumero(mayorNumero == null ? 1L : mayorNumero + 1);
        os.setCliente(cliente);
        os.setMaquinario(maquinario);
        os.setFechaApertura(LocalDate.now());
        os.setEstado(EstadoOrdenServicio.ABIERTA);
        os.setProblemaReportado(problemaReportado);
        os.setValorTotal(BigDecimal.ZERO);
        return ordemDeServicoDAO.guardar(os);
    }

    /** Marca la OS como concluida y registra la fecha de cierre (para la impresion/firma del cliente). */
    public OrdenDeServicio cerrar(OrdenDeServicio os) {
        os.setEstado(EstadoOrdenServicio.CONCLUIDA);
        os.setFechaCierre(LocalDate.now());
        return ordemDeServicoDAO.guardar(os);
    }

    public OrdenDeServicio cancelar(OrdenDeServicio os) {
        os.setEstado(EstadoOrdenServicio.CANCELADA);
        os.setFechaCierre(LocalDate.now());
        return ordemDeServicoDAO.guardar(os);
    }

    public OrdenDeServicio buscarPorId(Long id) {
        return ordemDeServicoDAO.buscarPorId(id);
    }

    public OrdenDeServicio buscarPorNumero(Long numero) {
        return ordemDeServicoDAO.buscarPorNumero(numero);
    }

    public List<OrdenDeServicio> listarTodos() {
        return ordemDeServicoDAO.listarTodos();
    }

    public List<OrdenDeServicio> listarPorEstado(EstadoOrdenServicio estado) {
        return ordemDeServicoDAO.listarPorEstado(estado);
    }

    public List<OrdenDeServicio> listarPorCliente(Cliente cliente) {
        return ordemDeServicoDAO.listarPorCliente(cliente);
    }

    public void eliminar(OrdenDeServicio os) {
        ordemDeServicoDAO.eliminar(os);
    }
}
