package views;

import controller.PagosController;
import model.Usuario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import controller.ProgramaController;
import model.Programa;
import java.awt.geom.Arc2D;

/**
 * Dashboard Financiero de AdminNexus.
 * Visualiza el estado global de pagos y permite acceder al historial por estudiante.
 */
public class PagosPanel extends JPanel {

    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG = new Color(249, 250, 251);
    private Color COLOR_CARD = Color.WHITE;
    private Color COLOR_TEXT = new Color(17, 24, 39);
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    private Color COLOR_BORDER = new Color(229, 231, 235);
    private Color COLOR_SELECTION = new Color(239, 246, 255);
    private Color COLOR_SUCCESS = new Color(34, 197, 94);
    private Color COLOR_WARNING = new Color(245, 158, 11);
    private Color COLOR_DANGER = new Color(239, 68, 68);

    private Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    private Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    private Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    private Usuario usuarioActual;
    private PagosController pagosController;
    private DefaultTableModel modeloTabla;
    private JTable tablaPagos;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private java.text.SimpleDateFormat tableDateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");

    // Labels de resumen
    private JLabel lblTotalRecaudado;
    private JLabel lblTotalPendiente;
    private JLabel lblTotalEstudiantes;
    private JComboBox<String> comboProgramas;
    private JTextField txtBuscar;

    // Paginación
    private int paginaActual = 1;
    private int totalEstudiantes = 0;
    private JLabel lblMostrando;
    private JButton btnPrev;
    private JButton btnNext;
    private JButton[] btnsPagina;

    // Nuevas Gráficas
    private DonutChart chartCartera;
    private BarChart chartIngresos;

    public PagosPanel(Usuario usuario) {
        this.usuarioActual = usuario;
        this.pagosController = new PagosController();
        
        cargarConfiguracion();
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        add(crearHeader(), BorderLayout.NORTH);
        
        JPanel contentArea = new JPanel(new BorderLayout(0, 20));
        contentArea.setOpaque(true);
        contentArea.setBackground(COLOR_BG);
        contentArea.setBorder(new EmptyBorder(0, 30, 30, 30));
        
        contentArea.add(crearResumenCards(), BorderLayout.NORTH);
        contentArea.add(crearCuerpoPrincipal(), BorderLayout.CENTER);
        
        JScrollPane globalScroll = new JScrollPane(contentArea);
        globalScroll.setBorder(null);
        globalScroll.getVerticalScrollBar().setUnitIncrement(16);
        globalScroll.getViewport().setBackground(COLOR_BG);
        
        add(globalScroll, BorderLayout.CENTER);
        
        refrescarDatos();
    }

