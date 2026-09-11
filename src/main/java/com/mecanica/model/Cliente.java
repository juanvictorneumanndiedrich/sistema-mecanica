package com.mecanica.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente de la mecanica/torneria. El pago del cliente descuenta su SALDO
 * GENERAL (no una Orden de Servicio especifica) -- por eso el saldo
 * esta aca en Cliente, y no en OrdenDeServicio.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** CI (pessoa fisica) ou RUC (empresa), documento paraguaio. */
    @Column(name = "documento", length = 30)
    private String documento;

    @Column(length = 30)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    /**
     * Saldo general del cliente: positivo = el cliente debe a la mecanica.
     * Se descuenta directamente con los pagos, sin vincular a una OS.
     */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Equipo> equipos = new ArrayList<>();

    @OneToMany(mappedBy = "cliente")
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

    public List<Equipo> getEquipos() {
        return equipos;
    }

    public void setEquipos(List<Equipo> equipos) {
        this.equipos = equipos;
    }

    public List<OrdenDeServicio> getOrdenesDeServicio() {
        return ordenesDeServicio;
    }

    public void setOrdenesDeServicio(List<OrdenDeServicio> ordenesDeServicio) {
        this.ordenesDeServicio = ordenesDeServicio;
    }
}
