package com.sgcv.dao;

import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloEstandar;
import com.sgcv.model.ArticuloPromocional;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Articulo implementando IArticuloDAO.
 * Materializa instancias polimórficas (ArticuloPromocional o ArticuloEstandar)
 * en concordancia exacta con la tabla relacional 'articulos' y el DER original.
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

    @Override
    public int crearArticulo(Articulo articulo) throws SQLException {
        String sql = """
                INSERT INTO articulos (codigo, descripcion, unidad_embalaje,
                                      precio_costo_reposicion, precio_venta_base,
                                      stock_deposito, stock_minimo_alerta, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, articulo.getCodigo().toUpperCase().trim());
            ps.setString(2, articulo.getDescripcion().trim());
            ps.setString(3, articulo.getUnidadEmbalaje() != null ? articulo.getUnidadEmbalaje().trim() : "Bulto");
            ps.setBigDecimal(4, articulo.getPrecioCostoReposicion());
            ps.setBigDecimal(5, articulo.getPrecioVentaBase());
            ps.setInt(6, articulo.getStockDeposito());
            ps.setInt(7, articulo.getStockMinimoAlerta());
            ps.setBoolean(8, articulo.isActivo());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    articulo.setIdArticulo(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean actualizarArticulo(Articulo articulo) throws SQLException {
        String sql = """
                UPDATE articulos
                SET codigo = ?, descripcion = ?, unidad_embalaje = ?,
                    precio_costo_reposicion = ?, precio_venta_base = ?,
                    stock_deposito = ?, stock_minimo_alerta = ?, activo = ?
                WHERE id_articulo = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, articulo.getCodigo().toUpperCase().trim());
            ps.setString(2, articulo.getDescripcion().trim());
            ps.setString(3, articulo.getUnidadEmbalaje() != null ? articulo.getUnidadEmbalaje().trim() : "Bulto");
            ps.setBigDecimal(4, articulo.getPrecioCostoReposicion());
            ps.setBigDecimal(5, articulo.getPrecioVentaBase());
            ps.setInt(6, articulo.getStockDeposito());
            ps.setInt(7, articulo.getStockMinimoAlerta());
            ps.setBoolean(8, articulo.isActivo());
            ps.setInt(9, articulo.getIdArticulo());

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminarArticulo(int idArticulo) throws SQLException {
        String sql = "UPDATE articulos SET activo = FALSE WHERE id_articulo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idArticulo);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean existeCodigo(String codigo, int idArticuloExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM articulos WHERE UPPER(codigo) = UPPER(?) AND id_articulo != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo.trim());
            ps.setInt(2, idArticuloExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
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
        if (codUpper.startsWith("POLL-ENT") || codUpper.startsWith("PATA-MUS")) {
            return new ArticuloPromocional(
                    id, codigo, descripcion, unidadEmbalaje, costo, venta, stock, stockMin, activo
            );
        } else {
            return new ArticuloEstandar(
                    id, codigo, descripcion, unidadEmbalaje, costo, venta, stock, stockMin, activo
            );
        }
    }
}
