package views;

import controller.EstudianteController;
import controller.ProgramaController;
import model.Estudiante;
import model.Estudiante.EstadoMatricula;
import model.Programa;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class EstudiantesPanel extends JPanel {

    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG      = new Color(249, 250, 251);
    private Color COLOR_CARD    = Color.WHITE;
    private Color COLOR_TEXT    = new Color(17, 24, 39);
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    private Color COLOR_BORDER  = new Color(229, 231, 235);
    private Color COLOR_SELECTION = new Color(239, 246, 255);
    
    private Font  FONT_MAIN     = new Font("Segoe UI", Font.PLAIN, 12);
    private Font  FONT_BOLD     = new Font("Segoe UI", Font.BOLD, 12);
    private Font  FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 24);
    private Font  FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    
    private java.util.prefs.Preferences prefs;
    private model.Usuario usuarioActual;

    // Controlador
    private EstudianteController estudianteController;

    // Tabla
    private DefaultTableModel modeloTabla;
    private JTable            tablaEstudiantes;

    // Labels de estado
    private JLabel lblTotalEstudiantes;
    private JLabel lblMostrando;

    // ============================
    // Estado de paginación
    // ============================
    private int    paginaActual       = 1;
    private int    totalEstudiantes   = 0;
    private String filtroPrograma     = "Todos los Programas";
    private String filtroEstado       = "Cualquier Estado";
    private String filtroBusqueda     = "";

    // Botones de paginación (referencias para actualizarlos)
    private JButton btnPrev;
    private JButton btnNext;
    private JButton[] btnsPagina;   // básicamente btn1..btn3 + btn128 en el original

    public EstudiantesPanel(model.Usuario usuario) {
        this.usuarioActual = usuario;
        this.prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuario.getIdusuario());
        this.estudianteController = new EstudianteController();
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
        
        JLabel titulo = new JLabel("Estudiantes");
        titulo.setFont(FONT_TITLE);
        titulo.setForeground(COLOR_TEXT);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subtitulo = new JLabel("Administre el registro y la informacion de cada estudiante en el sistema.");
        subtitulo.setFont(FONT_SUBTITLE);
        subtitulo.setForeground(COLOR_TEXT_MUTED);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftPanel.add(titulo);
        leftPanel.add(Box.createVerticalStrut(3));
        leftPanel.add(subtitulo);
        
        // Botón Nuevo Estudiante
        JButton btnNuevoEstudiante = new JButton("+ Nuevo Estudiante");
        btnNuevoEstudiante.setFont(FONT_BOLD);
        btnNuevoEstudiante.setForeground(Color.WHITE);
        btnNuevoEstudiante.setBackground(COLOR_PRIMARY);
        btnNuevoEstudiante.setFocusPainted(false);
        btnNuevoEstudiante.setBorderPainted(false);
        btnNuevoEstudiante.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNuevoEstudiante.setPreferredSize(new Dimension(160, 38));
        btnNuevoEstudiante.addActionListener(e -> mostrarFormularioNuevoEstudiante());
        
        header.add(leftPanel, BorderLayout.WEST);
        header.add(btnNuevoEstudiante, BorderLayout.EAST);
        
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
        
        // Filtro de Programa Académico
        JLabel lblPrograma = new JLabel("PROGRAMA ACADÉMICO");
        lblPrograma.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblPrograma.setForeground(COLOR_TEXT_MUTED);
        
        JComboBox<String> comboPrograma = new JComboBox<>();
        comboPrograma.addItem("Todos los Programas");
        cargarProgramasEnCombo(comboPrograma);
        comboPrograma.setPreferredSize(new Dimension(200, 35));
        
        // Filtro de Estado
        JLabel lblEstado = new JLabel("ESTADO");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblEstado.setForeground(COLOR_TEXT_MUTED);
        
        JComboBox<String> comboEstado = new JComboBox<>(new String[]{"Cualquier Estado", "Activo", "Inactivo", "Graduado", "Retirado"});
        comboEstado.setPreferredSize(new Dimension(160, 35));
        
        // Botón Limpiar Filtros
        JButton btnFiltros = new JButton("Limpiar Filtros");
        btnFiltros.setForeground(COLOR_PRIMARY);
        btnFiltros.setBackground(COLOR_CARD);
        btnFiltros.setFocusPainted(false);
        btnFiltros.setBorderPainted(true);
        btnFiltros.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Panel de Búsqueda
        JPanel busquedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        busquedaPanel.setBackground(COLOR_BG);
        
        JLabel lblBuscar = new JLabel("BUSCAR");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBuscar.setForeground(COLOR_TEXT_MUTED);
        
        // Buscador
        JTextField txtBuscar = new JTextField("Escribe nombre, código, email o teléfono...", 25);
        txtBuscar.setPreferredSize(new Dimension(300, 35));
        txtBuscar.setForeground(COLOR_TEXT_MUTED);
        txtBuscar.setBackground(COLOR_CARD);
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            new EmptyBorder(0, 10, 0, 10)
        ));
        
        txtBuscar.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtBuscar.getText().equals("Escribe nombre, código, email o teléfono...")) {
                    txtBuscar.setText("");
                    txtBuscar.setForeground(COLOR_TEXT);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (txtBuscar.getText().isEmpty()) {
                    txtBuscar.setForeground(COLOR_TEXT_MUTED);
                    txtBuscar.setText("Escribe nombre, código, email o teléfono...");
                }
            }
        });
        
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { buscar(); }
            public void removeUpdate(DocumentEvent e) { buscar(); }
            public void changedUpdate(DocumentEvent e) { buscar(); }
            
            private void buscar() {
                String texto = txtBuscar.getText();
                if (texto.equals("Escribe nombre, código, email o teléfono...")) return;
                
                filtroBusqueda = texto;
                paginaActual = 1;
                cargarPagina();
            }
        });
        
        // Acción de filtros — resetea a página 1 al cambiar filtros
        comboPrograma.addActionListener(e -> {
            filtroPrograma = (String) comboPrograma.getSelectedItem();
            paginaActual   = 1;
            cargarPagina();
        });

        comboEstado.addActionListener(e -> {
            filtroEstado = (String) comboEstado.getSelectedItem();
            paginaActual = 1;
            cargarPagina();
        });
        
        // Acción para limpiar filtros
        btnFiltros.addActionListener(e -> {
            comboPrograma.setSelectedIndex(0);
            comboEstado.setSelectedIndex(0);
            txtBuscar.setText("Escribe nombre, código, email o teléfono...");
            txtBuscar.setForeground(new Color(156, 163, 175));
            filtroPrograma = "Todos los Programas";
            filtroEstado = "Cualquier Estado";
            filtroBusqueda = "";
            paginaActual = 1;
            cargarPagina();
        });
        
        combosPanel.add(lblPrograma);
        combosPanel.add(Box.createHorizontalStrut(5));
        combosPanel.add(comboPrograma);
        combosPanel.add(Box.createHorizontalStrut(15));
        combosPanel.add(lblEstado);
        combosPanel.add(Box.createHorizontalStrut(5));
        combosPanel.add(comboEstado);
        combosPanel.add(Box.createHorizontalStrut(15));
        combosPanel.add(btnFiltros);
        
        busquedaPanel.add(lblBuscar);
        busquedaPanel.add(Box.createHorizontalStrut(5));
        busquedaPanel.add(txtBuscar);
        
        filtrosContainer.add(combosPanel, BorderLayout.NORTH);
        filtrosContainer.add(busquedaPanel, BorderLayout.SOUTH);
        
        // Tarjeta de total estudiantes
        JPanel tarjetaTotal = crearTarjetaTotal();
        
        topPanel.add(filtrosContainer, BorderLayout.WEST);
        topPanel.add(tarjetaTotal, BorderLayout.EAST);
        
        // Tabla de estudiantes
        JPanel tablaPanel = crearTabla();
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(tablaPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void cargarProgramasEnCombo(JComboBox<String> combo) {
        ProgramaController programaController = new ProgramaController();
        List<Programa> programas = programaController.obtenerTodosLosProgramas();
        for (Programa programa : programas) {
            combo.addItem(programa.getNombre());
        }
    }
    
    /**
     * @deprecated Reemplazado por cargarPagina() con soporte de filtros via BD.
     *             Se mantiene para evitar errores de compilación si hay referencias externas.
     */
    @Deprecated
    private void aplicarFiltros(String programa, String estado) {
        filtroPrograma = programa;
        filtroEstado   = estado;
        paginaActual   = 1;
        cargarPagina();
    }
    
    private JPanel crearTarjetaTotal() {
        JPanel tarjeta = new JPanel(new BorderLayout(12, 0));
        tarjeta.setBackground(prefs.get("app_mode", "light").equals("dark") ? new Color(30, 58, 138, 40) : new Color(239, 246, 255));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1),
            new EmptyBorder(10, 16, 10, 16)
        ));
        tarjeta.setPreferredSize(new Dimension(200, 90));
        
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 2, 0);
        
        JLabel lblTitulo = new JLabel("TOTAL ESTUDIANTES");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitulo.setForeground(COLOR_TEXT_MUTED);
        
        lblTotalEstudiantes = new JLabel("0");
        lblTotalEstudiantes.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTotalEstudiantes.setForeground(COLOR_TEXT);
        
        leftPanel.add(lblTitulo, gbc);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        leftPanel.add(lblTotalEstudiantes, gbc);
        
        // Icono de estudiantes
        JLabel iconoLabel = new JLabel(crearIconoEstudiantes());
        iconoLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        tarjeta.add(leftPanel, BorderLayout.WEST);
        tarjeta.add(iconoLabel, BorderLayout.EAST);
        
        return tarjeta;
    }
    
    private Icon crearIconoEstudiantes() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(59, 130, 246));
                g2.setStroke(new BasicStroke(2f));
                
                // Dos personas
                g2.drawOval(x + 6, y + 4, 8, 8);
                g2.drawArc(x + 3, y + 14, 14, 10, 0, 180);
                
                g2.drawOval(x + 18, y + 6, 6, 6);
                g2.drawArc(x + 16, y + 14, 10, 8, 0, 180);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 32; }
            
            @Override
            public int getIconHeight() { return 28; }
        };
    }
    
    private JPanel crearTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        
        // Columnas de la tabla
        String[] columnas = {"ID", "NOMBRE DEL ESTUDIANTE", "PROGRAMA ACADÉMICO", "FECHA DE MATRÍCULA", "ESTADO", "ACCIONES"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Solo la columna de acciones es editable
            }
        };
        
        tablaEstudiantes = new JTable(modeloTabla);
        tablaEstudiantes.setBackground(COLOR_CARD);
        tablaEstudiantes.setForeground(COLOR_TEXT);
        
        // Estilos FlatLaf para la tabla
        tablaEstudiantes.setFont(FONT_MAIN);
        tablaEstudiantes.setRowHeight(60);
        tablaEstudiantes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaEstudiantes.setShowVerticalLines(false);
        tablaEstudiantes.setShowHorizontalLines(true);
        tablaEstudiantes.setGridColor(COLOR_BORDER);
        tablaEstudiantes.setIntercellSpacing(new Dimension(0, 1));
        tablaEstudiantes.setSelectionBackground(COLOR_SELECTION);
        tablaEstudiantes.setSelectionForeground(COLOR_TEXT);
        tablaEstudiantes.setFillsViewportHeight(true);
        
        // Estilo del header
        tablaEstudiantes.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10));
        tablaEstudiantes.getTableHeader().setForeground(COLOR_TEXT_MUTED);
        tablaEstudiantes.getTableHeader().setBackground(COLOR_BG);
        tablaEstudiantes.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tablaEstudiantes.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_BORDER));
        tablaEstudiantes.getTableHeader().setReorderingAllowed(false);
        
        // Renderer personalizado para celdas normales
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
        for (int i = 0; i < tablaEstudiantes.getColumnCount(); i++) {
            if (i != 4 && i != 5) {
                tablaEstudiantes.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
            }
        }
        
        // Configurar anchos de columnas
        tablaEstudiantes.getColumnModel().getColumn(0).setPreferredWidth(100);
        tablaEstudiantes.getColumnModel().getColumn(1).setPreferredWidth(200);
        tablaEstudiantes.getColumnModel().getColumn(2).setPreferredWidth(180);
        tablaEstudiantes.getColumnModel().getColumn(3).setPreferredWidth(140);
        tablaEstudiantes.getColumnModel().getColumn(4).setPreferredWidth(100);
        tablaEstudiantes.getColumnModel().getColumn(5).setPreferredWidth(130);
        
        // Renderer personalizado para la columna de estado
        tablaEstudiantes.getColumnModel().getColumn(4).setCellRenderer(new EstadoCellRenderer());
        
        // Renderer personalizado para la columna de acciones
        tablaEstudiantes.getColumnModel().getColumn(5).setCellRenderer(new AccionesCellRenderer());
        tablaEstudiantes.getColumnModel().getColumn(5).setCellEditor(new AccionesCellEditor());
        
        // Cambiar cursor a mano cuando el mouse está sobre la columna de acciones
        tablaEstudiantes.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = tablaEstudiantes.columnAtPoint(e.getPoint());
                if (col == 5) {
                    tablaEstudiantes.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    tablaEstudiantes.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        // ScrollPane
        JScrollPane scrollPane = new JScrollPane(tablaEstudiantes);
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
     * Calcula el número total de páginas según el total de estudiantes.
     */
    private int calcularTotalPaginas() {
        if (totalEstudiantes == 0) return 1;
        return (int) Math.ceil((double) totalEstudiantes / EstudianteController.REGISTROS_POR_PAGINA);
    }

    /**
     * Carga la página actual de estudiantes desde la BD (LIMIT/OFFSET real).
     * También actualiza los botones de paginación y el label de estado.
     */
    private void cargarPagina() {
        // Contar total con los filtros actuales
        totalEstudiantes = estudianteController.contarEstudiantesConFiltro(filtroPrograma, filtroEstado, filtroBusqueda);

        // Limpiar tabla
        modeloTabla.setRowCount(0);

        // Cargar sólo los registros de esta página
        List<Estudiante> estudiantes = estudianteController.obtenerEstudiantesFiltradosPaginados(
                paginaActual, EstudianteController.REGISTROS_POR_PAGINA, filtroPrograma, filtroEstado, filtroBusqueda);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Estudiante estudiante : estudiantes) {
            modeloTabla.addRow(new Object[]{
                estudiante.getCodigo(),
                estudiante.getNombreCompleto(),
                estudiante.getNombrePrograma() != null ? estudiante.getNombrePrograma() : "Sin programa",
                estudiante.getFechaMatricula().format(formatter),
                estudiante.getEstado(),
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
        lblTotalEstudiantes.setText(String.format("%,d", totalEstudiantes));
        if (totalEstudiantes == 0) {
            lblMostrando.setText("Sin resultados");
        } else {
            int inicio = ((paginaActual - 1) * EstudianteController.REGISTROS_POR_PAGINA) + 1;
            int fin    = Math.min(paginaActual * EstudianteController.REGISTROS_POR_PAGINA, totalEstudiantes);
            lblMostrando.setText(String.format("Mostrando %d–%d de %,d estudiantes", inicio, fin, totalEstudiantes));
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
    
    private void mostrarFormularioNuevoEstudiante() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Dashboard) {
            ((Dashboard) window).cargarPanel(new NuevoEstudiantePanel(usuarioActual));
        } else {
            // Fallback para pruebas
            JDialog dialogo = new JDialog((Frame)null, "Nuevo Estudiante", true);
            dialogo.setSize(1000, 700);
            dialogo.setLocationRelativeTo(this);
            dialogo.add(new NuevoEstudiantePanel(usuarioActual));
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
            
            if (value instanceof EstadoMatricula) {
                EstadoMatricula estado = (EstadoMatricula) value;
                label.setText(estado.getNombre());
                
                switch (estado) {
                    case ACTIVO:
                        label.setBackground(new Color(220, 252, 231));
                        label.setForeground(new Color(22, 163, 74));
                        break;
                    case INACTIVO:
                        label.setBackground(new Color(229, 231, 235));
                        label.setForeground(new Color(107, 114, 128));
                        break;
                    case GRADUADO:
                        label.setBackground(new Color(219, 234, 254));
                        label.setForeground(new Color(37, 99, 235));
                        break;
                    case RETIRADO:
                        label.setBackground(new Color(254, 226, 226));
                        label.setForeground(new Color(220, 38, 38));
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
            
            // Botón Ver
            btnVer = new JButton(crearIconoOjo());
            btnVer.setPreferredSize(new Dimension(32, 32));
            btnVer.setBackground(new Color(59, 130, 246));
            btnVer.setForeground(Color.WHITE);
            btnVer.setFocusPainted(false);
            btnVer.setBorderPainted(false);
            btnVer.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnVer.setToolTipText("Ver detalles");
            
            // Botón Editar
            btnEditar = new JButton(crearIconoEditar());
            btnEditar.setPreferredSize(new Dimension(32, 32));
            btnEditar.setBackground(new Color(16, 185, 129));
            btnEditar.setForeground(Color.WHITE);
            btnEditar.setFocusPainted(false);
            btnEditar.setBorderPainted(false);
            btnEditar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnEditar.setToolTipText("Editar estudiante");
            
            // Botón Eliminar
            btnEliminar = new JButton(crearIconoEliminar());
            btnEliminar.setPreferredSize(new Dimension(32, 32));
            btnEliminar.setBackground(new Color(239, 68, 68));
            btnEliminar.setForeground(Color.WHITE);
            btnEliminar.setFocusPainted(false);
            btnEliminar.setBorderPainted(false);
            btnEliminar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnEliminar.setToolTipText("Eliminar estudiante");
            
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
                verEstudiante(filaActual);
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
            btnEditar.setToolTipText("Editar estudiante");
            btnEditar.addActionListener(e -> {
                editarEstudiante(filaActual);
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
            btnEliminar.setToolTipText("Eliminar estudiante");
            btnEliminar.addActionListener(e -> {
                eliminarEstudiante(filaActual);
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
    
    private Icon crearIconoOjo() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                
                g2.drawArc(x + 2, y + 6, 16, 8, 0, 180);
                g2.drawArc(x + 2, y + 6, 16, 8, 180, 180);
                g2.fillOval(x + 7, y + 8, 6, 6);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 20; }
            
            @Override
            public int getIconHeight() { return 20; }
        };
    }
    
    private Icon crearIconoEditar() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                
                int[] xPoints = {x + 14, x + 6, x + 4, x + 4, x + 6, x + 16};
                int[] yPoints = {y + 4, y + 12, y + 12, y + 14, y + 16, y + 6};
                g2.drawPolyline(xPoints, yPoints, 6);
                g2.drawLine(x + 14, y + 4, x + 16, y + 6);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() { return 20; }
            
            @Override
            public int getIconHeight() { return 20; }
        };
    }
    
    private Icon crearIconoEliminar() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                
                g2.drawLine(x + 3, y + 5, x + 17, y + 5);
                g2.drawLine(x + 7, y + 3, x + 13, y + 3);
                g2.drawLine(x + 5, y + 5, x + 6, y + 16);
                g2.drawLine(x + 15, y + 5, x + 14, y + 16);
                g2.drawLine(x + 6, y + 16, x + 14, y + 16);
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
    
    private void verEstudiante(int fila) {
        // Leer datos directamente de la página actual (ya cargada en tabla)
        if (fila < 0 || fila >= modeloTabla.getRowCount()) return;

        // Obtener el estudiante completo por su código (columna 0)
        String codigo = (String) modeloTabla.getValueAt(fila, 0);
        List<Estudiante> pagina = estudianteController.obtenerEstudiantesFiltradosPaginados(
                paginaActual, EstudianteController.REGISTROS_POR_PAGINA, filtroPrograma, filtroEstado, filtroBusqueda);

        Estudiante estudiante = pagina.stream()
                .filter(e -> e.getCodigo().equals(codigo))
                .findFirst().orElse(null);
        if (estudiante == null) return;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String mensaje = String.format(
            "Código: %s\n" +
            "Nombre: %s\n" +
            "Email: %s\n" +
            "Teléfono: %s\n" +
            "Dirección: %s\n" +
            "Fecha de Nacimiento: %s\n" +
            "Programa: %s\n" +
            "Fecha de Matrícula: %s\n" +
            "Estado: %s",
            estudiante.getCodigo(),
            estudiante.getNombreCompleto(),
            estudiante.getEmail(),
            estudiante.getTelefono(),
            estudiante.getDireccion(),
            estudiante.getFechaNacimiento() != null ? estudiante.getFechaNacimiento().format(formatter) : "N/A",
            estudiante.getNombrePrograma(),
            estudiante.getFechaMatricula().format(formatter),
            estudiante.getEstado().getNombre()
        );
        JOptionPane.showMessageDialog(this, mensaje, "Detalles del Estudiante", JOptionPane.INFORMATION_MESSAGE);
    }

    private void editarEstudiante(int fila) {
        if (fila < 0 || fila >= modeloTabla.getRowCount()) return;

        String codigo = (String) modeloTabla.getValueAt(fila, 0);
        List<Estudiante> pagina = estudianteController.obtenerEstudiantesFiltradosPaginados(
                paginaActual, EstudianteController.REGISTROS_POR_PAGINA, filtroPrograma, filtroEstado, filtroBusqueda);

        Estudiante estudiante = pagina.stream()
                .filter(e -> e.getCodigo().equals(codigo))
                .findFirst().orElse(null);
        if (estudiante == null) return;

        // Cargar el formulario de edicion dentro del Dashboard
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Dashboard) {
            ((Dashboard) window).cargarPanel(new NuevoEstudiantePanel(usuarioActual, estudiante));
        }
    }

    private void eliminarEstudiante(int fila) {
        if (fila < 0 || fila >= modeloTabla.getRowCount()) return;

        String codigo = (String) modeloTabla.getValueAt(fila, 0);
        List<Estudiante> pagina = estudianteController.obtenerEstudiantesFiltradosPaginados(
                paginaActual, EstudianteController.REGISTROS_POR_PAGINA, filtroPrograma, filtroEstado, filtroBusqueda);

        Estudiante estudiante = pagina.stream()
                .filter(e -> e.getCodigo().equals(codigo))
                .findFirst().orElse(null);
        if (estudiante == null) return;

        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro que desea eliminar al estudiante '" + estudiante.getNombreCompleto() + "'?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            if (estudianteController.eliminarEstudiante(estudiante.getIdEstudiante())) {
                JOptionPane.showMessageDialog(this,
                    "Estudiante eliminado exitosamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
                // Retroceder página si era el único registro en esta página
                if (modeloTabla.getRowCount() == 1 && paginaActual > 1) {
                    paginaActual--;
                }
                cargarPagina();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Error al eliminar el estudiante",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
