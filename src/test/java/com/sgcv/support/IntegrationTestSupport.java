package com.sgcv.support;

import com.sgcv.model.Cliente;
import com.sgcv.service.ClienteService;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utilidades compartidas por los tests de integración contra MySQL real.
 *
 * <p>Cada test crea sus propios clientes con un CUIT único que empieza con {@code 99-}
 * y los elimina en el bloque de limpieza. La limpieza borra únicamente las filas cuyo
 * {@code id_cliente} fue creado por el test y devuelve al depósito el stock que el test
 * descontó, de modo que el seed queda igual que antes de correr la suite.</p>
 *
 * <p>{@code arqueos_caja} no tiene columna {@code id_cliente} (sólo {@code id_usuario}),
 * por lo que los arqueos no pueden limpiarse por cliente; para eso existe
 * {@link #eliminarArqueo(int)}.</p>
 */
public final class IntegrationTestSupport {

    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private IntegrationTestSupport() {
        // Clase utilitaria
    }

    /**
     * Marca temporal única para razones sociales y CUIT de prueba.
     */
    public static String marcaTemporal() {
        return LocalDateTime.now().format(MARCA) + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    /**
     * Crea un cliente de prueba con CUIT único {@code 99-...} usando el servicio real.
     *
     * @param prefijoRazonSocial prefijo legible; se le agrega la marca temporal
     * @param limiteCredito      límite de crédito del cliente
     * @return el cliente persistido, con {@code idCliente} asignado
     */
    public static Cliente crearClienteDePrueba(String prefijoRazonSocial, BigDecimal limiteCredito) throws SQLException {
        String marca = marcaTemporal();
        Cliente cliente = new Cliente(
                0,
                prefijoRazonSocial + " " + marca,
                cuitDePrueba(marca),
                "Calle de Prueba 123",
                "11-0000-0000",
                "Zona Test",
                BigDecimal.ZERO,
                limiteCredito,
                true);
        new ClienteService().crearCliente(cliente);
        return cliente;
    }

    /**
     * Deriva un CUIT de prueba único de 20 caracteres ({@code clientes.cuit VARCHAR(20)}):
     * prefijo {@code 99-}, la cola de milisegundos de la marca temporal y tres dígitos al azar.
     */
    private static String cuitDePrueba(String marca) {
        String cola = marca.substring(marca.length() - 10);
        return "99-" + cola + "-" + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /**
     * Limpia todas las filas asociadas al cliente creado por un test, en el orden que
     * respetan las claves foráneas: {@code detalle_cobranzas} → {@code cobranzas} →
     * {@code detalle_entregas} → {@code entregas} → {@code arqueos} del test →
     * {@code clientes}. Antes de borrar los renglones de entrega devuelve al depósito
     * los bultos que el test descontó.
     *
     * @param idCliente identificador del cliente creado por el test
     */
    public static void eliminarDatosDeCliente(int idCliente) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                restaurarStockDelCliente(conn, idCliente);
                ejecutar(conn, "DELETE FROM detalle_cobranzas WHERE id_cobranza IN "
                        + "(SELECT id_cobranza FROM cobranzas WHERE id_cliente = ?)", idCliente);
                ejecutar(conn, "DELETE FROM cobranzas WHERE id_cliente = ?", idCliente);
                ejecutar(conn, "DELETE FROM detalle_entregas WHERE id_entrega IN "
                        + "(SELECT id_entrega FROM entregas WHERE id_cliente = ?)", idCliente);
                ejecutar(conn, "DELETE FROM entregas WHERE id_cliente = ?", idCliente);
                ejecutar(conn, "DELETE FROM clientes WHERE id_cliente = ?", idCliente);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Limpia un arqueo de caja creado por un test ({@code detalle_arqueos} → {@code arqueos_caja}).
     * Los arqueos no se vinculan a clientes, por eso se limpian por su propio identificador.
     *
     * @param idArqueo identificador del arqueo creado por el test
     */
    public static void eliminarArqueo(int idArqueo) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ejecutar(conn, "DELETE FROM detalle_arqueos WHERE id_arqueo = ?", idArqueo);
                ejecutar(conn, "DELETE FROM arqueos_caja WHERE id_arqueo = ?", idArqueo);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Cuenta las filas de {@code tabla} cuyo {@code columna} coincide con {@code valor}.
     * El valor viaja siempre como parámetro JDBC; los identificadores se validan.
     */
    public static int contarFilas(String tabla, String columna, int valor) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + identificador(tabla)
                + " WHERE " + identificador(columna) + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Cuenta las filas de {@code tabla} cuyo {@code columna} de texto coincide con {@code valor}.
     */
    public static int contarFilasTexto(String tabla, String columna, String valor) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + identificador(tabla)
                + " WHERE " + identificador(columna) + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Cuenta las filas de {@code tabla} cuyo {@code columna} de texto empieza con {@code prefijo}.
     */
    public static int contarFilasTextoQueEmpiezanCon(String tabla, String columna, String prefijo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + identificador(tabla)
                + " WHERE " + identificador(columna) + " LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefijo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Devuelve el saldo deudor actual del cliente leído desde la base.
     */
    public static BigDecimal leerSaldoCliente(int idCliente) throws SQLException {
        String sql = "SELECT saldo_cuenta_corriente FROM clientes WHERE id_cliente = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    /**
     * Suma los renglones de {@code detalle_cobranzas} de un cliente para un medio de pago.
     * Devuelve {@code 0.00} si no hay renglones.
     */
    public static BigDecimal sumarDetallesCobranzaPorMedio(int idCliente, String medioPago) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(dc.monto), 0.00)
                FROM detalle_cobranzas dc
                INNER JOIN cobranzas c ON dc.id_cobranza = c.id_cobranza
                WHERE c.id_cliente = ? AND dc.medio_pago = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setString(2, medioPago);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * Cuenta los renglones de {@code detalle_cobranzas} de un cliente para un medio de pago.
     */
    public static int contarDetallesCobranzaPorMedio(int idCliente, String medioPago) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM detalle_cobranzas dc
                INNER JOIN cobranzas c ON dc.id_cobranza = c.id_cobranza
                WHERE c.id_cliente = ? AND dc.medio_pago = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setString(2, medioPago);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Lee una columna de fecha ({@code DATE}/{@code DATETIME}) como {@link LocalDate}.
     */
    public static LocalDate leerFecha(String tabla, String columnaFecha, String columnaId, int id) throws SQLException {
        String sql = "SELECT DATE(" + identificador(columnaFecha) + ") FROM " + identificador(tabla)
                + " WHERE " + identificador(columnaId) + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDate(1).toLocalDate() : null;
            }
        }
    }

    /**
     * Lee el stock actual de un artículo del seed (sólo lectura).
     */
    public static int leerStock(int idArticulo) throws SQLException {
        String sql = "SELECT stock_deposito FROM articulos WHERE id_articulo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idArticulo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void restaurarStockDelCliente(Connection conn, int idCliente) throws SQLException {
        String sql = """
                SELECT de.id_articulo, SUM(de.cantidad_bultos) AS bultos
                FROM detalle_entregas de
                INNER JOIN entregas e ON de.id_entrega = e.id_entrega
                WHERE e.id_cliente = ?
                GROUP BY de.id_articulo
                """;
        java.util.Map<Integer, Integer> bultosPorArticulo = new java.util.LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bultosPorArticulo.put(rs.getInt("id_articulo"), rs.getInt("bultos"));
                }
            }
        }

        String update = "UPDATE articulos SET stock_deposito = stock_deposito + ? WHERE id_articulo = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            for (java.util.Map.Entry<Integer, Integer> entrada : bultosPorArticulo.entrySet()) {
                ps.setInt(1, entrada.getValue());
                ps.setInt(2, entrada.getKey());
                ps.addBatch();
            }
            if (!bultosPorArticulo.isEmpty()) {
                ps.executeBatch();
            }
        }
    }

    private static void ejecutar(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static String identificador(String valor) {
        if (valor == null || !valor.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Identificador SQL no permitido: " + valor);
        }
        return valor;
    }
}
