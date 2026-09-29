package com.mecanica.model;


/**
 * Uno de los 2 socios de la mecanica. La division de ganancia entre los socios es
 * siempre fija en 50%/50%, por eso NO existe un campo de porcentaje
 * configurable aca -- la regla se aplica en codigo (Controller), no en
 * un dato editable.
 */
public class Socio {

    private Long id;

    private String nombre;

    /** Cedula de identidad (CI), documento paraguaio. */
    private String documento;

    private String telefono;

    private boolean activo = true;

    public Socio() {
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

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
