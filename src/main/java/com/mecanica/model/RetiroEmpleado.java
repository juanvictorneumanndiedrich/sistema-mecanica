package com.mecanica.model;

import com.mecanica.enums.TipoRetiroEmpleado;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Retiro cargado para un empleado (vale semanal opcional de valor
 * fijo, o adelanto). El cierre mensual del empleado se calcula
 * sumando los retiros del periodo -- no hay una entidad separada para eso.
 */
@Entity
@Table(name = "retiro_empleado")
public class RetiroEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRetiroEmpleado tipo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(length = 200)
    private String observacion;

    public RetiroEmpleado() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public TipoRetiroEmpleado getTipo() {
        return tipo;
    }

    public void setTipo(TipoRetiroEmpleado tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
