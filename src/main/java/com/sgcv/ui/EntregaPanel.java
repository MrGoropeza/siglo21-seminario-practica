package com.sgcv.ui;

import com.sgcv.exception.LimiteCreditoExcedidoException;
import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Articulo;
import com.sgcv.model.Cliente;
import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Panel de captura operativa ultra-intuitivo para reparto en ruta (CU-01).
 * Optimizado para choferes y repartidores: botones grandes, filtros rápidos por zona,
 * tarjeta de estado crediticio interactiva y confirmación con snapshot de precios.
 */
public class EntregaPanel extends JPanel {

    private final IEntregaService entregaService;
    private final IClienteService clienteService;
    private final IArticuloService articuloService;

    // Datos en memoria
    private List<Cliente> clientesCargados = new ArrayList<>();
    private List<Articulo> articulosCargados = new ArrayList<>();
    private Entrega entregaActual;

    // Componentes Paso 1: Cliente & Crédito
    private JComboBox<String> comboZonas;
    private JComboBox<Cliente> comboClientes;
    private JPanel panelCardCredito;
    private JLabel lblCardCredito;

    // Componentes Paso 2: Mercadería y Bultos
    private JComboBox<Articulo> comboArticulos;
    private JSpinner spinnerCantidad;
    private JTable tablaItems;
    private DefaultTableModel tableModel;

    // Componentes Paso 3: Totales y Emisión
    private JLabel lblSubtotalBase;
    private JLabel lblDescuentoBonif;
    private JLabel lblTotalRemito;
    private JButton btnConfirmar;

    public EntregaPanel() {
        this(new EntregaService(), new ClienteService(), new ArticuloService());
    }

    public EntregaPanel(IEntregaService entregaService, IClienteService clienteService, IArticuloService articuloService) {
        this.entregaService = entregaService;
        this.clienteService = clienteService;
        this.articuloService = articuloService;
        this.entregaActual = new Entrega();

        inicializarUI();
        cargarDatosMaestros();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(10, 15, 15, 15));

        // 1. Banner Superior de Encabezado
        add(crearHeaderBanner(), BorderLayout.NORTH);

        // 2. Contenido Central (Paso 1, Paso 2 y Tabla)
        JPanel panelCuerpo = new JPanel(new BorderLayout(0, 10));
        panelCuerpo.add(crearPaso1ClientePanel(), BorderLayout.NORTH);
        panelCuerpo.add(crearPaso2BultosPanel(), BorderLayout.CENTER);
        add(panelCuerpo, BorderLayout.CENTER);

