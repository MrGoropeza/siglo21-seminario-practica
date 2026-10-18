package com.sgcv.dao;

import com.sgcv.model.Cliente;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
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

    @Override
    public List<com.sgcv.model.MovimientoCuentaCorriente> obtenerExtractoCuentaCorriente(int idCliente) throws SQLException {
        List<com.sgcv.model.MovimientoCuentaCorriente> extracto = new ArrayList<>();
        String sql = """
                SELECT fecha, tipo, comprobante, concepto, debe, haber
                FROM (
                    SELECT 
                        e.fecha_entrega AS fecha,
                        'REMITO' AS tipo,
                        e.numero_remito AS comprobante,
                        CONCAT('Entrega de bultos en ruta (', COUNT(de.id_detalle_entrega), ' renglones)') AS concepto,
                        e.total_entrega AS debe,
                        0.00 AS haber
                    FROM entregas e
                    LEFT JOIN detalle_entregas de ON e.id_entrega = de.id_entrega
                    WHERE e.id_cliente = ? AND e.estado = 'EMITIDA'
                    GROUP BY e.id_entrega, e.fecha_entrega, e.numero_remito, e.total_entrega

                    UNION ALL

                    SELECT 
                        c.fecha_cobranza AS fecha,
                        'RECIBO' AS tipo,
                        c.numero_recibo AS comprobante,
                        COALESCE(c.observaciones, 'Cobranza percibida en parada comercial') AS concepto,
                        0.00 AS debe,
                        c.monto_total AS haber
                    FROM cobranzas c
                    WHERE c.id_cliente = ?
                ) movs
                ORDER BY fecha ASC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal saldo = BigDecimal.ZERO;
                while (rs.next()) {
                    LocalDateTime fecha = rs.getTimestamp("fecha").toLocalDateTime();
                    String tipo = rs.getString("tipo");
                    String comprobante = rs.getString("comprobante");
                    String concepto = rs.getString("concepto");
                    BigDecimal debe = rs.getBigDecimal("debe");
                    BigDecimal haber = rs.getBigDecimal("haber");

                    saldo = saldo.add(debe != null ? debe : BigDecimal.ZERO)
                                 .subtract(haber != null ? haber : BigDecimal.ZERO);

                    extracto.add(new com.sgcv.model.MovimientoCuentaCorriente(
                            fecha, tipo, comprobante, concepto, debe, haber, saldo
                    ));
                }
            }
        }
        return extracto;
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
