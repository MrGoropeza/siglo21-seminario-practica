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
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla 3: Cuentas Corrientes con Libro Mayor / Extracto Histórico (CU-06).
 * Permite monitorear el estado crediticio de cada cliente minorista,
 * auditar cronológicamente cada remito (Debe) y recibo (Haber) con su saldo acumulado,
 * y registrar cobranzas directas a cuenta corriente.
 */
public class CuentasCorrientesPanel extends JPanel {

    private final IClienteService clienteService;
    private final ICobranzaService cobranzaService;
    private final Runnable onDatosModificados;

    private List<Cliente> listaClientes = new ArrayList<>();
    private Cliente clienteSeleccionado;

    // Componentes de Búsqueda y Selección
    private JTextField txtBuscarCliente;
    private JComboBox<Cliente> comboClientes;

    // Tarjeta de Estado Crediticio del Cliente
    private JLabel lblRazonSocial;
    private JLabel lblCuitZona;
    private JLabel lblSaldoDeudor;
    private JLabel lblLimiteCredito;
    private JLabel lblCreditoDisponible;
    private JLabel lblEstadoBadge;
    private JButton btnCobroDirecto;

    // Tabla del Libro Mayor (Extracto)
    private DefaultTableModel modelExtracto;
    private JTable tableExtracto;
    private JLabel lblTotalDebe;
    private JLabel lblTotalHaber;
    private JLabel lblSaldoFinal;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CuentasCorrientesPanel(IClienteService clienteService, ICobranzaService cobranzaService, Runnable onDatosModificados) {
        this.clienteService = clienteService;
        this.cobranzaService = cobranzaService != null ? cobranzaService : new CobranzaService();
        this.onDatosModificados = onDatosModificados;

        inicializarUI();
        recargarClientes(null);
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(12, 14, 14, 14));
        setBackground(new Color(245, 247, 250));

        // 1. Panel Superior: Selector de Cliente y Tarjeta Crediticia
        JPanel panelNorte = new JPanel(new BorderLayout(0, 8));
        panelNorte.setOpaque(false);
        panelNorte.add(crearBarraSeleccionCliente(), BorderLayout.NORTH);
        panelNorte.add(crearTarjetaCliente(), BorderLayout.CENTER);
        add(panelNorte, BorderLayout.NORTH);

        // 2. Panel Central: Libro Mayor Cronológico
        add(crearPanelLibroMayor(), BorderLayout.CENTER);
    }

    private JPanel crearBarraSeleccionCliente() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JLabel lblBuscar = new JLabel("🔍 Buscar Cliente:");
        lblBuscar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        bar.add(lblBuscar);

        txtBuscarCliente = new JTextField(16);
        txtBuscarCliente.setToolTipText("Escriba nombre o CUIT para filtrar");
        txtBuscarCliente.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrarComboClientes(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrarComboClientes(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarComboClientes(); }
        });
        bar.add(txtBuscarCliente);

        JLabel lblCombo = new JLabel("Cliente:");
        lblCombo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        bar.add(lblCombo);

        comboClientes = new JComboBox<>();
        comboClientes.setPreferredSize(new Dimension(340, 30));
        comboClientes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Cliente c) {
                    setText(String.format("%s - %s [Debe: $%,.2f]", c.getRazonSocial(), c.getZonaReparto(), c.getSaldoCuentaCorriente()));
                }
                return this;
            }
        });
        comboClientes.addActionListener(e -> onClienteSeleccionado());
        bar.add(comboClientes);

        JButton btnRefrescar = new JButton("🔄 Actualizar");
        btnRefrescar.addActionListener(e -> recargarClientes(clienteSeleccionado));
        bar.add(btnRefrescar);

        return bar;
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

        JLabel lblTit = new JLabel("📖 Libro Mayor de la Cuenta Corriente (Movimientos Cronológicos)");
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

        // Alineaciones y Colores
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tableExtracto.getColumnModel().getColumn(0).setPreferredWidth(130);
        tableExtracto.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Renderer Tipo Comprobante con Badge
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

        // Renderers Monetarios
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

    public void recargarClientes(Cliente seleccionar) {
        try {
            listaClientes = clienteService.listarActivos();
            filtrarComboClientes();
            if (seleccionar != null) {
                for (int i = 0; i < comboClientes.getItemCount(); i++) {
                    Cliente c = comboClientes.getItemAt(i);
                    if (c != null && c.getIdCliente() == seleccionar.getIdCliente()) {
                        comboClientes.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (comboClientes.getItemCount() > 0) {
                comboClientes.setSelectedIndex(0);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar clientes: " + ex.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filtrarComboClientes() {
        String filtro = txtBuscarCliente != null ? txtBuscarCliente.getText().trim().toLowerCase() : "";
        comboClientes.removeAllItems();
        for (Cliente c : listaClientes) {
            if (filtro.isEmpty() || c.getRazonSocial().toLowerCase().contains(filtro) || c.getCuit().contains(filtro)) {
                comboClientes.addItem(c);
            }
        }
    }

    private void onClienteSeleccionado() {
        clienteSeleccionado = (Cliente) comboClientes.getSelectedItem();
        if (clienteSeleccionado == null) {
            limpiarVistaExtracto();
            return;
        }

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
                        m.getFecha() != null ? m.getFecha().format(FORMATO_FECHA) : "-",
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

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Registrar Cobranza a Cuenta Corriente", true);
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
                actualizarTarjetaCliente(clienteSeleccionado);
                cargarExtracto(clienteSeleccionado.getIdCliente());
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
