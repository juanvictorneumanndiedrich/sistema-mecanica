package com.mecanica.model;

import com.mecanica.enums.EstadoCheque;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cheque pre-datado: recibido de un Cliente que nos esta pagando, o
 * entregado por nosotros a un Proveedor. El valor ya descuenta el SALDO
 * GENERAL (del Cliente o del Proveedor, segun el caso) apenas se registra
 * -- ver ChequePreDatadoController.registrarDeCliente/registrarDeProveedor
 * -- pero el efecto real en el flujo de caja (MovimientoFinanciero, pantalla
 * Financiero) queda pendiente hasta que el cheque venza y sea confirmado
 * (ver ChequePreDatadoController.confirmar), porque hasta esa fecha la
 * plata todavia no esta disponible de verdad.
 *
 * Solo una de las referencias (cliente o proveedor) queda completada, de
 * acuerdo con el origen del cheque -- mismo esquema de las referencias
 * opcionales que ya existe en MovimientoFinanciero.
 */
@Entity
@Table(name = "cheque_predatado")
public class ChequePreDatado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "numero_cheque", length = 40)
    private String numeroCheque;

    @Column(length = 80)
    private String banco;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    /** Fecha en la que el cheque puede depositarse/compensar -- el motivo de ser "pre-datado". */
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoCheque estado = EstadoCheque.PENDIENTE;

    @Column(name = "fecha_confirmacion")
    private LocalDate fechaConfirmacion;

    public ChequePreDatado() {
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

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public String getNumeroCheque() {
        return numeroCheque;
    }

    public void setNumeroCheque(String numeroCheque) {
        this.numeroCheque = numeroCheque;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoCheque getEstado() {
        return estado;
    }

    public void setEstado(EstadoCheque estado) {
        this.estado = estado;
    }

    public LocalDate getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void setFechaConfirmacion(LocalDate fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }
}
