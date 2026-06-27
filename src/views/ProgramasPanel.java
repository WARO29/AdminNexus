package views;

import controller.ProgramaController;
import model.Programa;
import model.Programa.EstadoPrograma;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

public class ProgramasPanel extends JPanel {
    
    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG      = new Color(249, 250, 251);
    private Color COLOR_CARD    = Color.WHITE;
    private Color COLOR_TEXT    = new Color(17, 24, 39);
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    private Color COLOR_BORDER  = new Color(229, 231, 235);
    private Color COLOR_SELECTION = new Color(239, 246, 255);
    
    private Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 12);
    private Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    private Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    private Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    
    private java.util.prefs.Preferences prefs;
    private model.Usuario usuarioActual;
    
    private ProgramaController programaController;
    private DefaultTableModel modeloTabla;
    private JTable tablaProgramas;
    private JLabel lblTotalProgramas;
    private JLabel lblMostrando;

    // ============================
    // Estado de paginación
    // ============================
    private int    paginaActual    = 1;
    private int    totalProgramas  = 0;
    private String filtroEstado    = "Cualquier estado";
    private String filtroDuracion  = "Cualquier duración";
    private String filtroBusqueda  = "";

    // Botones de paginación
    private JButton   btnPrev;
    private JButton   btnNext;
    private JButton[] btnsPagina;

    public ProgramasPanel(model.Usuario usuario) {
        this.usuarioActual = usuario;
        this.prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuario.getIdusuario());
        this.programaController = new ProgramaController();
        this.programaController.setNombreUsuario(usuario.getNombreAdmin());
        cargarConfiguracion();

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        add(crearHeader(), BorderLayout.NORTH);
        add(crearContenidoPrincipal(), BorderLayout.CENTER);

        cargarPagina();
    }
    
    private void cargarConfiguracion() {
        String mode = prefs.get("app_mode", "light");
        String theme = prefs.get("app_color", "blue");
        
        // Color primario
        switch(theme) {
            case "green":    COLOR_PRIMARY = new Color(16, 185, 129); break;
            case "gray":     COLOR_PRIMARY = new Color(75, 85, 99);   break;
            case "burgundy": COLOR_PRIMARY = new Color(153, 27, 27);  break;
            default:         COLOR_PRIMARY = new Color(26, 86, 219);  break;
        }
        
        if (mode.equals("dark")) {
            COLOR_BG = new Color(17, 24, 39);
            COLOR_CARD = new Color(31, 41, 55);
            COLOR_TEXT = Color.WHITE;
            COLOR_TEXT_MUTED = new Color(156, 163, 175);
            COLOR_BORDER = new Color(55, 65, 81);
            COLOR_SELECTION = new Color(59, 130, 246, 40);
        } else {
            COLOR_BG = new Color(249, 250, 251);
            COLOR_CARD = Color.WHITE;
            COLOR_TEXT = new Color(17, 24, 39);
            COLOR_TEXT_MUTED = new Color(107, 114, 128);
            COLOR_BORDER = new Color(229, 231, 235);
            COLOR_SELECTION = new Color(239, 246, 255);
        }
    }
    
    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_CARD);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        // Panel izquierdo con título y descripción
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel titulo = new JLabel("Programas Académicos");
        titulo.setFont(FONT_TITLE);
        titulo.setForeground(COLOR_TEXT);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subtitulo = new JLabel("Administre la oferta educativa, duraciones y costos de inscripción.");
        subtitulo.setFont(FONT_SUBTITLE);
        subtitulo.setForeground(COLOR_TEXT_MUTED);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftPanel.add(titulo);
        leftPanel.add(Box.createVerticalStrut(3));
        leftPanel.add(subtitulo);
        
        // Botón Nuevo Programa
        JButton btnNuevoPrograma = new JButton("+ Nuevo Programa");
        btnNuevoPrograma.setFont(FONT_BOLD);
        btnNuevoPrograma.setForeground(Color.WHITE);
        btnNuevoPrograma.setBackground(COLOR_PRIMARY);
        btnNuevoPrograma.setFocusPainted(false);
        btnNuevoPrograma.setBorderPainted(false);
        btnNuevoPrograma.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNuevoPrograma.setPreferredSize(new Dimension(160, 38));
        btnNuevoPrograma.addActionListener(e -> mostrarDialogoNuevoPrograma());
        
        header.add(leftPanel, BorderLayout.WEST);
        header.add(btnNuevoPrograma, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel crearContenidoPrincipal() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(0, 30, 30, 30));
        
        // Panel de filtros y tarjeta de total
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));
        topPanel.setBackground(COLOR_BG);
        
        // Contenedor principal de filtros
        JPanel filtrosContainer = new JPanel(new BorderLayout(0, 15));
        filtrosContainer.setBackground(COLOR_BG);
        
        // Panel de combos
        JPanel combosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        combosPanel.setBackground(COLOR_BG);
        
        // Filtro de Estado
        JLabel lblEstado = new JLabel("ESTADO");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblEstado.setForeground(COLOR_TEXT_MUTED);
        
        JComboBox<String> comboEstado = new JComboBox<>(new String[]{"Cualquier estado", "Activo", "Cerrado", "En Pausa"});
        comboEstado.setPreferredSize(new Dimension(160, 35));
        
        // Filtro de Duración
        JLabel lblDuracion = new JLabel("DURACIÓN");
        lblDuracion.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblDuracion.setForeground(COLOR_TEXT_MUTED);
        
        JComboBox<String> comboDuracion = new JComboBox<>(new String[]{"Cualquier duración", "1-4 Semestres", "5-8 Semestres", "9-12 Semestres", "Más de 12 Semestres"});
        comboDuracion.setPreferredSize(new Dimension(170, 35));
        
        JButton btnLimpiar = new JButton("Limpiar Filtros");
        btnLimpiar.setForeground(COLOR_PRIMARY);
        btnLimpiar.setBackground(COLOR_CARD);
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setBorderPainted(true);
        btnLimpiar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Panel de Búsqueda
        JPanel busquedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        busquedaPanel.setBackground(COLOR_BG);
        
        JLabel lblBuscar = new JLabel("BUSCAR");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBuscar.setForeground(COLOR_TEXT_MUTED);
        
        // Buscador de Programas
        JTextField txtBuscar = new JTextField("Escribe el nombre o código...", 25);
        txtBuscar.setPreferredSize(new Dimension(250, 35));
        txtBuscar.setForeground(COLOR_TEXT_MUTED);
        txtBuscar.setBackground(COLOR_CARD);
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            new EmptyBorder(0, 10, 0, 10)
        ));
        
        txtBuscar.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtBuscar.getText().equals("Escribe el nombre o código...")) {
                    txtBuscar.setText("");
                    txtBuscar.setForeground(COLOR_TEXT);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (txtBuscar.getText().isEmpty()) {
                    txtBuscar.setForeground(COLOR_TEXT_MUTED);
                    txtBuscar.setText("Escribe el nombre o código...");
                }
            }
        });
        
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { buscar(); }
            public void removeUpdate(DocumentEvent e) { buscar(); }
            public void changedUpdate(DocumentEvent e) { buscar(); }
            
            private void buscar() {
                String texto = txtBuscar.getText();
                if (texto.equals("Escribe el nombre o código...")) return;
                
                filtroBusqueda = texto;
                paginaActual = 1;
                cargarPagina();
            }
        });
        
        // Acción de filtros — resetea a página 1
        comboEstado.addActionListener(e -> {
            filtroEstado   = (String) comboEstado.getSelectedItem();
            paginaActual   = 1;
            cargarPagina();
        });

        comboDuracion.addActionListener(e -> {
            filtroDuracion = (String) comboDuracion.getSelectedItem();
            paginaActual   = 1;
            cargarPagina();
        });

        // Limpiar filtros
        btnLimpiar.addActionListener(e -> {
            comboEstado.setSelectedIndex(0);
            comboDuracion.setSelectedIndex(0);
            txtBuscar.setText("Escribe el nombre o código...");
            txtBuscar.setForeground(COLOR_TEXT_MUTED);
            filtroEstado   = "Cualquier estado";
            filtroDuracion = "Cualquier duración";
            filtroBusqueda = "";
            paginaActual   = 1;
            cargarPagina();
        });
        
        combosPanel.add(lblEstado);
        combosPanel.add(Box.createHorizontalStrut(5));
        combosPanel.add(comboEstado);
        combosPanel.add(Box.createHorizontalStrut(15));
        combosPanel.add(lblDuracion);
        combosPanel.add(Box.createHorizontalStrut(5));
        combosPanel.add(comboDuracion);
        combosPanel.add(Box.createHorizontalStrut(15));
        combosPanel.add(btnLimpiar);
        
        busquedaPanel.add(lblBuscar);
        busquedaPanel.add(Box.createHorizontalStrut(5));
        busquedaPanel.add(txtBuscar);
        
        filtrosContainer.add(combosPanel, BorderLayout.NORTH);
        filtrosContainer.add(busquedaPanel, BorderLayout.SOUTH);
        
        // Tarjeta de total programas
        JPanel tarjetaTotal = crearTarjetaTotal();
        
        topPanel.add(filtrosContainer, BorderLayout.WEST);
        topPanel.add(tarjetaTotal, BorderLayout.EAST);
        
        // Tabla de programas
        JPanel tablaPanel = crearTabla();
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(tablaPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * @deprecated Reemplazado por cargarPagina() con filtros directamente en BD.
     */
    @Deprecated
    private void aplicarFiltros(String estado, String duracion) {
        filtroEstado   = estado;
        filtroDuracion = duracion;
        paginaActual   = 1;
        cargarPagina();
    }

    /**
     * @deprecated La verificación ahora se hace en SQL.
     */
    @Deprecated
    private boolean cumpleFiltroRangoDuracion(int duracionSemestres, String rangoSeleccionado) {
        return true;
    }
    
    private JPanel crearTarjetaTotal() {
        JPanel tarjeta = new JPanel(new BorderLayout(12, 0));
        tarjeta.setBackground(prefs.get("app_mode", "light").equals("dark") ? new Color(30, 58, 138, 40) : new Color(239, 246, 255));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1),
            new EmptyBorder(10, 16, 10, 16)
        ));
        tarjeta.setPreferredSize(new Dimension(200, 70));
        
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 2, 0);
        
        JLabel lblTitulo = new JLabel("TOTAL PROGRAMAS");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitulo.setForeground(COLOR_TEXT_MUTED);
        
        lblTotalProgramas = new JLabel("0");
        lblTotalProgramas.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTotalProgramas.setForeground(COLOR_TEXT);
        
        leftPanel.add(lblTitulo, gbc);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        leftPanel.add(lblTotalProgramas, gbc);
        
        // Icono de libro dibujado
        JLabel iconoLabel = new JLabel(crearIconoLibro());
        iconoLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        tarjeta.add(leftPanel, BorderLayout.WEST);
        tarjeta.add(iconoLabel, BorderLayout.EAST);
        
        return tarjeta;
    }
    
    /**
     * Crea un icono de libro para la tarjeta de total
     */
    private Icon crearIconoLibro() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PRIMARY); // Color primario dinámico
                g2.setStroke(new BasicStroke(2f));
                
                // Libro cerrado
                g2.drawRect(x + 4, y + 6, 20, 16);
                
                // Línea del lomo
                g2.drawLine(x + 8, y + 6, x + 8, y + 22);
                
                // Páginas (líneas horizontales)
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(x + 10, y + 10, x + 22, y + 10);
                g2.drawLine(x + 10, y + 14, x + 22, y + 14);
                g2.drawLine(x + 10, y + 18, x + 22, y + 18);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 28; }
            
            @Override
            public int getIconHeight() { return 28; }
        };
    }
    
    private JPanel crearTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        
        // Columnas de la tabla
        String[] columnas = {"ID", "NOMBRE DEL PROGRAMA", "DURACIÓN", "INSCRITOS", "ESTADO", "ACCIONES"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Solo la columna de acciones es editable
            }
        };
        
        tablaProgramas = new JTable(modeloTabla);
        
        // Estilos FlatLaf para la tabla
        tablaProgramas.setFont(FONT_MAIN);
        tablaProgramas.setRowHeight(50);
        tablaProgramas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProgramas.setShowVerticalLines(false);
        tablaProgramas.setShowHorizontalLines(true);
        tablaProgramas.setGridColor(COLOR_BORDER);
        tablaProgramas.setIntercellSpacing(new Dimension(0, 1));
        tablaProgramas.setSelectionBackground(COLOR_SELECTION);
        tablaProgramas.setSelectionForeground(COLOR_TEXT);
        tablaProgramas.setFillsViewportHeight(true);
        
        // Estilo del header
        tablaProgramas.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10));
        tablaProgramas.getTableHeader().setForeground(COLOR_TEXT_MUTED);
        tablaProgramas.getTableHeader().setBackground(COLOR_BG);
        tablaProgramas.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tablaProgramas.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_BORDER));
        tablaProgramas.getTableHeader().setReorderingAllowed(false);
        
        // Renderer personalizado para celdas normales con efecto hover
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                          boolean isSelected, boolean hasFocus,
                                                          int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (isSelected) {
                    c.setBackground(COLOR_SELECTION);
                    c.setForeground(COLOR_TEXT);
                } else {
                    c.setBackground(COLOR_CARD);
                    c.setForeground(COLOR_TEXT);
                }
                
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                
                return c;
            }
        };
        
        // Aplicar renderer a todas las columnas excepto estado y acciones
        for (int i = 0; i < tablaProgramas.getColumnCount(); i++) {
            if (i != 4 && i != 5) {
                tablaProgramas.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
            }
        }
        
        // Configurar anchos de columnas
        tablaProgramas.getColumnModel().getColumn(0).setPreferredWidth(80);
        tablaProgramas.getColumnModel().getColumn(1).setPreferredWidth(250);
        tablaProgramas.getColumnModel().getColumn(2).setPreferredWidth(120);
        tablaProgramas.getColumnModel().getColumn(3).setPreferredWidth(100);
        tablaProgramas.getColumnModel().getColumn(4).setPreferredWidth(120);
        tablaProgramas.getColumnModel().getColumn(5).setPreferredWidth(130);
        
        // Renderer personalizado para la columna de estado (índice 4)
        tablaProgramas.getColumnModel().getColumn(4).setCellRenderer(new EstadoCellRenderer());
        
        // Renderer personalizado para la columna de acciones (índice 5)
        tablaProgramas.getColumnModel().getColumn(5).setCellRenderer(new AccionesCellRenderer());
        tablaProgramas.getColumnModel().getColumn(5).setCellEditor(new AccionesCellEditor());
        
        // Cambiar cursor a mano cuando el mouse está sobre la columna de acciones
        tablaProgramas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = tablaProgramas.columnAtPoint(e.getPoint());
                if (col == 5) {
                    tablaProgramas.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    tablaProgramas.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        // ScrollPane
        JScrollPane scrollPane = new JScrollPane(tablaProgramas);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(COLOR_BG);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de paginación REAL
        JPanel paginacionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        paginacionPanel.setBackground(COLOR_CARD);
        paginacionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER));

        lblMostrando = new JLabel("Cargando...");
        lblMostrando.setFont(FONT_MAIN);
        lblMostrando.setForeground(COLOR_TEXT_MUTED);

        btnPrev = crearBotonPagina("‹");
        btnNext = crearBotonPagina("›");

        // Crear 5 botones de número de página
        btnsPagina = new JButton[5];
        for (int i = 0; i < btnsPagina.length; i++) {
            btnsPagina[i] = crearBotonPagina(String.valueOf(i + 1));
        }

        // Listeners
        btnPrev.addActionListener(e -> {
            if (paginaActual > 1) {
                paginaActual--;
                cargarPagina();
            }
        });
        btnNext.addActionListener(e -> {
            int totalPaginas = calcularTotalPaginas();
            if (paginaActual < totalPaginas) {
                paginaActual++;
                cargarPagina();
            }
        });
        for (int i = 0; i < btnsPagina.length; i++) {
            final int pagina = i + 1;
            btnsPagina[i].addActionListener(e -> {
                paginaActual = pagina;
                cargarPagina();
            });
        }

        paginacionPanel.add(lblMostrando);
        paginacionPanel.add(Box.createHorizontalStrut(20));
        paginacionPanel.add(btnPrev);
        for (JButton btn : btnsPagina) {
            paginacionPanel.add(btn);
        }
        paginacionPanel.add(btnNext);

        panel.add(paginacionPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea un botón de paginación con estilo consistente.
     */
    private JButton crearBotonPagina(String texto) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(new Dimension(40, 40));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(COLOR_CARD);
        btn.setForeground(COLOR_TEXT);
        btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        btn.setFont(FONT_BOLD);
        return btn;
    }

    /**
     * Calcula el número total de páginas según el total de programas.
     */
    private int calcularTotalPaginas() {
        if (totalProgramas == 0) return 1;
        return (int) Math.ceil((double) totalProgramas / ProgramaController.REGISTROS_POR_PAGINA);
    }

    /**
     * Carga la página actual de programas desde la BD (LIMIT/OFFSET real).
     * También actualiza los botones de paginación y el label de estado.
     */
    private void cargarPagina() {
        // Contar total con los filtros actuales
        totalProgramas = programaController.contarProgramasConFiltro(filtroEstado, filtroDuracion, filtroBusqueda);

        // Limpiar tabla
        modeloTabla.setRowCount(0);

        // Cargar sólo los registros de esta página
        List<Programa> programas = programaController.obtenerProgramasFiltradosPaginados(
                paginaActual, ProgramaController.REGISTROS_POR_PAGINA, filtroEstado, filtroDuracion, filtroBusqueda);

        for (Programa programa : programas) {
            modeloTabla.addRow(new Object[]{
                programa.getCodigo(),
                programa.getNombre(),
                programa.getDuracionSemestres() + " Semestres",
                String.format("%,d", programa.getInscritos()),
                programa.getEstado(),
                "• • •"
            });
        }

        // Actualizar estado de paginación
        actualizarPaginacion();
    }

    /**
     * Actualiza los botones y label de paginación según el estado actual.
     */
    private void actualizarPaginacion() {
        int totalPaginas = calcularTotalPaginas();

        // Label resumen
        lblTotalProgramas.setText(String.format("%,d", totalProgramas));
        if (totalProgramas == 0) {
            lblMostrando.setText("Sin resultados");
        } else {
            int inicio = ((paginaActual - 1) * ProgramaController.REGISTROS_POR_PAGINA) + 1;
            int fin    = Math.min(paginaActual * ProgramaController.REGISTROS_POR_PAGINA, totalProgramas);
            lblMostrando.setText(String.format("Mostrando %d–%d de %,d programas", inicio, fin, totalProgramas));
        }

        // Calcular rango de páginas visibles (ventana deslizante de 5)
        int paginaInicio = Math.max(1, paginaActual - 2);
        int paginaFin    = Math.min(totalPaginas, paginaInicio + btnsPagina.length - 1);
        // Ajustar inicio si no alcanzamos a llenar los 5 botones
        paginaInicio = Math.max(1, paginaFin - btnsPagina.length + 1);

        for (int i = 0; i < btnsPagina.length; i++) {
            int numeroPagina = paginaInicio + i;
            if (numeroPagina <= totalPaginas) {
                btnsPagina[i].setText(String.valueOf(numeroPagina));
                btnsPagina[i].setVisible(true);
                // Resaltar página activa
                if (numeroPagina == paginaActual) {
                    btnsPagina[i].setBackground(COLOR_PRIMARY);
                    btnsPagina[i].setForeground(Color.WHITE);
                    btnsPagina[i].setBorder(null);
                } else {
                    btnsPagina[i].setBackground(COLOR_CARD);
                    btnsPagina[i].setForeground(COLOR_TEXT);
                    btnsPagina[i].setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
                }
                // Actualizar listener para apuntar al número correcto
                for (java.awt.event.ActionListener al : btnsPagina[i].getActionListeners()) {
                    btnsPagina[i].removeActionListener(al);
                }
                final int pFinal = numeroPagina;
                btnsPagina[i].addActionListener(ev -> {
                    paginaActual = pFinal;
                    cargarPagina();
                });
            } else {
                btnsPagina[i].setVisible(false);
            }
        }

        // Habilitar/deshabilitar anterior y siguiente
        btnPrev.setEnabled(paginaActual > 1);
        btnNext.setEnabled(paginaActual < totalPaginas);
        btnPrev.setForeground(paginaActual > 1 ? COLOR_TEXT : COLOR_TEXT_MUTED);
        btnNext.setForeground(paginaActual < totalPaginas ? COLOR_TEXT : COLOR_TEXT_MUTED);
    }
    
    private void mostrarDialogoNuevoPrograma() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Dashboard) {
            ((Dashboard) window).cargarPanel(new NuevoProgramaPanel(usuarioActual));
        } else {
            // Fallback para pruebas fuera del Dashboard
            JDialog dialogo = new JDialog((Frame)null, "Nuevo Programa", true);
            dialogo.setSize(1000, 700);
            dialogo.setLocationRelativeTo(this);
            dialogo.add(new NuevoProgramaPanel(usuarioActual));
            dialogo.setVisible(true);
        }
    }
    
    // Renderer para la columna de estado
    class EstadoCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                                                      boolean isSelected, boolean hasFocus, 
                                                      int row, int column) {
            JLabel label = new JLabel();
            label.setOpaque(true);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            
            if (value instanceof EstadoPrograma) {
                EstadoPrograma estado = (EstadoPrograma) value;
                label.setText(estado.getNombre());
                
                switch (estado) {
                    case ACTIVO:
                        label.setBackground(new Color(220, 252, 231));
                        label.setForeground(new Color(22, 163, 74));
                        break;
                    case CERRADO:
                        label.setBackground(new Color(254, 226, 226));
                        label.setForeground(new Color(220, 38, 38));
                        break;
                    case EN_PAUSA:
                        label.setBackground(new Color(254, 243, 199));
                        label.setForeground(new Color(217, 119, 6));
                        break;
                }
            }
            
            label.setBorder(new EmptyBorder(4, 8, 4, 8));
            return label;
        }
    }
    
    // Renderer para la columna de acciones
    class AccionesCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JButton btnVer;
        private JButton btnEditar;
        private JButton btnEliminar;
        
        public AccionesCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            setOpaque(true);
            setBackground(COLOR_CARD);
            
            // Botón Ver con icono de ojo
            btnVer = new JButton(crearIconoOjo());
            btnVer.setPreferredSize(new Dimension(32, 32));
            btnVer.setBackground(new Color(59, 130, 246));
            btnVer.setForeground(Color.WHITE);
            btnVer.setFocusPainted(false);
            btnVer.setBorderPainted(false);
            btnVer.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnVer.setToolTipText("Ver detalles");
            
            // Botón Editar con icono de lápiz
            btnEditar = new JButton(crearIconoEditar());
            btnEditar.setPreferredSize(new Dimension(32, 32));
            btnEditar.setBackground(new Color(16, 185, 129));
            btnEditar.setForeground(Color.WHITE);
            btnEditar.setFocusPainted(false);
            btnEditar.setBorderPainted(false);
            btnEditar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnEditar.setToolTipText("Editar programa");
            
            // Botón Eliminar con icono de papelera
            btnEliminar = new JButton(crearIconoEliminar());
            btnEliminar.setPreferredSize(new Dimension(32, 32));
            btnEliminar.setBackground(new Color(239, 68, 68));
            btnEliminar.setForeground(Color.WHITE);
            btnEliminar.setFocusPainted(false);
            btnEliminar.setBorderPainted(false);
            btnEliminar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnEliminar.setToolTipText("Eliminar programa");
            
            add(btnVer);
            add(btnEditar);
            add(btnEliminar);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                      boolean isSelected, boolean hasFocus,
                                                      int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(COLOR_CARD);
            }
            return this;
        }
    }
    
    // Editor para la columna de acciones
    class AccionesCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private JPanel panel;
        private JButton btnVer;
        private JButton btnEditar;
        private JButton btnEliminar;
        private int filaActual;
        
        public AccionesCellEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            panel.setBackground(COLOR_CARD);
            
            // Botón Ver
            btnVer = new JButton(crearIconoOjo());
            btnVer.setPreferredSize(new Dimension(32, 32));
            btnVer.setBackground(new Color(59, 130, 246));
            btnVer.setForeground(Color.WHITE);
            btnVer.setFocusPainted(false);
            btnVer.setBorderPainted(false);
            btnVer.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnVer.setToolTipText("Ver detalles");
            btnVer.addActionListener(e -> {
                verPrograma(filaActual);
                fireEditingStopped();
            });
            
            // Botón Editar
            btnEditar = new JButton(crearIconoEditar());
            btnEditar.setPreferredSize(new Dimension(32, 32));
            btnEditar.setBackground(new Color(16, 185, 129));
            btnEditar.setForeground(Color.WHITE);
            btnEditar.setFocusPainted(false);
            btnEditar.setBorderPainted(false);
            btnEditar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnEditar.setToolTipText("Editar programa");
            btnEditar.addActionListener(e -> {
                editarPrograma(filaActual);
                fireEditingStopped();
            });
            
            // Botón Eliminar
            btnEliminar = new JButton(crearIconoEliminar());
            btnEliminar.setPreferredSize(new Dimension(32, 32));
            btnEliminar.setBackground(new Color(239, 68, 68));
            btnEliminar.setForeground(Color.WHITE);
            btnEliminar.setFocusPainted(false);
            btnEliminar.setBorderPainted(false);
            btnEliminar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnEliminar.setToolTipText("Eliminar programa");
            btnEliminar.addActionListener(e -> {
                eliminarPrograma(filaActual);
                fireEditingStopped();
            });
            
            panel.add(btnVer);
            panel.add(btnEditar);
            panel.add(btnEliminar);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                    boolean isSelected, int row, int column) {
            filaActual = row;
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }
    
    /**
     * Crea un icono de ojo para el botón Ver
     */
    private Icon crearIconoOjo() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                
                // Ojo (elipse)
                g2.drawArc(x + 2, y + 6, 16, 8, 0, 180);
                g2.drawArc(x + 2, y + 6, 16, 8, 180, 180);
                
                // Pupila
                g2.fillOval(x + 7, y + 8, 6, 6);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 20; }
            
            @Override
            public int getIconHeight() { return 20; }
        };
    }
    
    /**
     * Crea un icono de lápiz para el botón Editar
     */
    private Icon crearIconoEditar() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                
                // Lápiz
                int[] xPoints = {x + 14, x + 6, x + 4, x + 4, x + 6, x + 16};
                int[] yPoints = {y + 4, y + 12, y + 12, y + 14, y + 16, y + 6};
                g2.drawPolyline(xPoints, yPoints, 6);
                
                // Punta del lápiz
                g2.drawLine(x + 14, y + 4, x + 16, y + 6);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 20; }
            
            @Override
            public int getIconHeight() { return 20; }
        };
    }
    
    /**
     * Crea un icono de papelera para el botón Eliminar
     */
    private Icon crearIconoEliminar() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                
                // Tapa de la papelera
                g2.drawLine(x + 3, y + 5, x + 17, y + 5);
                g2.drawLine(x + 7, y + 3, x + 13, y + 3);
                
                // Cuerpo de la papelera
                g2.drawLine(x + 5, y + 5, x + 6, y + 16);
                g2.drawLine(x + 15, y + 5, x + 14, y + 16);
                g2.drawLine(x + 6, y + 16, x + 14, y + 16);
                
                // Líneas internas
                g2.drawLine(x + 8, y + 7, x + 8, y + 14);
                g2.drawLine(x + 10, y + 7, x + 10, y + 14);
                g2.drawLine(x + 12, y + 7, x + 12, y + 14);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 20; }
            
            @Override
            public int getIconHeight() { return 20; }
        };
    }
    
    /**
     * Ver detalles de un programa
     */
    private void verPrograma(int fila) {
        if (fila < 0 || fila >= modeloTabla.getRowCount()) return;

        String codigo = (String) modeloTabla.getValueAt(fila, 0);
        List<Programa> pagina = programaController.obtenerProgramasFiltradosPaginados(
                paginaActual, ProgramaController.REGISTROS_POR_PAGINA, filtroEstado, filtroDuracion, filtroBusqueda);

        Programa programa = pagina.stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst().orElse(null);
        if (programa == null) return;

        String mensaje = String.format(
            "Código: %s\n" +
            "Nombre: %s\n" +
            "Duración: %d semestres\n" +
            "Inscritos: %d\n" +
            "Estado: %s",
            programa.getCodigo(),
            programa.getNombre(),
            programa.getDuracionSemestres(),
            programa.getInscritos(),
            programa.getEstado().getNombre()
        );

        JOptionPane.showMessageDialog(this, mensaje, "Detalles del Programa", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Editar un programa existente — carga el formulario con los datos pre-llenados
     */
    private void editarPrograma(int fila) {
        if (fila < 0 || fila >= modeloTabla.getRowCount()) return;

        String codigo = (String) modeloTabla.getValueAt(fila, 0);
        List<Programa> pagina = programaController.obtenerProgramasFiltradosPaginados(
                paginaActual, ProgramaController.REGISTROS_POR_PAGINA, filtroEstado, filtroDuracion, filtroBusqueda);

        Programa programa = pagina.stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst().orElse(null);
        if (programa == null) return;

        // Cargar el formulario de edicion dentro del Dashboard
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Dashboard) {
            ((Dashboard) window).cargarPanel(new NuevoProgramaPanel(usuarioActual, programa));
        }
    }

    /**
     * Eliminar un programa
     */
    private void eliminarPrograma(int fila) {
        if (fila < 0 || fila >= modeloTabla.getRowCount()) return;

        String codigo = (String) modeloTabla.getValueAt(fila, 0);
        List<Programa> pagina = programaController.obtenerProgramasFiltradosPaginados(
                paginaActual, ProgramaController.REGISTROS_POR_PAGINA, filtroEstado, filtroDuracion, filtroBusqueda);

        Programa programa = pagina.stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst().orElse(null);
        if (programa == null) return;

        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro que desea eliminar el programa '" + programa.getNombre() + "'?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            if (programaController.eliminarPrograma(programa.getIdPrograma())) {
                JOptionPane.showMessageDialog(this,
                    "Programa eliminado exitosamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
                // Retroceder página si era el único registro en esta página
                if (modeloTabla.getRowCount() == 1 && paginaActual > 1) {
                    paginaActual--;
                }
                cargarPagina();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Error al eliminar el programa",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Muestra el diálogo para editar un programa existente
     */
    private void mostrarDialogoEditarPrograma(Programa programa) {
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialogo = new JDialog(window, "Editar Programa", Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setSize(500, 550);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Campos del formulario pre-llenados
        JTextField txtCodigo = new JTextField(programa.getCodigo(), 20);
        JTextField txtNombre = new JTextField(programa.getNombre(), 20);
        JSpinner spinnerDuracion = new JSpinner(new SpinnerNumberModel(programa.getDuracionSemestres(), 1, 20, 1));
        JSpinner spinnerInscritos = new JSpinner(new SpinnerNumberModel(programa.getInscritos(), 0, 10000, 1));
        
        String[] estados = {"Activo", "Cerrado", "En Pausa"};
        JComboBox<String> comboEstado = new JComboBox<>(estados);
        comboEstado.setSelectedItem(programa.getEstado().getNombre());
        
        JTextField txtColor = new JTextField(programa.getIconoColor(), 20);
        
        // Agregar componentes
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Código:"), gbc);
        gbc.gridx = 1;
        panel.add(txtCodigo, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Nombre del Programa:"), gbc);
        gbc.gridx = 1;
        panel.add(txtNombre, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Duración (Semestres):"), gbc);
        gbc.gridx = 1;
        panel.add(spinnerDuracion, gbc);
        

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Inscritos:"), gbc);
        gbc.gridx = 1;
        panel.add(spinnerInscritos, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        panel.add(comboEstado, gbc);
        
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Color Icono (Hex):"), gbc);
        gbc.gridx = 1;
        panel.add(txtColor, gbc);
        
        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Guardar Cambios");
        JButton btnCancelar = new JButton("Cancelar");
        
        btnGuardar.setBackground(COLOR_PRIMARY);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnCancelar.setBackground(new Color(107, 114, 128));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnGuardar.addActionListener(e -> {
            try {
                programa.setCodigo(txtCodigo.getText().trim());
                programa.setNombre(txtNombre.getText().trim());
                programa.setDuracionSemestres((Integer) spinnerDuracion.getValue());
                programa.setInscritos((Integer) spinnerInscritos.getValue());
                
                String estadoSeleccionado = (String) comboEstado.getSelectedItem();
                if (estadoSeleccionado.equals("Activo")) {
                    programa.setEstado(EstadoPrograma.ACTIVO);
                } else if (estadoSeleccionado.equals("Cerrado")) {
                    programa.setEstado(EstadoPrograma.CERRADO);
                } else {
                    programa.setEstado(EstadoPrograma.EN_PAUSA);
                }
                
                programa.setIconoColor(txtColor.getText().trim());
                
                if (programaController.actualizarPrograma(programa)) {
                    JOptionPane.showMessageDialog(dialogo,
                        "Programa actualizado exitosamente",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                    cargarPagina();
                    dialogo.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialogo, 
                        "Error al actualizar programa", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialogo, 
                    "Por favor ingrese valores numéricos válidos", 
                    "Error de formato", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        btnCancelar.addActionListener(e -> dialogo.dispose());
        
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        
        dialogo.add(panel, BorderLayout.CENTER);
        dialogo.add(panelBotones, BorderLayout.SOUTH);
        dialogo.setVisible(true);
    }
}
