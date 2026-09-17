package com.mecanica.controller;

import com.mecanica.dao.RegistroAuditoriaDAO;
import com.mecanica.model.RegistroAuditoria;
import com.mecanica.model.Usuario;
import com.mecanica.util.Moneda;
import com.mecanica.util.Sesion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de actividad: los demas Controllers llaman a registrar(...)
 * despues de cada accion importante que salio bien (pagos, eliminaciones,
 * cierre del mes, pago de salario, cambios de usuarios, inicio de sesion).
 *
 * Grabar el registro NUNCA puede hacer fallar la accion del usuario: si
 * algo sale mal al guardar la linea, solo se imprime el error en la consola.
 */
public class AuditoriaController {

    private static final int LARGO_MAXIMO_DETALLE = 500;

    private final RegistroAuditoriaDAO registroDAO = new RegistroAuditoriaDAO();

    public void registrar(String accion, String detalle) {
        try {
            RegistroAuditoria registro = new RegistroAuditoria();
            registro.setFechaHora(LocalDateTime.now());
            Usuario usuario = Sesion.getUsuario();
            if (usuario != null) {
                registro.setUsuarioLogin(usuario.getLogin());
                registro.setUsuarioNombre(usuario.getNombre());
            }
            registro.setAccion(accion);
            if (detalle != null && detalle.length() > LARGO_MAXIMO_DETALLE) {
                detalle = detalle.substring(0, LARGO_MAXIMO_DETALLE);
            }
            registro.setDetalle(detalle);
            registroDAO.guardar(registro);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /** "Gs. 1.500.000" -- formato de valores para el detalle del registro. */
    public static String gs(BigDecimal valor) {
        return Moneda.formatearConGs(valor);
    }

    public List<RegistroAuditoria> listar(LocalDate desde, LocalDate hasta, String login) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El periodo es invalido.");
        }
        return registroDAO.listar(desde, hasta, login);
    }

    /** true si ya existe un registro con esa accion (para pasos que solo deben ejecutarse una vez, ej. migraciones). */
    public boolean yaSeRegistro(String accion) {
        return registroDAO.existeAccion(accion);
    }
}
