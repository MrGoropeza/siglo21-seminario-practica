package com.sgcv.ui;

import com.sgcv.exception.LimiteCreditoExcedidoException;
import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Articulo;
import com.sgcv.model.Cliente;
import com.sgcv.model.Cobranza;
import com.sgcv.model.DetalleCobranza;
import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;
import com.sgcv.model.ParadaResult;
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
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla 1: Parada Comercial en Ruta (CU-01 + CU-02).
 * Diseñada específicamente para reparto ágil en camioneta:
 * - Selección rápida de cliente con filtro de zona.
 * - Tarjeta en vivo del saldo deudor y crédito disponible.
 * - Matriz táctil directa de los 6 bultos avícolas estándar con botones (+, -, +5).
 * - Sección de cobranzas multi-medio (efectivo y transferencia).
 * - Botón inteligente adaptativo (Remito, Recibo de Deuda, o Ambos).
 */
public class EntregaPanel extends JPanel {

    private final IEntregaService entregaService;
    private final IClienteService clienteService;
    private final IArticuloService articuloService;
    private final Runnable onDatosModificados;

    // Listas maestras
    private List<Cliente> listaClientes = new ArrayList<>();
    private List<Articulo> listaArticulos = new ArrayList<>();
    private Cliente clienteSeleccionado;

    // Componentes Selección Cliente
    private JComboBox<String> comboFiltroZona;
    private JTextField txtBuscarCliente;
    private JComboBox<Cliente> comboClientes;

    // Tarjeta Estado Crediticio
    private JLabel lblRazonSocial;
    private JLabel lblCuitDireccion;
    private JLabel lblSaldoDeudorActual;
    private JLabel lblLimiteCredito;
    private JLabel lblCreditoDisponible;
    private JLabel lblBadgeEstado;

    // Matriz Dinámica de N Artículos (Buscador y Controles)
    private final Map<Integer, ArticuloControl> articulosControles = new HashMap<>();
    private JPanel panelMatrizBultos;
    private JTextField txtBuscarBulto;
    private JCheckBox chkSoloSeleccionados;

    // Cobranza en la Parada
    private JTextField txtCobroEfectivo;
    private JTextField txtCobroTransferencia;
    private JTextField txtRefTransferencia;

    // Totales y Saldo Proyectado
    private JLabel lblTotalBultos;
    private JLabel lblTotalMercaderia;
    private JLabel lblSaldoAnterior;
    private JLabel lblTotalCobrado;
    private JLabel lblNuevoSaldoFinal;

    // Botón de Acción Inteligente
    private JButton btnConfirmarParada;
    private JButton btnLimpiarParada;

    public EntregaPanel() {
        this(new EntregaService(), new ClienteService(), new ArticuloService(), null);
    }

    public EntregaPanel(IEntregaService entregaService, IClienteService clienteService, IArticuloService articuloService) {
        this(entregaService, clienteService, articuloService, null);
    }

    public EntregaPanel(IEntregaService entregaService, IClienteService clienteService, IArticuloService articuloService, Runnable onDatosModificados) {
        this.entregaService = entregaService;
        this.clienteService = clienteService;
        this.articuloService = articuloService;
        this.onDatosModificados = onDatosModificados;

        inicializarUI();
        recargarDatosMaestros();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(10, 14, 12, 14));
        setBackground(new Color(245, 247, 250));

        // 1. Norte: Selector de Cliente y Tarjeta de Cuenta Corriente
        JPanel panelNorte = new JPanel(new BorderLayout(0, 8));
        panelNorte.setOpaque(false);
        panelNorte.add(crearBarraSeleccionCliente(), BorderLayout.NORTH);
        panelNorte.add(crearTarjetaCrediticiaCliente(), BorderLayout.CENTER);
        add(panelNorte, BorderLayout.NORTH);

        // 2. Centro: Matriz Directa de los 6 Bultos Estándar
        add(crearPanelMatrizArticulos(), BorderLayout.CENTER);

