package com.sgcv.dao;

import com.sgcv.model.Articulo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de acceso a datos para la entidad Articulo del dominio avícola.
 */
public interface IArticuloDAO {

    /**
     * Recupera un artículo por su identificador primario.
     * @param idArticulo ID único
     * @return instancia especializada (ArticuloPromocional o ArticuloEstandar) o null si no existe.
     * @throws SQLException ante fallas de persistencia
     */
    Articulo buscarPorId(int idArticulo) throws SQLException;

    /**
     * Lista todos los artículos vigentes en el catálogo para venta (solo activos).
     * @return lista de artículos activos
     * @throws SQLException ante fallas de persistencia
     */
    List<Articulo> listarDisponibles() throws SQLException;

    /**
     * Lista todos los artículos del catálogo general (tanto activos como inactivos).
     * @return lista completa de artículos
     * @throws SQLException ante fallas de persistencia
     */
    List<Articulo> listarTodos() throws SQLException;

    /**
     * Reactiva un artículo previamente desactivado.
     * @param idArticulo ID del artículo
     * @return true si fue reactivado
     * @throws SQLException ante fallas de persistencia
     */
    boolean reactivarArticulo(int idArticulo) throws SQLException;

    /**
     * Decrementa atómicamente el stock físico dentro de una transacción activa.
     * @param idArticulo ID del artículo
     * @param cantidad bultos a descontar
     * @param conn conexión transaccional abierta
     * @return true si la fila fue actualizada con éxito
     * @throws SQLException ante fallas de base de datos
     */
    boolean decrementarStock(int idArticulo, int cantidad, Connection conn) throws SQLException;

    /**
     * Inserta un nuevo artículo en la base de datos.
     * @param articulo entidad a persistir
     * @return ID autogenerado
     * @throws SQLException ante errores SQL
     */
    int crearArticulo(Articulo articulo) throws SQLException;

    /**
     * Actualiza los datos maestros y precios de un artículo existente.
     * @param articulo entidad con datos modificados
     * @return true si se actualizó el registro
     * @throws SQLException ante errores SQL
     */
    boolean actualizarArticulo(Articulo articulo) throws SQLException;

    /**
     * Desactiva lógicamente un artículo del catálogo para evitar su venta.
     * @param idArticulo ID del artículo
     * @return true si fue desactivado
     * @throws SQLException ante errores SQL
     */
    boolean eliminarArticulo(int idArticulo) throws SQLException;

    /**
     * Valida si un código de producto ya se encuentra registrado.
     * @param codigo código a validar
     * @param idArticuloExcluir ID a ignorar (útil al editar)
     * @return true si ya existe
     * @throws SQLException ante errores SQL
     */
    boolean existeCodigo(String codigo, int idArticuloExcluir) throws SQLException;
}

