package com.mecanica.model;

import com.mecanica.enums.EstadoOrdenServicio;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de Servicio (OS). La impresion de la OS genera solo la via fisica para
 * la firma del cliente -- el sistema no guarda ninguna firma.
 */
@Entity
@Table(name = "orden_de_servicio")
public class OrdenDeServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero sequencial exibido na via impressa. */
    @Column(nullable = false, unique = true)
    private Long numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDate fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrdenServicio estado = EstadoOrdenServicio.ABIERTA;

    @Column(name = "problema_reportado", length = 500)
    private String problemaReportado;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "ordenDeServicio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrdenServicio> items = new ArrayList<>();

    public OrdenDeServicio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public void setEquipo(Equipo equipo) {
        this.equipo = equipo;
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDate fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public EstadoOrdenServicio getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenServicio estado) {
        this.estado = estado;
    }

    public String getProblemaReportado() {
        return problemaReportado;
    }

    public void setProblemaReportado(String problemaReportado) {
        this.problemaReportado = problemaReportado;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public List<ItemOrdenServicio> getItems() {
        return items;
    }

    public void setItems(List<ItemOrdenServicio> items) {
        this.items = items;
    }
}
