package com.mecanica.model;

import com.mecanica.enums.TipoMaquinario;

/**
 * Maquinario del cliente (camion, tractor, cosechadora, implemento
 * agricola, etc.) que pasa por la mecanica/torneria.
 */
public class Maquinario {

    private Long id;

    private Cliente cliente;

    private TipoMaquinario tipo;

    private String marca;

    private String modelo;

    /** Placa (caminhoes) ou outro numero de identificacion (maquinas sem placa). */
    private String identificacion;

    private String observacion;

    public Maquinario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public TipoMaquinario getTipo() {
        return tipo;
    }

    public void setTipo(TipoMaquinario tipo) {
        this.tipo = tipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
