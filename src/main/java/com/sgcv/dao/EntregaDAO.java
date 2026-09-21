package com.sgcv.dao;

import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;
import com.sgcv.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object para la entidad Entrega y sus renglones.
 */
public class EntregaDAO {

    public int insertar(Entrega entrega, Connection conn) throws SQLException {
        String sql = "INSERT INTO entregas (numero_remito, id_cliente, id_usuario, fecha_entrega, total_entrega, estado, observaciones) " +
                     "VALUES (?, ?, ?, NOW(), ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entrega.getNumeroRemito());
            ps.setInt(2, entrega.getIdCliente());
            ps.setInt(3, entrega.getIdUsuario() > 0 ? entrega.getIdUsuario() : 1);
            ps.setBigDecimal(4, entrega.getTotalEntrega());
            ps.setString(5, entrega.getEstado());
            ps.setString(6, entrega.getObservaciones());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean insertarDetalle(int idEntrega, DetalleEntrega det, Connection conn) throws SQLException {
        String sql = "INSERT INTO detalle_entregas (id_entrega, id_articulo, cantidad_bultos, " +
                     "precio_unitario_congelado, costo_unitario_congelado, descuento_unitario, subtotal) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEntrega);
            ps.setInt(2, det.getIdArticulo());
            ps.setInt(3, det.getCantidadBultos());
            ps.setBigDecimal(4, det.getPrecioUnitarioCongelado());
            ps.setBigDecimal(5, det.getCostoUnitarioCongelado());
            ps.setBigDecimal(6, det.getDescuentoUnitario());
            ps.setBigDecimal(7, det.getSubtotal());
            return ps.executeUpdate() > 0;
        }
    }

    public String obtenerSiguienteNumeroRemito() {
        String sql = "SELECT MAX(id_entrega) FROM entregas";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int siguiente = 1;
            if (rs.next()) {
                siguiente = rs.getInt(1) + 1;
            }
            return String.format("REM-0001-%08d", siguiente);
        } catch (SQLException e) {
            return "REM-0001-" + System.currentTimeMillis() % 100000000;
        }
    }
}
