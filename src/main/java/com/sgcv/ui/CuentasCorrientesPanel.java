package com.sgcv.ui;

import com.sgcv.model.Cliente;
import com.sgcv.model.Cobranza;
import com.sgcv.model.DetalleCobranza;
import com.sgcv.model.MovimientoCuentaCorriente;
import com.sgcv.service.CobranzaService;
import com.sgcv.service.IClienteService;
import com.sgcv.service.ICobranzaService;

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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pantalla 3: Cartera de Clientes y Cuentas Corrientes con Libro Mayor (CU-06).
 * Al ingresar presenta la tabla maestra de clientes (ordenada por updated_at DESC)
 * con CRUD para crear y actualizar información comercial y límites de crédito.
 * Permite seleccionar cualquier cliente para auditar su extracto cronológico (Debe/Haber).
 */
public class CuentasCorrientesPanel extends JPanel {

    private static final String CARD_LISTA = "LISTA";
    private static final String CARD_EXTRACTO = "EXTRACTO";

    private final IClienteService clienteService;
    private final ICobranzaService cobranzaService;
    private final Runnable onDatosModificados;

    private CardLayout cardLayout;
    private List<Cliente> listaClientes = new ArrayList<>();
    private Cliente clienteSeleccionado;

    // Componentes de la Vista 1: Tabla Maestra de Clientes
    private DefaultTableModel modelClientes;
    private JTable tableClientes;
    private TableRowSorter<DefaultTableModel> sorterClientes;
    private JTextField txtBuscarCliente;
    private JLabel lblTotalClientes;

    // Componentes de la Vista 2: Extracto de Cuenta Corriente (Libro Mayor)
    private ClienteAutoCompleteBox autoCompleteCliente;
    private JLabel lblTituloClienteExtracto;
    private JLabel lblRazonSocial;
    private JLabel lblCuitZona;
    private JLabel lblSaldoDeudor;
    private JLabel lblLimiteCredito;
    private JLabel lblCreditoDisponible;
    private JLabel lblEstadoBadge;
    private JButton btnCobroDirecto;

    private DefaultTableModel modelExtracto;
    private JTable tableExtracto;
    private JLabel lblTotalDebe;
    private JLabel lblTotalHaber;
    private JLabel lblSaldoFinal;

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CuentasCorrientesPanel(IClienteService clienteService, ICobranzaService cobranzaService, Runnable onDatosModificados) {
        this.clienteService = clienteService;
        this.cobranzaService = cobranzaService != null ? cobranzaService : new CobranzaService();
        this.onDatosModificados = onDatosModificados;

        inicializarUI();
        recargarClientes(null);
    }

    private void inicializarUI() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        setBackground(new Color(245, 247, 250));

        // Vista 1: Catálogo y CRUD de Clientes (Pantalla Principal de Entrada)
        add(crearVistaListaClientes(), CARD_LISTA);

        // Vista 2: Libro Mayor / Extracto del Cliente Seleccionado
        add(crearVistaExtractoCliente(), CARD_EXTRACTO);

