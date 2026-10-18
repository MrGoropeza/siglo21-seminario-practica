package com.sgcv.ui;

import com.sgcv.model.Articulo;
import com.sgcv.model.Cliente;
import com.sgcv.service.ArticuloService;
import com.sgcv.service.ClienteService;
import com.sgcv.service.EntregaService;
import com.sgcv.service.IArticuloService;
import com.sgcv.service.IClienteService;
import com.sgcv.service.IEntregaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ventana Principal de la aplicación de escritorio SGCV.
 * Utiliza interfaces de servicio desacopladas e integra JMenuBar con algoritmos de optimización.
 */
public class MainFrame extends JFrame {

    private final IClienteService clienteService;
    private final IArticuloService articuloService;
    private final IEntregaService entregaService;

    private JTabbedPane tabbedPane;
    private DefaultTableModel modelInventario;
    private DefaultTableModel modelCuentas;

    public MainFrame() {
        this(new ClienteService(), new ArticuloService(), new EntregaService());
    }

    public MainFrame(IClienteService clienteService, IArticuloService articuloService, IEntregaService entregaService) {
        super("SGCV - Sistema de Gestión Comercial y Distribución Avícola [Seminario de Práctica]");
        this.clienteService = clienteService;
        this.articuloService = articuloService;
        this.entregaService = entregaService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 700);
        setLocationRelativeTo(null);

