package com.sgcv.ui;

import com.sgcv.model.Cliente;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Componente unificado de búsqueda y autocompletado de clientes.
 * Permite buscar en tiempo real por razón social o CUIT, desplegando
 * sugerencias instantáneas e interactivas sin requerir dropdowns secundarios.
 */
public class ClienteAutoCompleteBox extends JPanel {

    private final JTextField txtInput;
    private final JButton btnDropdown;
    private final JButton btnLimpiar;
    private final JPopupMenu popupMenu;
    private final DefaultListModel<Cliente> listModel;
    private final JList<Cliente> listaSugerencias;

    private List<Cliente> todosLosClientes = new ArrayList<>();
    private String zonaFiltro = "Todas las Zonas";
    private Cliente clienteSeleccionado = null;
    private Consumer<Cliente> onClienteSeleccionadoListener;

    private boolean actualizandoInternamente = false;

    public ClienteAutoCompleteBox() {
        setLayout(new BorderLayout(4, 0));
        setOpaque(false);

        // Campo de texto principal con borde limpio
        txtInput = new JTextField();
        txtInput.setPreferredSize(new Dimension(340, 32));
        txtInput.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        txtInput.setToolTipText("Escriba nombre o CUIT del cliente para autocompletar");
        txtInput.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 195, 215), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));

        // Botón desplegar todo
        btnDropdown = new JButton("▼");
        btnDropdown.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        btnDropdown.setFocusable(false);
        btnDropdown.setPreferredSize(new Dimension(28, 32));
        btnDropdown.setToolTipText("Ver listado completo de clientes");
        btnDropdown.addActionListener(e -> alternarPopupCompleto());

        // Botón limpiar campo
        btnLimpiar = new JButton("✕");
        btnLimpiar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        btnLimpiar.setFocusable(false);
        btnLimpiar.setPreferredSize(new Dimension(26, 32));
        btnLimpiar.setToolTipText("Limpiar búsqueda");
        btnLimpiar.setVisible(false);
        btnLimpiar.addActionListener(e -> {
            limpiarSeleccion();
            txtInput.requestFocusInWindow();
        });

        JPanel pBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        pBotones.setOpaque(false);
        pBotones.add(btnLimpiar);
        pBotones.add(btnDropdown);

        add(txtInput, BorderLayout.CENTER);
        add(pBotones, BorderLayout.EAST);

        // Configuración de la lista de sugerencias en Popup
        listModel = new DefaultListModel<>();
        listaSugerencias = new JList<>(listModel);
        listaSugerencias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaSugerencias.setFocusable(false);
        listaSugerencias.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        listaSugerencias.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Cliente c) {
                    boolean tieneDeuda = c.getSaldoCuentaCorriente() != null && c.getSaldoCuentaCorriente().compareTo(BigDecimal.ZERO) > 0;
                    String colorDeuda = tieneDeuda ? "#b42814" : "#197832";
                    String texto = String.format("<html><b>%s</b> &nbsp;<font color='#666666'>(%s - CUIT: %s)</font> &nbsp;|&nbsp; <font color='%s'><b>Debe: $%,.2f</b></font></html>",
                            escapeHtml(c.getRazonSocial()),
                            escapeHtml(c.getZonaReparto()),
                            escapeHtml(c.getCuit()),
                            colorDeuda,
                            c.getSaldoCuentaCorriente() != null ? c.getSaldoCuentaCorriente() : BigDecimal.ZERO
                    );
                    setText(texto);
                    setBorder(new EmptyBorder(6, 8, 6, 8));
                }
                return this;
            }
        });

        JScrollPane scrollPopup = new JScrollPane(listaSugerencias);
        scrollPopup.setBorder(new LineBorder(new Color(180, 195, 215), 1));
        scrollPopup.setPreferredSize(new Dimension(450, 180));

        popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createEmptyBorder());
        popupMenu.add(scrollPopup);

        // Listeners de teclado y mouse
        configurarEventos();
    }

    private void configurarEventos() {
        txtInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onTextoCambiado(); }
            @Override public void removeUpdate(DocumentEvent e) { onTextoCambiado(); }
            @Override public void changedUpdate(DocumentEvent e) { onTextoCambiado(); }
        });

        txtInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(txtInput::selectAll);
            }
        });

        txtInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (popupMenu.isVisible() && listModel.getSize() > 0) {
                        int idx = listaSugerencias.getSelectedIndex();
                        if (idx < listModel.getSize() - 1) {
                            listaSugerencias.setSelectedIndex(idx + 1);
                            listaSugerencias.ensureIndexIsVisible(idx + 1);
                        }
                    } else {
                        mostrarSugerenciasFiltradas();
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (popupMenu.isVisible() && listModel.getSize() > 0) {
                        int idx = listaSugerencias.getSelectedIndex();
                        if (idx > 0) {
                            listaSugerencias.setSelectedIndex(idx - 1);
                            listaSugerencias.ensureIndexIsVisible(idx - 1);
                        }
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (popupMenu.isVisible()) {
                        Cliente sel = listaSugerencias.getSelectedValue();
                        if (sel != null) {
                            seleccionarClienteInterno(sel);
                        }
                        popupMenu.setVisible(false);
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popupMenu.setVisible(false);
                    e.consume();
                }
            }
        });

        listaSugerencias.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Cliente sel = listaSugerencias.getSelectedValue();
                if (sel != null) {
                    seleccionarClienteInterno(sel);
                    popupMenu.setVisible(false);
                }
            }
        });
    }

    private void onTextoCambiado() {
        if (actualizandoInternamente) return;

        String texto = txtInput.getText();
        btnLimpiar.setVisible(!texto.trim().isEmpty());

        // Si el usuario borró o cambió el texto respecto al cliente seleccionado, invalidar selección previa
        if (clienteSeleccionado != null && !clienteSeleccionado.getRazonSocial().equalsIgnoreCase(texto.trim())) {
            clienteSeleccionado = null;
            if (onClienteSeleccionadoListener != null) {
                onClienteSeleccionadoListener.accept(null);
            }
        }

        mostrarSugerenciasFiltradas();
    }

    private void mostrarSugerenciasFiltradas() {
        String texto = txtInput.getText().trim().toLowerCase();
        listModel.clear();

        for (Cliente c : todosLosClientes) {
            boolean coincideZona = zonaFiltro == null || "Todas las Zonas".equalsIgnoreCase(zonaFiltro) || zonaFiltro.equalsIgnoreCase(c.getZonaReparto());
            boolean coincideTexto = texto.isEmpty()
                    || c.getRazonSocial().toLowerCase().contains(texto)
                    || c.getCuit().replace("-", "").contains(texto.replace("-", ""));

            if (coincideZona && coincideTexto) {
                listModel.addElement(c);
            }
        }

        if (listModel.getSize() > 0 && txtInput.isShowing() && txtInput.hasFocus()) {
            if (listaSugerencias.getSelectedIndex() < 0) {
                listaSugerencias.setSelectedIndex(0);
            }
            popupMenu.show(txtInput, 0, txtInput.getHeight());
            txtInput.requestFocusInWindow();
        } else {
            popupMenu.setVisible(false);
        }
    }

    private void alternarPopupCompleto() {
        if (popupMenu.isVisible()) {
            popupMenu.setVisible(false);
        } else {
            listModel.clear();
            for (Cliente c : todosLosClientes) {
                boolean coincideZona = zonaFiltro == null || "Todas las Zonas".equalsIgnoreCase(zonaFiltro) || zonaFiltro.equalsIgnoreCase(c.getZonaReparto());
                if (coincideZona) {
                    listModel.addElement(c);
                }
            }
            if (listModel.getSize() > 0) {
                if (clienteSeleccionado != null) {
                    listaSugerencias.setSelectedValue(clienteSeleccionado, true);
                } else {
                    listaSugerencias.setSelectedIndex(0);
                }
                popupMenu.show(txtInput, 0, txtInput.getHeight());
                txtInput.requestFocusInWindow();
            }
        }
    }

    private void seleccionarClienteInterno(Cliente cliente) {
        this.clienteSeleccionado = cliente;
        this.actualizandoInternamente = true;
        try {
            txtInput.setText(cliente != null ? cliente.getRazonSocial() : "");
            btnLimpiar.setVisible(cliente != null);
        } finally {
            this.actualizandoInternamente = false;
        }

        if (onClienteSeleccionadoListener != null) {
            onClienteSeleccionadoListener.accept(cliente);
        }
    }

    // =========================================================================
    // API Pública
    // =========================================================================

    public void setTodosLosClientes(List<Cliente> clientes) {
        this.todosLosClientes = clientes != null ? new ArrayList<>(clientes) : new ArrayList<>();
    }

    public void setZonaFiltro(String zona) {
        this.zonaFiltro = zona;
        if (clienteSeleccionado != null) {
            boolean pertenece = zona == null || "Todas las Zonas".equalsIgnoreCase(zona) || zona.equalsIgnoreCase(clienteSeleccionado.getZonaReparto());
            if (!pertenece) {
                limpiarSeleccion();
            }
        }
    }

    public void seleccionarCliente(Cliente cliente) {
        seleccionarClienteInterno(cliente);
    }

    public void limpiarSeleccion() {
        seleccionarClienteInterno(null);
    }

    public Cliente getClienteSeleccionado() {
        return clienteSeleccionado;
    }

    public void setOnClienteSeleccionadoListener(Consumer<Cliente> listener) {
        this.onClienteSeleccionadoListener = listener;
    }

    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
