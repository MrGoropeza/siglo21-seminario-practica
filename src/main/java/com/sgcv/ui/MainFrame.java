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
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Ventana Principal de la aplicación de escritorio SGCV.
 * Rediseñada para máxima agilidad operativa en ruta:
 * - Tarjetas KPI resumen en cada módulo
 * - Filtrado en tiempo real sin popups invasivos
 * - Ordenamiento algorítmico (MergeSort) y búsqueda binaria directamente integrados en la tabla
 * - Barra de menú limpia y accesible
 */
public class MainFrame extends JFrame {

    private final IClienteService clienteService;
    private final IArticuloService articuloService;
    private final IEntregaService entregaService;

    private JTabbedPane tabbedPane;
    private EntregaPanel entregaPanel;

    // Panel Cuentas Corrientes
    private DefaultTableModel modelCuentas;
    private JTable tableCuentas;
    private TableRowSorter<DefaultTableModel> sorterCuentas;
    private JTextField txtBuscarCliente;
    private JComboBox<String> comboFiltroCredito;
    private JLabel lblKpiTotalClientes;
    private JLabel lblKpiClientesRiesgo;
    private JLabel lblKpiSaldoDeudorTotal;

    // Panel Inventario
    private DefaultTableModel modelInventario;
    private JTable tableInventario;
    private TableRowSorter<DefaultTableModel> sorterInventario;
    private JTextField txtBuscarArticulo;
    private JLabel lblStatusInventario;
    private JLabel lblKpiTotalBultos;
    private JLabel lblKpiBultosFrescos;
    private JLabel lblKpiBultosCongelados;
    private JLabel lblKpiCriticos;
    private List<Articulo> articulosMemoria = new ArrayList<>();

    public MainFrame() {
        this(new ClienteService(), new ArticuloService(), new EntregaService());
    }

    public MainFrame(IClienteService clienteService, IArticuloService articuloService, IEntregaService entregaService) {
        super("SGCV - Sistema de Gestión Comercial y Distribución Avícola [Operación en Ruta]");
        this.clienteService = clienteService;
        this.articuloService = articuloService;
        this.entregaService = entregaService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        configurarMenuBar();
        inicializarUI();
    }

    private void configurarMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 1. Menú Archivo
        JMenu menuArchivo = new JMenu("Archivo");
        menuArchivo.setMnemonic(KeyEvent.VK_A);

        JMenuItem itemRefrescar = new JMenuItem("Refrescar Todos los Datos (F5)");
        itemRefrescar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        itemRefrescar.addActionListener(e -> refrescarTodosLosDatos());
        menuArchivo.add(itemRefrescar);

        menuArchivo.addSeparator();

        JMenuItem itemSalir = new JMenuItem("Salir");
        itemSalir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemSalir.addActionListener(e -> System.exit(0));
        menuArchivo.add(itemSalir);

        menuBar.add(menuArchivo);

        // 2. Menú Navegación
        JMenu menuVistas = new JMenu("Navegación");
        menuVistas.setMnemonic(KeyEvent.VK_N);

        JMenuItem itemTab1 = new JMenuItem("1. Registro de Entregas");
        itemTab1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemTab1.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        menuVistas.add(itemTab1);

        JMenuItem itemTab2 = new JMenuItem("2. Cuentas Corrientes y Créditos");
        itemTab2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemTab2.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        menuVistas.add(itemTab2);

        JMenuItem itemTab3 = new JMenuItem("3. Inventario de Mercadería");
        itemTab3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemTab3.addActionListener(e -> tabbedPane.setSelectedIndex(2));
        menuVistas.add(itemTab3);

        menuBar.add(menuVistas);

        // 3. Menú Ayuda
        JMenu menuAyuda = new JMenu("Ayuda");
        menuAyuda.setMnemonic(KeyEvent.VK_Y);

        JMenuItem itemGuia = new JMenuItem("Guía Operativa para el Repartidor");
        itemGuia.addActionListener(e -> mostrarGuiaRepartidor());
        menuAyuda.add(itemGuia);

