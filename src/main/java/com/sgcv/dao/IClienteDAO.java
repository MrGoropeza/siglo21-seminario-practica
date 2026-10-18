package com.sgcv.dao;

import com.sgcv.model.Cliente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de persistencia para la gestión de clientes y saldos de cuentas corrientes.
 */
public interface IClienteDAO {

    /**
     * Busca un cliente por su clave primaria.
     * @param idCliente ID del cliente
     * @return Cliente encontrado o null
     * @throws SQLException ante errores de conexión/consulta
     */
    Cliente buscarPorId(int idCliente) throws SQLException;

    /**
     * Obtiene el listado completo de clientes activos en el sistema.
     * @return lista de clientes
     * @throws SQLException ante errores SQL
     */
    List<Cliente> listarActivos() throws SQLException;

    /**
     * Ajusta el saldo de la cuenta corriente de forma transaccional.
     * @param idCliente ID del cliente
     * @param variacion importe a sumar (débito) o restar (crédito)
     * @param conn conexión transaccional JDBC
     * @return true si la actualización afectó una fila
     * @throws SQLException ante errores de persistencia
     */
    boolean actualizarSaldo(int idCliente, BigDecimal variacion, Connection conn) throws SQLException;
}
