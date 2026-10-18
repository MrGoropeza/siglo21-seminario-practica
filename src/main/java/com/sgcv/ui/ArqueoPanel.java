package com.sgcv.ui;

import com.sgcv.model.ArqueoCaja;
import com.sgcv.model.DetalleArqueo;
import com.sgcv.model.ResumenDiarioDTO;
import com.sgcv.service.ArqueoService;
import com.sgcv.service.IArqueoService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pantalla 2: Cierre, Arqueo y Segregación Diaria (CU-04).
 * Automatiza la conciliación nocturna del reparto, eliminando las 2 a 3 horas de
 * cálculo manual en libreta. Segrega el fondo de reposición para frigoríficos del
 * margen neto diario y detecta desvíos de caja al instante.
 */
public class ArqueoPanel extends JPanel {

    private final IArqueoService arqueoService;
    private final Runnable onDatosModificados;

    private ResumenDiarioDTO resumenActual;

    // Métricas Operativas Superiores
    private JLabel lblBultosHoy;
    private JLabel lblFacturacionHoy;
    private JLabel lblCobranzasTotalHoy;
    private JLabel lblCobranzasEfectivoHoy;
    private JLabel lblCobranzasTransfHoy;

    // Arqueo de Efectivo Físico
    private JTextField txtSaldoInicial;
    private JLabel lblEfectivoCobrado;
    private JTextField txtGastosEfectivo;
    private JLabel lblEfectivoEsperado;
    private JTextField txtEfectivoReal;
    private JLabel lblDiferenciaCaja;

    // Segregación de Fondos
    private JLabel lblFondoReposicion;
    private JLabel lblMargenNeto;

    // Cierre y Guardado
    private JTextField txtObservaciones;
    private JButton btnCerrarArqueo;

    // Historial
    private DefaultTableModel modelHistorico;
    private JTable tableHistorico;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ArqueoPanel(IArqueoService arqueoService, Runnable onDatosModificados) {
        this.arqueoService = arqueoService != null ? arqueoService : new ArqueoService();
        this.onDatosModificados = onDatosModificados;

        inicializarUI();
        recargarMetricas();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(12, 16, 14, 16));
        setBackground(new Color(245, 247, 250));

        // 1. Tarjetas KPI del Día (Norte)
        add(crearPanelKpiOperativo(), BorderLayout.NORTH);

