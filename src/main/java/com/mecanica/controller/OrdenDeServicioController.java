package com.mecanica.controller;

import com.mecanica.dao.OrdenDeServicioDAO;
import com.mecanica.enums.EstadoOrdenServicio;
import com.mecanica.enums.Permiso;
import com.mecanica.model.Cliente;
import com.mecanica.model.Maquinario;
import com.mecanica.model.OrdenDeServicio;
import com.mecanica.util.Errores;
import com.mecanica.util.HibernateUtil;
import com.mecanica.util.Sesion;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
    private final AuditoriaController auditoria = new AuditoriaController();

    /**
     * Abre una nueva OS, generando el proximo numero secuencial automaticamente.
     * El maquinario es opcional -- una OS puede ser un "servicio general" sin
     * maquinario especifico (ej: cortar chapa, sacar un tornillo).
     */
    public OrdenDeServicio abrir(Cliente cliente, Maquinario maquinario, String problemaReportado) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio para abrir una OS.");
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

    /**
     * Marca la OS como concluida, registra la fecha de cierre (para la
     * impresion/firma del cliente) y SUMA el valorTotal de la OS al saldo
     * general del cliente -- recien en el cierre, porque es cuando el valor
     * total ya quedo definitivo (mientras la OS esta abierta se le pueden
     * seguir agregando/quitando items). Las dos operaciones ocurren en la
     * misma transaccion, para nunca cerrar la OS sin actualizar el saldo (o
     * vice-versa).
     */
    public OrdenDeServicio cerrar(OrdenDeServicio os) {
        if (os.getEstado() == EstadoOrdenServicio.CONCLUIDA || os.getEstado() == EstadoOrdenServicio.CANCELADA) {
            throw new IllegalStateException("Esa OS ya esta cerrada o cancelada.");
        }

        Transaction tx = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            tx = session.beginTransaction();

            OrdenDeServicio osGerenciada = session.get(OrdenDeServicio.class, os.getId());
            osGerenciada.setEstado(EstadoOrdenServicio.CONCLUIDA);
            osGerenciada.setFechaCierre(LocalDate.now());
            session.merge(osGerenciada);

            Cliente clienteGerenciado = session.get(Cliente.class, osGerenciada.getCliente().getId());
            clienteGerenciado.setSaldo(clienteGerenciado.getSaldo().add(osGerenciada.getValorTotal()));
            session.merge(clienteGerenciado);

            tx.commit();
            auditoria.registrar("OS CERRADA", "OS Nº " + osGerenciada.getNumero() + " - "
                    + clienteGerenciado.getNombre() + " - " + AuditoriaController.gs(osGerenciada.getValorTotal()));
            return osGerenciada;
        } catch (RuntimeException e) {
            Errores.revertir(tx);
            throw Errores.traducir(e);
        } finally {
            session.close();
        }
    }

    public OrdenDeServicio cancelar(OrdenDeServicio os) {
        Sesion.exigir(Permiso.CANCELAR_OS);
        os.setEstado(EstadoOrdenServicio.CANCELADA);
        os.setFechaCierre(LocalDate.now());
        OrdenDeServicio guardada = ordemDeServicoDAO.guardar(os);
        auditoria.registrar("OS CANCELADA", "OS Nº " + os.getNumero()
                + (os.getCliente() == null ? "" : " - " + os.getCliente().getNombre()));
        return guardada;
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
