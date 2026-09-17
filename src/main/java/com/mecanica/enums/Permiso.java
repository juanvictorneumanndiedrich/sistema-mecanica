package com.mecanica.enums;

/**
 * Todos los permisos que se le pueden dar a un Usuario. Hay dos clases:
 *
 * <ul>
 * <li>AREAS: una por cada boton del menu lateral. Sin el permiso del area,
 * el boton ni aparece.</li>
 * <li>ACCIONES: permisos mas finos dentro de un area (ver la pestana Socios,
 * pagar salario, eliminar registros, etc.). Sin el permiso, el boton o la
 * pestana correspondiente no aparece, y el Controller tambien rechaza la
 * accion (ver com.mecanica.util.Sesion.exigir).</li>
 * </ul>
 *
 * Cada permiso de accion que pertenece a un area (area() != null) solo vale
 * si el usuario tambien tiene esa area -- en el formulario de usuario la
 * casilla queda deshabilitada mientras el area este desmarcada.
 *
 * El valor de cada permiso se guarda en una columna booleana propia de la
 * tabla usuario (ver Usuario.tiene / Usuario.setPermiso).
 */
public enum Permiso {

    // ---- Areas del menu lateral
    CLIENTES_MAQUINARIOS("Clientes y Maquinarios", Grupo.AREAS),
    ORDENES_SERVICIO("Ordenes de Servicio", Grupo.AREAS),
    COMPRAS_PROVEEDORES("Compras y Proveedores", Grupo.AREAS),
    FINANCIERO("Financiero", Grupo.AREAS),
    EMPLEADOS_SOCIOS("Empleados y Socios", Grupo.AREAS),
    USUARIOS("Usuarios y Permisos (incluye el registro de actividad)", Grupo.AREAS),

    // ---- Dentro de Empleados y Socios
    SOCIOS("Ver la pestana Socios (cadastro y retiros de socio)", Grupo.EMPLEADOS_SOCIOS),
    EDITAR_EMPLEADOS("Crear, editar y eliminar empleados", Grupo.EMPLEADOS_SOCIOS),
    RETIROS_EMPLEADO("Registrar vale / adelanto de empleado", Grupo.EMPLEADOS_SOCIOS),
    PAGAR_SALARIO("Calcular y pagar salario, ver recibos de salario", Grupo.EMPLEADOS_SOCIOS),

    // ---- Dentro de Financiero
    CIERRE_MENSUAL("Ver la pestana Cierre Mensual (cerrar mes e imprimir acerto)", Grupo.FINANCIERO),
    MOVIMIENTO_MANUAL("Registrar movimiento manual (ingreso / egreso)", Grupo.FINANCIERO),

    // ---- Acciones delicadas (valen en cualquier area que el usuario tenga)
    ELIMINAR_REGISTROS("Eliminar clientes, maquinarios, proveedores, notas de compra y ordenes de servicio", Grupo.DELICADAS),
    CANCELAR_OS("Cancelar Ordenes de Servicio", Grupo.DELICADAS),
    RETIRAR_SALDO("Retirar saldo a favor (cliente / proveedor)", Grupo.DELICADAS);

    /** Agrupa los permisos en el formulario de usuario. */
    public enum Grupo {
        AREAS("AREAS DEL MENU"),
        EMPLEADOS_SOCIOS("DENTRO DE EMPLEADOS Y SOCIOS"),
        FINANCIERO("DENTRO DE FINANCIERO"),
        DELICADAS("ACCIONES DELICADAS");

        private final String titulo;

        Grupo(String titulo) {
            this.titulo = titulo;
        }

        public String getTitulo() {
            return titulo;
        }
    }

    private final String etiqueta;
    private final Grupo grupo;

    Permiso(String etiqueta, Grupo grupo) {
        this.etiqueta = etiqueta;
        this.grupo = grupo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public boolean esArea() {
        return grupo == Grupo.AREAS;
    }

    /** Area de la que depende este permiso de accion, o null si no depende de ninguna. */
    public Permiso area() {
        switch (grupo) {
            case EMPLEADOS_SOCIOS:
                return EMPLEADOS_SOCIOS;
            case FINANCIERO:
                return FINANCIERO;
            default:
                return null;
        }
    }
}
