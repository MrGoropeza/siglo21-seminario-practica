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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pantalla 2: Cierre y Arqueo Diario de Reparto (CU-04).
 * Automatiza la conciliación nocturna del reparto para el chofer y el distribuidor.
 * Diseñada para máxima claridad: control de billetes físicos, detección inmediata
 * de diferencias y segregación automática en sobre frigorífico vs margen comercial.
 */
public class ArqueoPanel extends JPanel {

    private final IArqueoService arqueoService;
    private final Runnable onDatosModificados;

    private ResumenDiarioDTO resumenActual;

    // Resumen Superior (Barra Rápida de Jornada)
    private JLabel lblBultosHoy;
    private JLabel lblFacturacionHoy;
    private JLabel lblCobranzasTotalHoy;
    private JLabel lblCobranzasEfectivoHoy;
    private JLabel lblCobranzasTransfHoy;

    // Paso 1: Arqueo de Efectivo Físico
    private JTextField txtSaldoInicial;
    private JLabel lblEfectivoCobrado;
    private JTextField txtGastosEfectivo;
    private JLabel lblEfectivoEsperado;
    private JTextField txtEfectivoReal;
    private JLabel lblDiferenciaCaja;
    private JPanel panelBadgeDiferencia;

    // Paso 2: Segregación de Fondos y Cierre
    private JLabel lblFondoReposicion;
    private JLabel lblMargenNeto;
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
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(10, 14, 12, 14));
        setBackground(new Color(245, 247, 250));

        // 1. Barra Resumen de la Jornada (Norte)
        add(crearBarraResumenJornada(), BorderLayout.NORTH);

        // 2. Centro dividido en 2 Pasos Clave: Control de Efectivo y Destino de Fondos
        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 14, 0));
        panelCentro.setOpaque(false);
        panelCentro.add(crearCardPaso1ArqueoEfectivo());
        panelCentro.add(crearCardPaso2SegregacionFondos());
        add(panelCentro, BorderLayout.CENTER);

        // 3. Panel Inferior: Historial de Arqueos Registrados (Compacto)
        add(crearPanelHistorico(), BorderLayout.SOUTH);
    }

    // =========================================================================
    // 1. Barra Resumen Superior (Concisa y Elegante)
    // =========================================================================

    private JPanel crearBarraResumenJornada() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));

        // Título y Fecha
        JPanel pIzq = new JPanel(new GridLayout(2, 1, 0, 2));
        pIzq.setOpaque(false);

        JLabel lblTit = new JLabel("📊 Conciliación Nocturna del Reparto");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        lblTit.setForeground(new Color(20, 35, 60));

        JLabel lblSub = new JLabel("Jornada: " + LocalDate.now().format(FORMATO_FECHA) + " | Estado: Listo para cierre");
        lblSub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 110, 125));

        pIzq.add(lblTit);
        pIzq.add(lblSub);
        bar.add(pIzq, BorderLayout.WEST);

        // Indicadores compactos a la derecha
        JPanel pDer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pDer.setOpaque(false);

        lblBultosHoy = new JLabel("0 cajones", SwingConstants.CENTER);
        lblFacturacionHoy = new JLabel("$0.00", SwingConstants.CENTER);
        lblCobranzasTotalHoy = new JLabel("$0.00", SwingConstants.CENTER);
        lblCobranzasEfectivoHoy = new JLabel("$0.00", SwingConstants.CENTER);
        lblCobranzasTransfHoy = new JLabel("$0.00", SwingConstants.CENTER);

        pDer.add(crearBadgeResumen("📦 REPARTIDOS", lblBultosHoy, new Color(15, 60, 130)));
        pDer.add(crearBadgeResumen("💵 COBRADO EFECTIVO", lblCobranzasEfectivoHoy, new Color(25, 120, 50)));
        pDer.add(crearBadgeResumen("📱 COBRADO TRANSFERENCIA", lblCobranzasTransfHoy, new Color(80, 50, 130)));

        JButton btnActualizar = new JButton("🔄 Recalcular");
        btnActualizar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnActualizar.addActionListener(e -> recargarMetricas());
        pDer.add(btnActualizar);

        bar.add(pDer, BorderLayout.EAST);
        return bar;
    }

    private JPanel crearBadgeResumen(String titulo, JLabel lblValor, Color color) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);

        JLabel lblT = new JLabel(titulo, SwingConstants.CENTER);
        lblT.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        lblT.setForeground(new Color(120, 130, 145));

        lblValor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblValor.setForeground(color);

        p.add(lblT);
        p.add(lblValor);
        return p;
    }

    // =========================================================================
    // 2. PASO 1: Control de Efectivo Físico en Mano
    // =========================================================================

    private JPanel crearCardPaso1ArqueoEfectivo() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 220, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Cabecera del Paso 1
        JPanel pHeader = new JPanel(new GridLayout(2, 1, 0, 2));
        pHeader.setOpaque(false);

        JLabel lblTit = new JLabel("💵 PASO 1: Control de Billetes en Mano");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblTit.setForeground(new Color(20, 40, 80));

        JLabel lblSub = new JLabel("Comprobá los gastos y contá la plata que tenés en la camioneta:");
        lblSub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblSub.setForeground(new Color(90, 100, 115));

        pHeader.add(lblTit);
        pHeader.add(lblSub);
        card.add(pHeader, BorderLayout.NORTH);

        // Centro: Formulario claro y directo
        JPanel pCentro = new JPanel();
        pCentro.setLayout(new BoxLayout(pCentro, BoxLayout.Y_AXIS));
        pCentro.setOpaque(false);

        // Fila 1: Saldo Inicial de Cambio
        pCentro.add(crearFilaConcepto("Saldo Inicial (Fondo de Cambio):",
                txtSaldoInicial = crearCampoMonto("50000.00", false, "Dinero con el que salió la camioneta a la mañana")));

        // Fila 2: (+) Cobranzas en Efectivo Hoy
        lblEfectivoCobrado = new JLabel("$0.00", SwingConstants.RIGHT);
        lblEfectivoCobrado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblEfectivoCobrado.setForeground(new Color(25, 120, 50));
        pCentro.add(crearFilaConcepto("(+) Cobranzas Efectivo de Hoy:", lblEfectivoCobrado));

        // Fila 3: (-) Gastos en Efectivo
        pCentro.add(crearFilaConcepto("(-) Gastos de Ruta (Peajes / Nafta):",
                txtGastosEfectivo = crearCampoMonto("0.00", false, "Total gastado en combustible, peajes o comida")));

        // Separador
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        pCentro.add(Box.createVerticalStrut(4));
        pCentro.add(sep);
        pCentro.add(Box.createVerticalStrut(4));

        // Fila 4: (=) Efectivo Teórico Esperado
        lblEfectivoEsperado = new JLabel("$0.00", SwingConstants.RIGHT);
        lblEfectivoEsperado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        lblEfectivoEsperado.setForeground(new Color(15, 45, 90));
        pCentro.add(crearFilaConcepto("(=) Total que debería haber en el bolso:", lblEfectivoEsperado));

        pCentro.add(Box.createVerticalStrut(8));

        // Fila 5: ENTRADA PRINCIPAL: Efectivo Real Contado
        JPanel pInputReal = new JPanel(new BorderLayout(8, 4));
        pInputReal.setBackground(new Color(254, 252, 232));
        pInputReal.setBorder(new CompoundBorder(
                new LineBorder(new Color(230, 205, 120), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel lblInput = new JLabel("<html><b>👉 TOTAL DE BILLETES CONTADOS ($):</b><br><font size='2' color='#666666'>Contá todos los billetes físicos e ingresá el monto acá</font></html>");
        lblInput.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        pInputReal.add(lblInput, BorderLayout.WEST);

        txtEfectivoReal = new JTextField();
        txtEfectivoReal.setPreferredSize(new Dimension(170, 36));
        txtEfectivoReal.setHorizontalAlignment(SwingConstants.RIGHT);
        txtEfectivoReal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        txtEfectivoReal.setForeground(new Color(20, 35, 60));
        txtEfectivoReal.setBackground(Color.WHITE);
        txtEfectivoReal.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 160, 50), 2, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        txtEfectivoReal.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txtEfectivoReal::selectAll); }
        });
        txtEfectivoReal.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcularCuadre(); }
        });
        pInputReal.add(txtEfectivoReal, BorderLayout.EAST);
        pCentro.add(pInputReal);

        pCentro.add(Box.createVerticalStrut(8));

        // Fila 6: Badge de Diferencia de Caja
        panelBadgeDiferencia = new JPanel(new BorderLayout());
        panelBadgeDiferencia.setBackground(new Color(240, 243, 246));
        panelBadgeDiferencia.setBorder(new CompoundBorder(
                new LineBorder(new Color(205, 215, 225), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        lblDiferenciaCaja = new JLabel("👉 Ingrese el efectivo contado para verificar el cuadre", SwingConstants.CENTER);
        lblDiferenciaCaja.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblDiferenciaCaja.setForeground(new Color(90, 100, 115));
        panelBadgeDiferencia.add(lblDiferenciaCaja, BorderLayout.CENTER);

        pCentro.add(panelBadgeDiferencia);
        card.add(pCentro, BorderLayout.CENTER);

        return card;
    }

    private JPanel crearFilaConcepto(String etiqueta, JComponent campo) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lbl.setForeground(new Color(50, 60, 75));

        fila.add(lbl, BorderLayout.WEST);
        fila.add(campo, BorderLayout.EAST);
        return fila;
    }

    private JTextField crearCampoMonto(String inicial, boolean destacado, String tooltip) {
        JTextField txt = new JTextField(inicial);
        txt.setPreferredSize(new Dimension(140, 26));
        txt.setHorizontalAlignment(SwingConstants.RIGHT);
        txt.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        txt.setToolTipText(tooltip);
        txt.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(txt::selectAll); }
        });
        txt.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcularCuadre(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcularCuadre(); }
        });
        return txt;
    }

    // =========================================================================
    // 3. PASO 2: Segregación Automática de Fondos y Cierre Formal
    // =========================================================================

    private JPanel crearCardPaso2SegregacionFondos() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 220, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Cabecera del Paso 2
        JPanel pHeader = new JPanel(new GridLayout(2, 1, 0, 2));
        pHeader.setOpaque(false);

        JLabel lblTit = new JLabel("🏦 PASO 2: Destino de Fondos y Cierre");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblTit.setForeground(new Color(20, 40, 80));

        JLabel lblSub = new JLabel("Separación automática del dinero para garantizar la reposición:");
        lblSub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblSub.setForeground(new Color(90, 100, 115));

        pHeader.add(lblTit);
        pHeader.add(lblSub);
        card.add(pHeader, BorderLayout.NORTH);

        // Centro: Los 2 Sobres Físicos de Dinero
        JPanel pSobres = new JPanel(new GridLayout(2, 1, 0, 8));
        pSobres.setOpaque(false);

        // Sobre 1: Reposición Frigoríficos (Soychú / Tres Arroyos)
        JPanel pSobre1 = new JPanel(new BorderLayout(0, 3));
        pSobre1.setBackground(new Color(240, 247, 255));
        pSobre1.setBorder(new CompoundBorder(
                new LineBorder(new Color(185, 215, 245), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblTitSobre1 = new JLabel("📦 SOBRE PARA FRIGORÍFICO (Costo de Reposición / Faena)");
        lblTitSobre1.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lblTitSobre1.setForeground(new Color(15, 60, 140));

        lblFondoReposicion = new JLabel("$0.00", SwingConstants.CENTER);
        lblFondoReposicion.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        lblFondoReposicion.setForeground(new Color(10, 50, 130));

        JLabel lblDescSobre1 = new JLabel("<html><i>Dinero intocable para pagar la faena del frigorífico mañana.</i></html>", SwingConstants.CENTER);
        lblDescSobre1.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        lblDescSobre1.setForeground(new Color(85, 100, 120));

        pSobre1.add(lblTitSobre1, BorderLayout.NORTH);
        pSobre1.add(lblFondoReposicion, BorderLayout.CENTER);
        pSobre1.add(lblDescSobre1, BorderLayout.SOUTH);
        pSobres.add(pSobre1);

        // Sobre 2: Margen Neto Real
        JPanel pSobre2 = new JPanel(new BorderLayout(0, 3));
        pSobre2.setBackground(new Color(242, 253, 245));
        pSobre2.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 230, 195), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblTitSobre2 = new JLabel("💰 MARGEN NETO / GANANCIA LIMPIA DE HOY");
        lblTitSobre2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lblTitSobre2.setForeground(new Color(20, 115, 45));

        lblMargenNeto = new JLabel("$0.00", SwingConstants.CENTER);
        lblMargenNeto.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        lblMargenNeto.setForeground(new Color(15, 115, 45));

        JLabel lblDescSobre2 = new JLabel("<html><i>Ganancia comercial efectiva tras cubrir el costo de mercadería.</i></html>", SwingConstants.CENTER);
        lblDescSobre2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        lblDescSobre2.setForeground(new Color(85, 100, 120));

        pSobre2.add(lblTitSobre2, BorderLayout.NORTH);
        pSobre2.add(lblMargenNeto, BorderLayout.CENTER);
        pSobre2.add(lblDescSobre2, BorderLayout.SOUTH);
        pSobres.add(pSobre2);

        card.add(pSobres, BorderLayout.CENTER);

        // Sur: Observaciones y Botón de Cierre
        JPanel pCierre = new JPanel(new BorderLayout(0, 6));
        pCierre.setOpaque(false);

        JPanel pObs = new JPanel(new BorderLayout(6, 0));
        pObs.setOpaque(false);
        pObs.add(new JLabel("Observaciones:"), BorderLayout.WEST);
        txtObservaciones = new JTextField("Cierre sin desvíos. Fondo segregado reservado para frigorífico.");
        pObs.add(txtObservaciones, BorderLayout.CENTER);
        pCierre.add(pObs, BorderLayout.NORTH);

        btnCerrarArqueo = new JButton("🔒 CONFIRMAR Y CERRAR CAJA DE HOY");
        btnCerrarArqueo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnCerrarArqueo.setBackground(new Color(18, 48, 85));
        btnCerrarArqueo.setForeground(Color.WHITE);
        btnCerrarArqueo.setPreferredSize(new Dimension(0, 40));
        btnCerrarArqueo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarArqueo.addActionListener(e -> confirmarYCerrarArqueo());
        pCierre.add(btnCerrarArqueo, BorderLayout.SOUTH);

        card.add(pCierre, BorderLayout.SOUTH);
        return card;
    }

    // =========================================================================
    // 4. Panel Inferior: Historial de Cierres de Caja Registrados
    // =========================================================================

    private JPanel crearPanelHistorico() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(0, 150));
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(218, 224, 233), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblTit = new JLabel("📑 Historial de Cierres de Caja Registrados");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
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

    // =========================================================================
    // Lógica Operativa y Recálculos de Caja
    // =========================================================================

    public void recargarMetricas() {
        try {
            resumenActual = arqueoService.calcularResumenDiario(LocalDate.now());

            lblBultosHoy.setText(String.format("%d cajones", resumenActual.getBultosEntregadosTotal()));
            lblFacturacionHoy.setText(String.format("$%,.2f", resumenActual.getFacturacionEntregasTotal()));
            lblCobranzasTotalHoy.setText(String.format("$%,.2f", resumenActual.getTotalCobrado()));
            lblCobranzasEfectivoHoy.setText(String.format("$%,.2f", resumenActual.getCobranzasEfectivoTotal()));
            lblCobranzasTransfHoy.setText(String.format("$%,.2f", resumenActual.getCobranzasTransferenciaTotal()));

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
            lblDiferenciaCaja.setText("👉 Ingrese el efectivo contado para verificar el cuadre");
            lblDiferenciaCaja.setForeground(new Color(90, 100, 115));
            panelBadgeDiferencia.setBackground(new Color(240, 243, 246));
            panelBadgeDiferencia.setBorder(new CompoundBorder(
                    new LineBorder(new Color(205, 215, 225), 1, true),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            return;
        }

        BigDecimal real = parseMonto(strReal, BigDecimal.ZERO);
        BigDecimal diferencia = real.subtract(esperado);

        if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
            lblDiferenciaCaja.setText("✅ ¡CAJA CUADRADA PERFECTA! ($0.00)");
            lblDiferenciaCaja.setForeground(new Color(27, 94, 32));
            panelBadgeDiferencia.setBackground(new Color(232, 245, 233));
            panelBadgeDiferencia.setBorder(new CompoundBorder(
                    new LineBorder(new Color(165, 214, 167), 1, true),
                    new EmptyBorder(8, 10, 8, 10)
            ));
        } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
            lblDiferenciaCaja.setText(String.format("⚠️ FALTANTE EN CAJA: -$%,.2f", diferencia.abs()));
            lblDiferenciaCaja.setForeground(new Color(183, 28, 28));
            panelBadgeDiferencia.setBackground(new Color(255, 235, 238));
            panelBadgeDiferencia.setBorder(new CompoundBorder(
                    new LineBorder(new Color(239, 154, 154), 1, true),
                    new EmptyBorder(8, 10, 8, 10)
            ));
        } else {
            lblDiferenciaCaja.setText(String.format("ℹ️ SOBRANTE EN CAJA: +$%,.2f", diferencia));
            lblDiferenciaCaja.setForeground(new Color(13, 71, 161));
            panelBadgeDiferencia.setBackground(new Color(227, 242, 253));
            panelBadgeDiferencia.setBorder(new CompoundBorder(
                    new LineBorder(new Color(144, 202, 249), 1, true),
                    new EmptyBorder(8, 10, 8, 10)
            ));
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
                String.format("¿Desea asentar el cierre del arqueo de hoy?\n\n" +
                                "• Efectivo Esperado en Mano: $%,.2f\n" +
                                "• Efectivo Real Contado: $%,.2f\n" +
                                "• Desvío / Diferencia de Caja: $%,.2f\n\n" +
                                "• Sobre para Frigoríficos (Soychú/Tres Arroyos): $%,.2f\n" +
                                "• Margen Neto Comercial: $%,.2f\n\n" +
                                "¿Confirmar y asentar cierre definitivo?",
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
            String limpio = texto.trim().replace("$", "").replace(" ", "");
            if (limpio.isEmpty()) return valorDefecto;
            if (limpio.contains(",") && limpio.contains(".")) {
                limpio = limpio.replace(".", "").replace(",", ".");
            } else if (limpio.contains(",")) {
                limpio = limpio.replace(",", ".");
            }
            BigDecimal bd = new BigDecimal(limpio);
            return bd.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : bd;
        } catch (NumberFormatException e) {
            return valorDefecto;
        }
    }
}
