package com.mecanica.model;

import jakarta.persistence.*;

/**
 * Usuario del sistema (login/clave). Hay 3 usuarios previstos: los 2 socios
 * y el/la secretario(a), pero el registro de Usuario es independiente de las
 * entidades de negocio Socio/Empleado -- es solo control de acceso.
 *
 * Los permisos son individuales por usuario (no fijos por "cargo"),
 * segun lo definido en la fase de pantallas: un booleano por area del sistema.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 60)
    private String login;

    /** Guardar sempre um hash (nunca a clave em texto puro). */
    @Column(nullable = false, length = 255)
    private String clave;

    @Column(nullable = false)
    private boolean activo = true;

    // Permisos individuales, uno por area de la navegacion principal (6 areas)
    @Column(name = "permiso_clientes_equipos", nullable = false)
    private boolean permisoClientesEquipos;

    @Column(name = "permiso_ordenes_servicio", nullable = false)
    private boolean permisoOrdenesServicio;

    @Column(name = "permiso_compras_proveedores", nullable = false)
    private boolean permisoComprasProveedores;

    @Column(name = "permiso_financiero", nullable = false)
    private boolean permisoFinanciero;

    @Column(name = "permiso_empleados_socios", nullable = false)
    private boolean permisoEmpleadosSocios;

    @Column(name = "permiso_usuarios", nullable = false)
    private boolean permisoUsuarios;

    public Usuario() {
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

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isPermisoClientesEquipos() {
        return permisoClientesEquipos;
    }

    public void setPermisoClientesEquipos(boolean permisoClientesEquipos) {
        this.permisoClientesEquipos = permisoClientesEquipos;
    }

    public boolean isPermisoOrdenesServicio() {
        return permisoOrdenesServicio;
    }

    public void setPermisoOrdenesServicio(boolean permisoOrdenesServicio) {
        this.permisoOrdenesServicio = permisoOrdenesServicio;
    }

    public boolean isPermisoComprasProveedores() {
        return permisoComprasProveedores;
    }

    public void setPermisoComprasProveedores(boolean permisoComprasProveedores) {
        this.permisoComprasProveedores = permisoComprasProveedores;
    }

    public boolean isPermisoFinanciero() {
        return permisoFinanciero;
    }

    public void setPermisoFinanciero(boolean permisoFinanciero) {
        this.permisoFinanciero = permisoFinanciero;
    }

    public boolean isPermisoEmpleadosSocios() {
        return permisoEmpleadosSocios;
    }

    public void setPermisoEmpleadosSocios(boolean permisoEmpleadosSocios) {
        this.permisoEmpleadosSocios = permisoEmpleadosSocios;
    }

    public boolean isPermisoUsuarios() {
        return permisoUsuarios;
    }

    public void setPermisoUsuarios(boolean permisoUsuarios) {
        this.permisoUsuarios = permisoUsuarios;
    }
}
