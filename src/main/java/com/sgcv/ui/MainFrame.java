package com.sgcv.ui;

import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloEstandar;
import com.sgcv.model.ArticuloPromocional;
import com.sgcv.service.ArticuloService;
import com.sgcv.service.ArqueoService;
import com.sgcv.service.ClienteService;
import com.sgcv.service.CobranzaService;
import com.sgcv.service.EntregaService;
import com.sgcv.service.IArticuloService;
import com.sgcv.service.IArqueoService;
import com.sgcv.service.IClienteService;
import com.sgcv.service.ICobranzaService;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Ventana Principal de la aplicación de escritorio SGCV.
 * Diseñada para el flujo real de trabajo en ruta y cierre nocturno:
 * 1. Parada Comercial en Ruta (CU-01 + CU-02): Carga táctil de los 6 bultos y cobranzas en camioneta.
 * 2. Cierre y Arqueo Diario (CU-04): Conciliación nocturna y segregación de fondos para frigoríficos.
 * 3. Cuentas Corrientes y Libro Mayor (CU-06): Auditoría cronológica de remitos (Debe) y recibos (Haber).
 * 4. Catálogo de Bultos (CU-05): Monitoreo de stock de depósito con ordenamiento y búsqueda rápida.
 */
public class MainFrame extends JFrame {

    private final IClienteService clienteService;
    private final IArticuloService articuloService;
    private final IEntregaService entregaService;
    private final ICobranzaService cobranzaService;
    private final IArqueoService arqueoService;

    private JTabbedPane tabbedPane;
    private EntregaPanel entregaPanel;
    private ArqueoPanel arqueoPanel;
    private CuentasCorrientesPanel cuentasCorrientesPanel;

    // Componentes del Panel Inventario
    private DefaultTableModel modelInventario;
    private JTable tableInventario;
    private TableRowSorter<DefaultTableModel> sorterInventario;
    private JTextField txtBuscarArticulo;
    private JButton btnDesactivar;
    private JLabel lblStatusInventario;
    private JLabel lblKpiTotalBultos;
    private JLabel lblKpiEstandar;
    private JLabel lblKpiPromocion;
    private JLabel lblKpiCriticos;
    private List<Articulo> articulosMemoria = new ArrayList<>();

    public MainFrame() {
        this(new ClienteService(), new ArticuloService(), new EntregaService(), new CobranzaService(), new ArqueoService());
    }

    public MainFrame(IClienteService clienteService, IArticuloService articuloService, IEntregaService entregaService) {
        this(clienteService, articuloService, entregaService, new CobranzaService(), new ArqueoService());
    }

    public MainFrame(IClienteService clienteService, IArticuloService articuloService, IEntregaService entregaService,
                     ICobranzaService cobranzaService, IArqueoService arqueoService) {
        super("SGCV - Sistema de Gestión Comercial y Distribución Avícola [Operación en Ruta]");
        this.clienteService = clienteService;
        this.articuloService = articuloService;
        this.entregaService = entregaService;
        this.cobranzaService = cobranzaService != null ? cobranzaService : new CobranzaService();
        this.arqueoService = arqueoService != null ? arqueoService : new ArqueoService();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 780);
        setMinimumSize(new Dimension(1020, 680));
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

        JMenuItem itemTab1 = new JMenuItem("1. Parada en Ruta (Remitos y Cobranzas)");
        itemTab1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemTab1.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        menuVistas.add(itemTab1);

        JMenuItem itemTab2 = new JMenuItem("2. Cierre y Arqueo Diario");
        itemTab2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemTab2.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        menuVistas.add(itemTab2);

        JMenuItem itemTab3 = new JMenuItem("3. Cuentas Corrientes y Extracto");
        itemTab3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemTab3.addActionListener(e -> tabbedPane.setSelectedIndex(2));
        menuVistas.add(itemTab3);

        JMenuItem itemTab4 = new JMenuItem("4. Catálogo de Bultos");
        itemTab4.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemTab4.addActionListener(e -> tabbedPane.setSelectedIndex(3));
        menuVistas.add(itemTab4);

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