        // 3. Sur: Panel de Cobranza, Totales y Botón Inteligente
        add(crearPanelLiquidacionYAccion(), BorderLayout.SOUTH);
    }

    // =========================================================================
    // 1. Selector de Cliente y Tarjeta de Cuenta Corriente
    // =========================================================================

    private JPanel crearBarraSeleccionCliente() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));

        bar.add(new JLabel("Zona:"));
        comboFiltroZona = new JComboBox<>(new String[]{"Todas las Zonas", "Zona Oeste", "Zona Norte", "Zona Sur"});
        comboFiltroZona.addActionListener(e -> filtrarClientes());
        bar.add(comboFiltroZona);

        bar.add(new JLabel("🔍 Buscar:"));
        txtBuscarCliente = new JTextField(12);
        txtBuscarCliente.setToolTipText("Filtrar por nombre o CUIT");
        txtBuscarCliente.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarClientes(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarClientes(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarClientes(); }
        });
        bar.add(txtBuscarCliente);

        bar.add(new JLabel("Cliente:"));
        comboClientes = new JComboBox<>();
        comboClientes.setPreferredSize(new Dimension(320, 30));
        comboClientes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Cliente c) {
                    setText(String.format("%s (%s) [Debe: $%,.2f]", c.getRazonSocial(), c.getZonaReparto(), c.getSaldoCuentaCorriente()));
                }
                return this;
            }
        });
        comboClientes.addActionListener(e -> onClienteCambiado());
        bar.add(comboClientes);

        JButton btnActualizar = new JButton("🔄 Recargar");
        btnActualizar.addActionListener(e -> recargarDatosMaestros());
        bar.add(btnActualizar);

        return bar;
    }

    private JPanel crearTarjetaCrediticiaCliente() {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(205, 215, 225), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        // Info General (Izquierda)
        JPanel pInfo = new JPanel(new GridLayout(2, 1, 0, 3));
        pInfo.setOpaque(false);

        lblRazonSocial = new JLabel("Seleccione un cliente para iniciar la parada");
        lblRazonSocial.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        lblRazonSocial.setForeground(new Color(18, 38, 58));

        lblCuitDireccion = new JLabel("CUIT: - | Dirección: -");
        lblCuitDireccion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblCuitDireccion.setForeground(new Color(100, 110, 125));

        pInfo.add(lblRazonSocial);
        pInfo.add(lblCuitDireccion);
        card.add(pInfo, BorderLayout.WEST);

        // Indicadores Financieros (Derecha)
        JPanel pFinanciero = new JPanel(new GridLayout(1, 4, 10, 0));
        pFinanciero.setOpaque(false);

        lblSaldoDeudorActual = new JLabel("$0.00", SwingConstants.CENTER);
        lblLimiteCredito = new JLabel("$0.00", SwingConstants.CENTER);
        lblCreditoDisponible = new JLabel("$0.00", SwingConstants.CENTER);
        lblBadgeEstado = new JLabel("AL DÍA", SwingConstants.CENTER);

        pFinanciero.add(crearMiniBadge("DEUDA ACTUAL", lblSaldoDeudorActual, new Color(180, 40, 20)));
        pFinanciero.add(crearMiniBadge("LÍMITE CRÉDITO", lblLimiteCredito, new Color(50, 70, 95)));
        pFinanciero.add(crearMiniBadge("DISPONIBLE", lblCreditoDisponible, new Color(25, 120, 50)));
        pFinanciero.add(crearMiniBadge("ESTADO CUENTA", lblBadgeEstado, new Color(24, 43, 73)));

        card.add(pFinanciero, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearMiniBadge(String titulo, JLabel lblValor, Color color) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);

        JLabel lblT = new JLabel(titulo, SwingConstants.CENTER);
        lblT.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        lblT.setForeground(new Color(110, 120, 135));

        lblValor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblValor.setForeground(color);

        p.add(lblT, BorderLayout.NORTH);
        p.add(lblValor, BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // 2. Matriz Dinámica de Bultos (N Artículos con Buscador en Ruta)
    // =========================================================================

    private JPanel crearPanelMatrizArticulos() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JPanel pHeaderBultos = new JPanel(new BorderLayout(10, 0));
        pHeaderBultos.setOpaque(false);

        JLabel lblTit = new JLabel("📦 Carga Directa de Bultos en Camioneta");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblTit.setForeground(new Color(20, 35, 60));
        pHeaderBultos.add(lblTit, BorderLayout.WEST);

        // Barra de búsqueda y filtro rápido en ruta
        JPanel pFiltros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pFiltros.setOpaque(false);

        pFiltros.add(new JLabel("🔍 Buscar bulto:"));
        txtBuscarBulto = new JTextField(14);
        txtBuscarBulto.setToolTipText("Filtrar por código o nombre del producto");
        txtBuscarBulto.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarTarjetasBultos(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarTarjetasBultos(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarTarjetasBultos(); }
        });
        pFiltros.add(txtBuscarBulto);

        chkSoloSeleccionados = new JCheckBox("Solo cargados (>0)");
        chkSoloSeleccionados.setOpaque(false);
        chkSoloSeleccionados.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        chkSoloSeleccionados.addActionListener(e -> filtrarTarjetasBultos());
        pFiltros.add(chkSoloSeleccionados);

        JButton btnLimpiarFiltro = new JButton("Ver Todos");
        btnLimpiarFiltro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        btnLimpiarFiltro.setMargin(new Insets(2, 6, 2, 6));
        btnLimpiarFiltro.addActionListener(e -> {
            txtBuscarBulto.setText("");
            chkSoloSeleccionados.setSelected(false);
            filtrarTarjetasBultos();
        });
        pFiltros.add(btnLimpiarFiltro);

        pHeaderBultos.add(pFiltros, BorderLayout.EAST);
        panel.add(pHeaderBultos, BorderLayout.NORTH);

        panelMatrizBultos = new JPanel(new GridLayout(0, 3, 10, 10));
        panelMatrizBultos.setOpaque(false);

        JPanel panelWrapper = new JPanel(new BorderLayout());
        panelWrapper.setOpaque(false);
        panelWrapper.add(panelMatrizBultos, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(panelWrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void construirTarjetasArticulos() {
        Map<Integer, Integer> cantidadesPrevias = new HashMap<>();
        for (Map.Entry<Integer, ArticuloControl> entry : articulosControles.entrySet()) {
            cantidadesPrevias.put(entry.getKey(), entry.getValue().cantidad);
        }

        panelMatrizBultos.removeAll();
        articulosControles.clear();

        for (Articulo art : listaArticulos) {
            ArticuloControl ctrl = new ArticuloControl(art);
            if (cantidadesPrevias.containsKey(art.getIdArticulo())) {
                ctrl.setCantidad(cantidadesPrevias.get(art.getIdArticulo()));
            }
            articulosControles.put(art.getIdArticulo(), ctrl);
            panelMatrizBultos.add(ctrl.panel);
        }

        filtrarTarjetasBultos();
    }

    private void filtrarTarjetasBultos() {
        String filtro = txtBuscarBulto != null ? txtBuscarBulto.getText().trim().toLowerCase() : "";
        boolean soloSeleccionados = chkSoloSeleccionados != null && chkSoloSeleccionados.isSelected();

        for (ArticuloControl ctrl : articulosControles.values()) {
            boolean coincideTexto = filtro.isEmpty() ||
                    ctrl.articulo.getCodigo().toLowerCase().contains(filtro) ||
                    ctrl.articulo.getDescripcion().toLowerCase().contains(filtro);
            boolean coincideSeleccion = !soloSeleccionados || ctrl.cantidad > 0;

            ctrl.panel.setVisible(coincideTexto && coincideSeleccion);
        }

        panelMatrizBultos.revalidate();
        panelMatrizBultos.repaint();
    }

    /**
     * Componente encapsulado para la tarjeta interactiva de cada uno de los 6 bultos.
     */
    private class ArticuloControl {
        final Articulo articulo;
        final JPanel panel;
        final JTextField txtCantidad;
        final JLabel lblSubtotal;
        final JLabel lblStockInfo;
        int cantidad = 0;

        ArticuloControl(Articulo art) {
            this.articulo = art;
            this.panel = new JPanel(new BorderLayout(0, 6));
            this.panel.setBackground(Color.WHITE);
            this.panel.setBorder(new CompoundBorder(
                    new LineBorder(new Color(210, 220, 230), 1, true),
                    new EmptyBorder(8, 10, 8, 10)
            ));

            // Encabezado del Producto
            JPanel pHead = new JPanel(new BorderLayout(0, 2));
            pHead.setOpaque(false);

            JLabel lblCodigo = new JLabel(art.getCodigo());
            lblCodigo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            lblCodigo.setForeground(new Color(15, 60, 130));

            JLabel lblNombre = new JLabel(art.getDescripcion());
            lblNombre.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            lblNombre.setToolTipText(art.getDescripcion());

            lblStockInfo = new JLabel(String.format("Precio: $%,.2f | Stock: %d bultos",
                    art.getPrecioVentaBase(), art.getStockDeposito()));
            lblStockInfo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            lblStockInfo.setForeground(art.getStockDeposito() < art.getStockMinimoAlerta() ? new Color(190, 30, 20) : new Color(70, 80, 95));

            pHead.add(lblCodigo, BorderLayout.NORTH);
            pHead.add(lblNombre, BorderLayout.CENTER);
            pHead.add(lblStockInfo, BorderLayout.SOUTH);
            panel.add(pHead, BorderLayout.NORTH);

            // Controles de Selección Numérica
            JPanel pControles = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
            pControles.setOpaque(false);

            JButton btnMenos = new JButton("-");
            btnMenos.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            btnMenos.setPreferredSize(new Dimension(38, 32));
            btnMenos.addActionListener(e -> modificarCantidad(-1));
            pControles.add(btnMenos);

            txtCantidad = new JTextField("0", 3);
            txtCantidad.setHorizontalAlignment(SwingConstants.CENTER);
            txtCantidad.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            txtCantidad.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { txtCantidad.selectAll(); }
                @Override public void focusLost(FocusEvent e) { parsearTextoCantidad(); }
            });
            txtCantidad.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { parsearTextoCantidad(); }
                @Override public void removeUpdate(DocumentEvent e) { parsearTextoCantidad(); }
                @Override public void changedUpdate(DocumentEvent e) { parsearTextoCantidad(); }
            });
            pControles.add(txtCantidad);

            JButton btnMas = new JButton("+");
            btnMas.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            btnMas.setPreferredSize(new Dimension(38, 32));
            btnMas.addActionListener(e -> modificarCantidad(1));
            pControles.add(btnMas);

            JButton btnMas5 = new JButton("+5");
            btnMas5.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            btnMas5.setPreferredSize(new Dimension(46, 32));
            btnMas5.setToolTipText("Sumar 5 bultos rápidamente");
            btnMas5.addActionListener(e -> modificarCantidad(5));
            pControles.add(btnMas5);

            panel.add(pControles, BorderLayout.CENTER);

            // Pie: Subtotal en tiempo real
            JPanel pPie = new JPanel(new BorderLayout());
            pPie.setOpaque(false);

            lblSubtotal = new JLabel("$0.00", SwingConstants.RIGHT);
            lblSubtotal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            lblSubtotal.setForeground(new Color(20, 40, 80));

            JButton btnCero = new JButton("Limpiar");
            btnCero.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            btnCero.setMargin(new Insets(1, 4, 1, 4));
            btnCero.addActionListener(e -> setCantidad(0));

            pPie.add(btnCero, BorderLayout.WEST);
            pPie.add(lblSubtotal, BorderLayout.EAST);
            panel.add(pPie, BorderLayout.SOUTH);
        }

        void modificarCantidad(int delta) {
            int nueva = Math.max(0, cantidad + delta);
            if (nueva > articulo.getStockDeposito()) {
                JOptionPane.showMessageDialog(EntregaPanel.this,
                        String.format("Stock insuficiente en camioneta para '%s'.\nStock disponible: %d bultos.",
                                articulo.getDescripcion(), articulo.getStockDeposito()),
                        "Límite de Stock", JOptionPane.WARNING_MESSAGE);
                nueva = articulo.getStockDeposito();
            }
            setCantidad(nueva);
        }

        void parsearTextoCantidad() {
            String texto = txtCantidad.getText().trim();
            if (texto.isEmpty()) return;
            try {
                int valor = Integer.parseInt(texto);
                if (valor < 0) valor = 0;
                if (valor > articulo.getStockDeposito()) {
                    valor = articulo.getStockDeposito();
                }
                if (valor != cantidad) {
                    cantidad = valor;
                    actualizarSubtotal();
                    recalcularParada();
                }
            } catch (NumberFormatException ignored) {}
        }

        void setCantidad(int val) {
            this.cantidad = Math.max(0, Math.min(val, articulo.getStockDeposito()));
            this.txtCantidad.setText(String.valueOf(this.cantidad));
            actualizarSubtotal();
            recalcularParada();
        }

        void actualizarSubtotal() {
            if (cantidad == 0) {
                lblSubtotal.setText("$0.00");
                lblSubtotal.setForeground(Color.GRAY);
                panel.setBorder(new CompoundBorder(
                        new LineBorder(new Color(210, 220, 230), 1, true),
                        new EmptyBorder(8, 10, 8, 10)
                ));
            } else {
                BigDecimal precioUnit = articulo.calcularPrecioVentaFinal(cantidad);
                BigDecimal sub = precioUnit.multiply(BigDecimal.valueOf(cantidad));
                lblSubtotal.setText(String.format("$%,.2f", sub));
                lblSubtotal.setForeground(new Color(20, 90, 40));
                panel.setBorder(new CompoundBorder(
                        new LineBorder(new Color(30, 130, 60), 2, true),
                        new EmptyBorder(7, 9, 7, 9)
                ));
            }
        }

        BigDecimal getSubtotal() {
            if (cantidad <= 0) return BigDecimal.ZERO;
            BigDecimal precioUnit = articulo.calcularPrecioVentaFinal(cantidad);
            return precioUnit.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    // =========================================================================
    // 3. Panel de Cobranza, Totales y Botón Inteligente
    // =========================================================================

    private JPanel crearPanelLiquidacionYAccion() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(205, 215, 225), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        // A. Izquierda: Cobranza en la Parada (Multi-Medio)
        JPanel pCobranza = new JPanel(new GridLayout(3, 2, 8, 6));
        pCobranza.setOpaque(false);
        pCobranza.setPreferredSize(new Dimension(380, 0));
        pCobranza.setBorder(new TitledBorder(new LineBorder(new Color(220, 225, 235), 1),
                "💵 Cobranza en Parada (Multi-Medio)", TitledBorder.LEFT, TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12), new Color(20, 40, 80)));

        pCobranza.add(new JLabel("Efectivo Recibido ($):"));
        txtCobroEfectivo = new JTextField("0.00");
        txtCobroEfectivo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        txtCobroEfectivo.setHorizontalAlignment(SwingConstants.RIGHT);
        txtCobroEfectivo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcularParada(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcularParada(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcularParada(); }
        });
        pCobranza.add(txtCobroEfectivo);

        pCobranza.add(new JLabel("Transferencia Recibida ($):"));
        txtCobroTransferencia = new JTextField("0.00");
        txtCobroTransferencia.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        txtCobroTransferencia.setHorizontalAlignment(SwingConstants.RIGHT);
        txtCobroTransferencia.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcularParada(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcularParada(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcularParada(); }
        });
        pCobranza.add(txtCobroTransferencia);

        pCobranza.add(new JLabel("N° Operación / Referencia:"));
        txtRefTransferencia = new JTextField();
        pCobranza.add(txtRefTransferencia);

        panel.add(pCobranza, BorderLayout.WEST);

        // B. Centro: Resumen y Proyección del Saldo
        JPanel pTotales = new JPanel(new GridLayout(5, 2, 8, 4));
        pTotales.setOpaque(false);

        lblTotalBultos = new JLabel("0 cajones", SwingConstants.RIGHT);
        lblTotalMercaderia = new JLabel("$0.00", SwingConstants.RIGHT);
        lblSaldoAnterior = new JLabel("$0.00", SwingConstants.RIGHT);
        lblTotalCobrado = new JLabel("$0.00", SwingConstants.RIGHT);
        lblNuevoSaldoFinal = new JLabel("$0.00", SwingConstants.RIGHT);

        lblTotalBultos.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        lblTotalMercaderia.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblSaldoAnterior.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblTotalCobrado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblTotalCobrado.setForeground(new Color(25, 125, 50));
        lblNuevoSaldoFinal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));

        pTotales.add(new JLabel("Bultos a Entregar:"));
        pTotales.add(lblTotalBultos);

        pTotales.add(new JLabel("Mercadería de Hoy:"));
        pTotales.add(lblTotalMercaderia);

        pTotales.add(new JLabel("Deuda Anterior:"));
        pTotales.add(lblSaldoAnterior);

        pTotales.add(new JLabel("Cobranza Percibida:"));
        pTotales.add(lblTotalCobrado);

        pTotales.add(new JLabel("Nuevo Saldo Deudor:"));
        pTotales.add(lblNuevoSaldoFinal);

        panel.add(pTotales, BorderLayout.CENTER);

        // C. Derecha: Botón de Confirmación Inteligente
        JPanel pAccion = new JPanel(new GridLayout(2, 1, 0, 8));
        pAccion.setOpaque(false);
        pAccion.setPreferredSize(new Dimension(280, 0));

        btnConfirmarParada = new JButton("Seleccione bultos o cobro");
        btnConfirmarParada.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        btnConfirmarParada.setEnabled(false);
        btnConfirmarParada.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConfirmarParada.addActionListener(e -> confirmarParadaComercial());
        pAccion.add(btnConfirmarParada);

        btnLimpiarParada = new JButton("🔄 Limpiar Parada");
        btnLimpiarParada.addActionListener(e -> limpiarCamposParada());
        pAccion.add(btnLimpiarParada);

        panel.add(pAccion, BorderLayout.EAST);
        return panel;
    }

    // =========================================================================
    // Lógica Operativa y Recálculos en Tiempo Real
    // =========================================================================

    public void recargarDatosMaestros() {
        try {
            listaClientes = clienteService.listarActivos();
            listaArticulos = articuloService.listarDisponibles();

            filtrarClientes();
            construirTarjetasArticulos();
            recalcularParada();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión al cargar datos maestros: " + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filtrarClientes() {
        String zona = (String) comboFiltroZona.getSelectedItem();
        String filtro = txtBuscarCliente != null ? txtBuscarCliente.getText().trim().toLowerCase() : "";

        comboClientes.removeAllItems();
        for (Cliente c : listaClientes) {
            boolean coincideZona = zona == null || "Todas las Zonas".equals(zona) || zona.equalsIgnoreCase(c.getZonaReparto());
            boolean coincideTexto = filtro.isEmpty() || c.getRazonSocial().toLowerCase().contains(filtro) || c.getCuit().contains(filtro);

            if (coincideZona && coincideTexto) {
                comboClientes.addItem(c);
            }
        }

        if (comboClientes.getItemCount() > 0) {
            comboClientes.setSelectedIndex(0);
        } else {
            clienteSeleccionado = null;
            actualizarTarjetaCliente(null);
        }
    }

    private void onClienteCambiado() {
        clienteSeleccionado = (Cliente) comboClientes.getSelectedItem();
        actualizarTarjetaCliente(clienteSeleccionado);
        recalcularParada();
    }

    private void actualizarTarjetaCliente(Cliente c) {
        if (c == null) {
            lblRazonSocial.setText("Seleccione un cliente para iniciar la parada");
            lblCuitDireccion.setText("CUIT: - | Dirección: -");
            lblSaldoDeudorActual.setText("$0.00");
            lblLimiteCredito.setText("$0.00");
            lblCreditoDisponible.setText("$0.00");
            lblBadgeEstado.setText("-");
            lblSaldoAnterior.setText("$0.00");
            return;
        }

        lblRazonSocial.setText(c.getRazonSocial());
        lblCuitDireccion.setText(String.format("CUIT: %s | Zona: %s | Dirección: %s",
                c.getCuit(), c.getZonaReparto(), c.getDireccion()));

        lblSaldoDeudorActual.setText(String.format("$%,.2f", c.getSaldoCuentaCorriente()));
        lblLimiteCredito.setText(String.format("$%,.2f", c.getLimiteCredito()));

        BigDecimal disponible = c.calcularCreditoDisponible();
        lblCreditoDisponible.setText(String.format("$%,.2f", disponible));

        if (c.estaExcedido(BigDecimal.ZERO)) {
            lblBadgeEstado.setText("EXCEDIDO");
            lblBadgeEstado.setForeground(new Color(200, 20, 20));
        } else {
            lblBadgeEstado.setText("AL DÍA");
            lblBadgeEstado.setForeground(new Color(25, 130, 50));
        }

        lblSaldoAnterior.setText(String.format("$%,.2f", c.getSaldoCuentaCorriente()));
    }

    private void recalcularParada() {
        int totalBultos = 0;
        BigDecimal totalMercaderia = BigDecimal.ZERO;

        for (ArticuloControl ctrl : articulosControles.values()) {
            totalBultos += ctrl.cantidad;
            totalMercaderia = totalMercaderia.add(ctrl.getSubtotal());
        }

        lblTotalBultos.setText(totalBultos + " cajones");
        lblTotalMercaderia.setText(String.format("$%,.2f", totalMercaderia));

        BigDecimal cobroEfec = parseMonto(txtCobroEfectivo.getText());
        BigDecimal cobroTransf = parseMonto(txtCobroTransferencia.getText());
        BigDecimal totalCobrado = cobroEfec.add(cobroTransf);

        lblTotalCobrado.setText(String.format("$%,.2f", totalCobrado));

        BigDecimal saldoAnterior = clienteSeleccionado != null ? clienteSeleccionado.getSaldoCuentaCorriente() : BigDecimal.ZERO;
        BigDecimal nuevoSaldo = saldoAnterior.add(totalMercaderia).subtract(totalCobrado);

        lblNuevoSaldoFinal.setText(String.format("$%,.2f", nuevoSaldo));
        if (clienteSeleccionado != null && nuevoSaldo.compareTo(clienteSeleccionado.getLimiteCredito()) > 0) {
            lblNuevoSaldoFinal.setForeground(new Color(200, 20, 20));
        } else {
            lblNuevoSaldoFinal.setForeground(new Color(20, 40, 80));
        }

        // Configurar Botón Inteligente
        boolean tieneBultos = totalBultos > 0;
        boolean tieneCobro = totalCobrado.compareTo(BigDecimal.ZERO) > 0;

        if (!tieneBultos && !tieneCobro) {
            btnConfirmarParada.setEnabled(false);
            btnConfirmarParada.setText("Seleccione bultos o cobro");
            btnConfirmarParada.setBackground(UIManager.getColor("Button.background"));
            btnConfirmarParada.setForeground(Color.GRAY);
        } else if (tieneBultos && !tieneCobro) {
            btnConfirmarParada.setEnabled(true);
            btnConfirmarParada.setText(String.format("🚚 EMITIR REMITO ($%,.2f)", totalMercaderia));
            btnConfirmarParada.setBackground(new Color(13, 71, 161)); // Azul
            btnConfirmarParada.setForeground(Color.WHITE);
        } else if (!tieneBultos && tieneCobro) {
            btnConfirmarParada.setEnabled(true);
            btnConfirmarParada.setText(String.format("💵 REGISTRAR PAGO ($%,.2f)", totalCobrado));
            btnConfirmarParada.setBackground(new Color(27, 94, 32)); // Verde Esmeralda
            btnConfirmarParada.setForeground(Color.WHITE);
        } else {
            btnConfirmarParada.setEnabled(true);
            btnConfirmarParada.setText("🚚💵 CONFIRMAR ENTREGA Y COBRANZA");
            btnConfirmarParada.setBackground(new Color(0, 77, 64)); // Teal Oscuro
            btnConfirmarParada.setForeground(Color.WHITE);
        }
    }

    private void confirmarParadaComercial() {
        if (clienteSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un cliente para operar.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int totalBultos = 0;
        Entrega entrega = null;

        for (ArticuloControl ctrl : articulosControles.values()) {
            if (ctrl.cantidad > 0) {
                if (entrega == null) {
                    entrega = new Entrega();
                    entrega.setIdCliente(clienteSeleccionado.getIdCliente());
                    entrega.setIdUsuario(1);
                    entrega.setEstado("EMITIDA");
                }
                DetalleEntrega det = new DetalleEntrega(
                        ctrl.articulo.getIdArticulo(),
                        ctrl.cantidad,
                        ctrl.articulo.calcularPrecioVentaFinal(ctrl.cantidad),
                        ctrl.articulo.getPrecioCostoReposicion(),
                        BigDecimal.ZERO
                );
                entrega.agregarDetalle(det);
                totalBultos += ctrl.cantidad;
            }
        }

        BigDecimal cobroEfec = parseMonto(txtCobroEfectivo.getText());
        BigDecimal cobroTransf = parseMonto(txtCobroTransferencia.getText());
        BigDecimal totalCobrado = cobroEfec.add(cobroTransf);

        Cobranza cobranza = null;
        if (totalCobrado.compareTo(BigDecimal.ZERO) > 0) {
            cobranza = new Cobranza();
            cobranza.setIdCliente(clienteSeleccionado.getIdCliente());
            cobranza.setIdUsuario(1);
            cobranza.setMontoTotal(totalCobrado);
            cobranza.setObservaciones("Cobranza percibida en parada comercial");

            if (cobroEfec.compareTo(BigDecimal.ZERO) > 0) {
                cobranza.agregarDetalle(new DetalleCobranza("EFECTIVO", cobroEfec, "Cobro en camioneta"));
            }
            if (cobroTransf.compareTo(BigDecimal.ZERO) > 0) {
                cobranza.agregarDetalle(new DetalleCobranza("TRANSFERENCIA", cobroTransf, txtRefTransferencia.getText().trim()));
            }
        }

        // Validación de confirmación
        boolean autorizarExcepcional = false;
        try {
            ParadaResult resultado = entregaService.registrarParadaCompleta(entrega, cobranza, false);
            mostrarDialogoExito(resultado);
            limpiarCamposParada();
            recargarDatosMaestros();
            if (onDatosModificados != null) onDatosModificados.run();

        } catch (LimiteCreditoExcedidoException ex) {
            int resp = JOptionPane.showConfirmDialog(this,
                    "⚠ ALERTA DE RIESGO CREDITICIO:\n\n" +
                            ex.getMessage() + "\n\n" +
                            "¿Desea autorizar excepcionalmente la entrega superando el límite?",
                    "Autorización Excepcional de Crédito",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (resp == JOptionPane.YES_OPTION) {
                try {
                    if (entrega != null) {
                        entrega.setObservaciones("Entrega autorizada con exceso crediticio excepcional");
                    }
                    ParadaResult resAutorizado = entregaService.registrarParadaCompleta(entrega, cobranza, true);
                    mostrarDialogoExito(resAutorizado);
                    limpiarCamposParada();
                    recargarDatosMaestros();
                    if (onDatosModificados != null) onDatosModificados.run();
                } catch (Exception e2) {
                    JOptionPane.showMessageDialog(this, "Error al registrar parada autorizada: " + e2.getMessage(),
                            "Error de Persistencia", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (StockInsuficienteException ex) {
            JOptionPane.showMessageDialog(this,
                    String.format("Stock físico insuficiente para el artículo '%s'.\nStock disponible: %d bultos | Solicitado: %d bultos.",
                            ex.getCodigoArticulo(), ex.getStockDisponible(), ex.getCantidadSolicitada()),
                    "Falta de Stock", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al procesar la parada comercial: " + ex.getMessage(),
                    "Error Inesperado", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarDialogoExito(ParadaResult res) {
        StringBuilder sb = new StringBuilder();
        sb.append("¡Operación Registrada con Éxito!\n\n");
        if (res.huboEntrega()) {
            sb.append(String.format("• Remito de Entrega: %s ($%,.2f)\n", res.getNumeroRemito(), res.getTotalEntrega()));
        }
        if (res.huboCobranza()) {
            sb.append(String.format("• Recibo de Cobranza: %s ($%,.2f)\n", res.getNumeroRecibo(), res.getTotalCobrado()));
        }
        sb.append(String.format("\nNuevo Saldo Deudor del Cliente: $%,.2f\n", res.getNuevoSaldoCliente()));
        sb.append("Stock y cuenta corriente actualizados automáticamente.");

        JOptionPane.showMessageDialog(this, sb.toString(), "Comprobantes Generados", JOptionPane.INFORMATION_MESSAGE);
    }

    private void limpiarCamposParada() {
        for (ArticuloControl ctrl : articulosControles.values()) {
            ctrl.setCantidad(0);
        }
        if (txtBuscarBulto != null) txtBuscarBulto.setText("");
        if (chkSoloSeleccionados != null) chkSoloSeleccionados.setSelected(false);
        filtrarTarjetasBultos();
        txtCobroEfectivo.setText("0.00");
        txtCobroTransferencia.setText("0.00");
        txtRefTransferencia.setText("");
        recalcularParada();
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
            BigDecimal bd = new BigDecimal(limpio);
            return bd.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : bd;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
