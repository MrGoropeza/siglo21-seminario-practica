package com.sgcv.ui;

import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Cliente;
import com.sgcv.service.IClienteService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

/**
 * Diálogo modal para la creación y edición de clientes minoristas (B2B).
 * Gestiona razón social, CUIT, zona de reparto, dirección, teléfono y límite de crédito.
 */
public class ClienteDialog extends JDialog {

    private final IClienteService clienteService;
    private final Cliente clienteExistente;
    private final Runnable onGuardado;

    private JTextField txtRazonSocial;
    private JTextField txtCuit;
    private JComboBox<String> comboZona;
    private JTextField txtDireccion;
    private JTextField txtTelefono;
    private JTextField txtLimiteCredito;
    private JTextField txtSaldoInicial;

    public ClienteDialog(Window parent, IClienteService clienteService, Cliente clienteExistente, Runnable onGuardado) {
        super(parent, clienteExistente == null ? "➕ Nuevo Cliente Comercial" : "✏️ Modificar Cliente: " + clienteExistente.getRazonSocial(), ModalityType.APPLICATION_MODAL);
        this.clienteService = clienteService;
        this.clienteExistente = clienteExistente;
        this.onGuardado = onGuardado;

        setSize(480, 480);
        setResizable(false);
        setLocationRelativeTo(parent);

        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(0, 10));
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        root.setBackground(Color.WHITE);

        // Cabecera
        JPanel pHeader = new JPanel(new GridLayout(2, 1, 0, 3));
        pHeader.setOpaque(false);

        JLabel lblTit = new JLabel(clienteExistente == null ? "👤 Alta de Cliente Minorista" : "✏️ Modificación de Datos de Cliente");
        lblTit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        lblTit.setForeground(new Color(20, 35, 60));

        JLabel lblSub = new JLabel(clienteExistente == null
                ? "Ingresá los datos comerciales y la línea de crédito acordada:"
                : "Modificá la información comercial o la línea de crédito autorizada:");
        lblSub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblSub.setForeground(new Color(90, 100, 115));

        pHeader.add(lblTit);
        pHeader.add(lblSub);
        root.add(pHeader, BorderLayout.NORTH);

