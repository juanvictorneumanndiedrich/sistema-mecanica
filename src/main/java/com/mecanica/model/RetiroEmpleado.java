package com.mecanica.model;

import com.mecanica.enums.TipoRetiroEmpleado;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Retiro cargado para un empleado (vale semanal opcional de valor
 * fijo, o adelanto). NO genera gasto en el momento -- solo sirve como
 * descuento a aplicar sobre el salario base, en el pago mensual real
 * (ver EmpleadoController.pagarSalario). El campo liquidado marca que
 * ese retiro ya fue usado/descontado en un pago de salario, para que no
 * se cuente de nuevo en el mes siguiente.
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

    @Column(nullable = false)
    private boolean liquidado = false;

    private LocalDate fechaLiquidacion;

    /**
     * Pago de salario (MovimientoFinanciero SALARIO_EMPLEADO) en el que este
     * retiro fue descontado -- null mientras no fue liquidado. Sirve para
     * reimprimir el recibo de un pago antiguo con sus descuentos exactos
     * (ver EmpleadoController.pagarSalario y ReporteController.reciboSalario).
     */
    @ManyToOne
    @JoinColumn(name = "pago_salario_id")
    private MovimientoFinanciero pagoSalario;

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

    public boolean isLiquidado() {
        return liquidado;
    }

    public void setLiquidado(boolean liquidado) {
        this.liquidado = liquidado;
    }

    public LocalDate getFechaLiquidacion() {
        return fechaLiquidacion;
    }

    public void setFechaLiquidacion(LocalDate fechaLiquidacion) {
        this.fechaLiquidacion = fechaLiquidacion;
    }

    public MovimientoFinanciero getPagoSalario() {
        return pagoSalario;
    }

    public void setPagoSalario(MovimientoFinanciero pagoSalario) {
        this.pagoSalario = pagoSalario;
    }
}