        // 2. Centro dividido: Arqueo de Efectivo a la Izquierda y Segregación a la Derecha
        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 14, 0));
        panelCentro.setOpaque(false);
        panelCentro.add(crearCardArqueoEfectivo());
        panelCentro.add(crearCardSegregacionFondos());
        add(panelCentro, BorderLayout.CENTER);

        // 3. Panel Inferior: Historial de Arqueos
        add(crearPanelHistorico(), BorderLayout.SOUTH);
    }

    private JPanel crearPanelKpiOperativo() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JLabel lblTit = new JLabel("📊 Resumen Operativo de la Jornada (" + LocalDate.now().format(FORMATO_FECHA) + ")");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        lblTit.setForeground(new Color(20, 35, 60));
        headerBar.add(lblTit, BorderLayout.WEST);

        JButton btnActualizar = new JButton("🔄 Recalcular Métricas");
        btnActualizar.addActionListener(e -> recargarMetricas());
        headerBar.add(btnActualizar, BorderLayout.EAST);
        panel.add(headerBar, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setOpaque(false);

        lblBultosHoy = new JLabel("0", SwingConstants.CENTER);
        lblFacturacionHoy = new JLabel("$0.00", SwingConstants.CENTER);
        lblCobranzasTotalHoy = new JLabel("$0.00", SwingConstants.CENTER);
        lblCobranzasEfectivoHoy = new JLabel("Efec: $0 | Transf: $0", SwingConstants.CENTER);

        cards.add(crearKpiCard("BULTOS ENTREGADOS HOY", lblBultosHoy, new Color(15, 60, 130), "Cajones totales repartidos en ruta"));
        cards.add(crearKpiCard("FACTURACIÓN EN REMITOS", lblFacturacionHoy, new Color(20, 40, 80), "Importe total de remitos emitidos"));
        cards.add(crearKpiCard("COBRANZAS PERCIBIDAS", lblCobranzasTotalHoy, new Color(25, 120, 50), "Cobrado hoy (deuda + entregas)"));
        cards.add(crearKpiCard("DESGLOSE DE MEDIOS", lblCobranzasEfectivoHoy, new Color(80, 50, 120), "Proporción efectivo vs banco"));

        panel.add(cards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearKpiCard(String titulo, JLabel lblValor, Color color, String tooltip) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setToolTipText(tooltip);

        JLabel lblT = new JLabel(titulo, SwingConstants.CENTER);
        lblT.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        lblT.setForeground(new Color(115, 125, 140));

        lblValor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        lblValor.setForeground(color);

        card.add(lblT, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearCardArqueoEfectivo() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 220, 230), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblTit = new JLabel("💵 Arqueo y Cuadre de Efectivo Físico");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblTit.setForeground(new Color(20, 35, 60));
        card.add(lblTit, BorderLayout.NORTH);

        JPanel pForm = new JPanel(new GridLayout(6, 2, 8, 8));
        pForm.setOpaque(false);

        // 1. Saldo Inicial
        pForm.add(new JLabel("Saldo Inicial en Camioneta (Cambio):"));
        txtSaldoInicial = new JTextField("50000.00");
        txtSaldoInicial.setHorizontalAlignment(SwingConstants.RIGHT);
        txtSaldoInicial.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcularCuadre(); }
        });
        pForm.add(txtSaldoInicial);

        // 2. (+) Cobranzas en Efectivo
        pForm.add(new JLabel("(+) Cobranzas Efectivo Hoy:"));
        lblEfectivoCobrado = new JLabel("$0.00", SwingConstants.RIGHT);
        lblEfectivoCobrado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblEfectivoCobrado.setForeground(new Color(25, 120, 50));
        pForm.add(lblEfectivoCobrado);

        // 3. (-) Gastos en Efectivo
        pForm.add(new JLabel("(-) Gastos Efectivo (Peajes/Combustible):"));
        txtGastosEfectivo = new JTextField("0.00");
        txtGastosEfectivo.setHorizontalAlignment(SwingConstants.RIGHT);
        txtGastosEfectivo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcularCuadre(); }
        });
        pForm.add(txtGastosEfectivo);

        // 4. (=) Efectivo Esperado
        pForm.add(new JLabel("(=) Efectivo Teórico Esperado:"));
        lblEfectivoEsperado = new JLabel("$0.00", SwingConstants.RIGHT);
        lblEfectivoEsperado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        lblEfectivoEsperado.setForeground(new Color(20, 45, 80));
        pForm.add(lblEfectivoEsperado);

        // 5. Efectivo Real Contado
        JLabel lblIngresarReal = new JLabel("EFECTIVO REAL EN MANO (Billetes):");
        lblIngresarReal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        pForm.add(lblIngresarReal);

        txtEfectivoReal = new JTextField();
        txtEfectivoReal.setHorizontalAlignment(SwingConstants.RIGHT);
        txtEfectivoReal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        txtEfectivoReal.setBackground(new Color(255, 253, 230));
        txtEfectivoReal.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcularCuadre(); }
        });
        pForm.add(txtEfectivoReal);

        // 6. Diferencia de Caja
        pForm.add(new JLabel("DIFERENCIA DE CAJA:"));
        lblDiferenciaCaja = new JLabel("Ingrese efectivo contado", SwingConstants.RIGHT);
        lblDiferenciaCaja.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        pForm.add(lblDiferenciaCaja);

        card.add(pForm, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearCardSegregacionFondos() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 220, 230), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblTit = new JLabel("🏦 Segregación Automática de Fondos (Finanzas SGCV)");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblTit.setForeground(new Color(20, 35, 60));
        card.add(lblTit, BorderLayout.NORTH);

        JPanel pCuerpo = new JPanel(new GridLayout(2, 1, 0, 10));
        pCuerpo.setOpaque(false);

        // Tarjeta Reposición Frigoríficos
        JPanel pFrigo = new JPanel(new BorderLayout(0, 4));
        pFrigo.setBackground(new Color(240, 247, 255));
        pFrigo.setBorder(new CompoundBorder(
                new LineBorder(new Color(190, 215, 245), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel lblTitFrigo = new JLabel("📦 SOBRE DE REPOSICIÓN (FRIGORÍFICOS SOYCHÚ / TRES ARROYOS)");
        lblTitFrigo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lblTitFrigo.setForeground(new Color(15, 60, 140));

        lblFondoReposicion = new JLabel("$0.00", SwingConstants.CENTER);
        lblFondoReposicion.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        lblFondoReposicion.setForeground(new Color(10, 50, 130));

        JLabel lblExplFrigo = new JLabel("<html><i>Dinero intocable para pagar la faena y reposición de mercadería de mañana.</i></html>", SwingConstants.CENTER);
        lblExplFrigo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        lblExplFrigo.setForeground(new Color(90, 105, 125));

        pFrigo.add(lblTitFrigo, BorderLayout.NORTH);
        pFrigo.add(lblFondoReposicion, BorderLayout.CENTER);
        pFrigo.add(lblExplFrigo, BorderLayout.SOUTH);
        pCuerpo.add(pFrigo);

        // Tarjeta Margen Neto Real
        JPanel pMargen = new JPanel(new BorderLayout(0, 4));
        pMargen.setBackground(new Color(243, 253, 245));
        pMargen.setBorder(new CompoundBorder(
                new LineBorder(new Color(185, 230, 195), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel lblTitMargen = new JLabel("💰 MARGEN NETO / GANANCIA REAL DE LA JORNADA");
        lblTitMargen.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lblTitMargen.setForeground(new Color(20, 110, 45));

        lblMargenNeto = new JLabel("$0.00", SwingConstants.CENTER);
        lblMargenNeto.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        lblMargenNeto.setForeground(new Color(15, 115, 45));

        JLabel lblExplMargen = new JLabel("<html><i>Ganancia comercial efectiva tras descontar el costo de reposición.</i></html>", SwingConstants.CENTER);
        lblExplMargen.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        lblExplMargen.setForeground(new Color(90, 105, 125));

        pMargen.add(lblTitMargen, BorderLayout.NORTH);
        pMargen.add(lblMargenNeto, BorderLayout.CENTER);
        pMargen.add(lblExplMargen, BorderLayout.SOUTH);
        pCuerpo.add(pMargen);

        card.add(pCuerpo, BorderLayout.CENTER);

        // Panel de Cierre
        JPanel pCierre = new JPanel(new BorderLayout(6, 6));
        pCierre.setOpaque(false);

        JPanel pObs = new JPanel(new BorderLayout(4, 0));
        pObs.setOpaque(false);
        pObs.add(new JLabel("Observaciones:"), BorderLayout.WEST);
        txtObservaciones = new JTextField("Cierre sin desvíos. Fondo segregado reservado para frigorífico.");
        pObs.add(txtObservaciones, BorderLayout.CENTER);
        pCierre.add(pObs, BorderLayout.NORTH);

        btnCerrarArqueo = new JButton("🔒 CERRAR ARQUEO Y GUARDAR JORNADA");
        btnCerrarArqueo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnCerrarArqueo.setBackground(new Color(20, 50, 90));
        btnCerrarArqueo.setForeground(Color.WHITE);
        btnCerrarArqueo.setPreferredSize(new Dimension(0, 38));
        btnCerrarArqueo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarArqueo.addActionListener(e -> confirmarYCerrarArqueo());
        pCierre.add(btnCerrarArqueo, BorderLayout.SOUTH);

        card.add(pCierre, BorderLayout.SOUTH);
        return card;
    }

    private JPanel crearPanelHistorico() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(0, 180));
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblTit = new JLabel("📑 Historial de Cierres de Caja Registrados");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblTit.setForeground(new Color(20, 35, 60));
        panel.add(lblTit, BorderLayout.NORTH);

        String[] cols = {"N° Arqueo", "Fecha Cierre", "Efectivo Esperado", "Efectivo Contado", "Diferencia", "Fondo Reposición", "Margen Neto", "Estado", "Observaciones"};
        modelHistorico = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tableHistorico = new JTable(modelHistorico);
        tableHistorico.setRowHeight(24);
        tableHistorico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        tableHistorico.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableHistorico.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableHistorico.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableHistorico.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        JScrollPane scroll = new JScrollPane(tableHistorico);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    public void recargarMetricas() {
        try {
            resumenActual = arqueoService.calcularResumenDiario(LocalDate.now());

            lblBultosHoy.setText(String.format("%d cajones", resumenActual.getBultosEntregadosTotal()));
            lblFacturacionHoy.setText(String.format("$%,.2f", resumenActual.getFacturacionEntregasTotal()));
            lblCobranzasTotalHoy.setText(String.format("$%,.2f", resumenActual.getTotalCobrado()));
            lblCobranzasEfectivoHoy.setText(String.format("Efec: $%,.2f | Banco: $%,.2f",
                    resumenActual.getCobranzasEfectivoTotal(), resumenActual.getCobranzasTransferenciaTotal()));

            lblEfectivoCobrado.setText(String.format("$%,.2f", resumenActual.getCobranzasEfectivoTotal()));
            lblFondoReposicion.setText(String.format("$%,.2f", resumenActual.getCostoReposicionTotal()));
            lblMargenNeto.setText(String.format("$%,.2f", resumenActual.getMargenBrutoEstimado()));

            recalcularCuadre();
            recargarHistorial();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al calcular métricas de arqueo: " + ex.getMessage(),
                    "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalcularCuadre() {
        if (resumenActual == null) return;

        BigDecimal saldoIni = parseMonto(txtSaldoInicial.getText(), new BigDecimal("50000.00"));
        BigDecimal gastos = parseMonto(txtGastosEfectivo.getText(), BigDecimal.ZERO);
        BigDecimal efecCobrado = resumenActual.getCobranzasEfectivoTotal();

        BigDecimal esperado = saldoIni.add(efecCobrado).subtract(gastos);
        lblEfectivoEsperado.setText(String.format("$%,.2f", esperado));

        String strReal = txtEfectivoReal.getText().trim();
        if (strReal.isEmpty()) {
            lblDiferenciaCaja.setText("Ingrese efectivo contado");
            lblDiferenciaCaja.setForeground(Color.GRAY);
            return;
        }

        BigDecimal real = parseMonto(strReal, BigDecimal.ZERO);
        BigDecimal diferencia = real.subtract(esperado);

        if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
            lblDiferenciaCaja.setText("✔ Caja Cuadrada ($0.00)");
            lblDiferenciaCaja.setForeground(new Color(25, 125, 50));
        } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
            lblDiferenciaCaja.setText(String.format("⚠ FALTANTE: -$%,.2f", diferencia.abs()));
            lblDiferenciaCaja.setForeground(new Color(200, 20, 20));
        } else {
            lblDiferenciaCaja.setText(String.format("ℹ SOBRANTE: +$%,.2f", diferencia));
            lblDiferenciaCaja.setForeground(new Color(20, 80, 180));
        }
    }

    private void recargarHistorial() {
        modelHistorico.setRowCount(0);
        try {
            List<ArqueoCaja> historico = arqueoService.listarHistorico();
            for (ArqueoCaja a : historico) {
                modelHistorico.addRow(new Object[]{
                        String.format("ARQ-%04d", a.getIdArqueo()),
                        a.getFechaArqueo() != null ? a.getFechaArqueo().format(FORMATO_FECHA) : "-",
                        String.format("$%,.2f", a.getEfectivoEsperado()),
                        String.format("$%,.2f", a.getEfectivoRealContado()),
                        String.format("$%,.2f", a.getDiferenciaEfectivo()),
                        String.format("$%,.2f", a.getFondoReposicionSegregado()),
                        String.format("$%,.2f", a.getMargenNetoDia()),
                        a.getEstado(),
                        a.getObservaciones()
                });
            }
        } catch (SQLException ex) {
            // Error silencioso en el historial
        }
    }

    private void confirmarYCerrarArqueo() {
        String strReal = txtEfectivoReal.getText().trim();
        if (strReal.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el efectivo físico real contado en mano.",
                    "Falta Efectivo Real", JOptionPane.WARNING_MESSAGE);
            txtEfectivoReal.requestFocus();
            return;
        }

        BigDecimal saldoIni = parseMonto(txtSaldoInicial.getText(), new BigDecimal("50000.00"));
        BigDecimal gastos = parseMonto(txtGastosEfectivo.getText(), BigDecimal.ZERO);
        BigDecimal efecCobrado = resumenActual.getCobranzasEfectivoTotal();
        BigDecimal esperado = saldoIni.add(efecCobrado).subtract(gastos);
        BigDecimal real = parseMonto(strReal, BigDecimal.ZERO);
        BigDecimal diferencia = real.subtract(esperado);

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("¿Desea cerrar el arqueo de la jornada de hoy?\n\n" +
                                "• Efectivo Esperado: $%,.2f\n" +
                                "• Efectivo Real: $%,.2f\n" +
                                "• Desvío de Caja: $%,.2f\n" +
                                "• Fondo de Reposición a Frigoríficos: $%,.2f\n" +
                                "• Margen Neto Comercial: $%,.2f\n\n" +
                                "Esta acción asentará el cierre formal en el sistema.",
                        esperado, real, diferencia, resumenActual.getCostoReposicionTotal(), resumenActual.getMargenBrutoEstimado()),
                "Confirmar Cierre de Caja", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            ArqueoCaja arq = new ArqueoCaja();
            arq.setIdUsuario(1);
            arq.setFechaArqueo(LocalDate.now());
            arq.setSaldoInicialEfectivo(saldoIni);
            arq.setEfectivoEsperado(esperado);
            arq.setEfectivoRealContado(real);
            arq.setDiferenciaEfectivo(diferencia);
            arq.setFondoReposicionSegregado(resumenActual.getCostoReposicionTotal());
            arq.setMargenNetoDia(resumenActual.getMargenBrutoEstimado());
            arq.setObservaciones(txtObservaciones.getText().trim());
            arq.setEstado("CERRADO");

            // Desglose de medios
            DetalleArqueo detEfec = new DetalleArqueo("EFECTIVO", resumenActual.getCobranzasEfectivoTotal(), gastos,
                    resumenActual.getCobranzasEfectivoTotal().subtract(gastos));
            DetalleArqueo detTransf = new DetalleArqueo("TRANSFERENCIA", resumenActual.getCobranzasTransferenciaTotal(), BigDecimal.ZERO,
                    resumenActual.getCobranzasTransferenciaTotal());

            arq.agregarDetalle(detEfec);
            arq.agregarDetalle(detTransf);

            int idArqueo = arqueoService.registrarArqueoCaja(arq);

            JOptionPane.showMessageDialog(this,
                    String.format("¡Arqueo ARQ-%04d registrado exitosamente!\n" +
                            "La jornada fue cerrada y los fondos han sido segregados correctamente.", idArqueo),
                    "Arqueo Concluido", JOptionPane.INFORMATION_MESSAGE);

            recargarMetricas();
            if (onDatosModificados != null) onDatosModificados.run();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar arqueo: " + ex.getMessage(),
                    "Error de Cierre", JOptionPane.ERROR_MESSAGE);
        }
    }

    private BigDecimal parseMonto(String texto, BigDecimal valorDefecto) {
        if (texto == null || texto.trim().isEmpty()) return valorDefecto;
        try {
            String limpio = texto.trim().replace("$", "").replace(".", "").replace(",", ".");
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return valorDefecto;
        }
    }
}
