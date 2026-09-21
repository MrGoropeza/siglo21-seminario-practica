package com.sgcv.ui;

import com.sgcv.dao.ArticuloDAO;
import com.sgcv.dao.ClienteDAO;
import com.sgcv.model.Articulo;
import com.sgcv.model.Cliente;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Ventana Principal de la aplicación de escritorio SGCV.
 */
public class MainFrame extends JFrame {

    public MainFrame() {
        super("SGCV - Sistema de Gestión Comercial y Distribución Avícola [Seminario de Práctica]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        inicializarUI();
    }

    private void inicializarUI() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Pestaña 1: Registro de Entregas (CU-01)
        tabbedPane.addTab("Registro de Entregas en Ruta (CU-01)", new EntregaPanel());

        // Pestaña 2: Monitor de Cuentas Corrientes y Créditos
        tabbedPane.addTab("Monitor de Cuentas Corrientes (CU-06)", crearPanelCuentasCorrientes());

        // Pestaña 3: Inventario y Stock en Depósito
        tabbedPane.addTab("Inventario de Bultos y Stock (CU-05)", crearPanelInventario());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel crearPanelCuentasCorrientes() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Razón Social", "CUIT", "Zona", "Saldo Deudor", "Límite Crédito", "Estado Crediticio"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        table.setRowHeight(24);

        try {
            List<Cliente> clientes = new ClienteDAO().listarActivos();
            for (Cliente c : clientes) {
                String estado = c.estaExcedido(java.math.BigDecimal.ZERO) ? "EXCEDIDO (RIESGO)" : "NORMAL";
                model.addRow(new Object[]{
                    c.getRazonSocial(), c.getCuit(), c.getZonaReparto(),
                    "$" + c.getSaldoCuentaCorriente(), "$" + c.getLimiteCredito(), estado
                });
            }
        } catch (Exception e) {
            // Manejo silencioso si DB no está conectada en preview
        }

        panel.add(new JLabel("Resumen de Clientes y Estado de Crédito:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelInventario() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Código", "Descripción", "Embalaje", "Costo Reposición", "Precio Venta", "Stock Depósito", "Alerta Mínima"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        table.setRowHeight(24);

        try {
            List<Articulo> arts = new ArticuloDAO().listarDisponibles();
            for (Articulo a : arts) {
                model.addRow(new Object[]{
                    a.getCodigo(), a.getDescripcion(), a.getUnidadEmbalaje(),
                    "$" + a.getPrecioCostoReposicion(), "$" + a.getPrecioVentaBase(),
                    a.getStockDeposito() + " bultos", a.getStockMinimoAlerta() + " bultos"
                });
            }
        } catch (Exception e) {
            // Manejo silencioso
        }

        panel.add(new JLabel("Catálogo de Bultos Cerrados y Stock Físico:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
