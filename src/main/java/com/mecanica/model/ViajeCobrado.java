package com.mecanica.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parte de VIAJE de una Orden de Servicio que el cliente ya pago. Esta plata
 * NO entra en Financiero: queda anotada aca y se muestra en la pestaña
 * "Viajes" -- ver ViajeController.
 *
 * El viaje de una OS es "todo o nada": mientras la OS no este paga entera
 * (osQuitada = false) la plata queda retenida -- fuera de Financiero y sin
 * aparecer en la pestaña -- y recien cuando la OS se quita aparece, de una
 * sola vez.
 *
 * Si el pago fue con cheque pre-datado, la fila nace con confirmado = false y
 * pasa a true cuando el cheque se confirma (hasta entonces la plata todavia
 * no esta disponible de verdad, igual que en Financiero).
 */
public class ViajeCobrado {

    private Long id;

    private LocalDate fecha;

    private Cliente cliente;

    private OrdenDeServicio ordenDeServicio;

    private BigDecimal valor;

    /** Id del cheque pre-datado con el que se pago (null si fue un pago normal). */
    private Long chequeId;

    private boolean confirmado = true;

    /** Estado editable desde la pestaña Viajes: false = "No pagado", true = "Pagado". */
    private boolean pagado = false;

    /** true cuando la OS ya esta paga entera: recien ahi el viaje aparece en la pestaña. */
    private boolean osQuitada = true;

    public ViajeCobrado() {
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

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public OrdenDeServicio getOrdenDeServicio() {
        return ordenDeServicio;
    }

    public void setOrdenDeServicio(OrdenDeServicio ordenDeServicio) {
        this.ordenDeServicio = ordenDeServicio;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Long getChequeId() {
        return chequeId;
    }

    public void setChequeId(Long chequeId) {
        this.chequeId = chequeId;
    }

    public boolean isPagado() {
        return pagado;
    }

    public void setPagado(boolean pagado) {
        this.pagado = pagado;
    }

    public boolean isOsQuitada() {
        return osQuitada;
    }

    public void setOsQuitada(boolean osQuitada) {
        this.osQuitada = osQuitada;
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public void setConfirmado(boolean confirmado) {
        this.confirmado = confirmado;
    }
}