        // Formulario
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // 1. Razón Social
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblRazon = new JLabel("Razón Social (*):");
        lblRazon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        form.add(lblRazon, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtRazonSocial = crearCampoTexto(clienteExistente != null ? clienteExistente.getRazonSocial() : "");
        form.add(txtRazonSocial, gbc);

        // 2. CUIT
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblCuit = new JLabel("CUIT / Identificación (*):");
        lblCuit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        form.add(lblCuit, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtCuit = crearCampoTexto(clienteExistente != null ? clienteExistente.getCuit() : "");
        form.add(txtCuit, gbc);

        // 3. Zona de Reparto
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblZona = new JLabel("Zona de Reparto:");
        lblZona.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        form.add(lblZona, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        String[] zonas = {"Zona Oeste", "Zona Norte", "Zona Sur", "CABA", "Zona Este"};
        comboZona = new JComboBox<>(zonas);
        comboZona.setEditable(true);
        comboZona.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        comboZona.setPreferredSize(new Dimension(0, 30));
        if (clienteExistente != null && clienteExistente.getZonaReparto() != null) {
            comboZona.setSelectedItem(clienteExistente.getZonaReparto());
        }
        form.add(comboZona, gbc);

        // 4. Dirección Comercial
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblDir = new JLabel("Dirección Comercial:");
        lblDir.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        form.add(lblDir, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtDireccion = crearCampoTexto(clienteExistente != null ? clienteExistente.getDireccion() : "");
        form.add(txtDireccion, gbc);

        // 5. Teléfono
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblTel = new JLabel("Teléfono de Contacto:");
        lblTel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        form.add(lblTel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtTelefono = crearCampoTexto(clienteExistente != null ? clienteExistente.getTelefono() : "");
        form.add(txtTelefono, gbc);

        // 6. Límite de Crédito
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblLim = new JLabel("Límite de Crédito ($):");
        lblLim.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        form.add(lblLim, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        BigDecimal lim = clienteExistente != null ? clienteExistente.getLimiteCredito() : new BigDecimal("250000.00");
        txtLimiteCredito = crearCampoTexto(lim.setScale(2, RoundingMode.HALF_UP).toString());
        form.add(txtLimiteCredito, gbc);

        // 7. Saldo Inicial / Actual
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblSaldo = new JLabel(clienteExistente == null ? "Saldo Inicial Deuda ($):" : "Saldo Actual en Cuenta ($):");
        lblSaldo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        form.add(lblSaldo, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        if (clienteExistente == null) {
            txtSaldoInicial = crearCampoTexto("0.00");
            form.add(txtSaldoInicial, gbc);
        } else {
            JLabel lblSaldoActual = new JLabel(String.format("$%,.2f", clienteExistente.getSaldoCuentaCorriente()));
            lblSaldoActual.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            lblSaldoActual.setForeground(clienteExistente.getSaldoCuentaCorriente().compareTo(BigDecimal.ZERO) > 0
                    ? new Color(180, 30, 20) : new Color(30, 120, 50));
            form.add(lblSaldoActual, gbc);
        }

        root.add(form, BorderLayout.CENTER);

        // Botones Inferiores
        JPanel pBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pBotones.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnCancelar.addActionListener(e -> dispose());
        pBotones.add(btnCancelar);

        JButton btnGuardar = new JButton(clienteExistente == null ? "💾 Guardar Cliente" : "💾 Guardar Cambios");
        btnGuardar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        btnGuardar.setBackground(new Color(20, 60, 120));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> guardar());
        pBotones.add(btnGuardar);

        root.add(pBotones, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JTextField crearCampoTexto(String valorInicial) {
        JTextField txt = new JTextField(valorInicial);
        txt.setPreferredSize(new Dimension(0, 30));
        txt.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        txt.setBorder(new CompoundBorder(
                new LineBorder(new Color(205, 215, 225), 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        txt.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(txt::selectAll);
            }
        });
        return txt;
    }

    private void guardar() {
        String razon = txtRazonSocial.getText().trim();
        String cuit = txtCuit.getText().trim();
        String zona = comboZona.getSelectedItem() != null ? comboZona.getSelectedItem().toString().trim() : "";
        String direccion = txtDireccion.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String strLimite = txtLimiteCredito.getText().trim();

        if (razon.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La razón social del cliente es obligatoria.", "Dato Faltante", JOptionPane.WARNING_MESSAGE);
            txtRazonSocial.requestFocus();
            return;
        }

        if (cuit.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El CUIT o número de identificación tributaria es obligatorio.", "Dato Faltante", JOptionPane.WARNING_MESSAGE);
            txtCuit.requestFocus();
            return;
        }

        BigDecimal limite;
        try {
            limite = new BigDecimal(strLimite.replace("$", "").replace(" ", "").replace(",", "."));
            if (limite.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this, "El límite de crédito no puede ser negativo.", "Límite Inválido", JOptionPane.WARNING_MESSAGE);
                txtLimiteCredito.requestFocus();
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un importe numérico válido para el límite de crédito.", "Error de Formato", JOptionPane.WARNING_MESSAGE);
            txtLimiteCredito.requestFocus();
            return;
        }

        BigDecimal saldoInicial = BigDecimal.ZERO;
        if (clienteExistente == null && txtSaldoInicial != null) {
            try {
                String strSaldo = txtSaldoInicial.getText().trim();
                if (!strSaldo.isEmpty()) {
                    saldoInicial = new BigDecimal(strSaldo.replace("$", "").replace(" ", "").replace(",", "."));
                    if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) saldoInicial = BigDecimal.ZERO;
                }
            } catch (NumberFormatException ex) {
                saldoInicial = BigDecimal.ZERO;
            }
        }

        try {
            if (clienteExistente == null) {
                Cliente nuevo = new Cliente(
                        0, razon, cuit, direccion, telefono, zona, saldoInicial, limite, true
                );
                int idGenerado = clienteService.crearCliente(nuevo);
                JOptionPane.showMessageDialog(this,
                        String.format("¡Cliente '%s' registrado exitosamente con ID %d!", razon, idGenerado),
                        "Cliente Creado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                clienteExistente.setRazonSocial(razon);
                clienteExistente.setCuit(cuit);
                clienteExistente.setZonaReparto(zona);
                clienteExistente.setDireccion(direccion);
                clienteExistente.setTelefono(telefono);
                clienteExistente.setLimiteCredito(limite);

                clienteService.actualizarCliente(clienteExistente);
                JOptionPane.showMessageDialog(this,
                        String.format("¡Datos de '%s' actualizados exitosamente!", razon),
                        "Cliente Actualizado", JOptionPane.INFORMATION_MESSAGE);
            }

            dispose();
            if (onGuardado != null) onGuardado.run();

        } catch (ValidacionException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validación de Negocio", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            boolean duplicado = ex.getErrorCode() == 1062 || "23000".equals(ex.getSQLState());
            if (duplicado) {
                JOptionPane.showMessageDialog(this, "Ya existe otro cliente con el CUIT " + cuit + ". Por favor verifique.", "CUIT Duplicado", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar cliente: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
