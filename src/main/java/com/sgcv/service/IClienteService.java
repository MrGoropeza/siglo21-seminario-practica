package com.sgcv.service;

import com.sgcv.model.Cliente;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de servicio para la gestión de clientes y monitoreo de riesgo crediticio.
 */
public interface IClienteService {

    /**
     * Lista todos los clientes activos del canal minorista con sus saldos y límites.
     * @return lista de clientes
     * @throws SQLException ante errores de acceso a datos
     */
    List<Cliente> listarActivos() throws SQLException;

    /**
     * Recupera un cliente por su identificador primario.
     * @param id identificador
     * @return Cliente o null
     * @throws SQLException ante errores SQL
     */
    Cliente buscarPorId(int id) throws SQLException;

    /**
     * Verifica si una nueva venta o entrega genera exceso sobre la línea de crédito autorizada.
     * @param idCliente identificador del cliente
     * @param montoNuevo importe de la operación a validar
     * @return true si el cliente quedará en estado de riesgo crediticio (saldo + monto > límite)
     * @throws SQLException ante errores SQL
     */
    boolean verificarRiesgoCrediticio(int idCliente, BigDecimal montoNuevo) throws SQLException;

    /**
     * Registra un nuevo cliente con sus invariantes comerciales validadas.
     * @param cliente datos del nuevo cliente
     * @return ID autogenerado
     * @throws SQLException ante errores SQL
     */
    int crearCliente(Cliente cliente) throws SQLException;

    /**
     * Actualiza la información de un cliente existente con validación de reglas de negocio.
     * @param cliente cliente a modificar
     * @return true si se actualizó correctamente
     * @throws SQLException ante errores SQL
     */
    boolean actualizarCliente(Cliente cliente) throws SQLException;

    /**
     * Obtiene el extracto histórico cronológico (Libro Mayor) de la cuenta corriente.
     * @param idCliente identificador del cliente
     * @return lista de movimientos con Debe, Haber y Saldo
     * @throws SQLException ante errores SQL
     */
    List<com.sgcv.model.MovimientoCuentaCorriente> obtenerExtractoCuentaCorriente(int idCliente) throws SQLException;
}

