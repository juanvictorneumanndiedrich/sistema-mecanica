package com.mecanica.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Empleado de la mecanica (ej: soldador, tornero, ayudante).
 * El cierre mensual (recibo con vales, adelantos, total descontado
 * y valor liquido) se calcula a partir de los RetiroEmpleado cargados
 * en el periodo -- por eso no existe una entidad "CierreEmpleado"
 * separada, a diferencia de lo que pasa con el proveedor.
 */
public class Empleado {

    private Long id;

    private String nombre;

    private String documento;

    private String telefono;

    private String cargo;

    private BigDecimal salarioBase;

    private LocalDate fechaAdmision;

    private boolean activo = true;

    public Empleado() {
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

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    public LocalDate getFechaAdmision() {
        return fechaAdmision;
    }

    public void setFechaAdmision(LocalDate fechaAdmision) {
        this.fechaAdmision = fechaAdmision;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
