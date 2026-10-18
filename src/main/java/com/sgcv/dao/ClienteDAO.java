package com.sgcv.dao;

import com.sgcv.model.Cliente;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Cliente implementando IClienteDAO.
 */
public class ClienteDAO implements IClienteDAO {

    @Override
    public Cliente buscarPorId(int idCliente) throws SQLException {
        String sql = "SELECT id_cliente, razon_social, cuit, direccion, telefono, zona_reparto, " +
                     "saldo_cuenta_corriente, limite_credito, activo FROM clientes WHERE id_cliente = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Cliente> listarActivos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, razon_social, cuit, direccion, telefono, zona_reparto, " +
                     "saldo_cuenta_corriente, limite_credito, activo FROM clientes WHERE activo = TRUE ORDER BY razon_social";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCliente(rs));
            }
        }
        return lista;
    }

    @Override
    public boolean actualizarSaldo(int idCliente, BigDecimal variacion, Connection conn) throws SQLException {
        String sql = "UPDATE clientes SET saldo_cuenta_corriente = saldo_cuenta_corriente + ? WHERE id_cliente = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, variacion);
            ps.setInt(2, idCliente);
            return ps.executeUpdate() > 0;
        }
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        return new Cliente(
            rs.getInt("id_cliente"),
            rs.getString("razon_social"),
            rs.getString("cuit"),
            rs.getString("direccion"),
            rs.getString("telefono"),
            rs.getString("zona_reparto"),
            rs.getBigDecimal("saldo_cuenta_corriente"),
            rs.getBigDecimal("limite_credito"),
            rs.getBoolean("activo")
        );
    }
}
