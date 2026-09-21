package com.sgcv.ui;

import com.sgcv.model.Articulo;
import com.sgcv.model.Cliente;
import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;
import com.sgcv.service.EntregaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel de captura operativa para el registro de entregas en ruta (CU-01).
 */
public class EntregaPanel extends JPanel {

    private final EntregaService entregaService;
    private JComboBox<Cliente> comboClientes;
    private JComboBox<Articulo> comboArticulos;
    private JSpinner spinnerCantidad;
    private JTable tablaItems;
    private DefaultTableModel tableModel;
    private JLabel lblTotal;
    private JLabel lblSaldoActual;
    private JLabel lblLimiteCredito;
    private JButton btnConfirmar;

    private Entrega entregaActual;

    public EntregaPanel() {
        this.entregaService = new EntregaService();
        this.entregaActual = new Entrega();
        inicializarComponentes();
        cargarDatosMaestros();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel Superior: Selección de Cliente y Estado Crediticio
        JPanel panelNorte = new JPanel(new GridLayout(2, 1, 5, 5));
        panelNorte.setBorder(BorderFactory.createTitledBorder("1. Datos del Cliente y Cuenta Corriente"));

        JPanel filaCliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filaCliente.add(new JLabel("Cliente:"));
        comboClientes = new JComboBox<>();
        comboClientes.setPreferredSize(new Dimension(380, 30));
        comboClientes.addActionListener(e -> actualizarInfoCliente());
        filaCliente.add(comboClientes);
        panelNorte.add(filaCliente);

        JPanel filaCredito = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        lblSaldoActual = new JLabel("Saldo Deudor: $0.00");
        lblSaldoActual.setFont(lblSaldoActual.getFont().deriveFont(Font.BOLD));
        lblLimiteCredito = new JLabel("Límite Autorizado: $0.00");
        filaCredito.add(lblSaldoActual);
        filaCredito.add(lblLimiteCredito);
        panelNorte.add(filaCredito);
        add(panelNorte, BorderLayout.NORTH);

        // Panel Central: Carga de Bultos y Tabla de Renglones
        JPanel panelCentro = new JPanel(new BorderLayout(5, 5));
        panelCentro.setBorder(BorderFactory.createTitledBorder("2. Mercadería y Bultos Cerrados"));

        JPanel panelAgregar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelAgregar.add(new JLabel("Artículo:"));
        comboArticulos = new JComboBox<>();
        comboArticulos.setPreferredSize(new Dimension(340, 30));
        panelAgregar.add(comboArticulos);

        panelAgregar.add(new JLabel("Cantidad Bultos:"));
        spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
        panelAgregar.add(spinnerCantidad);

        JButton btnAgregar = new JButton("Agregar Renglón");
        btnAgregar.addActionListener(e -> agregarItem());
        panelAgregar.add(btnAgregar);
        panelCentro.add(panelAgregar, BorderLayout.NORTH);

        String[] columnas = {"Código", "Descripción", "Bultos", "Precio Unitario", "Subtotal"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaItems = new JTable(tableModel);
        tablaItems.setRowHeight(24);
        panelCentro.add(new JScrollPane(tablaItems), BorderLayout.CENTER);
        add(panelCentro, BorderLayout.CENTER);

        // Panel Sur: Totales y Confirmación
        JPanel panelSur = new JPanel(new BorderLayout());
        JPanel panelTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        lblTotal = new JLabel("TOTAL REMITO: $0.00");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 16f));
        lblTotal.setForeground(new Color(14, 40, 65));
        panelTotal.add(lblTotal);

        btnConfirmar = new JButton("Confirmar y Emitir Remito");
        btnConfirmar.setFont(btnConfirmar.getFont().deriveFont(Font.BOLD, 14f));
        btnConfirmar.setPreferredSize(new Dimension(240, 40));
        btnConfirmar.addActionListener(e -> confirmarEntrega());
        panelTotal.add(btnConfirmar);

