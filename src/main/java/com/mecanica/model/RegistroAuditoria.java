package com.mecanica.model;


import java.time.LocalDateTime;

/**
 * Una linea del registro de actividad: quien hizo que y cuando (pagos,
 * eliminaciones, cierre del mes, pago de salario, cambios de usuarios...).
 *
 * Guarda el login y el nombre del usuario como texto, y no una referencia a
 * Usuario, a proposito: si un usuario se elimina despues, su historial tiene
 * que seguir existiendo tal cual.
 */
public class RegistroAuditoria {

    private Long id;

    private LocalDateTime fechaHora;

    private String usuarioLogin;

    private String usuarioNombre;

    private String accion;

    private String detalle;

    public RegistroAuditoria() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
