package com.mecanica.model;

import com.mecanica.enums.TipoEquipo;
import jakarta.persistence.*;

/**
 * Equipo del cliente (camion, tractor, cosechadora, implemento
 * agricola, etc.) que pasa por la mecanica/torneria.
 */
@Entity
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEquipo tipo;

    @Column(length = 60)
    private String marca;

    @Column(length = 60)
    private String modelo;

    /** Placa (caminhoes) ou outro numero de identificacion (maquinas sem placa). */
    @Column(name = "identificacion", length = 30)
    private String identificacion;

    @Column(length = 200)
    private String observacion;

    public Equipo() {
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

    public TipoEquipo getTipo() {
        return tipo;
    }

    public void setTipo(TipoEquipo tipo) {
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
