package com.mecanica.model;

import com.mecanica.enums.Permiso;

/**
 * Usuario del sistema (login/clave). Hay 3 usuarios previstos: los 2 socios
 * y el/la secretario(a), pero el registro de Usuario es independiente de las
 * entidades de negocio Socio/Empleado -- es solo control de acceso.
 *
 * Los permisos son individuales por usuario (no fijos por "cargo"),
 * segun lo definido en la fase de pantallas: un booleano por area del sistema.
 */
public class Usuario {

    private Long id;

    private String nombre;

    private String login;

    /** Guardar sempre um hash (nunca a clave em texto puro). */
    private String clave;

    private boolean activo = true;

    // Permisos individuales, uno por area de la navegacion principal (6 areas)
    private boolean permisoClientesMaquinarios;

    private boolean permisoOrdenesServicio;

    private boolean permisoComprasProveedores;

    private boolean permisoFinanciero;

    private boolean permisoEmpleadosSocios;

    private boolean permisoUsuarios;

    // Permisos de accion, dentro de las areas (ver enums.Permiso). Agregados
    // el 2026-09-17: en la base estas columnas tienen "default false", asi los
    // usuarios que ya existian quedaron todos en false.
    private boolean permisoSocios;

    private boolean permisoEditarEmpleados;

    private boolean permisoRetirosEmpleado;

    private boolean permisoPagarSalario;

    private boolean permisoCierreMensual;

    private boolean permisoMovimientoManual;

    private boolean permisoEliminarRegistros;

    private boolean permisoCancelarOs;

    private boolean permisoRetirarSaldo;

    /** Ayuda en pantalla (boton "?", explicaciones al pasar el mouse): cada usuario la prende o la apaga. */
    private boolean mostrarAyuda = true;

    /** true cuando al usuario ya se le ofrecio el recorrido de bienvenida (para no ofrecerlo en cada ingreso). */
    private boolean tourVisto;


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

    public boolean isPermisoClientesMaquinarios() {
        return permisoClientesMaquinarios;
    }

    public void setPermisoClientesMaquinarios(boolean permisoClientesMaquinarios) {
        this.permisoClientesMaquinarios = permisoClientesMaquinarios;
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

    public boolean isPermisoSocios() {
        return permisoSocios;
    }

    public void setPermisoSocios(boolean permisoSocios) {
        this.permisoSocios = permisoSocios;
    }

    public boolean isPermisoEditarEmpleados() {
        return permisoEditarEmpleados;
    }

    public void setPermisoEditarEmpleados(boolean permisoEditarEmpleados) {
        this.permisoEditarEmpleados = permisoEditarEmpleados;
    }

    public boolean isPermisoRetirosEmpleado() {
        return permisoRetirosEmpleado;
    }

    public void setPermisoRetirosEmpleado(boolean permisoRetirosEmpleado) {
        this.permisoRetirosEmpleado = permisoRetirosEmpleado;
    }

    public boolean isPermisoPagarSalario() {
        return permisoPagarSalario;
    }

    public void setPermisoPagarSalario(boolean permisoPagarSalario) {
        this.permisoPagarSalario = permisoPagarSalario;
    }

    public boolean isPermisoCierreMensual() {
        return permisoCierreMensual;
    }

    public void setPermisoCierreMensual(boolean permisoCierreMensual) {
        this.permisoCierreMensual = permisoCierreMensual;
    }

    public boolean isPermisoMovimientoManual() {
        return permisoMovimientoManual;
    }

    public void setPermisoMovimientoManual(boolean permisoMovimientoManual) {
        this.permisoMovimientoManual = permisoMovimientoManual;
    }

    public boolean isPermisoEliminarRegistros() {
        return permisoEliminarRegistros;
    }

    public void setPermisoEliminarRegistros(boolean permisoEliminarRegistros) {
        this.permisoEliminarRegistros = permisoEliminarRegistros;
    }

    public boolean isPermisoCancelarOs() {
        return permisoCancelarOs;
    }

    public void setPermisoCancelarOs(boolean permisoCancelarOs) {
        this.permisoCancelarOs = permisoCancelarOs;
    }

    public boolean isPermisoRetirarSaldo() {
        return permisoRetirarSaldo;
    }

    public void setPermisoRetirarSaldo(boolean permisoRetirarSaldo) {
        this.permisoRetirarSaldo = permisoRetirarSaldo;
    }

    public boolean isMostrarAyuda() {
        return mostrarAyuda;
    }

    public void setMostrarAyuda(boolean mostrarAyuda) {
        this.mostrarAyuda = mostrarAyuda;
    }

    public boolean isTourVisto() {
        return tourVisto;
    }

    public void setTourVisto(boolean tourVisto) {
        this.tourVisto = tourVisto;
    }

    /** Valor guardado del permiso (sin mirar el area de la que depende -- para eso, Sesion.tiene). */
    public boolean tiene(Permiso permiso) {
        switch (permiso) {
            case CLIENTES_MAQUINARIOS: return permisoClientesMaquinarios;
            case ORDENES_SERVICIO: return permisoOrdenesServicio;
            case COMPRAS_PROVEEDORES: return permisoComprasProveedores;
            case FINANCIERO: return permisoFinanciero;
            case EMPLEADOS_SOCIOS: return permisoEmpleadosSocios;
            case USUARIOS: return permisoUsuarios;
            case SOCIOS: return permisoSocios;
            case EDITAR_EMPLEADOS: return permisoEditarEmpleados;
            case RETIROS_EMPLEADO: return permisoRetirosEmpleado;
            case PAGAR_SALARIO: return permisoPagarSalario;
            case CIERRE_MENSUAL: return permisoCierreMensual;
            case MOVIMIENTO_MANUAL: return permisoMovimientoManual;
            case ELIMINAR_REGISTROS: return permisoEliminarRegistros;
            case CANCELAR_OS: return permisoCancelarOs;
            case RETIRAR_SALDO: return permisoRetirarSaldo;
            default: return false;
        }
    }

    public void setPermiso(Permiso permiso, boolean valor) {
        switch (permiso) {
            case CLIENTES_MAQUINARIOS: permisoClientesMaquinarios = valor; break;
            case ORDENES_SERVICIO: permisoOrdenesServicio = valor; break;
            case COMPRAS_PROVEEDORES: permisoComprasProveedores = valor; break;
            case FINANCIERO: permisoFinanciero = valor; break;
            case EMPLEADOS_SOCIOS: permisoEmpleadosSocios = valor; break;
            case USUARIOS: permisoUsuarios = valor; break;
            case SOCIOS: permisoSocios = valor; break;
            case EDITAR_EMPLEADOS: permisoEditarEmpleados = valor; break;
            case RETIROS_EMPLEADO: permisoRetirosEmpleado = valor; break;
            case PAGAR_SALARIO: permisoPagarSalario = valor; break;
            case CIERRE_MENSUAL: permisoCierreMensual = valor; break;
            case MOVIMIENTO_MANUAL: permisoMovimientoManual = valor; break;
            case ELIMINAR_REGISTROS: permisoEliminarRegistros = valor; break;
            case CANCELAR_OS: permisoCancelarOs = valor; break;
            case RETIRAR_SALDO: permisoRetirarSaldo = valor; break;
            default: break;
        }
    }
}
