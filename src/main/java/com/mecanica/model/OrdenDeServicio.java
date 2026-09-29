package com.mecanica.model;

import com.mecanica.enums.EstadoOrdenServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de Servicio (OS). La impresion de la OS genera solo la via fisica para
 * la firma del cliente -- el sistema no guarda ninguna firma.
 */
public class OrdenDeServicio {

    private Long id;

    /** Numero sequencial exibido na via impressa. */
    private Long numero;

    private Cliente cliente;

    /**
     * Opcional: hay servicios simples (cortar un pedazo de chapa, sacar un
     * tornillo, enderezar algo) que no estan ligados a ningun maquinario
     * especifico del cliente -- en ese caso este campo queda en null y la OS
     * se trata como "servicio general".
     */
    private Maquinario maquinario;

    private LocalDate fechaApertura;

    private LocalDate fechaCierre;

    private EstadoOrdenServicio estado = EstadoOrdenServicio.ABIERTA;

    private String problemaReportado;

    private BigDecimal valorTotal = BigDecimal.ZERO;

    private List<ItemOrdenServicio> items = new ArrayList<>();

    public OrdenDeServicio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Maquinario getMaquinario() {
        return maquinario;
    }

    public void setMaquinario(Maquinario maquinario) {
        this.maquinario = maquinario;
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDate fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public EstadoOrdenServicio getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenServicio estado) {
        this.estado = estado;
    }

    public String getProblemaReportado() {
        return problemaReportado;
    }

    public void setProblemaReportado(String problemaReportado) {
        this.problemaReportado = problemaReportado;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public List<ItemOrdenServicio> getItems() {
        return items;
    }

    public void setItems(List<ItemOrdenServicio> items) {
        this.items = items;
    }
}
