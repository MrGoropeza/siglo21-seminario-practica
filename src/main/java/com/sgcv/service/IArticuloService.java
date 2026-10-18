package com.sgcv.service;

import com.sgcv.model.Articulo;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato del servicio de gestión de catálogo, inventario y algoritmos de optimización logística.
 */
public interface IArticuloService {

    /**
     * Obtiene el catálogo completo de bultos avícolas disponibles en depósito.
     * @return lista de artículos activos
     * @throws SQLException ante errores de acceso a datos
     */
    List<Articulo> listarDisponibles() throws SQLException;

    /**
     * Recupera un artículo por su identificador primario.
     * @param id identificador
     * @return artículo encontrado o null
     * @throws SQLException ante errores SQL
     */
    Articulo buscarPorId(int id) throws SQLException;

    /**
     * Ordena una colección de artículos en orden ascendente por su stock físico en depósito (MergeSort O(n log n)).
     * @param lista colección a ordenar
     * @return nueva lista ordenada ascendentemente por stockDeposito
     */
    List<Articulo> ordenarPorStockAscendente(List<Articulo> lista);

    /**
     * Búsqueda binaria clásica (O(log n)) sobre una lista previamente ordenada alfabéticamente por código.
     * @param listaOrdenadaPorCodigo lista ordenada por Articulo::getCodigo
     * @param codigoBuscado clave de código a localizar
     * @return índice del artículo en la lista, o -1 si no se encuentra
     */
    int busquedaBinariaPorCodigo(List<Articulo> listaOrdenadaPorCodigo, String codigoBuscado);

    /**
     * Registra un nuevo producto en el catálogo.
     */
    int crearArticulo(Articulo articulo) throws SQLException;

    /**
     * Actualiza los datos y precios de un producto en el catálogo.
     */
    boolean actualizarArticulo(Articulo articulo) throws SQLException;

    /**
     * Desactiva un producto del catálogo para evitar su venta.
     */
    boolean eliminarArticulo(int idArticulo) throws SQLException;

    /**
     * Valida si un código ya se encuentra en uso.
     */
    boolean existeCodigo(String codigo, int idArticuloExcluir) throws SQLException;
}

