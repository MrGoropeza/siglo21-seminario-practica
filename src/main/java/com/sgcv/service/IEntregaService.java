package com.sgcv.service;

import com.sgcv.dao.IArticuloDAO;
import com.sgcv.dao.IClienteDAO;
import com.sgcv.dao.IEntregaDAO;
import com.sgcv.exception.LimiteCreditoExcedidoException;
import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.model.Entrega;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Contrato de servicio transaccional para la emisión de remitos y control de inventario/crédito.
 */
public interface IEntregaService {

    /**
     * Valida si una entrega proyectada supera la línea de crédito autorizada del cliente.
     * @param idCliente identificador del cliente
     * @param totalEstimado total proyectado
     * @return true si excede el límite
     * @throws SQLException ante errores de acceso a datos
     */
    boolean validarExcesoLimiteCredito(int idCliente, BigDecimal totalEstimado) throws SQLException;

    /**
     * Registra una entrega validando stock y límite de crédito sin excepción autorizada previa.
     * @param entrega comprobante y renglones de mercadería
     * @return true si la transacción se confirmó exitosamente
     * @throws SQLException ante errores de concurrencia o persistencia
     * @throws StockInsuficienteException si no hay stock suficiente para algún renglón
     * @throws LimiteCreditoExcedidoException si el cliente supera su límite de crédito sin autorización
     */
    boolean registrarEntrega(Entrega entrega)
            throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException;

    /**
     * Registra una entrega con la opción de forzar o autorizar excepcionalmente el exceso crediticio (CU-01 S10).
     * @param entrega comprobante y renglones
     * @param autorizarExcesoCredito si es true, aprueba la operación a pesar de la alerta de exceso
     * @return true si la transacción se confirmó exitosamente
     * @throws SQLException ante errores de base de datos
     * @throws StockInsuficienteException si no hay stock disponible
     * @throws LimiteCreditoExcedidoException si hay exceso crediticio y autorizarExcesoCredito es false
     */
    boolean registrarEntrega(Entrega entrega, boolean autorizarExcesoCredito)
            throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException;

    IEntregaDAO getEntregaDAO();
    IClienteDAO getClienteDAO();
    IArticuloDAO getArticuloDAO();
}
