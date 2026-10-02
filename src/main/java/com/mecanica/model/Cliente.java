package com.mecanica.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente de la mecanica/torneria. El SALDO GENERAL del cliente (no vinculado
 * a una Orden de Servicio especifica) sube cuando se cierra una OS (se suma
 * el valorTotal, ver OrdenDeServicioController.cerrar) y baja cuando el
 * cliente paga (ClienteController.registrarPagamento) -- por eso el saldo
 * esta aca en Cliente, y no en OrdenDeServicio.
 */
public class Cliente {

    private Long id;

    private String nombre;

    /** CI (pessoa fisica) ou RUC (empresa), documento paraguaio. */
    private String documento;

    private String telefono;

    private String direccion;

    /**
     * Saldo general del cliente: positivo = el cliente debe a la mecanica.
     * Sube al cerrar una OS (se suma el valorTotal) y baja con los pagos --
     * en ningun caso queda vinculado a una OS especifica.
     */
    private BigDecimal saldo = BigDecimal.ZERO;

    // Hasta tres alias (nombres alternativos) para encontrar al cliente en las busquedas. Opcionales.
    private String alias1;

    private String alias2;

    private String alias3;

    private List<Maquinario> maquinarios = new ArrayList<>();

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

    public String getAlias1() {
        return alias1;
    }

    public void setAlias1(String alias1) {
        this.alias1 = alias1;
    }

    public String getAlias2() {
        return alias2;
    }

    public void setAlias2(String alias2) {
        this.alias2 = alias2;
    }

    public String getAlias3() {
        return alias3;
    }

    public void setAlias3(String alias3) {
        this.alias3 = alias3;
    }

    /** Los alias cargados, separados por coma ("" si no tiene ninguno). */
    public String getAliasTexto() {
        StringBuilder texto = new StringBuilder();
        for (String alias : new String[] {alias1, alias2, alias3}) {
            if (alias != null && !alias.isBlank()) {
                texto.append(texto.length() == 0 ? "" : ", ").append(alias.trim());
            }
        }
        return texto.toString();
    }

    public List<Maquinario> getMaquinarios() {
        return maquinarios;
    }

    public void setMaquinarios(List<Maquinario> maquinarios) {
        this.maquinarios = maquinarios;
    }

    public List<OrdenDeServicio> getOrdenesDeServicio() {
        return ordenesDeServicio;
    }

    public void setOrdenesDeServicio(List<OrdenDeServicio> ordenesDeServicio) {
        this.ordenesDeServicio = ordenesDeServicio;
    }
}