        // 3. Panel Inferior de Caja / Emisión (Paso 3)
        add(crearPaso3EmisionPanel(), BorderLayout.SOUTH);
    }

    /**
     * Banner superior con estética visual clara y guía de tres pasos para el repartidor.
     */
    private JPanel crearHeaderBanner() {
        JPanel banner = new JPanel(new BorderLayout(5, 3));
        banner.setBackground(new Color(24, 43, 73));
        banner.setBorder(new CompoundBorder(
                new LineBorder(new Color(16, 29, 50), 1),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JLabel lblTitulo = new JLabel("🚚 Registro Rápido en Reparto — Emisión de Remito y Cobranza en Ruta");
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblPasos = new JLabel("Paso 1: Seleccionar Cliente y Zona  ➔  Paso 2: Cargar Bultos Cerrados  ➔  Paso 3: Confirmar y Emitir Remito");
        lblPasos.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblPasos.setForeground(new Color(212, 228, 250));

        banner.add(lblTitulo, BorderLayout.NORTH);
        banner.add(lblPasos, BorderLayout.SOUTH);
        return banner;
    }

    /**
     * Paso 1: Selección ágil de cliente con filtro de zona y tarjeta de estado crediticio.
     */
    private JPanel crearPaso1ClientePanel() {
        JPanel panelPaso1 = new JPanel(new BorderLayout(8, 8));
        panelPaso1.setBorder(BorderFactory.createTitledBorder("Paso 1: Cliente y Control de Cuenta Corriente"));

        // Fila de Filtro de Zona y Selector de Cliente
        JPanel filaSeleccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));

        filaSeleccion.add(new JLabel("Zona:"));
        comboZonas = new JComboBox<>(new String[]{"Todas las Zonas"});
        comboZonas.setPreferredSize(new Dimension(160, 32));
        comboZonas.addActionListener(e -> filtrarClientesPorZona());
        filaSeleccion.add(comboZonas);

        filaSeleccion.add(new JLabel("Cliente (Razón Social / CUIT):"));
        comboClientes = new JComboBox<>();
        comboClientes.setPreferredSize(new Dimension(420, 32));
        comboClientes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Cliente c) {
                    setText(c.getRazonSocial() + "  [CUIT: " + c.getCuit() + " | " + c.getZonaReparto() + "]");
                }
                return this;
            }
        });
        comboClientes.addActionListener(e -> actualizarCardCredito());
        filaSeleccion.add(comboClientes);

        JButton btnRefrescar = new JButton("🔄 Refrescar");
        btnRefrescar.setPreferredSize(new Dimension(110, 32));
        btnRefrescar.addActionListener(e -> cargarDatosMaestros());
        filaSeleccion.add(btnRefrescar);

        panelPaso1.add(filaSeleccion, BorderLayout.NORTH);

        // Tarjeta Interactiva de Estado Crediticio
        panelCardCredito = new JPanel(new BorderLayout());
        panelCardCredito.setBorder(new CompoundBorder(
                new LineBorder(new Color(40, 167, 69), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        panelCardCredito.setBackground(new Color(236, 253, 243));

        lblCardCredito = new JLabel("Cargando estado crediticio...");
        lblCardCredito.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        panelCardCredito.add(lblCardCredito, BorderLayout.CENTER);

        panelPaso1.add(panelCardCredito, BorderLayout.SOUTH);
        return panelPaso1;
    }

    /**
     * Paso 2: Carga rápida de mercadería con botones de incremento (+1, +5, +10) y tabla.
     */
    private JPanel crearPaso2BultosPanel() {
        JPanel panelPaso2 = new JPanel(new BorderLayout(8, 8));
        panelPaso2.setBorder(BorderFactory.createTitledBorder("Paso 2: Carga Rápida de Bultos Cerrados y Lista del Remito"));

        // Barra de inserción rápida
        JPanel filaAgregar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));

        filaAgregar.add(new JLabel("Artículo:"));
        comboArticulos = new JComboBox<>();
        comboArticulos.setPreferredSize(new Dimension(380, 32));
        comboArticulos.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Articulo a) {
                    setText(a.getCodigo() + " - " + a.getDescripcion() + " [Stock: " + a.getStockDeposito() + "] ($" + a.getPrecioVentaBase() + ")");
                }
                return this;
            }
        });
        filaAgregar.add(comboArticulos);

        filaAgregar.add(new JLabel("Cantidad:"));
        spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spinnerCantidad.setPreferredSize(new Dimension(75, 32));
        spinnerCantidad.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        filaAgregar.add(spinnerCantidad);

        // Botones de incremento rápido para el repartidor
        JButton btnMas1 = new JButton("+1");
        btnMas1.setPreferredSize(new Dimension(48, 32));
        btnMas1.addActionListener(e -> incrementarSpinner(1));
        filaAgregar.add(btnMas1);

        JButton btnMas5 = new JButton("+5");
        btnMas5.setPreferredSize(new Dimension(52, 32));
        btnMas5.addActionListener(e -> incrementarSpinner(5));
        filaAgregar.add(btnMas5);

        JButton btnMas10 = new JButton("+10");
        btnMas10.setPreferredSize(new Dimension(58, 32));
        btnMas10.addActionListener(e -> incrementarSpinner(10));
        filaAgregar.add(btnMas10);

        JButton btnAgregar = new JButton("➕ Agregar al Remito");
        btnAgregar.setPreferredSize(new Dimension(175, 32));
        btnAgregar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnAgregar.setBackground(new Color(28, 88, 160));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.addActionListener(e -> agregarItem());
        filaAgregar.add(btnAgregar);

        panelPaso2.add(filaAgregar, BorderLayout.NORTH);

        // Tabla de Renglones cargados
        String[] columnas = {"#", "Código", "Descripción", "Conservación", "Bultos", "Precio Unit.", "Subtotal"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaItems = new JTable(tableModel);
        tablaItems.setRowHeight(30);
        tablaItems.setAutoCreateRowSorter(true);
        tablaItems.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        tablaItems.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        // Ajuste de anchos de columna
        tablaItems.getColumnModel().getColumn(0).setPreferredWidth(35);
        tablaItems.getColumnModel().getColumn(1).setPreferredWidth(100);
        tablaItems.getColumnModel().getColumn(2).setPreferredWidth(260);
        tablaItems.getColumnModel().getColumn(3).setPreferredWidth(180);
        tablaItems.getColumnModel().getColumn(4).setPreferredWidth(70);
        tablaItems.getColumnModel().getColumn(5).setPreferredWidth(100);
        tablaItems.getColumnModel().getColumn(6).setPreferredWidth(110);

        // Alineación derecha para importes y cantidades
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tablaItems.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        tablaItems.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        tablaItems.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = new JScrollPane(tablaItems);
        scrollPane.setPreferredSize(new Dimension(750, 180));
        panelPaso2.add(scrollPane, BorderLayout.CENTER);

        // Barra de acciones sobre la tabla (quitar renglón, vaciar)
        JPanel barraTablaAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        JButton btnQuitar = new JButton("🗑️ Quitar Renglón");
        btnQuitar.addActionListener(e -> quitarItemSeleccionado());
        barraTablaAcciones.add(btnQuitar);

        JButton btnVaciar = new JButton("❌ Vaciar Remito");
        btnVaciar.addActionListener(e -> vaciarRemito());
        barraTablaAcciones.add(btnVaciar);

        panelPaso2.add(barraTablaAcciones, BorderLayout.SOUTH);
        return panelPaso2;
    }

    /**
     * Paso 3: Consolidación de totales en display estilo caja registradora y botón prominente de emisión.
     */
    private JPanel crearPaso3EmisionPanel() {
        JPanel panelPaso3 = new JPanel(new BorderLayout(15, 8));
        panelPaso3.setBorder(BorderFactory.createTitledBorder("Paso 3: Liquidación de Importes y Emisión Oficial"));

        // Display de Totales (Subtotal base, Bonificación polimórfica, Total Neto)
        JPanel panelDisplayTotales = new JPanel(new GridLayout(3, 1, 4, 4));
        panelDisplayTotales.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 215, 230), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));
        panelDisplayTotales.setBackground(new Color(246, 249, 252));

        lblSubtotalBase = new JLabel("Subtotal Lista: $0.00");
        lblSubtotalBase.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        lblSubtotalBase.setForeground(new Color(80, 90, 105));

        lblDescuentoBonif = new JLabel("Descuento por Volumen: $0.00");
        lblDescuentoBonif.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblDescuentoBonif.setForeground(new Color(30, 130, 60));

        lblTotalRemito = new JLabel("TOTAL REMITO: $0.00");
        lblTotalRemito.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        lblTotalRemito.setForeground(new Color(14, 40, 75));

        panelDisplayTotales.add(lblSubtotalBase);
        panelDisplayTotales.add(lblDescuentoBonif);
        panelDisplayTotales.add(lblTotalRemito);

        panelPaso3.add(panelDisplayTotales, BorderLayout.CENTER);

        // Botón gigante para confirmación por chofer en pantalla táctil o mouse
        btnConfirmar = new JButton("💾 CONFIRMAR Y EMITIR REMITO");
        btnConfirmar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        btnConfirmar.setPreferredSize(new Dimension(320, 52));
        btnConfirmar.setBackground(new Color(34, 139, 34));
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConfirmar.addActionListener(e -> confirmarEntrega());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        panelBoton.add(btnConfirmar);
        panelPaso3.add(panelBoton, BorderLayout.EAST);

        return panelPaso3;
    }

    private void incrementarSpinner(int delta) {
        int actual = (Integer) spinnerCantidad.getValue();
        spinnerCantidad.setValue(Math.min(actual + delta, 999));
    }

    public void cargarDatosMaestros() {
        try {
            clientesCargados = clienteService.listarActivos();
            articulosCargados = articuloService.listarDisponibles();

            // Población de zonas únicas
            Set<String> zonas = new LinkedHashSet<>();
            zonas.add("Todas las Zonas");
            for (Cliente c : clientesCargados) {
                if (c.getZonaReparto() != null && !c.getZonaReparto().isBlank()) {
                    zonas.add(c.getZonaReparto());
                }
            }

            comboZonas.removeAllItems();
            for (String z : zonas) {
                comboZonas.addItem(z);
            }

            filtrarClientesPorZona();

            comboArticulos.removeAllItems();
            for (Articulo a : articulosCargados) {
                comboArticulos.addItem(a);
            }

            actualizarCardCredito();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error conectando con la base de datos MySQL:\n" + e.getMessage(),
                    "Error de Conectividad", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filtrarClientesPorZona() {
        String zonaSeleccionada = (String) comboZonas.getSelectedItem();
        comboClientes.removeAllItems();

        for (Cliente c : clientesCargados) {
            if (zonaSeleccionada == null || "Todas las Zonas".equals(zonaSeleccionada)
                    || zonaSeleccionada.equalsIgnoreCase(c.getZonaReparto())) {
                comboClientes.addItem(c);
            }
        }
        actualizarCardCredito();
    }

    private void actualizarCardCredito() {
        Cliente cli = (Cliente) comboClientes.getSelectedItem();
        if (cli == null) {
            lblCardCredito.setText("Seleccione un cliente para evaluar su margen crediticio.");
            panelCardCredito.setBackground(new Color(245, 245, 245));
            panelCardCredito.setBorder(new LineBorder(Color.LIGHT_GRAY, 1, true));
            return;
        }

        BigDecimal saldo = cli.getSaldoCuentaCorriente();
        BigDecimal limite = cli.getLimiteCredito();
        BigDecimal adicionalEstimado = entregaActual.getTotalEntrega();
        boolean excedido = cli.estaExcedido(adicionalEstimado);

        if (excedido) {
            BigDecimal exceso = saldo.add(adicionalEstimado).subtract(limite).max(BigDecimal.ZERO);
            panelCardCredito.setBackground(new Color(254, 242, 242));
            panelCardCredito.setBorder(new CompoundBorder(
                    new LineBorder(new Color(220, 53, 69), 1, true),
                    new EmptyBorder(8, 14, 8, 14)
            ));
            lblCardCredito.setText(String.format(
                    "<html><b style='color:#b71c1c;'>⚠️ ALERTA: LÍMITE DE CRÉDITO EXCEDIDO</b> &nbsp;|&nbsp; " +
                    "Saldo Deudor: <b>$%s</b> &nbsp;|&nbsp; Límite Autorizado: <b>$%s</b> &nbsp;|&nbsp; " +
                    "<span style='color:#b71c1c;'><b>Exceso Proyectado: $%s</b></span></html>",
                    saldo, limite, exceso
            ));
        } else {
            BigDecimal disponible = limite.subtract(saldo.add(adicionalEstimado)).max(BigDecimal.ZERO);
            panelCardCredito.setBackground(new Color(236, 253, 243));
            panelCardCredito.setBorder(new CompoundBorder(
                    new LineBorder(new Color(40, 167, 69), 1, true),
                    new EmptyBorder(8, 14, 8, 14)
            ));
            lblCardCredito.setText(String.format(
                    "<html><b style='color:#155724;'>✅ CLIENTE HABILITADO PARA DESPACHO</b> &nbsp;|&nbsp; " +
                    "Saldo Deudor: <b>$%s</b> &nbsp;|&nbsp; Límite: <b>$%s</b> &nbsp;|&nbsp; " +
                    "<span style='color:#155724;'><b>Margen Disponible: $%s</b></span></html>",
                    saldo, limite, disponible
            ));
        }
    }

    private void agregarItem() {
        Articulo art = (Articulo) comboArticulos.getSelectedItem();
        int cant = (Integer) spinnerCantidad.getValue();
        if (art == null) return;

        if (!art.tieneStock(cant)) {
            JOptionPane.showMessageDialog(this,
                    "Stock físico insuficiente en el furgón térmico para: " + art.getCodigo() +
                    "\nDisponible en depósito: " + art.getStockDeposito() + " bultos. Solicitado: " + cant + " bultos.",
                    "Alerta de Stock Físico", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Cálculo polimórfico del precio final (5% dto en Frescos >= 10, o 4% dto en Congelados >= 20 / recargo 3% < 5)
        BigDecimal precioPolimorfico = art.calcularPrecioVentaFinal(cant);
        DetalleEntrega det = new DetalleEntrega(
                art.getIdArticulo(), art.getCodigo(), art.getDescripcion(),
                cant, precioPolimorfico, art.getPrecioCostoReposicion(), BigDecimal.ZERO
        );
        entregaActual.agregarDetalle(det);

        int numeroRenglon = tableModel.getRowCount() + 1;
        tableModel.addRow(new Object[]{
                numeroRenglon, art.getCodigo(), art.getDescripcion(),
                art.getCategoriaConservacion(), cant,
                "$" + precioPolimorfico, "$" + det.getSubtotal()
        });

        // Resetear spinner a 1 para el siguiente artículo
        spinnerCantidad.setValue(1);

        recalcularTotalesUI();
        actualizarCardCredito();
    }

    private void quitarItemSeleccionado() {
        int fila = tablaItems.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un renglón de la tabla para eliminar.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = tablaItems.convertRowIndexToModel(fila);
        tableModel.removeRow(modelRow);

        // Reconstruir lista de entregaActual
        entregaActual.getDetalles().remove(modelRow);
        entregaActual.recalcularTotal();

        // Renumerar columna #
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(i + 1, i, 0);
        }

        recalcularTotalesUI();
        actualizarCardCredito();
    }

    private void vaciarRemito() {
        if (tableModel.getRowCount() == 0) return;
        int resp = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de que desea vaciar todos los renglones cargados en este remito?",
                "Confirmar Cancelación", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (resp == JOptionPane.YES_OPTION) {
            resetearRemito();
        }
    }

    private void recalcularTotalesUI() {
        BigDecimal subtotalLista = BigDecimal.ZERO;
        for (DetalleEntrega det : entregaActual.getDetalles()) {
            // Buscar precio de lista original
            for (Articulo a : articulosCargados) {
                if (a.getIdArticulo() == det.getIdArticulo()) {
                    subtotalLista = subtotalLista.add(a.getPrecioVentaBase().multiply(BigDecimal.valueOf(det.getCantidadBultos())));
                    break;
                }
            }
        }

        BigDecimal totalFacturado = entregaActual.getTotalEntrega();
        BigDecimal bonificacionTotal = subtotalLista.subtract(totalFacturado);

        lblSubtotalBase.setText("Subtotal Tarifa Lista: $" + subtotalLista.setScale(2, RoundingMode.HALF_UP));

        if (bonificacionTotal.compareTo(BigDecimal.ZERO) > 0) {
            lblDescuentoBonif.setText("Descuento por Volumen: -$" + bonificacionTotal.setScale(2, RoundingMode.HALF_UP) + " (Ahorro Cliente)");
            lblDescuentoBonif.setForeground(new Color(30, 130, 60));
        } else if (bonificacionTotal.compareTo(BigDecimal.ZERO) < 0) {
            lblDescuentoBonif.setText("Recargo Logística Cadena de Frío: +$" + bonificacionTotal.abs().setScale(2, RoundingMode.HALF_UP));
            lblDescuentoBonif.setForeground(new Color(180, 80, 20));
        } else {
            lblDescuentoBonif.setText("Descuento por Volumen: $0.00");
            lblDescuentoBonif.setForeground(new Color(80, 90, 105));
        }

        lblTotalRemito.setText("TOTAL REMITO: $" + totalFacturado.setScale(2, RoundingMode.HALF_UP));
    }

    private void confirmarEntrega() {
        Cliente cli = (Cliente) comboClientes.getSelectedItem();
        if (cli == null || entregaActual.getDetalles().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar un cliente y cargar al menos un artículo para emitir el remito oficial.",
                    "Datos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        entregaActual.setIdCliente(cli.getIdCliente());
        entregaActual.setIdUsuario(1);

        try {
            boolean exito = entregaService.registrarEntrega(entregaActual);
            if (exito) {
                finalizarEmisionExitosa();
            }
        } catch (StockInsuficienteException sie) {
            JOptionPane.showMessageDialog(this,
                    sie.getMessage(),
                    "Alerta de Stock Físico",
                    JOptionPane.WARNING_MESSAGE);
        } catch (LimiteCreditoExcedidoException lce) {
            int resp = JOptionPane.showConfirmDialog(this,
                    lce.getMessage() + "\n\n¿Desea autorizar excepcionalmente la entrega en ruta como supervisor?",
                    "Alerta de Límite de Crédito",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (resp == JOptionPane.YES_OPTION) {
                entregaActual.setObservaciones("Entrega con exceso de crédito autorizada en ruta por chofer/supervisor.");
                try {
                    boolean exito = entregaService.registrarEntrega(entregaActual, true);
                    if (exito) {
                        finalizarEmisionExitosa();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error al autorizar remito excepcional: " + ex.getMessage(),
                            "Error Transaccional", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (ValidacionException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(),
                    "Validación Operativa", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al confirmar entrega: " + ex.getMessage(),
                    "Fallo de Persistencia", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void finalizarEmisionExitosa() {
        String msg = String.format("""
                ¡Remito emitido exitosamente!
                -------------------------------------------------
                Número Oficial: %s
                Cliente: %s
                Total Facturado: $%s
                
                ¿Desea iniciar inmediatamente una nueva entrega para el siguiente cliente?
                """,
                entregaActual.getNumeroRemito(),
                ((Cliente) comboClientes.getSelectedItem()).getRazonSocial(),
                entregaActual.getTotalEntrega());

        int opcion = JOptionPane.showConfirmDialog(this, msg,
                "Operación Confirmada", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

        resetearRemito();
        cargarDatosMaestros();

        if (opcion == JOptionPane.YES_OPTION) {
            comboClientes.requestFocusInWindow();
        }
    }

    private void resetearRemito() {
        this.entregaActual = new Entrega();
        tableModel.setRowCount(0);
        spinnerCantidad.setValue(1);
        recalcularTotalesUI();
        actualizarCardCredito();
    }
}
