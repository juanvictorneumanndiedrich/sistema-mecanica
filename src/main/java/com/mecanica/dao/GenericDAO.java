package com.mecanica.dao;

import java.util.List;

/**
 * Contrato basico que todo DAO especifico (ClienteDAO, OrdenDeServicioDAO,
 * etc.) debe seguir. Sigue el flujo de capas ya definido en la arquitectura:
 * View -> Controller -> DAO -> Model.
 *
 * @param <T>  tipo de la entidad (ej: Cliente)
 * @param <ID> tipo de la clave primaria de la entidad (ej: Long)
 */
public interface GenericDAO<T, ID> {

    T guardar(T entidad);

    T buscarPorId(ID id);

    List<T> listarTodos();

    void eliminar(T entidad);
}
