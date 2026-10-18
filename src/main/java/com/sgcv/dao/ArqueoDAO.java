package com.sgcv.dao;

import com.sgcv.model.ArqueoCaja;
import com.sgcv.model.DetalleArqueo;
import com.sgcv.model.ResumenDiarioDTO;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ArqueoDAO implements IArqueoDAO {

    @Override
    public int registrarArqueo(ArqueoCaja arqueo) throws SQLException {
        String sqlArqueo = """
                INSERT INTO arqueos_caja (id_usuario, fecha_arqueo, saldo_inicial_efectivo,
                                         efectivo_esperado, efectivo_real_contado, diferencia_efectivo,
                                         fondo_reposicion_segregado, margen_neto_dia, observaciones, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String sqlDetalle = """
                INSERT INTO detalle_arqueos (id_arqueo, medio_pago, total_cobranzas, total_pagos_proveedor, saldo_neto)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idArqueoGenerado = 0;
                try (PreparedStatement ps = conn.prepareStatement(sqlArqueo, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, arqueo.getIdUsuario() > 0 ? arqueo.getIdUsuario() : 1);
                    ps.setDate(2, java.sql.Date.valueOf(arqueo.getFechaArqueo() != null ? arqueo.getFechaArqueo() : LocalDate.now()));
                    ps.setBigDecimal(3, arqueo.getSaldoInicialEfectivo());
                    ps.setBigDecimal(4, arqueo.getEfectivoEsperado());
                    ps.setBigDecimal(5, arqueo.getEfectivoRealContado());
                    ps.setBigDecimal(6, arqueo.getDiferenciaEfectivo());
                    ps.setBigDecimal(7, arqueo.getFondoReposicionSegregado());
                    ps.setBigDecimal(8, arqueo.getMargenNetoDia());
                    ps.setString(9, arqueo.getObservaciones());
                    ps.setString(10, arqueo.getEstado() != null ? arqueo.getEstado() : "CERRADO");

                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            idArqueoGenerado = rs.getInt(1);
                            arqueo.setIdArqueo(idArqueoGenerado);
                        }
                    }
                }

                // Insertar desgloses de medio de pago
                try (PreparedStatement psDet = conn.prepareStatement(sqlDetalle)) {
                    for (DetalleArqueo det : arqueo.getDetalles()) {
                        psDet.setInt(1, idArqueoGenerado);
                        psDet.setString(2, det.getMedioPago());
                        psDet.setBigDecimal(3, det.getTotalCobranzas());
                        psDet.setBigDecimal(4, det.getTotalPagosProveedor());
                        psDet.setBigDecimal(5, det.getSaldoNeto());
                        psDet.addBatch();
                    }
                    psDet.executeBatch();
                }

                conn.commit();
                return idArqueoGenerado;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    @Override
    public ArqueoCaja obtenerUltimoArqueo() throws SQLException {
        String sql = """
                SELECT id_arqueo, id_usuario, fecha_arqueo, saldo_inicial_efectivo,
                       efectivo_esperado, efectivo_real_contado, diferencia_efectivo,
                       fondo_reposicion_segregado, margen_neto_dia, observaciones, estado, fecha_cierre
                FROM arqueos_caja
                ORDER BY id_arqueo DESC
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new ArqueoCaja(
                        rs.getInt("id_arqueo"),
                        rs.getInt("id_usuario"),
                        rs.getDate("fecha_arqueo").toLocalDate(),
                        rs.getBigDecimal("saldo_inicial_efectivo"),
                        rs.getBigDecimal("efectivo_esperado"),
                        rs.getBigDecimal("efectivo_real_contado"),
                        rs.getBigDecimal("diferencia_efectivo"),
                        rs.getBigDecimal("fondo_reposicion_segregado"),
                        rs.getBigDecimal("margen_neto_dia"),
                        rs.getString("observaciones"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_cierre").toLocalDateTime()
                );
            }
        }
        return null;
    }

    @Override
    public List<ArqueoCaja> listarHistorico() throws SQLException {
        List<ArqueoCaja> lista = new ArrayList<>();
        String sql = """
                SELECT id_arqueo, id_usuario, fecha_arqueo, saldo_inicial_efectivo,
                       efectivo_esperado, efectivo_real_contado, diferencia_efectivo,
                       fondo_reposicion_segregado, margen_neto_dia, observaciones, estado, fecha_cierre
                FROM arqueos_caja
                ORDER BY fecha_arqueo DESC, id_arqueo DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new ArqueoCaja(
                        rs.getInt("id_arqueo"),
                        rs.getInt("id_usuario"),
                        rs.getDate("fecha_arqueo").toLocalDate(),
                        rs.getBigDecimal("saldo_inicial_efectivo"),
                        rs.getBigDecimal("efectivo_esperado"),
                        rs.getBigDecimal("efectivo_real_contado"),
                        rs.getBigDecimal("diferencia_efectivo"),
                        rs.getBigDecimal("fondo_reposicion_segregado"),
                        rs.getBigDecimal("margen_neto_dia"),
                        rs.getString("observaciones"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_cierre").toLocalDateTime()
                ));
            }
        }
        return lista;
    }

    @Override
    public ResumenDiarioDTO calcularResumenDiario(LocalDate fecha) throws SQLException {
        if (fecha == null) fecha = LocalDate.now();
        ResumenDiarioDTO dto = new ResumenDiarioDTO();
        dto.setFecha(fecha);

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Entregas y Costos del día
            String sqlEntregas = """
                    SELECT 
                        COALESCE(SUM(de.cantidad_bultos), 0) AS total_bultos,
                        COALESCE(SUM(de.subtotal), 0.00) AS total_facturado,
                        COALESCE(SUM(de.cantidad_bultos * de.costo_unitario_congelado), 0.00) AS total_costo_reposicion
                    FROM detalle_entregas de
                    INNER JOIN entregas e ON de.id_entrega = e.id_entrega
                    WHERE DATE(e.fecha_entrega) = ? AND e.estado = 'EMITIDA'
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sqlEntregas)) {
                ps.setDate(1, java.sql.Date.valueOf(fecha));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dto.setBultosEntregadosTotal(rs.getInt("total_bultos"));
                        dto.setFacturacionEntregasTotal(rs.getBigDecimal("total_facturado"));
                        dto.setCostoReposicionTotal(rs.getBigDecimal("total_costo_reposicion"));
                    }
                }
            }

            // 2. Cobranzas por medio de pago del día
            String sqlCobranzas = """
                    SELECT 
                        dc.medio_pago,
                        COALESCE(SUM(dc.monto), 0.00) AS total
                    FROM detalle_cobranzas dc
                    INNER JOIN cobranzas c ON dc.id_cobranza = c.id_cobranza
                    WHERE DATE(c.fecha_cobranza) = ?
                    GROUP BY dc.medio_pago
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sqlCobranzas)) {
                ps.setDate(1, java.sql.Date.valueOf(fecha));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String medio = rs.getString("medio_pago");
                        BigDecimal monto = rs.getBigDecimal("total");
                        if ("EFECTIVO".equalsIgnoreCase(medio)) {
                            dto.setCobranzasEfectivoTotal(monto);
                        } else if ("TRANSFERENCIA".equalsIgnoreCase(medio)) {
                            dto.setCobranzasTransferenciaTotal(monto);
                        }
                    }
                }
            }

            // 3. Pagos a proveedores por medio de pago del día
            String sqlPagos = """
                    SELECT 
                        dp.medio_pago,
                        COALESCE(SUM(dp.monto), 0.00) AS total
                    FROM detalle_pagos_proveedor dp
                    INNER JOIN pagos_proveedor p ON dp.id_pago_proveedor = p.id_pago_proveedor
                    WHERE DATE(p.fecha_pago) = ?
                    GROUP BY dp.medio_pago
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sqlPagos)) {
                ps.setDate(1, java.sql.Date.valueOf(fecha));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String medio = rs.getString("medio_pago");
                        BigDecimal monto = rs.getBigDecimal("total");
                        if ("EFECTIVO".equalsIgnoreCase(medio)) {
                            dto.setPagosProveedorEfectivoTotal(monto);
                        } else if ("TRANSFERENCIA".equalsIgnoreCase(medio)) {
                            dto.setPagosProveedorTransferenciaTotal(monto);
                        }
                    }
                }
            }

            // Saldo inicial típico de cambio en mano (configurable)
            dto.setSaldoInicialEfectivo(new BigDecimal("50000.00"));
            dto.recalcularEfectivoEsperado();
        }

        return dto;
    }
}