        JMenuItem itemAcercaDe = new JMenuItem("Acerca de SGCV");
        itemAcercaDe.addActionListener(e -> mostrarAcercaDe());
        menuAyuda.add(itemAcercaDe);

        menuBar.add(menuAyuda);

        setJMenuBar(menuBar);
    }

    private void inicializarUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        // Pestaña 1: Registro de Entregas
        entregaPanel = new EntregaPanel(entregaService, clienteService, articuloService);
        tabbedPane.addTab("🚚 Registro de Entregas", entregaPanel);

        // Pestaña 2: Cuentas Corrientes
        tabbedPane.addTab("💳 Cuentas Corrientes", crearPanelCuentasCorrientes());

        // Pestaña 3: Inventario
        tabbedPane.addTab("📦 Inventario de Mercadería", crearPanelInventario());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // Panel: Monitor de Cuentas Corrientes (CU-06)
    // =========================================================================

    private JPanel crearPanelCuentasCorrientes() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 14, 14, 14));

        // 1. Tarjetas KPI
        JPanel panelKpi = new JPanel(new GridLayout(1, 3, 12, 0));
        lblKpiTotalClientes = new JLabel("0", SwingConstants.CENTER);
        lblKpiClientesRiesgo = new JLabel("0", SwingConstants.CENTER);
        lblKpiSaldoDeudorTotal = new JLabel("$0.00", SwingConstants.CENTER);

        panelKpi.add(crearKpiCard("TOTAL CLIENTES EN RUTA", lblKpiTotalClientes, new Color(24, 43, 73)));
        panelKpi.add(crearKpiCard("CLIENTES EN RIESGO / EXCEDIDOS", lblKpiClientesRiesgo, new Color(220, 53, 69)));
        panelKpi.add(crearKpiCard("TOTAL SALDO DEUDOR EN CALLE", lblKpiSaldoDeudorTotal, new Color(180, 80, 20)));
        panel.add(panelKpi, BorderLayout.NORTH);

        // 2. Barra de herramientas integrada (Búsqueda en tiempo real + Filtro)
        JPanel panelCentro = new JPanel(new BorderLayout(0, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolbar.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 225, 230), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        toolbar.setBackground(new Color(250, 252, 255));

        toolbar.add(new JLabel("🔍 Buscar:"));
        txtBuscarCliente = new JTextField(20);
        txtBuscarCliente.setToolTipText("Filtrado instantáneo por Razón Social o CUIT");
        txtBuscarCliente.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltroCuentas(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltroCuentas(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltroCuentas(); }
        });
        toolbar.add(txtBuscarCliente);

        toolbar.add(new JLabel("Filtrar Estado:"));
        comboFiltroCredito = new JComboBox<>(new String[]{"Todos los Clientes", "Solo con Saldo Deudor", "Solo en Riesgo / Excedidos"});
        comboFiltroCredito.addActionListener(e -> aplicarFiltroCuentas());
        toolbar.add(comboFiltroCredito);

        JButton btnActualizar = new JButton("🔄 Actualizar");
        btnActualizar.addActionListener(e -> refrescarTablaCuentas());
        toolbar.add(btnActualizar);

        panelCentro.add(toolbar, BorderLayout.NORTH);

        // 3. Tabla Interactiva
        String[] cols = {"Razón Social", "CUIT", "Zona Reparto", "Saldo Deudor", "Límite Crédito", "Crédito Disponible", "Estado Crediticio"};
        modelCuentas = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableCuentas = new JTable(modelCuentas);
        tableCuentas.setRowHeight(28);
        tableCuentas.setAutoCreateRowSorter(true);
        tableCuentas.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        tableCuentas.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        sorterCuentas = new TableRowSorter<>(modelCuentas);
        tableCuentas.setRowSorter(sorterCuentas);

        // Renderizador con Badge de color para el estado crediticio
        tableCuentas.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                String texto = value != null ? value.toString() : "";
                if (texto.contains("EXCEDIDO")) {
                    lbl.setForeground(new Color(180, 20, 20));
                    lbl.setText("● " + texto);
                } else {
                    lbl.setForeground(new Color(30, 130, 50));
                    lbl.setText("✔ " + texto);
                }
                return lbl;
            }
        });

        // Alineación derecha para importes
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tableCuentas.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tableCuentas.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        tableCuentas.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);

        panelCentro.add(new JScrollPane(tableCuentas), BorderLayout.CENTER);
        panel.add(panelCentro, BorderLayout.CENTER);

        refrescarTablaCuentas();
        return panel;
    }

    private void refrescarTablaCuentas() {
        if (modelCuentas == null) return;
        modelCuentas.setRowCount(0);

        int totalClientes = 0;
        int enRiesgo = 0;
        BigDecimal saldoTotal = BigDecimal.ZERO;

        try {
            List<Cliente> clientes = clienteService.listarActivos();
            for (Cliente c : clientes) {
                totalClientes++;
                saldoTotal = saldoTotal.add(c.getSaldoCuentaCorriente());
                boolean excedido = c.estaExcedido(BigDecimal.ZERO);
                if (excedido) enRiesgo++;

                String estado = excedido ? "EXCEDIDO (RIESGO)" : "NORMAL";
                BigDecimal disponible = c.getCreditoDisponible();

                modelCuentas.addRow(new Object[]{
                        c.getRazonSocial(), c.getCuit(), c.getZonaReparto(),
                        "$" + c.getSaldoCuentaCorriente().setScale(2, RoundingMode.HALF_UP),
                        "$" + c.getLimiteCredito().setScale(2, RoundingMode.HALF_UP),
                        "$" + disponible.setScale(2, RoundingMode.HALF_UP),
                        estado
                });
            }
        } catch (Exception e) {
            // Modo desconectado silencioso
        }

        lblKpiTotalClientes.setText(String.valueOf(totalClientes));
        lblKpiClientesRiesgo.setText(String.valueOf(enRiesgo));
        lblKpiSaldoDeudorTotal.setText("$" + saldoTotal.setScale(2, RoundingMode.HALF_UP));
    }

    private void aplicarFiltroCuentas() {
        if (sorterCuentas == null) return;

        String busqueda = txtBuscarCliente.getText().trim();
        String filtroEstado = (String) comboFiltroCredito.getSelectedItem();

        List<RowFilter<DefaultTableModel, Object>> filtros = new ArrayList<>();

        if (!busqueda.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + Pattern.quote(busqueda), 0, 1, 2));
        }

        if ("Solo con Saldo Deudor".equals(filtroEstado)) {
            filtros.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ?> entry) {
                    String saldoStr = entry.getStringValue(3).replace("$", "").trim();
                    try {
                        return new BigDecimal(saldoStr).compareTo(BigDecimal.ZERO) > 0;
                    } catch (Exception ex) {
                        return true;
                    }
                }
            });
        } else if ("Solo en Riesgo / Excedidos".equals(filtroEstado)) {
            filtros.add(RowFilter.regexFilter("EXCEDIDO", 6));
        }

        if (filtros.isEmpty()) {
            sorterCuentas.setRowFilter(null);
        } else {
            sorterCuentas.setRowFilter(RowFilter.andFilter(filtros));
        }
    }

    // =========================================================================
    // Panel: Inventario y Stock (CU-05) con Algoritmos Explícitos
    // =========================================================================

    private JPanel crearPanelInventario() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 14, 14, 14));

        // 1. Tarjetas KPI
        JPanel panelKpi = new JPanel(new GridLayout(1, 4, 12, 0));
        lblKpiTotalBultos = new JLabel("0", SwingConstants.CENTER);
        lblKpiBultosFrescos = new JLabel("0", SwingConstants.CENTER);
        lblKpiBultosCongelados = new JLabel("0", SwingConstants.CENTER);
        lblKpiCriticos = new JLabel("0", SwingConstants.CENTER);

        panelKpi.add(crearKpiCard("TOTAL BULTOS EN DEPÓSITO", lblKpiTotalBultos, new Color(24, 43, 73)));
        panelKpi.add(crearKpiCard("BULTOS REFRIGERADOS FRESCOS", lblKpiBultosFrescos, new Color(13, 110, 253)));
        panelKpi.add(crearKpiCard("BULTOS CONGELADOS IQF (-18°C)", lblKpiBultosCongelados, new Color(111, 66, 193)));
        panelKpi.add(crearKpiCard("ALERTAS CRÍTICAS (STOCK <= MÍNIMO)", lblKpiCriticos, new Color(220, 53, 69)));
        panel.add(panelKpi, BorderLayout.NORTH);

        // 2. Barra de herramientas integrada (Búsqueda en tiempo real + MergeSort + Búsqueda Binaria)
        JPanel panelCentro = new JPanel(new BorderLayout(0, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolbar.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 225, 230), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        toolbar.setBackground(new Color(250, 252, 255));

        toolbar.add(new JLabel("🔍 Buscar:"));
        txtBuscarArticulo = new JTextField(18);
        txtBuscarArticulo.setToolTipText("Filtrado en tiempo real por código o descripción");
        txtBuscarArticulo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltroInventario(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltroInventario(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltroInventario(); }
        });
        toolbar.add(txtBuscarArticulo);

        JButton btnOrdenarMergeSort = new JButton("🔄 Ordenar por Stock");
        btnOrdenarMergeSort.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnOrdenarMergeSort.setToolTipText("Ordenar productos de menor a mayor stock físico");
        btnOrdenarMergeSort.addActionListener(e -> ejecutarMergeSortEnPantalla());
        toolbar.add(btnOrdenarMergeSort);

        JButton btnBusquedaBinaria = new JButton("⚡ Ir a Código");
        btnBusquedaBinaria.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnBusquedaBinaria.setToolTipText("Buscar y resaltar producto por su código");
        btnBusquedaBinaria.addActionListener(e -> ejecutarBusquedaBinariaEnPantalla());
        toolbar.add(btnBusquedaBinaria);

        JButton btnRefrescar = new JButton("🔄 Recargar Catálogo");
        btnRefrescar.addActionListener(e -> refrescarTablaInventario());
        toolbar.add(btnRefrescar);

        panelCentro.add(toolbar, BorderLayout.NORTH);

        // 3. Tabla de Artículos
        String[] cols = {"Código", "Descripción", "Categoría Térmica", "Embalaje", "Precio Base", "Stock Físico", "Alerta Mínima", "Caducidad"};
        modelInventario = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableInventario = new JTable(modelInventario);
        tableInventario.setRowHeight(28);
        tableInventario.setAutoCreateRowSorter(true);
        tableInventario.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        tableInventario.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        sorterInventario = new TableRowSorter<>(modelInventario);
        tableInventario.setRowSorter(sorterInventario);

        // Renderizador con resaltado para alertas de stock mínimo crítico
        tableInventario.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    try {
                        String stockStr = table.getValueAt(row, 5).toString().replaceAll("[^0-9]", "");
                        String alertaStr = table.getValueAt(row, 6).toString().replaceAll("[^0-9]", "");
                        int stock = Integer.parseInt(stockStr);
                        int alerta = Integer.parseInt(alertaStr);
                        if (stock <= alerta) {
                            c.setBackground(new Color(255, 243, 205)); // Alerta suave amarilla/naranja
                        } else {
                            c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                        }
                    } catch (Exception ex) {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        panelCentro.add(new JScrollPane(tableInventario), BorderLayout.CENTER);

        // 4. Barra de Estado inferior de la pestaña
        lblStatusInventario = new JLabel("Catálogo cargado y listo para optimización algorítmica.");
        lblStatusInventario.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblStatusInventario.setForeground(new Color(70, 80, 95));
        lblStatusInventario.setBorder(new EmptyBorder(6, 4, 2, 4));
        panelCentro.add(lblStatusInventario, BorderLayout.SOUTH);

        panel.add(panelCentro, BorderLayout.CENTER);

        refrescarTablaInventario();
        return panel;
    }

    private void refrescarTablaInventario() {
        if (modelInventario == null) return;
        try {
            articulosMemoria = articuloService.listarDisponibles();
            poblarTablaInventario(articulosMemoria);
            lblStatusInventario.setText("Catálogo cargado con " + articulosMemoria.size() + " bultos estándares vigentes.");
            lblStatusInventario.setForeground(new Color(70, 80, 95));
        } catch (Exception e) {
            // Silencioso en modo desconectado
        }
    }

    private void poblarTablaInventario(List<Articulo> lista) {
        modelInventario.setRowCount(0);
        int totalBultos = 0;
        int frescos = 0;
        int congelados = 0;
        int criticos = 0;

        for (Articulo a : lista) {
            totalBultos += a.getStockDeposito();
            if (a.getCategoriaConservacion().contains("FRESCO")) {
                frescos += a.getStockDeposito();
            } else {
                congelados += a.getStockDeposito();
            }
            if (a.getStockDeposito() <= a.getStockMinimoAlerta()) {
                criticos++;
            }

            modelInventario.addRow(new Object[]{
                    a.getCodigo(), a.getDescripcion(), a.getCategoriaConservacion(),
                    a.getUnidadEmbalaje(), "$" + a.getPrecioVentaBase().setScale(2, RoundingMode.HALF_UP),
                    a.getStockDeposito() + " bultos", a.getStockMinimoAlerta() + " bultos",
                    a.getDiasCaducidadRecomendada() + " días"
            });
        }

        lblKpiTotalBultos.setText(String.valueOf(totalBultos));
        lblKpiBultosFrescos.setText(String.valueOf(frescos));
        lblKpiBultosCongelados.setText(String.valueOf(congelados));
        lblKpiCriticos.setText(String.valueOf(criticos));
    }

    private void aplicarFiltroInventario() {
        if (sorterInventario == null) return;
        String busqueda = txtBuscarArticulo.getText().trim();
        if (busqueda.isEmpty()) {
            sorterInventario.setRowFilter(null);
        } else {
            sorterInventario.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(busqueda), 0, 1, 2));
        }
    }

    private void ejecutarMergeSortEnPantalla() {
        if (articulosMemoria == null || articulosMemoria.isEmpty()) {
            lblStatusInventario.setText("No hay artículos en memoria para ordenar.");
            return;
        }

        // Ejecutar algoritmo clásico MergeSort
        List<Articulo> ordenados = articuloService.ordenarPorStockAscendente(articulosMemoria);
        poblarTablaInventario(ordenados);

        lblStatusInventario.setText("✅ Catálogo ordenado por stock en depósito (de menor a mayor).");
        lblStatusInventario.setForeground(new Color(25, 135, 84));
    }

    private void ejecutarBusquedaBinariaEnPantalla() {
        if (articulosMemoria == null || articulosMemoria.isEmpty()) {
            lblStatusInventario.setText("No hay artículos en catálogo para buscar.");
            return;
        }

        String codigoBuscado = txtBuscarArticulo.getText().trim();
        if (codigoBuscado.isEmpty()) {
            codigoBuscado = JOptionPane.showInputDialog(this,
                    "Ingrese el código del producto a localizar en inventario:",
                    "Localizar Producto", JOptionPane.QUESTION_MESSAGE);
            if (codigoBuscado == null || codigoBuscado.trim().isEmpty()) {
                return;
            }
            codigoBuscado = codigoBuscado.trim();
        }

        // Ordenar copia por código alfabético para satisfacer la precondición de búsqueda binaria
        List<Articulo> listaOrdenadaPorCodigo = new ArrayList<>(articulosMemoria);
        listaOrdenadaPorCodigo.sort(Comparator.comparing(Articulo::getCodigo, String.CASE_INSENSITIVE_ORDER));

        int indice = articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, codigoBuscado);

        if (indice >= 0) {
            Articulo encontrado = listaOrdenadaPorCodigo.get(indice);

            // Localizar y seleccionar la fila en la tabla visual
            for (int r = 0; r < tableInventario.getRowCount(); r++) {
                String codEnTabla = (String) tableInventario.getValueAt(r, 0);
                if (codEnTabla.equalsIgnoreCase(encontrado.getCodigo())) {
                    tableInventario.setRowSelectionInterval(r, r);
                    tableInventario.scrollRectToVisible(tableInventario.getCellRect(r, 0, true));
                    break;
                }
            }

            lblStatusInventario.setText(String.format(
                    "✅ Producto '%s - %s' localizado (Stock: %d bultos | $%s).",
                    encontrado.getCodigo(), encontrado.getDescripcion(),
                    encontrado.getStockDeposito(), encontrado.getPrecioVentaBase()));
            lblStatusInventario.setForeground(new Color(25, 135, 84));
        } else {
            lblStatusInventario.setText("⚠️ No se encontró ningún producto con el código '" + codigoBuscado + "'.");
            lblStatusInventario.setForeground(new Color(220, 53, 69));
        }
    }

    // =========================================================================
    // Métodos Auxiliares de UI
    // =========================================================================

    private JPanel crearKpiCard(String titulo, JLabel lblValor, Color colorValor) {
        JPanel card = new JPanel(new BorderLayout(4, 2));
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(215, 225, 235), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        card.setBackground(Color.WHITE);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        lblTitulo.setForeground(new Color(110, 120, 135));

        lblValor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblValor.setForeground(colorValor);

        card.add(lblTitulo, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    private void refrescarTodosLosDatos() {
        refrescarTablaCuentas();
        refrescarTablaInventario();
        if (entregaPanel != null) {
            entregaPanel.cargarDatosMaestros();
        }
    }

    private void mostrarGuiaRepartidor() {
        String guia = """
                ==========================================================
                GUÍA OPERATIVA EN RUTA PARA CHOFER / REPARTIDOR (SGCV)
                ==========================================================
                
                PASO 1: SELECCIONAR CLIENTE
                • Utilice el filtro de Zona para acotar la lista a su recorrido.
                • Observe la tarjeta de crédito: Verde = Habilitado | Rojo = Excedido.
                
                PASO 2: CARGA RÁPIDA DE BULTOS
                • Seleccione el artículo avícola (Cajas de Pollo, Pata-Muslo, etc.).
                • Use los botones rápidos [+1], [+5], [+10] para sumar bultos al instante.
                • Presione [➕ Agregar al Remito] para incorporar el renglón.
                • Las bonificaciones por volumen se calculan automáticamente:
                  - Frescos: 5% de descuento en pedidos >= 10 bultos.
                  - Congelados IQF: 4% de descuento en pedidos >= 20 bultos.
                
                PASO 3: CONFIRMAR Y EMITIR REMITO
                • Verifique el TOTAL REMITO en el display inferior.
                • Presione el botón grande [💾 CONFIRMAR Y EMITIR REMITO].
                • Si el cliente supera su crédito, el sistema le consultará si
                  desea registrar la excepción con autorización en ruta.
                
                ATAJOS DE TECLADO:
                • [F5] Refrescar todos los datos.
                • [Ctrl/Cmd + 1] Ir a Emisión de Remitos.
                • [Ctrl/Cmd + 2] Ir a Monitor de Cuentas Corrientes.
                • [Ctrl/Cmd + 3] Ir a Inventario de Depósito.
                """;

        JTextArea ta = new JTextArea(guia, 20, 60);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Guía Operativa para el Repartidor", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarAcercaDe() {
        String info = """
                SGCV - Sistema de Gestión Comercial y Distribución Avícola
                Seminario de Práctica (Universidad Siglo 21)
                
                Arquitectura de Software:
                • POO Java 17 con encapsulamiento, invariantes de dominio y polimorfismo.
                • Desacoplamiento de capas (Model, DAO, Service, UI Swing).
                • Algoritmos explícitos: MergeSort O(n log n) y Búsqueda Binaria O(log n).
                • Transacciones atómicas ACID (JDBC) y snapshots inmutables de precios.
                • Cobertura completa de pruebas unitarias JUnit 5.
                """;
        JOptionPane.showMessageDialog(this, info, "Acerca de SGCV", JOptionPane.INFORMATION_MESSAGE);
    }
}
