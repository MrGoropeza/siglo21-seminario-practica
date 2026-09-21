package com.sgcv.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestor centralizado del ciclo de vida de conexiones JDBC para MySQL.
 * Carga configuraciones dinámicas desde db.properties en el classpath.
 */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "db.properties";
    private static final Properties properties = new Properties();

    private static String url;
    private static String username;
    private static String password;

    static {
        loadConfiguration();
    }

    private DatabaseConnection() {
        // Constructor privado para evitar instanciación de clase utilitaria
    }

    private static void loadConfiguration() {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                url = properties.getProperty("db.url");
                username = properties.getProperty("db.username");
                password = properties.getProperty("db.password");
                String driver = properties.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
                Class.forName(driver);
            } else {
                // Configuración de respaldo por defecto
                url = "jdbc:mysql://localhost:3306/sgcv_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                username = "root";
                password = "root";
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] Error cargando configuración JDBC: " + e.getMessage());
            url = "jdbc:mysql://localhost:3306/sgcv_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            username = "root";
            password = "root";
        }
    }

    /**
     * Obtiene una nueva conexión activa a la base de datos MySQL.
     *
     * @return java.sql.Connection abierta
     * @throws SQLException si ocurre un error de autenticación o conexión de red
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Valida si la conexión actual a la base de datos responde activamente.
     *
     * @param timeoutSegundos tiempo límite para considerar la prueba fallida
     * @return true si la conexión está operativa
     */
    public static boolean testConnection(int timeoutSegundos) {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(timeoutSegundos);
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Test de conexión fallido: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cierra de forma segura una conexión abierta silenciando posibles excepciones secundarias.
     *
     * @param conn Conexión a cerrar
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ignored) {
                // Silenciado intencionalmente
            }
        }
    }
}
