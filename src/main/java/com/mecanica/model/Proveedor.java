package com.mecanica.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Proveedor de repuestos/materiales usados en los servicios. El SALDO
 * GENERAL del proveedor (no vinculado a una Compra especifica) sube cuando
 * se cierra una Compra (se suma el valorTotal de la notinha, ver
 * CompraController.cerrar) y baja cuando la mecanica le paga
 * (ProveedorController.registrarPagamento) -- mismo esquema del saldo del
 * Cliente, pero al reves: aca positivo significa que la mecanica debe al
 * proveedor.
 */
@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** RUC do proveedor. */
    @Column(name = "documento", length = 30)
    private String documento;

    @Column(length = 30)
    private String telefono;

    @Column(length = 120)
    private String contacto;

    /**
     * Saldo general del proveedor: positivo = la mecanica le debe. Sube al
     * cerrar una Compra (se suma el valorTotal) y baja con los pagos -- en
     * ningun caso queda vinculado a una Compra especifica.
     */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    public Proveedor() {
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

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }
}
