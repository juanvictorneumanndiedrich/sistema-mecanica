package com.mecanica.model;

import com.mecanica.enums.CategoriaMovimientoFinanciero;
import com.mecanica.enums.TipoMovimientoFinanciero;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimiento en el flujo de caja general (pantalla Financiero). Pago de
 * cliente, compra de proveedor y retiro de empleado generan un
 * registro aca, para dar una vision consolidada de entradas y salidas -- las
 * referencias de abajo son opcionales y apuntan al origen del movimiento,
 * cuando exista.
 *
 * IMPORTANTE: el retiro de socio NO genera MovimientoFinanciero. No se
 * trata como gasto -- solo se descuenta de la parte de ese socio en la
 * liquidacion (division de ganancia 50/50), controlada solo con la entidad
 * RetiroSocio. Por eso aca no hay una referencia a Socio.
 *
 * El campo cierre marca a que CierreMensual entro este movimiento (null =
 * todavia pendiente, no entro en ningun cierre). El usuario elige con
 * checkbox, en la pestaña "Cierre Mensual" de Financiero, cuales movimientos
 * entran en cada cierre -- no es necesariamente por fecha calendario, para
 * poder dejar algo para el cierre siguiente o traer algo de uno anterior.
 */
@Entity
@Table(name = "movimiento_financiero")
public class MovimientoFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimientoFinanciero tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaMovimientoFinanciero categoria;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(length = 200)
    private String descripcion;

    // Referencias opcionales al origen del movimiento (solo una de ellas
    // queda completada, de acuerdo con la categoria).
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    @ManyToOne
    @JoinColumn(name = "cierre_id")
    private CierreMensual cierre;

    public MovimientoFinanciero() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public TipoMovimientoFinanciero getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimientoFinanciero tipo) {
        this.tipo = tipo;
    }

    public CategoriaMovimientoFinanciero getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaMovimientoFinanciero categoria) {
        this.categoria = categoria;
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

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public CierreMensual getCierre() {
        return cierre;
    }

    public void setCierre(CierreMensual cierre) {
        this.cierre = cierre;
    }
}
