package com.sgcv;

import com.sgcv.ui.MainFrame;
import com.sgcv.util.DatabaseConnection;

import javax.swing.*;

/**
 * Punto de entrada del prototipo operacional SGCV.
 */
public class App {

    public static void main(String[] args) {
        // Inicializar Look and Feel (FlatLaf si está disponible en classpath, o nativo del sistema)
        try {
            Class<?> lafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            LookAndFeel laf = (LookAndFeel) lafClass.getDeclaredConstructor().newInstance();
            UIManager.setLookAndFeel(laf);
        } catch (Exception ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Test de conectividad a MySQL
        boolean dbOnline = DatabaseConnection.testConnection(2);
        if (!dbOnline) {
            System.out.println("[AVISO] Servidor MySQL no detectado en localhost:3306. Iniciando en modo desconectado/prototipo.");
        } else {
            System.out.println("[INFO] Conexión exitosa a MySQL (sgcv_db).");
        }

        // Lanzar interfaz gráfica en el hilo de despacho de eventos (EDT)
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