        // Pestaña 1: Parada Comercial en Ruta (CU-01 + CU-02)
        entregaPanel = new EntregaPanel(entregaService, clienteService, articuloService, this::refrescarTodosLosDatos);
        tabbedPane.addTab("🚚 Parada en Ruta", entregaPanel);

        // Pestaña 2: Cierre y Arqueo Diario (CU-04)
        arqueoPanel = new ArqueoPanel(arqueoService, this::refrescarTodosLosDatos);
        tabbedPane.addTab("📊 Cierre y Arqueo Diario", arqueoPanel);

        // Pestaña 3: Cuentas Corrientes y Extracto (CU-06)
        cuentasCorrientesPanel = new CuentasCorrientesPanel(clienteService, cobranzaService, this::refrescarTodosLosDatos);
        tabbedPane.addTab("💳 Cuentas Corrientes y Extracto", cuentasCorrientesPanel);

        // Pestaña 4: Catálogo de Mercadería (CU-05)
        tabbedPane.addTab("📦 Catálogo de Bultos", crearPanelInventario());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // Panel: Inventario y Stock (CU-05) con Búsqueda y Ordenamiento
    // =========================================================================

    private JPanel crearPanelInventario() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 14, 14, 14));
        panel.setBackground(new Color(245, 247, 250));

        // 1. Tarjetas KPI
        JPanel panelKpi = new JPanel(new GridLayout(1, 4, 12, 0));
        panelKpi.setOpaque(false);
        lblKpiTotalBultos = new JLabel("0", SwingConstants.CENTER);
        lblKpiEstandar = new JLabel("0", SwingConstants.CENTER);
        lblKpiPromocion = new JLabel("0", SwingConstants.CENTER);
        lblKpiCriticos = new JLabel("0", SwingConstants.CENTER);

        panelKpi.add(crearKpiCard("TOTAL BULTOS EN DEPÓSITO", lblKpiTotalBultos, new Color(24, 43, 73)));
        panelKpi.add(crearKpiCard("ARTÍCULOS ESTÁNDAR", lblKpiEstandar, new Color(13, 110, 253)));
        panelKpi.add(crearKpiCard("ARTÍCULOS EN PROMOCIÓN", lblKpiPromocion, new Color(111, 66, 193)));
        panelKpi.add(crearKpiCard("ALERTAS CRÍTICAS (STOCK <= MÍNIMO)", lblKpiCriticos, new Color(220, 53, 69)));
        panel.add(panelKpi, BorderLayout.NORTH);

        // 2. Barra de herramientas integrada (Búsqueda en tiempo real + Ordenar por Stock + Ir a Código)
        JPanel panelCentro = new JPanel(new BorderLayout(0, 8));
        panelCentro.setOpaque(false);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolbar.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 225, 230), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        toolbar.setBackground(Color.WHITE);

        toolbar.add(new JLabel("🔍 Buscar:"));
        txtBuscarArticulo = new JTextField(18);
        txtBuscarArticulo.setToolTipText("Filtrado instantáneo por código o descripción");
        txtBuscarArticulo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltroInventario(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltroInventario(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltroInventario(); }
        });
        toolbar.add(txtBuscarArticulo);

        JButton btnNuevo = new JButton("➕ Nuevo Producto");
        btnNuevo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnNuevo.setBackground(new Color(25, 125, 55));
        btnNuevo.setForeground(Color.WHITE);
        btnNuevo.setToolTipText("Crear un nuevo producto en el catálogo general");
        btnNuevo.addActionListener(e -> abrirModalArticulo(null));
        toolbar.add(btnNuevo);

        JButton btnModificar = new JButton("✏️ Modificar");
        btnModificar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnModificar.setToolTipText("Editar descripción, embalaje, costo o precio del producto seleccionado");
        btnModificar.addActionListener(e -> modificarArticuloSeleccionado());
        toolbar.add(btnModificar);

        btnDesactivar = new JButton("🗑️ Desactivar");
        btnDesactivar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnDesactivar.setToolTipText("Desactivar o reactivar producto para el catálogo de venta");
        btnDesactivar.addActionListener(e -> alternarEstadoArticuloSeleccionado());
        toolbar.add(btnDesactivar);

        JButton btnBusquedaBinaria = new JButton("⚡ Ir a Código");
        btnBusquedaBinaria.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnBusquedaBinaria.setToolTipText("Buscar y resaltar producto por su código");
        btnBusquedaBinaria.addActionListener(e -> ejecutarBusquedaPorCodigo());
        toolbar.add(btnBusquedaBinaria);

        JButton btnRefrescar = new JButton("🔄 Recargar");
        btnRefrescar.addActionListener(e -> refrescarTablaInventario());
        toolbar.add(btnRefrescar);

        panelCentro.add(toolbar, BorderLayout.NORTH);

        // 3. Tabla de Artículos con Cabeceras Clickeables y Selección Unitaria
        String[] cols = {"Código", "Descripción", "Tipo Comercial", "Embalaje", "Costo Reposición", "Precio Venta", "Stock Depósito", "Alerta Mínima", "Estado"};
        modelInventario = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableInventario = new JTable(modelInventario);
        tableInventario.setRowHeight(28);
        tableInventario.setAutoCreateRowSorter(true);
        tableInventario.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableInventario.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        tableInventario.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        tableInventario.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarBotonEstadoInventario();
            }
        });

        tableInventario.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    modificarArticuloSeleccionado();
                }
            }
        });

        sorterInventario = new TableRowSorter<>(modelInventario);
        tableInventario.setRowSorter(sorterInventario);

        // Renderizador con soporte para inactivos y alertas de stock mínimo crítico
        tableInventario.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    try {
                        String estado = table.getValueAt(row, 8).toString();
                        boolean esInactivo = estado.contains("INACTIVO");
                        if (esInactivo) {
                            c.setBackground(new Color(245, 245, 245));
                            c.setForeground(new Color(130, 130, 130));
                        } else {
                            c.setForeground(Color.BLACK);
                            String stockStr = table.getValueAt(row, 6).toString().replaceAll("[^0-9]", "");
                            String alertaStr = table.getValueAt(row, 7).toString().replaceAll("[^0-9]", "");
                            int stock = Integer.parseInt(stockStr);
                            int alerta = Integer.parseInt(alertaStr);
                            if (stock <= alerta) {
                                c.setBackground(new Color(255, 243, 205)); // Alerta suave amarilla
                            } else {
                                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                            }
                        }
                    } catch (Exception ex) {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

        panelCentro.add(new JScrollPane(tableInventario), BorderLayout.CENTER);

        // 4. Barra de Estado inferior de la pestaña
        lblStatusInventario = new JLabel("Catálogo cargado y listo para consulta de stock.");
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
            articulosMemoria = articuloService.listarTodos();
            poblarTablaInventario(articulosMemoria);
            lblStatusInventario.setText("Catálogo cargado con " + articulosMemoria.size() + " bultos totales (activos e inactivos).");
            lblStatusInventario.setForeground(new Color(70, 80, 95));
            actualizarBotonEstadoInventario();
        } catch (Exception e) {
            // Silencioso en modo desconectado
        }
    }

    private void poblarTablaInventario(List<Articulo> lista) {
        modelInventario.setRowCount(0);
        int totalBultos = 0;
        int estandar = 0;
        int promocion = 0;
        int criticos = 0;

        for (Articulo a : lista) {
            if (a.isActivo()) {
                totalBultos += a.getStockDeposito();
                if (a.getTipoArticulo().contains("PROMOCIÓN")) {
                    promocion++;
                } else {
                    estandar++;
                }
                if (a.getStockDeposito() <= a.getStockMinimoAlerta()) {
                    criticos++;
                }
            }

            String estadoStr = a.isActivo() ? "🟢 ACTIVO" : "🔴 INACTIVO";
            modelInventario.addRow(new Object[]{
                    a.getCodigo(),
                    a.getDescripcion(),
                    a.getTipoArticulo(),
                    a.getUnidadEmbalaje(),
                    "$" + a.getPrecioCostoReposicion().setScale(2, RoundingMode.HALF_UP),
                    "$" + a.getPrecioVentaBase().setScale(2, RoundingMode.HALF_UP),
                    a.getStockDeposito() + " bultos",
                    a.getStockMinimoAlerta() + " bultos",
                    estadoStr
            });
        }

        lblKpiTotalBultos.setText(String.valueOf(totalBultos));
        lblKpiEstandar.setText(String.valueOf(estandar));
        lblKpiPromocion.setText(String.valueOf(promocion));
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

    private void ejecutarOrdenamientoPorStock() {
        if (articulosMemoria == null || articulosMemoria.isEmpty()) {
            lblStatusInventario.setText("No hay artículos en memoria para ordenar.");
            return;
        }

        List<Articulo> ordenados = articuloService.ordenarPorStockAscendente(articulosMemoria);
        poblarTablaInventario(ordenados);

        lblStatusInventario.setText("✔ Catálogo ordenado por stock en depósito (de menor a mayor).");
        lblStatusInventario.setForeground(new Color(25, 135, 84));
    }

    private void ejecutarBusquedaPorCodigo() {
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

        List<Articulo> listaOrdenadaPorCodigo = new ArrayList<>(articulosMemoria);
        listaOrdenadaPorCodigo.sort(Comparator.comparing(Articulo::getCodigo, String.CASE_INSENSITIVE_ORDER));

        int indice = articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, codigoBuscado);

        if (indice >= 0) {
            Articulo encontrado = listaOrdenadaPorCodigo.get(indice);

            for (int r = 0; r < tableInventario.getRowCount(); r++) {
                String codEnTabla = (String) tableInventario.getValueAt(r, 0);
                if (codEnTabla.equalsIgnoreCase(encontrado.getCodigo())) {
                    tableInventario.setRowSelectionInterval(r, r);
                    tableInventario.scrollRectToVisible(tableInventario.getCellRect(r, 0, true));
                    break;
                }
            }

            lblStatusInventario.setText(String.format(
                    "✔ Producto '%s - %s' localizado (Stock: %d bultos | $%s).",
                    encontrado.getCodigo(), encontrado.getDescripcion(),
                    encontrado.getStockDeposito(), encontrado.getPrecioVentaBase()));
            lblStatusInventario.setForeground(new Color(25, 135, 84));
        } else {
            lblStatusInventario.setText("⚠ No se encontró ningún producto con el código '" + codigoBuscado + "'.");
            lblStatusInventario.setForeground(new Color(220, 53, 69));
        }
    }

    private void modificarArticuloSeleccionado() {
        int selectedRow = tableInventario.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la tabla para modificar.",
                    "Selección Requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tableInventario.convertRowIndexToModel(selectedRow);
        String codigo = (String) modelInventario.getValueAt(modelRow, 0);
        Articulo encontrado = articulosMemoria.stream()
                .filter(a -> a.getCodigo().equalsIgnoreCase(codigo))
                .findFirst().orElse(null);

        if (encontrado != null) {
            abrirModalArticulo(encontrado);
        }
    }

    private void actualizarBotonEstadoInventario() {
        if (btnDesactivar == null) return;
        int selectedRow = tableInventario.getSelectedRow();
        if (selectedRow < 0) {
            btnDesactivar.setText("🗑️ Desactivar");
            btnDesactivar.setForeground(Color.DARK_GRAY);
            btnDesactivar.setToolTipText("Seleccione un producto para desactivar o reactivar");
            return;
        }
        int modelRow = tableInventario.convertRowIndexToModel(selectedRow);
        String codigo = (String) modelInventario.getValueAt(modelRow, 0);
        Articulo encontrado = articulosMemoria.stream()
                .filter(a -> a.getCodigo().equalsIgnoreCase(codigo))
                .findFirst().orElse(null);

        if (encontrado != null) {
            if (encontrado.isActivo()) {
                btnDesactivar.setText("🗑️ Desactivar");
                btnDesactivar.setForeground(new Color(190, 30, 20));
                btnDesactivar.setToolTipText("Desactivar producto para pausar su venta en ruta");
            } else {
                btnDesactivar.setText("🔄 Reactivar");
                btnDesactivar.setForeground(new Color(25, 125, 55));
                btnDesactivar.setToolTipText("Reactivar producto para volver a venderlo en ruta");
            }
        }
    }

    private void alternarEstadoArticuloSeleccionado() {
        int selectedRow = tableInventario.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la tabla para modificar su estado.",
                    "Selección Requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tableInventario.convertRowIndexToModel(selectedRow);
        String codigo = (String) modelInventario.getValueAt(modelRow, 0);
        Articulo encontrado = articulosMemoria.stream()
                .filter(a -> a.getCodigo().equalsIgnoreCase(codigo))
                .findFirst().orElse(null);

        if (encontrado != null) {
            if (encontrado.isActivo()) {
                int resp = JOptionPane.showConfirmDialog(this,
                        String.format("¿Desea desactivar el producto '%s - %s'?\n\n" +
                                        "El producto permanecerá visible en este catálogo como INACTIVO pero no se ofrecerá para venta en la camioneta.",
                                encontrado.getCodigo(), encontrado.getDescripcion()),
                        "Desactivar Producto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (resp == JOptionPane.YES_OPTION) {
                    try {
                        articuloService.eliminarArticulo(encontrado.getIdArticulo());
                        JOptionPane.showMessageDialog(this, "Producto desactivado correctamente.", "Catálogo Actualizado", JOptionPane.INFORMATION_MESSAGE);
                        refrescarTodosLosDatos();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Error al desactivar producto: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                int resp = JOptionPane.showConfirmDialog(this,
                        String.format("¿Desea reactivar el producto '%s - %s'?\n\n" +
                                        "Volverá a aparecer en la pantalla de parada en ruta para su venta directa.",
                                encontrado.getCodigo(), encontrado.getDescripcion()),
                        "Reactivar Producto", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (resp == JOptionPane.YES_OPTION) {
                    try {
                        articuloService.reactivarArticulo(encontrado.getIdArticulo());
                        JOptionPane.showMessageDialog(this, "Producto reactivado correctamente.", "Catálogo Actualizado", JOptionPane.INFORMATION_MESSAGE);
                        refrescarTodosLosDatos();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Error al reactivar producto: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    private void abrirModalArticulo(Articulo existente) {
        boolean esEdicion = existente != null;
        JDialog dialog = new JDialog(this, esEdicion ? "Modificar Producto (" + existente.getCodigo() + ")" : "Nuevo Bulto Avícola", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);

        JPanel pForm = new JPanel(new GridLayout(9, 2, 8, 8));
        pForm.setBorder(new EmptyBorder(15, 20, 15, 20));

        pForm.add(new JLabel("Código Único:"));
        JTextField txtCodigo = new JTextField(esEdicion ? existente.getCodigo() : "");
        txtCodigo.setToolTipText("Ejemplo: POLL-ENT-20, PATA-MUS-15, PECH-SUP-15");
        txtCodigo.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtCodigo::selectAll); }
        });
        if (esEdicion) txtCodigo.setEditable(false);
        pForm.add(txtCodigo);

        pForm.add(new JLabel("Descripción Comercial:"));
        JTextField txtDesc = new JTextField(esEdicion ? existente.getDescripcion() : "");
        txtDesc.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtDesc::selectAll); }
        });
        pForm.add(txtDesc);

        pForm.add(new JLabel("Tipo Comercial:"));
        JComboBox<String> comboTipo = new JComboBox<>(new String[]{
                "Estándar (sin bonificación)",
                "Promocional (5% desc. >= 10 bultos)"
        });
        if (esEdicion && existente.getTipoArticulo().contains("PROMOCIÓN")) {
            comboTipo.setSelectedIndex(1);
        }
        pForm.add(comboTipo);

        pForm.add(new JLabel("Unidad de Embalaje:"));
        JTextField txtEmbalaje = new JTextField(esEdicion ? existente.getUnidadEmbalaje() : "Caja x 20kg");
        txtEmbalaje.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtEmbalaje::selectAll); }
        });
        pForm.add(txtEmbalaje);

        pForm.add(new JLabel("Costo de Reposición ($):"));
        JTextField txtCosto = new JTextField(esEdicion ? String.format("%.2f", existente.getPrecioCostoReposicion()).replace(",", ".") : "");
        txtCosto.setToolTipText("Ejemplo: 28000 o 28000.50");
        txtCosto.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtCosto::selectAll); }
        });
        pForm.add(txtCosto);

        pForm.add(new JLabel("Precio de Venta Base ($):"));
        JTextField txtPrecio = new JTextField(esEdicion ? String.format("%.2f", existente.getPrecioVentaBase()).replace(",", ".") : "");
        txtPrecio.setToolTipText("Ejemplo: 35000 o 35000.00");
        txtPrecio.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtPrecio::selectAll); }
        });
        pForm.add(txtPrecio);

        pForm.add(new JLabel("Stock Depósito (bultos):"));
        JTextField txtStock = new JTextField(esEdicion ? String.valueOf(existente.getStockDeposito()) : "0");
        txtStock.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtStock::selectAll); }
        });
        pForm.add(txtStock);

        pForm.add(new JLabel("Stock Mínimo de Alerta:"));
        JTextField txtStockMin = new JTextField(esEdicion ? String.valueOf(existente.getStockMinimoAlerta()) : "5");
        txtStockMin.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtStockMin::selectAll); }
        });
        pForm.add(txtStockMin);

        pForm.add(new JLabel("Estado:"));
        JCheckBox chkActivo = new JCheckBox("Activo para venta", esEdicion ? existente.isActivo() : true);
        pForm.add(chkActivo);

        dialog.add(pForm, BorderLayout.CENTER);

        JPanel pBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());
        pBotones.add(btnCancelar);

        JButton btnGuardar = new JButton(esEdicion ? "✔ Guardar Cambios" : "➕ Crear Producto");
        btnGuardar.setBackground(new Color(25, 120, 50));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> {
            try {
                String codigo = txtCodigo.getText().trim().toUpperCase();
                String desc = txtDesc.getText().trim();
                String emb = txtEmbalaje.getText().trim();
                BigDecimal costo = parseMonto(txtCosto.getText());
                BigDecimal precio = parseMonto(txtPrecio.getText());
                if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                    JOptionPane.showMessageDialog(dialog, "El precio de venta base debe ser mayor a cero.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String strStock = txtStock.getText().trim();
                int stock = strStock.isEmpty() ? 0 : Integer.parseInt(strStock);
                String strStockMin = txtStockMin.getText().trim();
                int stockMin = strStockMin.isEmpty() ? 0 : Integer.parseInt(strStockMin);
                boolean activo = chkActivo.isSelected();

                boolean esPromo = comboTipo.getSelectedIndex() == 1;
                Articulo nuevoArt = esPromo ?
                        new ArticuloPromocional(esEdicion ? existente.getIdArticulo() : 0, codigo, desc, emb, costo, precio, stock, stockMin, activo) :
                        new ArticuloEstandar(esEdicion ? existente.getIdArticulo() : 0, codigo, desc, emb, costo, precio, stock, stockMin, activo);

                if (esEdicion) {
                    articuloService.actualizarArticulo(nuevoArt);
                    JOptionPane.showMessageDialog(dialog, "Producto actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    articuloService.crearArticulo(nuevoArt);
                    JOptionPane.showMessageDialog(dialog, "Producto creado exitosamente con código " + codigo + ".", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                }

                dialog.dispose();
                refrescarTodosLosDatos();

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Valores numéricos inválidos en costo, precio o stock.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        pBotones.add(btnGuardar);
        dialog.add(pBotones, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }


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
        if (entregaPanel != null) entregaPanel.recargarDatosMaestros();
        if (arqueoPanel != null) arqueoPanel.recargarMetricas();
        if (cuentasCorrientesPanel != null) cuentasCorrientesPanel.recargarClientes(null);
        refrescarTablaInventario();
    }

    private void mostrarGuiaRepartidor() {
        String guia = """
                ==========================================================
                GUÍA OPERATIVA EN RUTA PARA CHOFER / REPARTIDOR (SGCV)
                ==========================================================
                
                PESTAÑA 1: PARADA EN RUTA (REMITO Y COBRANZA)
                • Seleccione el cliente de la lista (puede filtrar por zona).
                • Verifique el Saldo Deudor y el Crédito Disponible.
                • Seleccione los bultos entregados directamente con [+], [-] o [+5].
                • Si el cliente paga en la parada, ingrese el monto en Efectivo o
                  Transferencia bancaria.
                • El botón inferior adapta su función automáticamente:
                  - Si entrega bultos sin cobro: EMITE REMITO A CUENTA CORRIENTE.
                  - Si cobra deuda anterior sin dejar bultos: EMITE RECIBO DE PAGO.
                  - Si entrega y cobra simultáneamente: EMITE REMITO Y RECIBO.
                
                PESTAÑA 2: CIERRE Y ARQUEO DIARIO
                • Consolida los bultos entregados y la facturación de la jornada.
                • Ingrese el Efectivo Real Contado en mano: el sistema calcula
                  inmediatamente si la caja está cuadrada o si existe desvío.
                • Segrega automáticamente el SOBRE PARA FRIGORÍFICO (Fondo de
                  reposición intocable para pagar a Soychú/Tres Arroyos) del
                  MARGEN NETO (ganancia real comercial del día).
                • Presione [CERRAR ARQUEO Y GUARDAR JORNADA] para asentar el cierre.
                
                PESTAÑA 3: CUENTAS CORRIENTES Y LIBRO MAYOR
                • Seleccione cualquier cliente para consultar su extracto histórico.
                • Visualice cada comprobante cronológico: Remitos (Debe), Recibos (Haber)
                  y el saldo acumulado en tiempo real.
                • Permite registrar cobranzas directas a cuenta corriente.
                
                ATAJOS DE TECLADO:
                • [F5] Refrescar todos los datos.
                • [Ctrl/Cmd + 1] Parada en Ruta.
                • [Ctrl/Cmd + 2] Cierre y Arqueo Diario.
                • [Ctrl/Cmd + 3] Cuentas Corrientes y Extracto.
                • [Ctrl/Cmd + 4] Catálogo de Bultos.
                """;

        JTextArea ta = new JTextArea(guia, 22, 62);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Guía Operativa para el Repartidor", JOptionPane.INFORMATION_MESSAGE);
    }

    private BigDecimal parseMonto(String texto) {
        if (texto == null || texto.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            String limpio = texto.trim().replace("$", "").replace(" ", "");
            if (limpio.isEmpty()) return BigDecimal.ZERO;
            if (limpio.contains(",") && limpio.contains(".")) {
                limpio = limpio.replace(".", "").replace(",", ".");
            } else if (limpio.contains(",")) {
                limpio = limpio.replace(",", ".");
            }
            return new BigDecimal(limpio);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void mostrarAcercaDe() {
        String info = """
                SGCV - Sistema de Gestión Comercial y Distribución Avícola
                Seminario de Práctica (Universidad Siglo 21)
                
                Arquitectura de Software:
                • POO Java 17 con encapsulamiento, invariantes de dominio y polimorfismo.
                • Desacoplamiento de capas (Model, DAO, Service, UI Swing).
                • Algoritmos: MergeSort O(n log n) y Búsqueda Binaria O(log n).
                • Transacciones atómicas ACID (JDBC) y snapshots inmutables de precios.
                • Cobertura completa de pruebas unitarias JUnit 5.
                """;
        JOptionPane.showMessageDialog(this, info, "Acerca de SGCV", JOptionPane.INFORMATION_MESSAGE);
    }
}