        panelSur.add(panelTotal, BorderLayout.EAST);
        add(panelSur, BorderLayout.SOUTH);
    }

    private void cargarDatosMaestros() {
        try {
            List<Cliente> clientes = entregaService.getClienteDAO().listarActivos();
            comboClientes.removeAllItems();
            for (Cliente c : clientes) {
                comboClientes.addItem(c);
            }

            List<Articulo> articulos = entregaService.getArticuloDAO().listarDisponibles();
            comboArticulos.removeAllItems();
            for (Articulo a : articulos) {
                comboArticulos.addItem(a);
            }
            actualizarInfoCliente();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error conectando con la base de datos MySQL: " + e.getMessage(), 
                "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarInfoCliente() {
        Cliente cli = (Cliente) comboClientes.getSelectedItem();
        if (cli != null) {
            lblSaldoActual.setText("Saldo Deudor: $" + cli.getSaldoCuentaCorriente());
            lblLimiteCredito.setText("Límite Autorizado: $" + cli.getLimiteCredito());
            if (cli.estaExcedido(BigDecimal.ZERO)) {
                lblSaldoActual.setForeground(Color.RED);
            } else {
                lblSaldoActual.setForeground(new Color(0, 103, 86));
            }
        }
    }

    private void agregarItem() {
        Articulo art = (Articulo) comboArticulos.getSelectedItem();
        int cant = (Integer) spinnerCantidad.getValue();
        if (art == null) return;

        if (!art.tieneStock(cant)) {
            JOptionPane.showMessageDialog(this,
                "Stock insuficiente para el artículo: " + art.getCodigo() + 
                "\nDisponible en depósito: " + art.getStockDeposito() + " bultos.",
                "Alerta de Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DetalleEntrega det = new DetalleEntrega(
            art.getIdArticulo(), art.getCodigo(), art.getDescripcion(),
            cant, art.getPrecioVentaBase(), art.getPrecioCostoReposicion(), BigDecimal.ZERO
        );
        entregaActual.agregarDetalle(det);

        tableModel.addRow(new Object[]{
            art.getCodigo(), art.getDescripcion(), cant,
            "$" + art.getPrecioVentaBase(), "$" + det.getSubtotal()
        });

        lblTotal.setText("TOTAL REMITO: $" + entregaActual.getTotalEntrega());
    }

    private void confirmarEntrega() {
        Cliente cli = (Cliente) comboClientes.getSelectedItem();
        if (cli == null || entregaActual.getDetalles().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe agregar al menos un artículo para emitir la entrega.", 
                "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        entregaActual.setIdCliente(cli.getIdCliente());
        entregaActual.setIdUsuario(1);

        // Regla de Negocio: Validación de límite de crédito (CU-01 S10)
        if (cli.estaExcedido(entregaActual.getTotalEntrega())) {
            BigDecimal exceso = cli.getSaldoCuentaCorriente().add(entregaActual.getTotalEntrega()).subtract(cli.getLimiteCredito());
            int resp = JOptionPane.showConfirmDialog(this,
                "¡ADVERTENCIA DE CRÉDITO EXCEDIDO (CU-01)!\n" +
                "El saldo proyectado superará el límite autorizado por $" + exceso + ".\n" +
                "¿Desea autorizar excepcionalmente la salida de mercadería en ruta?",
                "Alerta de Riesgo Crediticio", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resp != JOptionPane.YES_OPTION) {
                return;
            }
            entregaActual.setObservaciones("Entrega con exceso de crédito autorizada por operador.");
        }

        try {
            boolean exito = entregaService.registrarEntrega(entregaActual);
            if (exito) {
                JOptionPane.showMessageDialog(this,
                    "¡Entrega confirmada exitosamente!\n" +
                    "Número de Remito: " + entregaActual.getNumeroRemito() + "\n" +
                    "Total Facturado: $" + entregaActual.getTotalEntrega(),
                    "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                // Resetear panel
                this.entregaActual = new Entrega();
                tableModel.setRowCount(0);
                lblTotal.setText("TOTAL REMITO: $0.00");
                cargarDatosMaestros();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al confirmar entrega: " + ex.getMessage(),
                "Error Transaccional", JOptionPane.ERROR_MESSAGE);
        }
    }
}
