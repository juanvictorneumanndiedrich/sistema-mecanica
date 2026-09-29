package com.mecanica.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente de la mecanica/torneria. El SALDO GENERAL del cliente (no vinculado
 * a una Orden de Servicio especifica) sube cuando se cierra una OS (se suma
 * el valorTotal, ver OrdenDeServicioController.cerrar) y baja cuando el
 * cliente paga (ClienteController.registrarPagamento) -- por eso el saldo
 * esta aca en Cliente, y no en OrdenDeServicio.
 */
public class Cliente {

    private Long id;

    private String nombre;

    /** CI (pessoa fisica) ou RUC (empresa), documento paraguaio. */
    private String documento;

    private String telefono;

    private String direccion;

    /**
     * Saldo general del cliente: positivo = el cliente debe a la mecanica.
     * Sube al cerrar una OS (se suma el valorTotal) y baja con los pagos --
     * en ningun caso queda vinculado a una OS especifica.
     */
    private BigDecimal saldo = BigDecimal.ZERO;

    private List<Maquinario> maquinarios = new ArrayList<>();

    private List<OrdenDeServicio> ordenesDeServicio = new ArrayList<>();

    public Cliente() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public List<Maquinario> getMaquinarios() {
        return maquinarios;
    }

    public void setMaquinarios(List<Maquinario> maquinarios) {
        this.maquinarios = maquinarios;
    }

    public List<OrdenDeServicio> getOrdenesDeServicio() {
        return ordenesDeServicio;
    }

    public void setOrdenesDeServicio(List<OrdenDeServicio> ordenesDeServicio) {
        this.ordenesDeServicio = ordenesDeServicio;
    }
}
