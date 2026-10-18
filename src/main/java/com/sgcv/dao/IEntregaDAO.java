package com.sgcv.dao;

import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contrato de acceso a datos para la emisión de remitos y renglones de entrega.
 */
public interface IEntregaDAO {

    /**
     * Inserta la cabecera del remito de entrega dentro de una transacción.
     * @param entrega remito a persistir
     * @param conn conexión transaccional abierta
     * @return identificador autogenerado de la entrega
     * @throws SQLException ante fallas de persistencia
     */
    int insertar(Entrega entrega, Connection conn) throws SQLException;

    /**
     * Inserta un renglón de detalle asociado a una entrega.
     * @param idEntrega ID de la entrega padre
     * @param det renglón con snapshot de precios
     * @param conn conexión transaccional
     * @return true si la inserción fue exitosa
     * @throws SQLException ante fallas SQL
     */
    boolean insertarDetalle(int idEntrega, DetalleEntrega det, Connection conn) throws SQLException;

    /**
     * Calcula o genera la siguiente numeración oficial para el remito físico.
     * @return código de remito con formato legal (ej. REM-0001-XXXXXXXX)
     */
    String obtenerSiguienteNumeroRemito();
}