        configurarMenuBar();
        inicializarUI();
    }

    private void configurarMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 1. Menú Operaciones
        JMenu menuOperaciones = new JMenu("Operaciones");
        JMenuItem itemNuevaEntrega = new JMenuItem("Nueva Entrega (CU-01)");
        itemNuevaEntrega.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        JMenuItem itemSalir = new JMenuItem("Salir");
        itemSalir.addActionListener(e -> System.exit(0));
        menuOperaciones.add(itemNuevaEntrega);
        menuOperaciones.addSeparator();
        menuOperaciones.add(itemSalir);
        menuBar.add(menuOperaciones);

        // 2. Menú Inventario
        JMenu menuInventario = new JMenu("Inventario");
        JMenuItem itemCatalogo = new JMenuItem("Ver Catálogo Completo");
        itemCatalogo.addActionListener(e -> {
            tabbedPane.setSelectedIndex(2);
            refrescarTablaInventario();
        });

        JMenuItem itemOrdenarStock = new JMenuItem("Ordenar por Stock Depósito");
        itemOrdenarStock.addActionListener(e -> ejecutarOrdenamientoStock());

        JMenuItem itemBusquedaBinaria = new JMenuItem("Búsqueda Binaria por Código");
        itemBusquedaBinaria.addActionListener(e -> ejecutarBusquedaBinaria());

        menuInventario.add(itemCatalogo);
        menuInventario.add(itemOrdenarStock);
        menuInventario.add(itemBusquedaBinaria);
        menuBar.add(menuInventario);

        // 3. Menú Cuentas Corrientes
        JMenu menuCuentas = new JMenu("Cuentas Corrientes");
        JMenuItem itemMonitoreoCredito = new JMenuItem("Monitoreo Crediticio");
        itemMonitoreoCredito.addActionListener(e -> {
            tabbedPane.setSelectedIndex(1);
            refrescarTablaCuentas();
        });
        menuCuentas.add(itemMonitoreoCredito);
        menuBar.add(menuCuentas);

        // 4. Menú Ayuda
        JMenu menuAyuda = new JMenu("Ayuda");
        JMenuItem itemAcercaDe = new JMenuItem("Acerca de SGCV (PUD Construcción)");
        itemAcercaDe.addActionListener(e -> mostrarAcercaDe());
        menuAyuda.add(itemAcercaDe);
        menuBar.add(menuAyuda);

        setJMenuBar(menuBar);
    }

    private void inicializarUI() {
        tabbedPane = new JTabbedPane();

        // Pestaña 1: Registro de Entregas (CU-01)
        tabbedPane.addTab("Registro de Entregas en Ruta (CU-01)",
                new EntregaPanel(entregaService, clienteService, articuloService));

        // Pestaña 2: Monitor de Cuentas Corrientes y Créditos (CU-06)
        tabbedPane.addTab("Monitor de Cuentas Corrientes (CU-06)", crearPanelCuentasCorrientes());

        // Pestaña 3: Inventario y Stock en Depósito (CU-05)
        tabbedPane.addTab("Inventario de Bultos y Stock (CU-05)", crearPanelInventario());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel crearPanelCuentasCorrientes() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Razón Social", "CUIT", "Zona Reparto", "Saldo Deudor", "Límite Crédito", "Estado Crediticio"};
        modelCuentas = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(modelCuentas);
        table.setRowHeight(24);

        refrescarTablaCuentas();

        panel.add(new JLabel("Resumen de Clientes y Estado de Crédito:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refrescarTablaCuentas() {
        if (modelCuentas == null) return;
        modelCuentas.setRowCount(0);
        try {
            List<Cliente> clientes = clienteService.listarActivos();
            for (Cliente c : clientes) {
                String estado = c.estaExcedido(java.math.BigDecimal.ZERO) ? "EXCEDIDO (RIESGO)" : "NORMAL";
                modelCuentas.addRow(new Object[]{
                    c.getRazonSocial(), c.getCuit(), c.getZonaReparto(),
                    "$" + c.getSaldoCuentaCorriente(), "$" + c.getLimiteCredito(), estado
                });
            }
        } catch (Exception e) {
            // Modo desconectado silencioso
        }
    }

    private JPanel crearPanelInventario() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Código", "Descripción", "Categoría Frío", "Embalaje", "Precio Base", "Stock Depósito", "Alerta Mínima", "Caducidad"};
        modelInventario = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(modelInventario);
        table.setRowHeight(24);

        refrescarTablaInventario();

        panel.add(new JLabel("Catálogo de Bultos Cerrados y Stock Físico:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refrescarTablaInventario() {
        if (modelInventario == null) return;
        modelInventario.setRowCount(0);
        try {
            List<Articulo> arts = articuloService.listarDisponibles();
            for (Articulo a : arts) {
                modelInventario.addRow(new Object[]{
                    a.getCodigo(), a.getDescripcion(), a.getCategoriaConservacion(), a.getUnidadEmbalaje(),
                    "$" + a.getPrecioVentaBase(), a.getStockDeposito() + " bultos",
                    a.getStockMinimoAlerta() + " bultos", a.getDiasCaducidadRecomendada() + " días"
                });
            }
        } catch (Exception e) {
            // Modo desconectado silencioso
        }
    }

    private void ejecutarOrdenamientoStock() {
        try {
            List<Articulo> arts = articuloService.listarDisponibles();
            if (arts == null || arts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay artículos disponibles para ordenar.", "Inventario", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Aplicar algoritmo clásico MergeSort
            List<Articulo> ordenados = articuloService.ordenarPorStockAscendente(arts);

            StringBuilder sb = new StringBuilder("=== CATÁLOGO ORDENADO POR STOCK DEPÓSITO (MERGESORT ASCENDENTE) ===\n\n");
            for (Articulo a : ordenados) {
                sb.append(String.format("• [%-12s] %-30s | Stock: %3d bultos | %s%n",
                        a.getCodigo(), a.getDescripcion(), a.getStockDeposito(), a.getCategoriaConservacion()));
            }

            JTextArea textArea = new JTextArea(sb.toString(), 15, 60);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                    "Ordenamiento Algorítmico por Stock (MergeSort)", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al ordenar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ejecutarBusquedaBinaria() {
        try {
            List<Articulo> arts = articuloService.listarDisponibles();
            if (arts == null || arts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay artículos disponibles para buscar.", "Inventario", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String codigoBuscado = JOptionPane.showInputDialog(this,
                    "Ingrese el código del artículo a buscar (ej. PECH-SUP-15, POLL-ENT-20):",
                    "Búsqueda Binaria O(log n)", JOptionPane.QUESTION_MESSAGE);

            if (codigoBuscado == null || codigoBuscado.trim().isEmpty()) {
                return;
            }

            // Garantizar precondición de ordenamiento lexicográfico
            List<Articulo> listaOrdenada = new ArrayList<>(arts);
            listaOrdenada.sort(Comparator.comparing(Articulo::getCodigo, String.CASE_INSENSITIVE_ORDER));

            int indice = articuloService.busquedaBinariaPorCodigo(listaOrdenada, codigoBuscado.trim());

            if (indice >= 0) {
                Articulo encontrado = listaOrdenada.get(indice);
                String mensaje = String.format("""
                        ¡Artículo Localizado Exitosamente (Índice: %d)!
                        -------------------------------------------------
                        Código: %s
                        Descripción: %s
                        Categoría Térmica: %s
                        Unidad Embalaje: %s
                        Precio Base: $%s
                        Stock en Depósito: %d bultos
                        Alerta Mínima: %d bultos
                        Vida Útil Sugerida: %d días
                        """,
                        indice, encontrado.getCodigo(), encontrado.getDescripcion(),
                        encontrado.getCategoriaConservacion(), encontrado.getUnidadEmbalaje(),
                        encontrado.getPrecioVentaBase(), encontrado.getStockDeposito(),
                        encontrado.getStockMinimoAlerta(), encontrado.getDiasCaducidadRecomendada());

                JOptionPane.showMessageDialog(this, mensaje, "Resultado de Búsqueda Binaria", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No se encontró ningún artículo con el código '" + codigoBuscado + "'.",
                        "Artículo No Encontrado", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en la búsqueda: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarAcercaDe() {
        String info = """
                SGCV - Sistema de Gestión Comercial y Distribución Avícola
                Seminario de Práctica (Universidad Siglo 21)
                
                Arquitectura de Software:
                • POO Java 17 con encapsulamiento, invariantes de dominio y polimorfismo.
                • Desacoplamiento en capas de Modelo, DAO, Servicios y UI Swing.
                • Algoritmos explícitos de ordenamiento (MergeSort) y búsqueda binaria.
                • Transacciones atómicas ACID (JDBC) y snapshots inmutables de precios.
                • Cobertura completa de pruebas unitarias JUnit 5.
                """;
        JOptionPane.showMessageDialog(this, info, "Acerca de SGCV", JOptionPane.INFORMATION_MESSAGE);
    }
}
