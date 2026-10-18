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
     * @return instancia especializada (ArticuloFresco o ArticuloCongelado) o null si no existe.
     * @throws SQLException ante fallas de persistencia
     */
    Articulo buscarPorId(int idArticulo) throws SQLException;

    /**
     * Lista todos los artículos vigentes en el catálogo para venta.
     * @return lista de artículos activos
     * @throws SQLException ante fallas de persistencia
     */
    List<Articulo> listarDisponibles() throws SQLException;

    /**
     * Decrementa atómicamente el stock físico dentro de una transacción activa.
     * @param idArticulo ID del artículo
     * @param cantidad bultos a descontar
     * @param conn conexión transaccional abierta
     * @return true si la fila fue actualizada con éxito
     * @throws SQLException ante fallas de base de datos
     */
    boolean decrementarStock(int idArticulo, int cantidad, Connection conn) throws SQLException;
}
