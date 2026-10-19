package com.sgcv.dao;

import com.sgcv.model.Cobranza;
import com.sgcv.model.DetalleCobranza;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CobranzaDAO implements ICobranzaDAO {

    @Override
    public int registrarCobranza(Cobranza cobranza) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = registrarCobranza(cobranza, conn);
                conn.commit();
                return id;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    @Override
    public int registrarCobranza(Cobranza cobranza, Connection conn) throws SQLException {
        // Generar número de recibo correlativo si no viene seteado
        if (cobranza.getNumeroRecibo() == null || cobranza.getNumeroRecibo().trim().isEmpty()) {
            cobranza.setNumeroRecibo(generarNumeroRecibo(conn));
        }

        String sqlCobranza = """
                INSERT INTO cobranzas (numero_recibo, id_cliente, id_usuario, fecha_cobranza, monto_total, observaciones)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        int idCobranzaGenerado = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlCobranza, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cobranza.getNumeroRecibo());
            ps.setInt(2, cobranza.getIdCliente());
            ps.setInt(3, cobranza.getIdUsuario());
            // setObject con LocalDateTime envia la fecha-hora local sin conversion de zona horaria
            ps.setObject(4, cobranza.getFechaCobranza() != null ? cobranza.getFechaCobranza() : LocalDateTime.now());
            ps.setBigDecimal(5, cobranza.getMontoTotal());
            ps.setString(6, cobranza.getObservaciones());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    idCobranzaGenerado = rs.getInt(1);
                    cobranza.setIdCobranza(idCobranzaGenerado);
                }
            }
        }

        // Insertar renglones multi-tender
        String sqlDetalle = """
                INSERT INTO detalle_cobranzas (id_cobranza, medio_pago, monto, referencia_operacion)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement psDet = conn.prepareStatement(sqlDetalle)) {
            for (DetalleCobranza det : cobranza.getDetalles()) {
                psDet.setInt(1, idCobranzaGenerado);
                psDet.setString(2, det.getMedioPago());
                psDet.setBigDecimal(3, det.getMonto());
                psDet.setString(4, det.getReferenciaOperacion());
                psDet.addBatch();
            }
            psDet.executeBatch();
        }

        // Actualizar saldo deudor de la cuenta corriente del cliente (reduce saldo) y fecha de actividad
        String sqlUpdateCliente = """
                UPDATE clientes
                SET saldo_cuenta_corriente = saldo_cuenta_corriente - ?, updated_at = CURRENT_TIMESTAMP
                WHERE id_cliente = ?
                """;

        try (PreparedStatement psCli = conn.prepareStatement(sqlUpdateCliente)) {
            psCli.setBigDecimal(1, cobranza.getMontoTotal());
            psCli.setInt(2, cobranza.getIdCliente());
            psCli.executeUpdate();
        }

        return idCobranzaGenerado;
    }

    @Override
    public List<Cobranza> listarPorFecha(LocalDate fecha) throws SQLException {
        List<Cobranza> lista = new ArrayList<>();
        String sql = """
                SELECT id_cobranza, numero_recibo, id_cliente, id_usuario, fecha_cobranza, monto_total, observaciones
                FROM cobranzas
                WHERE DATE(fecha_cobranza) = ?
                ORDER BY fecha_cobranza ASC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(fecha != null ? fecha : LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cobranza c = new Cobranza(
                            rs.getInt("id_cobranza"),
                            rs.getString("numero_recibo"),
                            rs.getInt("id_cliente"),
                            rs.getInt("id_usuario"),
                            rs.getTimestamp("fecha_cobranza").toLocalDateTime(),
                            rs.getBigDecimal("monto_total"),
                            rs.getString("observaciones")
                    );
                    lista.add(c);
                }
            }
        }
        return lista;
    }

    @Override
    public BigDecimal obtenerTotalCobradoHoyPorMedio(String medioPago, LocalDate fecha, Connection conn) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(dc.monto), 0.00) AS total
                FROM detalle_cobranzas dc
                INNER JOIN cobranzas c ON dc.id_cobranza = c.id_cobranza
                WHERE dc.medio_pago = ? AND DATE(c.fecha_cobranza) = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, medioPago.toUpperCase());
            ps.setDate(2, java.sql.Date.valueOf(fecha != null ? fecha : LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private String generarNumeroRecibo(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id_cobranza), 0) + 1 AS siguiente FROM cobranzas";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int sig = rs.getInt("siguiente");
                return String.format("REC-0001-%08d", sig);
            }
        }
        return "REC-0001-00000001";
    }
}
