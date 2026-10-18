package com.sgcv.dao;

import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloCongelado;
import com.sgcv.model.ArticuloFresco;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Articulo implementando IArticuloDAO.
 * Materializa instancias polimórficas (ArticuloFresco o ArticuloCongelado) según la categoría del producto.
 */
public class ArticuloDAO implements IArticuloDAO {

    @Override
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

    @Override
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

    @Override
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
        int id = rs.getInt("id_articulo");
        String codigo = rs.getString("codigo");
        String descripcion = rs.getString("descripcion");
        String unidadEmbalaje = rs.getString("unidad_embalaje");
        BigDecimal costo = rs.getBigDecimal("precio_costo_reposicion");
        BigDecimal venta = rs.getBigDecimal("precio_venta_base");
        int stock = rs.getInt("stock_deposito");
        int stockMin = rs.getInt("stock_minimo_alerta");
        boolean activo = rs.getBoolean("activo");

        String codUpper = codigo != null ? codigo.toUpperCase() : "";
        if (codUpper.startsWith("PECH") || codUpper.startsWith("MILA")) {
            return new ArticuloCongelado(
                    id, codigo, descripcion, unidadEmbalaje, costo, venta, stock, stockMin, activo,
                    -18.0, new BigDecimal("3.00")
            );
        } else {
            return new ArticuloFresco(
                    id, codigo, descripcion, unidadEmbalaje, costo, venta, stock, stockMin, activo,
                    7, 4.0
            );
        }
    }
}