        cardLayout.show(this, CARD_LISTA);
    }

    // =========================================================================
    // VISTA 1: Catálogo y CRUD de Clientes (Ordenado por updated_at DESC)
    // =========================================================================

    private JPanel crearVistaListaClientes() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 14, 14, 14));
        panel.setBackground(new Color(245, 247, 250));

        // Cabecera y Barra de Herramientas
        JPanel pNorte = new JPanel(new BorderLayout(0, 8));
        pNorte.setOpaque(false);

        // Título
        JPanel pTit = new JPanel(new GridLayout(2, 1, 0, 2));
        pTit.setOpaque(false);

        JLabel lblTit = new JLabel("👥 Cartera de Clientes y Cuentas Corrientes");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        lblTit.setForeground(new Color(20, 35, 60));

        JLabel lblSub = new JLabel("Administración comercial de clientes minoristas, líneas de crédito y estados de saldo (ordenado por última actividad):");
        lblSub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblSub.setForeground(new Color(90, 100, 115));

        pTit.add(lblTit);
        pTit.add(lblSub);
        pNorte.add(pTit, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));

        toolbar.add(new JLabel("🔍 Buscar:"));
        txtBuscarCliente = new JTextField(16);
        txtBuscarCliente.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        txtBuscarCliente.setToolTipText("Filtrado en tiempo real por razón social, CUIT, zona o dirección");
        txtBuscarCliente.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltroClientes(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltroClientes(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltroClientes(); }
        });
        toolbar.add(txtBuscarCliente);

        JButton btnNuevo = new JButton("➕ Nuevo Cliente");
        btnNuevo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnNuevo.setBackground(new Color(22, 101, 52));
        btnNuevo.setForeground(Color.WHITE);
        btnNuevo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNuevo.addActionListener(e -> abrirModalNuevoCliente());
        toolbar.add(btnNuevo);

        JButton btnModificar = new JButton("✏️ Modificar");
        btnModificar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnModificar.setBackground(new Color(15, 60, 130));
        btnModificar.setForeground(Color.WHITE);
        btnModificar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnModificar.addActionListener(e -> abrirModalEditarClienteSeleccionado());
        toolbar.add(btnModificar);

        JButton btnVerExtracto = new JButton("📖 Ver Cuenta Corriente");
        btnVerExtracto.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnVerExtracto.setBackground(new Color(90, 45, 130));
        btnVerExtracto.setForeground(Color.WHITE);
        btnVerExtracto.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerExtracto.addActionListener(e -> abrirExtractoClienteSeleccionado());
        toolbar.add(btnVerExtracto);

        JButton btnCobrar = new JButton("💵 Cobrar Deuda");
        btnCobrar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnCobrar.setBackground(new Color(30, 125, 60));
        btnCobrar.setForeground(Color.WHITE);
        btnCobrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCobrar.addActionListener(e -> cobrarClienteSeleccionado());
        toolbar.add(btnCobrar);

        JButton btnRefrescar = new JButton("🔄 Refrescar");
        btnRefrescar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnRefrescar.addActionListener(e -> recargarClientes(null));
        toolbar.add(btnRefrescar);

        pNorte.add(toolbar, BorderLayout.CENTER);
        panel.add(pNorte, BorderLayout.NORTH);

        // Tabla de Clientes
        String[] cols = {
                "ID", "Razón Social", "CUIT", "Zona Reparto", "Teléfono", "Dirección",
                "Saldo Deudor ($)", "Límite Crédito ($)", "Disponible ($)", "Estado", "Últ. Modificación"
        };

        modelClientes = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Integer.class;
                if (columnIndex == 6 || columnIndex == 7 || columnIndex == 8) return BigDecimal.class;
                return String.class;
            }
        };

        tableClientes = new JTable(modelClientes);
        tableClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableClientes.setRowHeight(28);
        tableClientes.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tableClientes.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        tableClientes.getTableHeader().setReorderingAllowed(false);

        sorterClientes = new TableRowSorter<>(modelClientes);
        tableClientes.setRowSorter(sorterClientes);

        // Alineación y Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tableClientes.getColumnModel().getColumn(0).setPreferredWidth(45);
        tableClientes.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        tableClientes.getColumnModel().getColumn(1).setPreferredWidth(190);
        tableClientes.getColumnModel().getColumn(2).setPreferredWidth(110);
        tableClientes.getColumnModel().getColumn(2).setCellRenderer(centerRender);

        tableClientes.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableClientes.getColumnModel().getColumn(4).setPreferredWidth(110);
        tableClientes.getColumnModel().getColumn(5).setPreferredWidth(180);

        // Renderers Monetarios
        DefaultTableCellRenderer renderSaldo = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                if (value instanceof BigDecimal bd) {
                    l.setText(String.format("$%,.2f", bd));
                    l.setForeground(bd.compareTo(BigDecimal.ZERO) > 0 ? new Color(180, 20, 20) : new Color(30, 120, 50));
                }
                return l;
            }
        };
        tableClientes.getColumnModel().getColumn(6).setCellRenderer(renderSaldo);

        DefaultTableCellRenderer renderLimite = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                if (value instanceof BigDecimal bd) {
                    l.setText(String.format("$%,.2f", bd));
                    l.setForeground(new Color(60, 75, 95));
                }
                return l;
            }
        };
        tableClientes.getColumnModel().getColumn(7).setCellRenderer(renderLimite);

        DefaultTableCellRenderer renderDisponible = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                if (value instanceof BigDecimal bd) {
                    l.setText(String.format("$%,.2f", bd));
                    l.setForeground(bd.compareTo(BigDecimal.ZERO) > 0 ? new Color(25, 125, 50) : new Color(180, 20, 20));
                }
                return l;
            }
        };
        tableClientes.getColumnModel().getColumn(8).setCellRenderer(renderDisponible);

        // Badge de Estado Crediticio
        tableClientes.getColumnModel().getColumn(9).setPreferredWidth(100);
        tableClientes.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                String st = value != null ? value.toString() : "";
                if (st.contains("EXCEDIDO")) {
                    l.setForeground(new Color(185, 25, 25));
                } else if (st.contains("RIESGO")) {
                    l.setForeground(new Color(190, 110, 10));
                } else {
                    l.setForeground(new Color(25, 125, 50));
                }
                return l;
            }
        });

        tableClientes.getColumnModel().getColumn(10).setPreferredWidth(120);
        tableClientes.getColumnModel().getColumn(10).setCellRenderer(centerRender);

        // Doble click o Enter para abrir cuenta corriente
        tableClientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirExtractoClienteSeleccionado();
                }
            }
        });

        tableClientes.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    abrirExtractoClienteSeleccionado();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tableClientes);
        panel.add(scroll, BorderLayout.CENTER);

        // Pie de Tabla
        JPanel pPie = new JPanel(new BorderLayout());
        pPie.setOpaque(false);
        lblTotalClientes = new JLabel("Cargando clientes...");
        lblTotalClientes.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblTotalClientes.setForeground(new Color(90, 100, 115));
        pPie.add(lblTotalClientes, BorderLayout.WEST);

        JLabel lblTip = new JLabel("💡 Doble clic o [Enter] sobre un cliente para ver su Libro Mayor / Extracto");
        lblTip.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        lblTip.setForeground(new Color(100, 115, 130));
        pPie.add(lblTip, BorderLayout.EAST);

        panel.add(pPie, BorderLayout.SOUTH);

        return panel;
    }

    private void aplicarFiltroClientes() {
        if (sorterClientes == null) return;
        String busqueda = txtBuscarCliente.getText().trim();
        if (busqueda.isEmpty()) {
            sorterClientes.setRowFilter(null);
        } else {
            sorterClientes.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(busqueda), 1, 2, 3, 5));
        }
    }

    private Cliente getClienteDeFilaSeleccionada() {
        int selectedRow = tableClientes.getSelectedRow();
        if (selectedRow < 0) return null;
        int modelRow = tableClientes.convertRowIndexToModel(selectedRow);
        int idCliente = (Integer) modelClientes.getValueAt(modelRow, 0);
        return listaClientes.stream()
                .filter(c -> c.getIdCliente() == idCliente)
                .findFirst().orElse(null);
    }

    private void abrirModalNuevoCliente() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        ClienteDialog dialog = new ClienteDialog(parent, clienteService, null, () -> {
            recargarClientes(null);
            if (onDatosModificados != null) onDatosModificados.run();
        });
        dialog.setVisible(true);
    }

    private void abrirModalEditarClienteSeleccionado() {
        Cliente c = getClienteDeFilaSeleccionada();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un cliente de la tabla para modificar sus datos.",
                    "Selección Requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window parent = SwingUtilities.getWindowAncestor(this);
        ClienteDialog dialog = new ClienteDialog(parent, clienteService, c, () -> {
            recargarClientes(c);
            if (onDatosModificados != null) onDatosModificados.run();
        });
        dialog.setVisible(true);
    }

    private void abrirExtractoClienteSeleccionado() {
        Cliente c = getClienteDeFilaSeleccionada();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un cliente para consultar su extracto de cuenta corriente.",
                    "Selección Requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        mostrarExtracto(c);
    }

    private void cobrarClienteSeleccionado() {
        Cliente c = getClienteDeFilaSeleccionada();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un cliente para registrar la cobranza de su deuda.",
                    "Selección Requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        this.clienteSeleccionado = c;
        abrirModalCobroDirecto();
    }

    // =========================================================================
    // VISTA 2: Extracto de Cuenta Corriente (Libro Mayor)
    // =========================================================================

    private JPanel crearVistaExtractoCliente() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 14, 14, 14));
        panel.setBackground(new Color(245, 247, 250));

        // 1. Barra de Navegación y Selector
        JPanel pNorte = new JPanel(new BorderLayout(0, 8));
        pNorte.setOpaque(false);

        JPanel pNav = new JPanel(new BorderLayout(12, 0));
        pNav.setBackground(Color.WHITE);
        pNav.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));

        JButton btnVolver = new JButton("⬅️ Volver al Listado de Clientes");
        btnVolver.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnVolver.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> cardLayout.show(this, CARD_LISTA));
        pNav.add(btnVolver, BorderLayout.WEST);

        lblTituloClienteExtracto = new JLabel("📖 Extracto Histórico y Libro Mayor", SwingConstants.CENTER);
        lblTituloClienteExtracto.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblTituloClienteExtracto.setForeground(new Color(20, 35, 60));
        pNav.add(lblTituloClienteExtracto, BorderLayout.CENTER);

        JPanel pSelectorDer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pSelectorDer.setOpaque(false);
        autoCompleteCliente = new ClienteAutoCompleteBox();
        autoCompleteCliente.setOnClienteSeleccionadoListener(this::onClienteSeleccionado);
        pSelectorDer.add(new JLabel("Cambiar:"));
        pSelectorDer.add(autoCompleteCliente);
        pNav.add(pSelectorDer, BorderLayout.EAST);

        pNorte.add(pNav, BorderLayout.NORTH);
        pNorte.add(crearTarjetaCliente(), BorderLayout.CENTER);
        panel.add(pNorte, BorderLayout.NORTH);

        // 2. Panel Central: Libro Mayor Cronológico
        panel.add(crearPanelLibroMayor(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearTarjetaCliente() {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(205, 215, 225), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Info General (Izquierda)
        JPanel panelInfo = new JPanel(new GridLayout(2, 1, 0, 4));
        panelInfo.setOpaque(false);

        lblRazonSocial = new JLabel("Seleccione un cliente para consultar su extracto");
        lblRazonSocial.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        lblRazonSocial.setForeground(new Color(18, 38, 58));

        lblCuitZona = new JLabel("CUIT: - | Zona: - | Dirección: -");
        lblCuitZona.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblCuitZona.setForeground(new Color(100, 110, 125));

        panelInfo.add(lblRazonSocial);
        panelInfo.add(lblCuitZona);
        card.add(panelInfo, BorderLayout.WEST);

        // Métricas Financieras (Centro)
        JPanel panelMetricas = new JPanel(new GridLayout(1, 4, 12, 0));
        panelMetricas.setOpaque(false);

        lblSaldoDeudor = new JLabel("$0.00", SwingConstants.CENTER);
        lblLimiteCredito = new JLabel("$0.00", SwingConstants.CENTER);
        lblCreditoDisponible = new JLabel("$0.00", SwingConstants.CENTER);
        lblEstadoBadge = new JLabel("AL DÍA", SwingConstants.CENTER);

        panelMetricas.add(crearMiniCard("SALDO DEUDOR", lblSaldoDeudor, new Color(180, 40, 20)));
        panelMetricas.add(crearMiniCard("LÍMITE CRÉDITO", lblLimiteCredito, new Color(50, 70, 95)));
        panelMetricas.add(crearMiniCard("DISPONIBLE", lblCreditoDisponible, new Color(25, 120, 50)));
        panelMetricas.add(crearMiniCard("ESTADO", lblEstadoBadge, new Color(24, 43, 73)));

        card.add(panelMetricas, BorderLayout.CENTER);

        // Botón Cobro Rápido (Derecha)
        btnCobroDirecto = new JButton("💵 Cobrar Deuda");
        btnCobroDirecto.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        btnCobroDirecto.setBackground(new Color(30, 125, 60));
        btnCobroDirecto.setForeground(Color.WHITE);
        btnCobroDirecto.setFocusPainted(false);
        btnCobroDirecto.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCobroDirecto.setToolTipText("Registrar pago de deuda en efectivo o transferencia para este cliente");
        btnCobroDirecto.addActionListener(e -> abrirModalCobroDirecto());
        card.add(btnCobroDirecto, BorderLayout.EAST);

        return card;
    }

    private JPanel crearMiniCard(String titulo, JLabel lblValor, Color colorValor) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        lblTit.setForeground(new Color(110, 120, 135));

        lblValor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        lblValor.setForeground(colorValor);

        p.add(lblTit, BorderLayout.NORTH);
        p.add(lblValor, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearPanelLibroMayor() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblTit = new JLabel("📖 Movimientos Cronológicos del Libro Mayor (Remitos y Recibos)");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblTit.setForeground(new Color(20, 35, 60));
        panel.add(lblTit, BorderLayout.NORTH);

        String[] cols = {"Fecha / Hora", "Tipo", "N° Comprobante", "Concepto / Detalle", "Debe (+)", "Haber (-)", "Saldo Deudor"};
        modelExtracto = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4 || columnIndex == 5 || columnIndex == 6) return BigDecimal.class;
                return String.class;
            }
        };

        tableExtracto = new JTable(modelExtracto);
        tableExtracto.setRowHeight(26);
        tableExtracto.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tableExtracto.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        tableExtracto.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tableExtracto.getColumnModel().getColumn(0).setPreferredWidth(130);
        tableExtracto.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        tableExtracto.getColumnModel().getColumn(1).setPreferredWidth(90);
        tableExtracto.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                String tipo = value != null ? value.toString() : "";
                if ("REMITO".equalsIgnoreCase(tipo)) {
                    l.setForeground(new Color(15, 80, 160));
                    l.setText("🚚 REMITO");
                } else if ("RECIBO".equalsIgnoreCase(tipo)) {
                    l.setForeground(new Color(25, 125, 55));
                    l.setText("💵 RECIBO");
                }
                return l;
            }
        });

        tableExtracto.getColumnModel().getColumn(2).setPreferredWidth(140);
        tableExtracto.getColumnModel().getColumn(3).setPreferredWidth(320);

        DefaultTableCellRenderer renderDebe = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                if (value instanceof BigDecimal bd && bd.compareTo(BigDecimal.ZERO) > 0) {
                    l.setText(String.format("$%,.2f", bd));
                    l.setForeground(new Color(175, 40, 20));
                } else {
                    l.setText("-");
                    l.setForeground(Color.GRAY);
                }
                return l;
            }
        };
        tableExtracto.getColumnModel().getColumn(4).setCellRenderer(renderDebe);

        DefaultTableCellRenderer renderHaber = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                if (value instanceof BigDecimal bd && bd.compareTo(BigDecimal.ZERO) > 0) {
                    l.setText(String.format("$%,.2f", bd));
                    l.setForeground(new Color(25, 120, 50));
                } else {
                    l.setText("-");
                    l.setForeground(Color.GRAY);
                }
                return l;
            }
        };
        tableExtracto.getColumnModel().getColumn(5).setCellRenderer(renderHaber);

        DefaultTableCellRenderer renderSaldo = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                if (value instanceof BigDecimal bd) {
                    l.setText(String.format("$%,.2f", bd));
                    l.setForeground(bd.compareTo(BigDecimal.ZERO) > 0 ? new Color(170, 30, 20) : new Color(30, 110, 50));
                }
                return l;
            }
        };
        tableExtracto.getColumnModel().getColumn(6).setCellRenderer(renderSaldo);

        JScrollPane scroll = new JScrollPane(tableExtracto);
        panel.add(scroll, BorderLayout.CENTER);

        // Pie de Totales Acumulados
        JPanel panelPie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 6));
        panelPie.setBackground(new Color(248, 250, 253));
        panelPie.setBorder(new LineBorder(new Color(225, 230, 238), 1));

        lblTotalDebe = new JLabel("Total Entregado (Debe): $0.00");
        lblTotalDebe.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        lblTotalDebe.setForeground(new Color(160, 40, 20));

        lblTotalHaber = new JLabel("Total Cobrado (Haber): $0.00");
        lblTotalHaber.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        lblTotalHaber.setForeground(new Color(20, 115, 45));

        lblSaldoFinal = new JLabel("Saldo Actual: $0.00");
        lblSaldoFinal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblSaldoFinal.setForeground(new Color(15, 35, 60));

        panelPie.add(lblTotalDebe);
        panelPie.add(lblTotalHaber);
        panelPie.add(lblSaldoFinal);
        panel.add(panelPie, BorderLayout.SOUTH);

        return panel;
    }

    public void mostrarExtracto(Cliente c) {
        if (c == null) return;
        this.clienteSeleccionado = c;
        if (autoCompleteCliente != null) {
            autoCompleteCliente.seleccionarCliente(c);
        }
        actualizarTarjetaCliente(c);
        cargarExtracto(c.getIdCliente());
        lblTituloClienteExtracto.setText("📖 Extracto Histórico: " + c.getRazonSocial());
        cardLayout.show(this, CARD_EXTRACTO);
    }

    public void recargarClientes(Cliente seleccionar) {
        try {
            listaClientes = clienteService.listarActivos();

            // 1. Actualizar Tabla Maestra de Clientes
            modelClientes.setRowCount(0);
            for (Cliente c : listaClientes) {
                String badgeEstado;
                if (c.estaExcedido(BigDecimal.ZERO)) {
                    badgeEstado = "🔴 EXCEDIDO";
                } else if (c.getSaldoCuentaCorriente().compareTo(c.getLimiteCredito().multiply(new BigDecimal("0.85"))) >= 0
                        && c.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0) {
                    badgeEstado = "⚠️ RIESGO";
                } else {
                    badgeEstado = "✅ AL DÍA";
                }

                String fechaMod = c.getUpdatedAt() != null ? c.getUpdatedAt().format(FORMATO_FECHA_HORA) : "-";

                modelClientes.addRow(new Object[]{
                        c.getIdCliente(),
                        c.getRazonSocial(),
                        c.getCuit(),
                        c.getZonaReparto(),
                        c.getTelefono(),
                        c.getDireccion(),
                        c.getSaldoCuentaCorriente(),
                        c.getLimiteCredito(),
                        c.calcularCreditoDisponible(),
                        badgeEstado,
                        fechaMod
                });
            }

            lblTotalClientes.setText(String.format("Mostrando %d clientes registrados (ordenados por última modificación descendente).", listaClientes.size()));

            // 2. Actualizar Autocompletar del Extracto
            if (autoCompleteCliente != null) {
                autoCompleteCliente.setTodosLosClientes(listaClientes);
            }

            // 3. Restaurar selección si existe
            if (seleccionar != null) {
                Cliente encontrado = listaClientes.stream()
                        .filter(c -> c.getIdCliente() == seleccionar.getIdCliente())
                        .findFirst().orElse(null);
                if (encontrado != null) {
                    this.clienteSeleccionado = encontrado;
                    seleccionarEnTabla(encontrado.getIdCliente());
                    if (autoCompleteCliente != null) autoCompleteCliente.seleccionarCliente(encontrado);
                    actualizarTarjetaCliente(encontrado);
                    cargarExtracto(encontrado.getIdCliente());
                }
            } else if (!listaClientes.isEmpty()) {
                tableClientes.setRowSelectionInterval(0, 0);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar clientes: " + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarEnTabla(int idCliente) {
        for (int r = 0; r < tableClientes.getRowCount(); r++) {
            int modelRow = tableClientes.convertRowIndexToModel(r);
            if ((Integer) modelClientes.getValueAt(modelRow, 0) == idCliente) {
                tableClientes.setRowSelectionInterval(r, r);
                tableClientes.scrollRectToVisible(tableClientes.getCellRect(r, 0, true));
                break;
            }
        }
    }

    private void onClienteSeleccionado(Cliente c) {
        this.clienteSeleccionado = c;
        if (clienteSeleccionado == null) {
            limpiarVistaExtracto();
            return;
        }

        lblTituloClienteExtracto.setText("📖 Extracto Histórico: " + c.getRazonSocial());
        actualizarTarjetaCliente(clienteSeleccionado);
        cargarExtracto(clienteSeleccionado.getIdCliente());
    }

    private void actualizarTarjetaCliente(Cliente c) {
        lblRazonSocial.setText(c.getRazonSocial());
        lblCuitZona.setText(String.format("CUIT: %s | Zona: %s | Dirección: %s",
                c.getCuit(), c.getZonaReparto(), c.getDireccion()));

        lblSaldoDeudor.setText(String.format("$%,.2f", c.getSaldoCuentaCorriente()));
        lblLimiteCredito.setText(String.format("$%,.2f", c.getLimiteCredito()));

        BigDecimal disponible = c.calcularCreditoDisponible();
        lblCreditoDisponible.setText(String.format("$%,.2f", disponible));

        if (c.estaExcedido(BigDecimal.ZERO)) {
            lblEstadoBadge.setText("EXCEDIDO");
            lblEstadoBadge.setForeground(new Color(200, 20, 20));
        } else {
            lblEstadoBadge.setText("AL DÍA");
            lblEstadoBadge.setForeground(new Color(25, 130, 50));
        }

        btnCobroDirecto.setEnabled(c.getSaldoCuentaCorriente().compareTo(BigDecimal.ZERO) > 0);
    }

    private void cargarExtracto(int idCliente) {
        modelExtracto.setRowCount(0);
        BigDecimal sumDebe = BigDecimal.ZERO;
        BigDecimal sumHaber = BigDecimal.ZERO;
        BigDecimal saldoUltimo = BigDecimal.ZERO;

        try {
            List<MovimientoCuentaCorriente> movimientos = clienteService.obtenerExtractoCuentaCorriente(idCliente);
            for (MovimientoCuentaCorriente m : movimientos) {
                modelExtracto.addRow(new Object[]{
                        m.getFecha() != null ? m.getFecha().format(FORMATO_FECHA_HORA) : "-",
                        m.getTipo(),
                        m.getComprobante(),
                        m.getConcepto(),
                        m.getDebe(),
                        m.getHaber(),
                        m.getSaldoAcumulado()
                });
                if (m.getDebe() != null) sumDebe = sumDebe.add(m.getDebe());
                if (m.getHaber() != null) sumHaber = sumHaber.add(m.getHaber());
                saldoUltimo = m.getSaldoAcumulado();
            }

            lblTotalDebe.setText(String.format("Total Entregado (Debe): $%,.2f", sumDebe));
            lblTotalHaber.setText(String.format("Total Cobrado (Haber): $%,.2f", sumHaber));
            lblSaldoFinal.setText(String.format("Saldo Actual: $%,.2f", saldoUltimo));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al recuperar extracto de cuenta corriente: " + ex.getMessage(),
                    "Error de Datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarVistaExtracto() {
        lblRazonSocial.setText("Seleccione un cliente para consultar su extracto");
        lblCuitZona.setText("CUIT: - | Zona: - | Dirección: -");
        lblSaldoDeudor.setText("$0.00");
        lblLimiteCredito.setText("$0.00");
        lblCreditoDisponible.setText("$0.00");
        lblEstadoBadge.setText("-");
        modelExtracto.setRowCount(0);
        lblTotalDebe.setText("Total Entregado (Debe): $0.00");
        lblTotalHaber.setText("Total Cobrado (Haber): $0.00");
        lblSaldoFinal.setText("Saldo Actual: $0.00");
        btnCobroDirecto.setEnabled(false);
    }

    private void abrirModalCobroDirecto() {
        if (clienteSeleccionado == null) return;

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "Registrar Cobranza a Cuenta Corriente", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(this);

        JPanel pForm = new JPanel(new GridLayout(6, 2, 8, 8));
        pForm.setBorder(new EmptyBorder(15, 20, 15, 20));

        pForm.add(new JLabel("Cliente:"));
        JLabel lblCli = new JLabel(clienteSeleccionado.getRazonSocial());
        lblCli.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        pForm.add(lblCli);

        pForm.add(new JLabel("Deuda Actual:"));
        JLabel lblDeuda = new JLabel(String.format("$%,.2f", clienteSeleccionado.getSaldoCuentaCorriente()));
        lblDeuda.setForeground(new Color(180, 20, 20));
        lblDeuda.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        pForm.add(lblDeuda);

        pForm.add(new JLabel("Medio de Pago:"));
        JComboBox<String> comboMedio = new JComboBox<>(new String[]{"EFECTIVO", "TRANSFERENCIA"});
        pForm.add(comboMedio);

        pForm.add(new JLabel("Importe a Cobrar ($):"));
        JTextField txtMonto = new JTextField(String.format("%.2f", clienteSeleccionado.getSaldoCuentaCorriente()).replace(",", "."));
        txtMonto.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        txtMonto.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtMonto::selectAll); }
        });
        pForm.add(txtMonto);

        pForm.add(new JLabel("N° Referencia / Comp.:"));
        JTextField txtRef = new JTextField();
        pForm.add(txtRef);

        pForm.add(new JLabel("Observaciones:"));
        JTextField txtObs = new JTextField("Pago en depósito / transferencia bancaria");
        pForm.add(txtObs);

        dialog.add(pForm, BorderLayout.CENTER);

        JPanel pBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());
        pBotones.add(btnCancelar);

        JButton btnGuardar = new JButton("✔ Confirmar Cobranza");
        btnGuardar.setBackground(new Color(25, 125, 50));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.addActionListener(e -> {
            try {
                String strMonto = txtMonto.getText().trim().replace("$", "").replace(" ", "");
                if (strMonto.contains(",") && strMonto.contains(".")) {
                    strMonto = strMonto.replace(".", "").replace(",", ".");
                } else if (strMonto.contains(",")) {
                    strMonto = strMonto.replace(",", ".");
                }
                BigDecimal monto = new BigDecimal(strMonto);
                if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                    JOptionPane.showMessageDialog(dialog, "El monto debe ser mayor a cero.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Cobranza cob = new Cobranza();
                cob.setIdCliente(clienteSeleccionado.getIdCliente());
                cob.setIdUsuario(1);
                cob.setMontoTotal(monto);
                cob.setObservaciones(txtObs.getText().trim());

                String medio = (String) comboMedio.getSelectedItem();
                DetalleCobranza det = new DetalleCobranza(medio, monto, txtRef.getText().trim());
                cob.agregarDetalle(det);

                cobranzaService.registrarCobranza(cob);

                JOptionPane.showMessageDialog(dialog,
                        String.format("Cobranza registrada exitosamente.\nRecibo N°: %s\nImporte: $%,.2f",
                                cob.getNumeroRecibo(), monto),
                        "Cobranza Registrada", JOptionPane.INFORMATION_MESSAGE);

                dialog.dispose();

                // Recargar datos locales y notificar a la app
                clienteSeleccionado = clienteService.buscarPorId(clienteSeleccionado.getIdCliente());
                recargarClientes(clienteSeleccionado);
                if (onDatosModificados != null) onDatosModificados.run();

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Monto inválido. Ingrese un valor numérico.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error al registrar cobranza: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            }
        });
        pBotones.add(btnGuardar);
        dialog.add(pBotones, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
