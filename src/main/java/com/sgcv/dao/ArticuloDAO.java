package com.sgcv.dao;

import com.sgcv.model.Articulo;
import com.sgcv.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Articulo.
 */
public class ArticuloDAO {

    public Articulo buscarPorId(int idArticulo) throws SQLException {
        String sql = "SELECT id_articulo, codigo, descripcion, unidad_embalaje, " +
                     "precio_costo_reposicion, precio_venta_base, stock_deposito, stock_minimo_alerta, activo " +
                     "FROM articulos WHERE id_articulo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idArticulo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearArticulo(rs);
                }
            }
        }
        return null;
    }

    public List<Articulo> listarDisponibles() throws SQLException {
        List<Articulo> lista = new ArrayList<>();
        String sql = "SELECT id_articulo, codigo, descripcion, unidad_embalaje, " +
                     "precio_costo_reposicion, precio_venta_base, stock_deposito, stock_minimo_alerta, activo " +
                     "FROM articulos WHERE activo = TRUE ORDER BY codigo";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearArticulo(rs));
            }
        }
        return lista;
    }

    public boolean decrementarStock(int idArticulo, int cantidad, Connection conn) throws SQLException {
        String sql = "UPDATE articulos SET stock_deposito = stock_deposito - ? WHERE id_articulo = ? AND stock_deposito >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idArticulo);
            ps.setInt(3, cantidad);
            return ps.executeUpdate() > 0;
        }
    }

    private Articulo mapearArticulo(ResultSet rs) throws SQLException {
        return new Articulo(
            rs.getInt("id_articulo"),
            rs.getString("codigo"),
            rs.getString("descripcion"),
            rs.getString("unidad_embalaje"),
            rs.getBigDecimal("precio_costo_reposicion"),
            rs.getBigDecimal("precio_venta_base"),
            rs.getInt("stock_deposito"),
            rs.getInt("stock_minimo_alerta"),
            rs.getBoolean("activo")
        );
    }
}