    private void cargarConfiguracion() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuarioActual.getIdusuario());
        String mode = prefs.get("app_mode", "light");
        if (mode.equals("dark")) {
            COLOR_BG = new Color(17, 24, 39);
            COLOR_CARD = new Color(31, 41, 55);
            COLOR_TEXT = Color.WHITE;
            COLOR_TEXT_MUTED = new Color(156, 163, 175);
            COLOR_BORDER = new Color(55, 65, 81);
            COLOR_SELECTION = new Color(59, 130, 246, 40);
        } else {
            COLOR_SELECTION = new Color(239, 246, 255);
        }
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_CARD);
        header.setBorder(new EmptyBorder(25, 30, 25, 30));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Centro Financiero");
        title.setFont(FONT_TITLE);
        title.setForeground(COLOR_TEXT);

        JLabel subtitle = new JLabel("Monitoreo de ingresos, saldos pendientes y gestión de cobranza.");
        subtitle.setFont(FONT_SUBTITLE);
        subtitle.setForeground(COLOR_TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);

        // Botón de Sincronización
        JButton btnSincronizar = new JButton("Sincronizar Estudiantes");
        estilizarBotonEncabezado(btnSincronizar);
        btnSincronizar.addActionListener(e -> ejecutarSincronizacion());
        
        header.add(btnSincronizar, BorderLayout.EAST);
        
        return header;
    }

    private void estilizarBotonEncabezado(JButton btn) {
        btn.setFont(FONT_BOLD);
        btn.setBackground(COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_PRIMARY.darker()),
            new EmptyBorder(8, 15, 8, 15)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void ejecutarSincronizacion() {
        int pendientes = pagosController.contarEstudiantesSinPlan();
        if (pendientes == 0) {
            JOptionPane.showMessageDialog(this, 
                "No hay estudiantes nuevos por sincronizar.", 
                "Sincronización", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Crear Panel personalizado para el diálogo
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Se han encontrado " + pendientes + " estudiantes sin plan de pago."));
        panel.add(new JLabel("Monto base: $500.000"));
        
        JCheckBox chkDescuento = new JCheckBox("¿Aplicar descuento especial?");
        JTextField txtDescuento = new JTextField("0");
        txtDescuento.setEnabled(false);
        
        chkDescuento.addActionListener(e -> txtDescuento.setEnabled(chkDescuento.isSelected()));
        
        panel.add(chkDescuento);
        panel.add(new JLabel("Porcentaje de descuento (0-100):"));
        panel.add(txtDescuento);

        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Sincronización Masiva", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                double descuento = 0;
                if (chkDescuento.isSelected()) {
                    descuento = Double.parseDouble(txtDescuento.getText());
                    if (descuento < 0 || descuento > 100) {
                        throw new NumberFormatException();
                    }
                }

                int creados = pagosController.sincronizarEstudiantes(descuento);
                if (creados > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "Sincronización exitosa. Se crearon " + creados + " planes de pago.", 
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    refrescarDatos();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "No se pudo completar la sincronización.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Por favor ingrese un porcentaje válido entre 0 y 100.", 
                    "Error de Formato", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel crearResumenCards() {
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setOpaque(false);

        lblTotalRecaudado = new JLabel("$0");
        lblTotalPendiente = new JLabel("$0");
        lblTotalEstudiantes = new JLabel("0");

        cardsPanel.add(crearStatCard("Total Recaudado", lblTotalRecaudado, COLOR_SUCCESS));
        cardsPanel.add(crearStatCard("Saldo Pendiente", lblTotalPendiente, COLOR_WARNING));
        cardsPanel.add(crearStatCard("Estudiantes en Mora", lblTotalEstudiantes, COLOR_DANGER));

        return cardsPanel;
    }

    private JPanel crearStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(COLOR_TEXT);

        JPanel accentBar = new JPanel();
        accentBar.setPreferredSize(new Dimension(4, 0));
        accentBar.setBackground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(accentBar, BorderLayout.WEST);

        return card;
    }

    private JPanel crearCuerpoPrincipal() {
        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        // 1. Fila de Analíticas (Gráficas)
        JPanel analyticsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        analyticsPanel.setOpaque(false);
        analyticsPanel.setPreferredSize(new Dimension(0, 250));

        analyticsPanel.add(crearChartCard("Estado de Cartera", chartCartera = new DonutChart()));
        analyticsPanel.add(crearChartCard("Recaudación Mensual (Últimos 6 meses)", chartIngresos = new BarChart()));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        main.add(analyticsPanel, gbc);

        // 2. Fila de Filtros y Tabla
        JPanel tableContainer = new JPanel(new BorderLayout(0, 15));
        tableContainer.setOpaque(false);

        JPanel filterPanel = new JPanel(new BorderLayout(15, 0));
        filterPanel.setOpaque(false);

        // Buscador
        txtBuscar = new JTextField();
        txtBuscar.setPreferredSize(new Dimension(300, 40));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(0, 15, 0, 15)
        ));
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                paginaActual = 1;
                filtrarTabla();
            }
        });

        // Combo Programas
        comboProgramas = new JComboBox<>(new String[]{"Todos los programas"});
        cargarProgramasEnCombo();
        comboProgramas.setPreferredSize(new Dimension(250, 40));
        comboProgramas.setFont(FONT_BOLD);
        comboProgramas.addActionListener(e -> filtrarTabla());

        filterPanel.add(txtBuscar, BorderLayout.CENTER);
        filterPanel.add(comboProgramas, BorderLayout.EAST);

        tableContainer.add(filterPanel, BorderLayout.NORTH);
        tableContainer.add(crearCuerpoTabla(), BorderLayout.CENTER);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        main.add(tableContainer, gbc);

        return main;
    }

    private JPanel crearChartCard(String title, JComponent chart) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_BOLD);
        lblTitle.setForeground(COLOR_TEXT);
        lblTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);

        return card;
    }

    private JPanel crearCuerpoTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_CARD);
        panel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        panel.add(crearTablaPagos(), BorderLayout.CENTER);
        panel.add(crearPanelPaginacion(), BorderLayout.SOUTH);

        return panel;
    }

    private JComponent crearTablaPagos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_CARD);
        panel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        String[] cols = {"ESTUDIANTE", "MONTO TOTAL", "SALDO", "CUOTAS", "ESTADO", "ÚLTIMO PAGO", "SIG. PAGO", "DESC. %", "APLICAR", "ACCIONES", "ID", "SALDO_RAW"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { 
                return col == 8 || col == 9 || col == 10 || col == 11; 
            }
        };

        tablaPagos = new JTable(modeloTabla) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (col == 9 && row != -1) {
                    TableCellRenderer renderer = getCellRenderer(row, col);
                    Component c = renderer.getTableCellRendererComponent(this, getValueAt(row, col), false, false, row, col);
                    if (c instanceof Container) {
                        Container container = (Container) c;
                        container.setSize(getColumnModel().getColumn(col).getWidth(), getRowHeight(row));
                        container.doLayout();
                        
                        Rectangle cellRect = getCellRect(row, col, false);
                        Point pPoint = new Point(e.getX() - cellRect.x, e.getY() - cellRect.y);
                        Component comp = container.getComponentAt(pPoint);
                        if (comp instanceof JComponent && comp != container) {
                            return ((JComponent) comp).getToolTipText();
                        }
                    }
                }
                return super.getToolTipText(e);
            }
        };
        
        // Estilos modernos (FlatLaf inspired - Estudiantes Style)
        tablaPagos.setRowHeight(60);
        tablaPagos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaPagos.setBackground(COLOR_CARD);
        tablaPagos.setForeground(COLOR_TEXT);
        tablaPagos.setSelectionBackground(COLOR_SELECTION);
        tablaPagos.setSelectionForeground(COLOR_TEXT);
        tablaPagos.setShowVerticalLines(false);
        tablaPagos.setShowHorizontalLines(true);
        tablaPagos.setGridColor(COLOR_BORDER);
        tablaPagos.setIntercellSpacing(new Dimension(0, 1));
        tablaPagos.setFillsViewportHeight(true);
        
        // Header con estilo moderno (Estudiantes Style)
        tablaPagos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10));
        tablaPagos.getTableHeader().setBackground(COLOR_BG);
        tablaPagos.getTableHeader().setForeground(COLOR_TEXT_MUTED);
        tablaPagos.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tablaPagos.getTableHeader().setReorderingAllowed(false);
        tablaPagos.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_BORDER));
        
        // Habilitar ToolTips
        tablaPagos.setToolTipText("");

        // Ajustar anchos de columnas
        tablaPagos.getColumnModel().getColumn(0).setPreferredWidth(160); // Estudiante
        tablaPagos.getColumnModel().getColumn(1).setPreferredWidth(100); // Monto Total
        tablaPagos.getColumnModel().getColumn(2).setPreferredWidth(100); // Saldo
        tablaPagos.getColumnModel().getColumn(3).setPreferredWidth(70);  // Cuotas
        tablaPagos.getColumnModel().getColumn(4).setPreferredWidth(90);  // Estado
        tablaPagos.getColumnModel().getColumn(5).setPreferredWidth(110); // Último Pago
        tablaPagos.getColumnModel().getColumn(6).setPreferredWidth(110); // Sig. Pago
        tablaPagos.getColumnModel().getColumn(7).setPreferredWidth(75);  // Desc. %
        tablaPagos.getColumnModel().getColumn(8).setPreferredWidth(85);  // Aplicar
        tablaPagos.getColumnModel().getColumn(9).setMinWidth(160);       // Acciones (4 botones)
        tablaPagos.getColumnModel().getColumn(9).setPreferredWidth(160);
        
        // Ocultar columna ID
        tablaPagos.getColumnModel().getColumn(10).setMinWidth(0);
        tablaPagos.getColumnModel().getColumn(10).setMaxWidth(0);
        tablaPagos.getColumnModel().getColumn(10).setPreferredWidth(0);
        
        // Ocultar columna SALDO_RAW
        tablaPagos.getColumnModel().getColumn(11).setMinWidth(0);
        tablaPagos.getColumnModel().getColumn(11).setMaxWidth(0);
        tablaPagos.getColumnModel().getColumn(11).setPreferredWidth(0);

        // Renderers
        tablaPagos.getColumnModel().getColumn(0).setCellRenderer(new NombreEstudianteRenderer());
        tablaPagos.getColumnModel().getColumn(4).setCellRenderer(new EstadoPagoRenderer());
        tablaPagos.getColumnModel().getColumn(9).setCellRenderer(new AccionesCellRenderer());
        tablaPagos.getColumnModel().getColumn(9).setCellEditor(new AccionesCellEditor());
        
        // Renderer para descuento
        tablaPagos.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });
        
        // Renderer para el checkbox de aplicar
        tablaPagos.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                if (value instanceof JCheckBox) {
                    JCheckBox chk = (JCheckBox) value;
                    chk.setBackground(isSelected ? COLOR_SELECTION : COLOR_CARD);
                    return chk;
                }
                if (value instanceof String && value.equals("APLICADO")) {
                    JLabel lbl = new JLabel("✓ Bloqueado");
                    lbl.setOpaque(true);
                    lbl.setBackground(isSelected ? COLOR_SELECTION : COLOR_CARD);
                    lbl.setForeground(COLOR_SUCCESS);
                    lbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    return lbl;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            }
        });
        
        // Renderer base para celdas de datos para mantener consistencia con Estudiantes
        DefaultTableCellRenderer dataRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setBackground(isSelected ? COLOR_SELECTION : COLOR_CARD);
                c.setForeground(COLOR_TEXT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return c;
            }
        };
        
        // Aplicar a columnas de datos
        tablaPagos.getColumnModel().getColumn(1).setCellRenderer(dataRenderer);
        tablaPagos.getColumnModel().getColumn(2).setCellRenderer(dataRenderer);
        tablaPagos.getColumnModel().getColumn(3).setCellRenderer(dataRenderer);
        tablaPagos.getColumnModel().getColumn(5).setCellRenderer(dataRenderer);
        tablaPagos.getColumnModel().getColumn(6).setCellRenderer(dataRenderer);
        tablaPagos.getColumnModel().getColumn(7).setCellRenderer(dataRenderer);
        
        // Centrar columnas específicas
        ((DefaultTableCellRenderer)tablaPagos.getColumnModel().getColumn(3).getCellRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        ((DefaultTableCellRenderer)tablaPagos.getColumnModel().getColumn(5).getCellRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        ((DefaultTableCellRenderer)tablaPagos.getColumnModel().getColumn(6).getCellRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        
        // Listener para abrir perfil (Ahora solo en columna 0)
        tablaPagos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tablaPagos.rowAtPoint(e.getPoint());
                int col = tablaPagos.columnAtPoint(e.getPoint());
                if (row == -1) return;
                
                if (col == 8) { // Columna APLICAR
                    Object val = modeloTabla.getValueAt(row, 8);
                    if (val instanceof JCheckBox) {
                        gestionarAplicacionDescuento(row);
                    }
                } else if (col == 0) { // Abrir perfil al hacer clic en el nombre
                    abrirPerfilEstudiante(row);
                }
            }
        });

        // Cambiar cursor al pasar por columnas clickeables (Nombre, Aplicar y Acciones)
        tablaPagos.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tablaPagos.rowAtPoint(e.getPoint());
                int col = tablaPagos.columnAtPoint(e.getPoint());
                
                if (row >= 0 && (col == 0 || col == 8 || col == 9)) {
                    if (tablaPagos.getCursor().getType() != Cursor.HAND_CURSOR) {
                        tablaPagos.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                } else {
                    if (tablaPagos.getCursor().getType() != Cursor.DEFAULT_CURSOR) {
                        tablaPagos.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            }
        });
        
        // Asegurar que el cursor se restablezca cuando sale de la tabla
        tablaPagos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                tablaPagos.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });
        
        JScrollPane scroll = new JScrollPane(tablaPagos);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(COLOR_CARD);
        
        return scroll;
    }

    private JPanel crearPanelPaginacion() {
        JPanel paginacionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        paginacionPanel.setBackground(COLOR_CARD);
        paginacionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER));

        lblMostrando = new JLabel("Cargando...");
        lblMostrando.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMostrando.setForeground(COLOR_TEXT_MUTED);

        btnPrev = crearBotonPagina("‹");
        btnNext = crearBotonPagina("›");

        btnsPagina = new JButton[5];
        for (int i = 0; i < btnsPagina.length; i++) {
            btnsPagina[i] = crearBotonPagina(String.valueOf(i + 1));
        }

        btnPrev.addActionListener(e -> {
            if (paginaActual > 1) {
                paginaActual--;
                filtrarTabla();
            }
        });

        btnNext.addActionListener(e -> {
            int totalPaginas = (int) Math.ceil((double) totalEstudiantes / PagosController.REGISTROS_POR_PAGINA);
            if (paginaActual < totalPaginas) {
                paginaActual++;
                filtrarTabla();
            }
        });

        paginacionPanel.add(lblMostrando);
        paginacionPanel.add(Box.createHorizontalStrut(20));
        paginacionPanel.add(btnPrev);
        for (JButton btn : btnsPagina) {
            paginacionPanel.add(btn);
        }
        paginacionPanel.add(btnNext);

        return paginacionPanel;
    }

    private JButton crearBotonPagina(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(40, 40));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(COLOR_CARD);
        btn.setForeground(COLOR_TEXT);
        btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return btn;
    }

    private void refrescarDatos() {
        Map<String, Double> resumen = pagosController.obtenerResumenGeneral();
        lblTotalRecaudado.setText(currencyFormat.format(resumen.getOrDefault("total_recaudado", 0.0)));
        lblTotalPendiente.setText(currencyFormat.format(resumen.getOrDefault("total_pendiente", 0.0)));
        
        Map<String, Integer> estados = pagosController.obtenerConteoPorEstado();
        lblTotalEstudiantes.setText(String.valueOf(estados.getOrDefault("ATRASADO", 0)));

        // Actualizar gráficas
        if (chartCartera != null) chartCartera.setData(estados);
        if (chartIngresos != null) chartIngresos.setData(pagosController.obtenerIngresosMensuales());

        paginaActual = 1;
        filtrarTabla();
    }

    private void cargarProgramasEnCombo() {
        ProgramaController progCtrl = new ProgramaController();
        List<Programa> programas = progCtrl.obtenerTodosLosProgramas();
        for (Programa p : programas) {
            comboProgramas.addItem(p.getNombre());
        }
    }

    private void gestionarAplicacionDescuento(int row) {
        int idEstudiante = (int) modeloTabla.getValueAt(row, 10);
        String nombre = modeloTabla.getValueAt(row, 0).toString().replaceAll("<[^>]*>", " ").trim();
        
        String input = JOptionPane.showInputDialog(this, 
            "Ingrese el porcentaje de descuento para:\n" + nombre, 
            "Aplicar Descuento", JOptionPane.QUESTION_MESSAGE);
            
        if (input != null && !input.trim().isEmpty()) {
            try {
                double porcentaje = Double.parseDouble(input);
                if (porcentaje < 0 || porcentaje > 100) {
                    throw new NumberFormatException();
                }
                
                if (pagosController.aplicarDescuento(idEstudiante, porcentaje)) {
                    JOptionPane.showMessageDialog(this, "Descuento aplicado correctamente.");
                    refrescarDatos();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo aplicar el descuento.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Por favor ingrese un porcentaje válido (0-100).", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void filtrarTabla() {
        String query = txtBuscar.getText();
        String programa = comboProgramas.getSelectedItem().toString();
        
        totalEstudiantes = pagosController.contarEstudiantesConFiltro(query, programa);
        modeloTabla.setRowCount(0);

        List<Map<String, Object>> data = pagosController.obtenerEstadoPagosEstudiantesPaginado(
                paginaActual, PagosController.REGISTROS_POR_PAGINA, query, programa);

        for (Map<String, Object> row : data) {
            Object aplicadoObj = row.get("descuento_aplicado");
            boolean aplicado = aplicadoObj != null && (boolean) aplicadoObj;
            
            Object pctObj = row.get("descuento_porcentaje");
            double pct = pctObj != null ? (double) pctObj : 0.0;
            
            JCheckBox chk = null;
            if (!aplicado) {
                chk = new JCheckBox();
                chk.setHorizontalAlignment(SwingConstants.CENTER);
                chk.setBackground(COLOR_CARD);
                chk.setOpaque(false);
            }

            Object ultimoObj = row.get("fecha_ultimo_pago");
            String ultimo = "N/A";
            if (ultimoObj instanceof java.util.Date) {
                ultimo = tableDateFormat.format((java.util.Date) ultimoObj);
            } else if (ultimoObj != null) {
                ultimo = ultimoObj.toString();
            }

            double saldoPendiente = row.get("saldo_pendiente") != null ? (double) row.get("saldo_pendiente") : 0.0;
            String estadoStr = row.get("estado") != null ? row.get("estado").toString() : "SIN PLAN";

            Object proximoObj = row.get("fecha_proximo_pago");
            String proximo;

            if (saldoPendiente <= 0.001) {
                // Pago completado: no hay próximo cobro
                proximo = "No procede";
            } else if (proximoObj instanceof java.util.Date) {
                // Fecha ya calculada y guardada en la BD
                proximo = tableDateFormat.format((java.util.Date) proximoObj);
            } else {
                // No hay fecha_proximo_pago: calcularla en tiempo real desde fecha_matricula
                Object matriculaObj = row.get("fecha_matricula");
                if (matriculaObj instanceof java.sql.Date) {
                    java.time.LocalDate fechaMatricula = ((java.sql.Date) matriculaObj).toLocalDate();
                    java.time.LocalDate proximaFecha = PagosController.calcularFechaVencimiento(fechaMatricula, 30);
                    proximo = tableDateFormat.format(java.sql.Date.valueOf(proximaFecha));
                } else {
                    proximo = "Pendiente";
                }
            }

            modeloTabla.addRow(new Object[]{
                "<html><b>" + row.get("nombre") + "</b><br><small style='color:gray'>" + row.get("codigo") + "</small></html>",
                row.get("monto_total") != null ? currencyFormat.format(row.get("monto_total")) : "N/A",
                row.get("saldo_pendiente") != null ? currencyFormat.format(row.get("saldo_pendiente")) : "N/A",
                row.get("progreso") != null ? row.get("progreso") : "-",
                estadoStr,
                ultimo,
                proximo,
                aplicado ? (pct + "%") : "-",
                aplicado ? "APLICADO" : (row.get("id_estudiante") != null ? chk : "-"),
                "ACCIONES",
                row.get("id_estudiante"),
                row.get("saldo_pendiente") != null ? row.get("saldo_pendiente") : 0.0
            });
        }
        
        actualizarPaginacion();
    }

    private void actualizarPaginacion() {
        int totalPaginas = (int) Math.ceil((double) totalEstudiantes / PagosController.REGISTROS_POR_PAGINA);
        if (totalPaginas == 0) totalPaginas = 1;

        // Label resumen
        if (totalEstudiantes == 0) {
            lblMostrando.setText("Sin resultados");
        } else {
            int inicio = ((paginaActual - 1) * PagosController.REGISTROS_POR_PAGINA) + 1;
            int fin = Math.min(paginaActual * PagosController.REGISTROS_POR_PAGINA, totalEstudiantes);
            lblMostrando.setText(String.format("Mostrando %d–%d de %,d estudiantes", inicio, fin, totalEstudiantes));
        }

        // Calcular rango de páginas visibles
        int paginaInicio = Math.max(1, paginaActual - 2);
        int paginaFin = Math.min(totalPaginas, paginaInicio + btnsPagina.length - 1);
        paginaInicio = Math.max(1, paginaFin - btnsPagina.length + 1);

        for (int i = 0; i < btnsPagina.length; i++) {
            int numeroPagina = paginaInicio + i;
            if (numeroPagina <= totalPaginas) {
                btnsPagina[i].setText(String.valueOf(numeroPagina));
                btnsPagina[i].setVisible(true);
                if (numeroPagina == paginaActual) {
                    btnsPagina[i].setBackground(COLOR_PRIMARY);
                    btnsPagina[i].setForeground(Color.WHITE);
                    btnsPagina[i].setBorder(null);
                } else {
                    btnsPagina[i].setBackground(COLOR_CARD);
                    btnsPagina[i].setForeground(COLOR_TEXT);
                    btnsPagina[i].setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
                }
                for (java.awt.event.ActionListener al : btnsPagina[i].getActionListeners()) {
                    btnsPagina[i].removeActionListener(al);
                }
                final int pFinal = numeroPagina;
                btnsPagina[i].addActionListener(ev -> {
                    paginaActual = pFinal;
                    filtrarTabla();
                });
            } else {
                btnsPagina[i].setVisible(false);
            }
        }

        btnPrev.setEnabled(paginaActual > 1);
        btnNext.setEnabled(paginaActual < totalPaginas);
        btnPrev.setForeground(paginaActual > 1 ? COLOR_TEXT : COLOR_TEXT_MUTED);
        btnNext.setForeground(paginaActual < totalPaginas ? COLOR_TEXT : COLOR_TEXT_MUTED);
    }

    private void abrirGeneradorOrdenPago(int idEstudiante) {
        Map<String, Object> data = pagosController.obtenerDetalleFinancieroEstudiante(idEstudiante);
        if (data == null) {
            JOptionPane.showMessageDialog(this, "No se encontró información financiera para este estudiante.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        OrdenPagoDialog dlg = new OrdenPagoDialog(owner, data);
        dlg.setVisible(true);
    }

    private void abrirPerfilEstudiante(int row) {
        int idEstudiante = (int) modeloTabla.getValueAt(row, 10);
        
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof Dashboard) {
            ((Dashboard) w).cargarPanel(new PerfilPagoEstudiantePanel(usuarioActual, idEstudiante));
        }
    }

    // ================= RENDERERS =================

    class EstadoPagoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = new JLabel(value != null ? value.toString() : "N/A");
            label.setOpaque(true);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 10));
            
            String estado = value != null ? value.toString() : "";
            if (estado.equals("AL_DIA") || estado.equals("PAGADO")) {
                label.setBackground(new Color(220, 252, 231));
                label.setForeground(new Color(22, 163, 74));
            } else if (estado.equals("POR_VENCER")) {
                label.setBackground(new Color(254, 243, 199));
                label.setForeground(new Color(180, 83, 9));
            } else {
                label.setBackground(new Color(254, 226, 226));
                label.setForeground(new Color(220, 38, 38));
            }
            label.setBorder(new EmptyBorder(4, 10, 4, 10));
            return label;
        }
    }

    class NombreEstudianteRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            if (!isSelected) {
                label.setForeground(COLOR_PRIMARY);
            }
            label.setBackground(isSelected ? COLOR_SELECTION : COLOR_CARD);
            return label;
        }
    }

    class AccionesCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JButton btnRegistrar;
        private JButton btnRecibo;
        private JButton btnPreferencial;
        private JButton btnAnular;
        
        public AccionesCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));
            setOpaque(true);
            
            btnRegistrar = new JButton(crearIconoPago());
            btnRegistrar.setPreferredSize(new Dimension(28, 28));
            btnRegistrar.setBackground(new Color(59, 130, 246));
            btnRegistrar.setToolTipText("Registrar Pago");
            btnRegistrar.setBorderPainted(false);
            
            btnRecibo = new JButton(crearIconoImpresora());
            btnRecibo.setPreferredSize(new Dimension(28, 28));
            btnRecibo.setBackground(new Color(16, 185, 129)); // Verde
            btnRecibo.setToolTipText("Generar Orden de Pago / Recibo");
            btnRecibo.setBorderPainted(false);
            
            btnPreferencial = new JButton(crearIconoPreferencial());
            btnPreferencial.setPreferredSize(new Dimension(28, 28));
            btnPreferencial.setBackground(new Color(139, 92, 246)); // Púrpura
            btnPreferencial.setToolTipText("Pago Preferencial (Especial)");
            btnPreferencial.setBorderPainted(false);
            
            btnAnular = new JButton(crearIconoAnular());
            btnAnular.setPreferredSize(new Dimension(28, 28));
            btnAnular.setBackground(new Color(239, 68, 68));
            btnAnular.setToolTipText("Anular / Cancelar Pago");
            btnAnular.setBorderPainted(false);
            
            add(btnRegistrar);
            add(btnRecibo);
            add(btnPreferencial);
            add(btnAnular);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setBackground(isSelected ? table.getSelectionBackground() : COLOR_CARD);
            
            Object saldoObj = table.getModel().getValueAt(table.convertRowIndexToModel(row), 11);
            boolean isPagado = false;
            if (saldoObj instanceof Number) {
                isPagado = ((Number) saldoObj).doubleValue() <= 0.001;
            }
            
            btnRegistrar.setEnabled(!isPagado);
            btnRegistrar.setBackground(isPagado ? Color.GRAY : new Color(59, 130, 246));
            
            btnRecibo.setEnabled(!isPagado);
            btnRecibo.setBackground(isPagado ? Color.GRAY : new Color(16, 185, 129));
            
            btnPreferencial.setEnabled(!isPagado);
            btnPreferencial.setBackground(isPagado ? Color.GRAY : new Color(139, 92, 246));
            
            return this;
        }
    }

    class AccionesCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private JPanel panel;
        private JButton btnRegistrar;
        private JButton btnRecibo;
        private JButton btnPreferencial;
        private JButton btnAnular;
        private int filaActual;
        private JTable table;
        
        public AccionesCellEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 10));
            panel.setOpaque(true);
            panel.setBackground(COLOR_CARD);
            
            btnRegistrar = new JButton(crearIconoPago());
            btnRegistrar.setPreferredSize(new Dimension(28, 28));
            btnRegistrar.setBackground(new Color(59, 130, 246));
            btnRegistrar.setToolTipText("Registrar Pago");
            btnRegistrar.addActionListener(e -> {
                int idEstudiante = (int) modeloTabla.getValueAt(filaActual, 10);
                Window owner = SwingUtilities.getWindowAncestor(table);
                RegistroPagoDialog dlg = new RegistroPagoDialog(owner, usuarioActual, idEstudiante);
                dlg.setVisible(true);
                if (dlg.isSuccess()) {
                    refrescarDatos();
                }
                fireEditingStopped();
            });
            
            btnRecibo = new JButton(crearIconoImpresora());
            btnRecibo.setPreferredSize(new Dimension(28, 28));
            btnRecibo.setBackground(new Color(16, 185, 129));
            btnRecibo.setToolTipText("Generar Orden de Pago / Recibo");
            btnRecibo.addActionListener(e -> {
                int idEstudiante = (int) modeloTabla.getValueAt(filaActual, 10);
                abrirGeneradorOrdenPago(idEstudiante);
                fireEditingStopped();
            });
            
            btnPreferencial = new JButton(crearIconoPreferencial());
            btnPreferencial.setPreferredSize(new Dimension(28, 28));
            btnPreferencial.setBackground(new Color(139, 92, 246));
            btnPreferencial.setToolTipText("Pago Preferencial (Especial)");
            btnPreferencial.addActionListener(e -> {
                int idEstudiante = (int) modeloTabla.getValueAt(filaActual, 10);
                Window owner = SwingUtilities.getWindowAncestor(table);
                RegistroPagoDialog dlg = new RegistroPagoDialog(owner, usuarioActual, idEstudiante, true);
                dlg.setVisible(true);
                if (dlg.isSuccess()) {
                    refrescarDatos();
                }
                fireEditingStopped();
            });
            
            btnAnular = new JButton(crearIconoAnular());
            btnAnular.setPreferredSize(new Dimension(28, 28));
            btnAnular.setBackground(new Color(239, 68, 68));
            btnAnular.setToolTipText("Anular / Cancelar Pago");
            btnAnular.addActionListener(e -> {
                JOptionPane.showMessageDialog(null, "Funcionalidad de anulación próximamente.");
                fireEditingStopped();
            });
            
            panel.add(btnRegistrar);
            panel.add(btnRecibo);
            panel.add(btnPreferencial);
            panel.add(btnAnular);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            this.table = table;
            filaActual = row;
            panel.setBackground(table.getSelectionBackground());
            
            Object saldoObj = table.getModel().getValueAt(table.convertRowIndexToModel(row), 11);
            boolean isPagado = false;
            if (saldoObj instanceof Number) {
                isPagado = ((Number) saldoObj).doubleValue() <= 0.001;
            }
            
            btnRegistrar.setEnabled(!isPagado);
            btnRegistrar.setBackground(isPagado ? Color.GRAY : new Color(59, 130, 246));
            
            btnRecibo.setEnabled(!isPagado);
            btnRecibo.setBackground(isPagado ? Color.GRAY : new Color(16, 185, 129));
            
            btnPreferencial.setEnabled(!isPagado);
            btnPreferencial.setBackground(isPagado ? Color.GRAY : new Color(139, 92, 246));
            
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() { return null; }
    }

    private Icon crearIconoPreferencial() {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.translate(x, y);
                int[] xp = {14, 18, 25, 19, 21, 14, 7, 9, 3, 10};
                int[] yp = {4, 12, 12, 17, 24, 19, 24, 17, 12, 12};
                g2.fillPolygon(xp, yp, 10);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 28; }
            @Override public int getIconHeight() { return 28; }
        };
    }

    private Icon crearIconoPago() {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x + 4, y + 6, 20, 14, 3, 3);
                g2.drawLine(x + 4, y + 10, x + 24, y + 10);
                g2.fillOval(x + 18, y + 15, 3, 3);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 28; }
            @Override public int getIconHeight() { return 28; }
        };
    }

    private Icon crearIconoImpresora() {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(x + 6, y + 10, 16, 10);
                g2.drawRect(x + 9, y + 6, 10, 4);
                g2.drawLine(x + 9, y + 14, x + 19, y + 14);
                g2.drawLine(x + 9, y + 17, x + 19, y + 17);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 28; }
            @Override public int getIconHeight() { return 28; }
        };
    }

    private Icon crearIconoAnular() {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x + 6, y + 6, 16, 16);
                g2.drawLine(x + 8, y + 8, x + 20, y + 20);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 28; }
            @Override public int getIconHeight() { return 28; }
        };
    }

    // Componente de Gráfica de Dona
    class DonutChart extends JComponent {
        private Map<String, Integer> data;

        public void setData(Map<String, Integer> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int size = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            if (data == null || data.isEmpty()) {
                g2.setColor(COLOR_BORDER);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x, y, size, size);
                g2.dispose();
                return;
            }

            double total = data.values().stream().mapToInt(Integer::intValue).sum();
            if (total == 0) total = 1;

            int startAngle = 90;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int angle = (int) Math.round((entry.getValue() / total) * 360.0);
                g2.setColor(getColorForEstado(entry.getKey()));
                g2.fillArc(x, y, size, size, startAngle, angle);
                startAngle += angle;
            }

            // El agujero de la dona
            g2.setColor(COLOR_CARD);
            int holeSize = (int) (size * 0.6);
            g2.fillOval(x + (size - holeSize) / 2, y + (size - holeSize) / 2, holeSize, holeSize);

            // Leyenda simplificada
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            int ly = 20;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                g2.setColor(getColorForEstado(entry.getKey()));
                g2.fillRect(5, ly - 8, 8, 8);
                g2.setColor(COLOR_TEXT_MUTED);
                g2.drawString(entry.getKey() + ": " + entry.getValue(), 18, ly);
                ly += 15;
            }

            g2.dispose();
        }

        private Color getColorForEstado(String estado) {
            switch (estado) {
                case "AL_DIA": return COLOR_SUCCESS;
                case "POR_VENCER": return COLOR_WARNING;
                case "ATRASADO": return COLOR_DANGER;
                default: return COLOR_TEXT_MUTED;
            }
        }
    }

    // Componente de Gráfica de Barras
    class BarChart extends JComponent {
        private Map<String, Double> data;
        private int hoveredBarIndex = -1;
        private java.util.List<Rectangle> barBounds;

        public BarChart() {
            barBounds = new java.util.ArrayList<>();
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int oldIndex = hoveredBarIndex;
                    hoveredBarIndex = -1;
                    for (int i = 0; i < barBounds.size(); i++) {
                        if (barBounds.get(i).contains(e.getPoint())) {
                            hoveredBarIndex = i;
                            break;
                        }
                    }
                    if (oldIndex != hoveredBarIndex) {
                        repaint();
                        setCursor(new Cursor(hoveredBarIndex != -1 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoveredBarIndex != -1) {
                        hoveredBarIndex = -1;
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }
            });
        }

        public void setData(Map<String, Double> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (data == null || data.isEmpty()) {
                g2.setColor(COLOR_TEXT_MUTED);
                g2.drawString("No hay datos de recaudación", 20, getHeight() / 2);
                g2.dispose();
                return;
            }

            double max = data.values().stream().max(Double::compare).orElse(1.0);
            if (max == 0) max = 1;

            // Redondear el max a un valor limpio para el eje Y
            double yMax = Math.ceil(max / 1000.0) * 1000.0;
            if (yMax == 0) yMax = 1000.0;

            int leftMargin = 55;
            int rightMargin = 20;
            int topMargin = 30;
            int bottomMargin = 40;

            int width = getWidth() - leftMargin - rightMargin;
            int height = getHeight() - topMargin - bottomMargin;

            // Dibujar líneas de cuadrícula horizontales y etiquetas del Eje Y
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();
            int numLines = 4;
            for (int i = 0; i <= numLines; i++) {
                int y = topMargin + height - (i * height / numLines);
                double val = (yMax / numLines) * i;

                // Línea de cuadrícula punteada
                g2.setColor(COLOR_BORDER);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f}, 0f));
                g2.drawLine(leftMargin, y, getWidth() - rightMargin, y);

                // Etiqueta Eje Y
                g2.setColor(COLOR_TEXT_MUTED);
                String label = currencyFormat.format(val / 1000) + "k";
                g2.drawString(label, leftMargin - fm.stringWidth(label) - 8, y + 4);
            }

            int barSpacing = 20;
            int barWidth = Math.min(45, (width / Math.max(1, data.size())) - barSpacing);
            int totalBarsWidth = data.size() * (barWidth + barSpacing) - barSpacing;
            int startX = leftMargin + (width - totalBarsWidth) / 2;

            barBounds.clear();
            int i = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double val = entry.getValue();
                int barHeight = (int) ((val / yMax) * height);
                if (val > 0 && barHeight < 4) barHeight = 4; // Altura mínima visual
                
                int bx = startX + i * (barWidth + barSpacing);
                int by = topMargin + height - barHeight;

                Rectangle bounds = new Rectangle(bx, by, barWidth, barHeight);
                barBounds.add(bounds);

                boolean isHovered = (i == hoveredBarIndex);

                // Sombra sutil al hacer hover
                if (isHovered) {
                    g2.setColor(new Color(0, 0, 0, 15));
                    g2.fillRoundRect(bx - 3, by - 3, barWidth + 6, barHeight + 6, 10, 10);
                }

                // Barra con Gradiente
                Color baseColor = isHovered ? COLOR_PRIMARY.brighter() : COLOR_PRIMARY;
                Color topColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 180);
                GradientPaint gp = new GradientPaint(bx, by, topColor, bx, by + barHeight, baseColor);
                g2.setPaint(gp);
                
                // Redondeamos arriba pero dejamos plano abajo
                g2.fillRoundRect(bx, by, barWidth, barHeight + 10, 8, 8); 
                g2.fillRect(bx, by + barHeight - 5, barWidth, 5);

                // Etiqueta del Eje X (Mes)
                g2.setColor(isHovered ? COLOR_TEXT : COLOR_TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", isHovered ? Font.BOLD : Font.PLAIN, 11));
                String label = entry.getKey();
                g2.drawString(label, bx + (barWidth - fm.stringWidth(label)) / 2, topMargin + height + 20);

                // Valor sobre la barra
                if (isHovered || val > 0) {
                    String valStr = currencyFormat.format(val / 1000) + "k";
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    g2.setColor(isHovered ? COLOR_PRIMARY : COLOR_TEXT_MUTED);
                    g2.drawString(valStr, bx + (barWidth - g2.getFontMetrics().stringWidth(valStr)) / 2, by - 8);
                }

                i++;
            }

            // Línea base sólida del Eje X
            g2.setColor(COLOR_BORDER);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(leftMargin, topMargin + height, getWidth() - rightMargin, topMargin + height);

            g2.dispose();
        }
    }
}
