package views;

import controller.ProgramaController;
import model.Programa;
import model.Programa.EstadoPrograma;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;

public class NuevoProgramaPanel extends JPanel {

    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG      = new Color(249, 250, 251);
    private Color COLOR_CARD    = Color.WHITE;
    private Color COLOR_TEXT    = new Color(17, 24, 39);
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    private Color COLOR_BORDER  = new Color(229, 231, 235);
    private Color COLOR_SIDEBAR_BG = new Color(219, 234, 254);
    
    private java.util.prefs.Preferences prefs;
    private model.Usuario usuarioActual;
    
    private final Font FONT_BREADCRUMB = new Font("Segoe UI", Font.BOLD, 10);
    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    private final Font FONT_FIELD = new Font("Segoe UI", Font.PLAIN, 14);
    
    private ProgramaController programaController;
    private JTextField txtCodigo, txtNombre, txtColorHex;
    private JSpinner spinnerDuracion, spinnerProyeccion;
    private JComboBox<String> comboEstado;
    private JCheckBox cbPlanEstudios, cbDocentesBase;
    
    // Modo edición
    private Programa programaEditar = null;

    public NuevoProgramaPanel(model.Usuario usuario) {
        this(usuario, null);
    }

    /**
     * Constructor en modo edición. Recibe el programa a editar y pre-llena los campos.
     */
    public NuevoProgramaPanel(model.Usuario usuario, Programa programa) {
        this.usuarioActual = usuario;
        this.programaEditar = programa;
        this.prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuario.getIdusuario());
        this.programaController = new ProgramaController();
        this.programaController.setNombreUsuario(usuario.getNombreAdmin());
        cargarConfiguracion();

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(10, 20, 20, 20));

        // Header Section
        JPanel header = crearHeader();
        add(header, BorderLayout.NORTH);

        // Main Content in ScrollPane
        JPanel contentGrid = new JPanel(new BorderLayout(30, 0));
        contentGrid.setBackground(COLOR_BG);

        // Left Content (Form Cards)
        contentGrid.add(crearCuerpoFormulario(), BorderLayout.CENTER);

        // Right Content (Sidebar)
        contentGrid.add(crearSidebar(), BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(contentGrid);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(COLOR_BG);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);

        // Si es edición, pre-llenar campos
        if (programaEditar != null) {
            precargarDatosEdicion();
        }
        
        setupShortcuts();
    }

    private JPanel crearHeader() {
        boolean modoEdicion = (programaEditar != null);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_BG);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Left Side: Breadcrumbs and Title
        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setBackground(COLOR_BG);
        leftHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel breadcrumbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        breadcrumbPanel.setBackground(COLOR_BG);
        breadcrumbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lblProgramas = new JLabel("PROGRAMAS");
        lblProgramas.setFont(FONT_BREADCRUMB);
        lblProgramas.setForeground(COLOR_PRIMARY);
        lblProgramas.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblProgramas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { volver(); }
            @Override
            public void mouseEntered(MouseEvent e) { lblProgramas.setText("<html><u>PROGRAMAS</u></html>"); }
            @Override
            public void mouseExited(MouseEvent e) { lblProgramas.setText("PROGRAMAS"); }
        });
        
        JLabel lblSeparator = new JLabel(modoEdicion ? "  ›  EDITAR" : "  ›  NUEVO");
        lblSeparator.setFont(FONT_BREADCRUMB);
        lblSeparator.setForeground(COLOR_TEXT_MUTED);
        
        breadcrumbPanel.add(lblProgramas);
        breadcrumbPanel.add(lblSeparator);

        JLabel lblTitle = new JLabel(modoEdicion ? "Editar Programa" : "Crear Nuevo Programa");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(COLOR_TEXT);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSubtitle = new JLabel(modoEdicion
            ? "Modifique la información del programa académico y guarde los cambios."
            : "Complete la información a continuación para registrar un nuevo programa académico en el sistema central.");
        lblSubtitle.setFont(FONT_SUBTITLE);
        lblSubtitle.setForeground(COLOR_TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftHeader.add(breadcrumbPanel);
        leftHeader.add(Box.createVerticalStrut(8));
        leftHeader.add(lblTitle);
        leftHeader.add(Box.createVerticalStrut(4));
        leftHeader.add(lblSubtitle);

        // Right Side: Action Buttons
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightHeader.setBackground(COLOR_BG);
        rightHeader.setAlignmentY(Component.TOP_ALIGNMENT);

        JButton btnCancelar = new JButton("Cancelar");
        estilizarBotonSecundario(btnCancelar);
        btnCancelar.addActionListener(e -> volver());

        JButton btnAccion = new JButton(modoEdicion ? "Guardar Cambios" : "Crear Programa");
        estilizarBotonPrimario(btnAccion, new SaveIcon(Color.WHITE));
        btnAccion.addActionListener(e -> guardarPrograma());

        rightHeader.add(btnCancelar);
        rightHeader.add(btnAccion);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);

        return header;
    }

    /**
     * Pre-carga los datos del programa en el formulario cuando está en modo edición.
     * Se llama después de construir la UI.
     */
    private void precargarDatosEdicion() {
        txtCodigo.setText(programaEditar.getCodigo());
        txtNombre.setText(programaEditar.getNombre());
        txtNombre.setForeground(COLOR_TEXT);
        spinnerDuracion.setValue(programaEditar.getDuracionSemestres());
        spinnerProyeccion.setValue(programaEditar.getInscritos());
        
        // Estado
        switch (programaEditar.getEstado()) {
            case ACTIVO:    comboEstado.setSelectedIndex(0); break;
            case CERRADO:   comboEstado.setSelectedIndex(1); break;
            case EN_PAUSA:  comboEstado.setSelectedIndex(2); break;
        }
        
        // Color
        String color = programaEditar.getIconoColor();
        txtColorHex.setText(color != null && !color.isEmpty() ? color : "#005DB5");
    }

    private JPanel crearCuerpoFormulario() {
        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setBackground(COLOR_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 20, 0);

        // 1. Información General (Full Width Card)
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        formBody.add(crearCardInfoGeneral(), gbc);

        // 2. Métricas Base (Left Half)
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 0, 0, 10);
        formBody.add(crearCardMetricas(), gbc);

        // 3. Configuración (Right Half)
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 0, 0);
        formBody.add(crearCardConfiguracion(), gbc);

        return formBody;
    }

    private JPanel crearCardInfoGeneral() {
        RoundedPanel card = new RoundedPanel(15, COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(25, 30, 25, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.gridx = 0;

        // Title with Icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(new JLabel(new InfoIcon(COLOR_PRIMARY)));
        JLabel lblTitle = new JLabel("Información General");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(COLOR_TEXT);
        titlePanel.add(lblTitle);
        
        gbc.gridy = 0;
        content.add(titlePanel, gbc);

        // Fields
        gbc.gridy = 1;
        txtCodigo = new JTextField(programaController.generarCodigoPrograma());
        txtCodigo.setEditable(false);
        txtCodigo.setBackground(COLOR_BG);
        txtCodigo.setForeground(COLOR_TEXT_MUTED);
        content.add(crearLabeledField("Código del Programa", txtCodigo, ""), gbc);
        
        gbc.gridy = 2;
        gbc.insets = new Insets(20, 0, 10, 0);
        content.add(crearLabeledField("Nombre del Programa", txtNombre = new JTextField(), "Ej. Ingeniería de Sistemas y Computación"), gbc);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearCardMetricas() {
        RoundedPanel card = new RoundedPanel(15, COLOR_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(new JLabel(new MetricsIcon(COLOR_PRIMARY)));
        JLabel lblTitle = new JLabel("Métricas Base");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(COLOR_TEXT);
        titlePanel.add(lblTitle);
        
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 25, 0);
        content.add(titlePanel, gbc);

        // Duración
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        spinnerDuracion = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        content.add(crearLabeledSpinner("Duración (Semestres)", spinnerDuracion, "semestres estándar"), gbc);

        // Proyección
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        spinnerProyeccion = new JSpinner(new SpinnerNumberModel(0, 0, 500, 5));
        content.add(crearLabeledSpinner("Proyección Inicial (Estudiantes)", spinnerProyeccion, "cupos disponibles"), gbc);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearCardConfiguracion() {
        RoundedPanel card = new RoundedPanel(15, COLOR_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(new JLabel(new SettingsIcon(COLOR_PRIMARY)));
        JLabel lblTitle = new JLabel("Configuración");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(COLOR_TEXT);
        titlePanel.add(lblTitle);
        
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 25, 0);
        content.add(titlePanel, gbc);

        // Estado
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        comboEstado = new JComboBox<>(new String[]{"Activo (Visible para matricula)", "Cerrado", "En Pausa"});
        content.add(crearLabeledCombo("Estado del Programa", comboEstado), gbc);

        // Color (campo oculto; se guarda con valor predeterminado)
        txtColorHex = new JTextField("#005DB5");

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_BG);
        sidebar.setPreferredSize(new Dimension(280, 0));

        // Requisitos Card
        sidebar.add(crearTarjetaRequisitos());
        sidebar.add(Box.createVerticalStrut(15));
        
        // Actividad Reciente Card
        sidebar.add(crearTarjetaActividad());
        sidebar.add(Box.createVerticalStrut(15));
        
        // Atajos Card
        sidebar.add(crearTarjetaAtajos());
        
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel crearTarjetaRequisitos() {
        RoundedPanel card = new RoundedPanel(15, COLOR_SIDEBAR_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(prefs.get("app_mode", "light").equals("dark") ? COLOR_BORDER : new Color(191, 219, 254)));
        card.setMaximumSize(new Dimension(300, 220));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 15));
        header.setOpaque(false);
        header.add(new JLabel(new CheckIcon(prefs.get("app_mode", "light").equals("dark") ? COLOR_PRIMARY : new Color(30, 64, 175))));
        JLabel title = new JLabel("Requisitos Previos");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(prefs.get("app_mode", "light").equals("dark") ? COLOR_TEXT : new Color(30, 64, 175));
        header.add(title);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 15, 15, 15));

        JLabel info = new JLabel("<html>Verifique los siguientes items antes de activar el programa en el sistema.</html>");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        info.setForeground(prefs.get("app_mode", "light").equals("dark") ? COLOR_TEXT_MUTED : new Color(30, 64, 175));
        body.add(info);
        body.add(Box.createVerticalStrut(10));

        cbPlanEstudios = new JCheckBox("Plan de estudios aprobado por comité");
        cbPlanEstudios.setOpaque(false);
        cbPlanEstudios.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cbPlanEstudios.setForeground(prefs.get("app_mode", "light").equals("dark") ? COLOR_TEXT : new Color(30, 64, 175));
        body.add(cbPlanEstudios);

        cbDocentesBase = new JCheckBox("Docentes base asignados");
        cbDocentesBase.setOpaque(false);
        cbDocentesBase.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cbDocentesBase.setForeground(prefs.get("app_mode", "light").equals("dark") ? COLOR_TEXT : new Color(30, 64, 175));
        body.add(cbDocentesBase);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearTarjetaActividad() {
        RoundedPanel card = new RoundedPanel(15, COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        card.setMaximumSize(new Dimension(300, 180));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        header.setOpaque(false);
        JLabel title = new JLabel("Actividad Reciente");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(COLOR_TEXT);
        header.add(title);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 15, 15, 15));

        // Fetch last 2 programs
        List<Programa> recientes = programaController.obtenerTodosLosProgramas();
        int count = 0;
        for (Programa p : recientes) {
            if (count++ >= 2) break;
            body.add(crearItemActividad(p.getNombre(), "Creado recientemente", count == 1));
            if (count == 1) body.add(Box.createVerticalStrut(10));
        }

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearItemActividad(String name, String time, boolean isFirst) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setOpaque(false);

        JLabel icon = new JLabel(isFirst ? "+" : "✎", SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(32, 32));
        icon.setOpaque(true);
        icon.setBackground(COLOR_BG);
        icon.setForeground(COLOR_PRIMARY);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        icon.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        
        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblName.setForeground(COLOR_TEXT);
        
        JLabel lblTime = new JLabel(time);
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblTime.setForeground(COLOR_TEXT_MUTED);
        
        info.add(lblName);
        info.add(lblTime);

        item.add(icon, BorderLayout.WEST);
        item.add(info, BorderLayout.CENTER);
        return item;
    }

    private JPanel crearTarjetaAtajos() {
        RoundedPanel card = new RoundedPanel(15, COLOR_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        card.setMaximumSize(new Dimension(300, 120));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(15, 15, 15, 15));

        body.add(crearItemAtajo("Guardar programa", "Ctrl + S"));
        body.add(Box.createVerticalStrut(8));
        body.add(crearItemAtajo("Cancelar y salir", "Esc"));

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // --- Utility UI Builders ---

    private JPanel crearLabeledField(String label, JTextField field, String placeholder) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(COLOR_TEXT);
        
        field.setPreferredSize(new Dimension(0, 42));
        field.setFont(FONT_FIELD);
        field.setBackground(COLOR_CARD);
        field.setForeground(COLOR_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
            new EmptyBorder(0, 5, 0, 5)
        ));

        if (!placeholder.isEmpty()) {
            field.setText(placeholder);
            field.setForeground(COLOR_TEXT_MUTED);
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (field.getText().equals(placeholder)) {
                        field.setText("");
                        field.setForeground(COLOR_TEXT);
                    }
                }
            });
        }

        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearLabeledSpinner(String label, JSpinner spinner, String desc) {
        JPanel p = new JPanel(new BorderLayout(10, 5));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(COLOR_TEXT);
        
        JPanel fieldPanel = new JPanel(new BorderLayout(10, 0));
        fieldPanel.setOpaque(false);
        
        spinner.setPreferredSize(new Dimension(80, 36));
        spinner.setFont(FONT_FIELD);
        
        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(COLOR_TEXT_MUTED);
        
        fieldPanel.add(spinner, BorderLayout.WEST);
        fieldPanel.add(lblDesc, BorderLayout.CENTER);

        p.add(lbl, BorderLayout.NORTH);
        p.add(fieldPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearLabeledCombo(String label, JComboBox<String> combo) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(COLOR_TEXT);
        
        combo.setPreferredSize(new Dimension(0, 36));
        combo.setFont(FONT_FIELD);
        combo.setBackground(COLOR_CARD);
        combo.setForeground(COLOR_TEXT);

        p.add(lbl, BorderLayout.NORTH);
        p.add(combo, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearLabeledColorField(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(COLOR_TEXT);
        
        JPanel fieldPanel = new JPanel(new BorderLayout(10, 0));
        fieldPanel.setOpaque(false);
        
        JPanel colorSquare = new JPanel();
        colorSquare.setPreferredSize(new Dimension(36, 36));
        colorSquare.setBackground(new Color(0, 93, 181));
        
        field.setPreferredSize(new Dimension(0, 36));
        field.setFont(FONT_FIELD);
        field.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

        fieldPanel.add(colorSquare, BorderLayout.WEST);
        fieldPanel.add(field, BorderLayout.CENTER);

        p.add(lbl, BorderLayout.NORTH);
        p.add(fieldPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearItemAtajo(String label, String key) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(COLOR_TEXT_MUTED);
        JLabel k = new JLabel(key);
        k.setFont(new Font("Monospaced", Font.BOLD, 10));
        k.setBackground(COLOR_CARD);
        k.setForeground(COLOR_TEXT);
        k.setOpaque(true);
        k.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_BORDER), new EmptyBorder(2, 5, 2, 5)));
        p.add(lbl, BorderLayout.WEST);
        p.add(k, BorderLayout.EAST);
        return p;
    }

    private void estilizarBotonPrimario(JButton btn, Icon icon) {
        btn.setPreferredSize(new Dimension(160, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setIcon(icon);
        btn.setIconTextGap(10);
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void estilizarBotonSecundario(JButton btn) {
        btn.setPreferredSize(new Dimension(100, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(COLOR_CARD);
        btn.setForeground(COLOR_TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void cargarConfiguracion() {
        String mode = prefs.get("app_mode", "light");
        String theme = prefs.get("app_color", "blue");
        
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
            COLOR_SIDEBAR_BG = new Color(30, 58, 138, 40); // Azul oscuro transparente
        } else {
            COLOR_BG = new Color(249, 250, 251);
            COLOR_CARD = Color.WHITE;
            COLOR_TEXT = new Color(17, 24, 39);
            COLOR_TEXT_MUTED = new Color(107, 114, 128);
            COLOR_BORDER = new Color(229, 231, 235);
            COLOR_SIDEBAR_BG = new Color(219, 234, 254);
        }
    }
    
    private void setupShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "guardar");
        am.put("guardar", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { guardarPrograma(); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "volver");
        am.put("volver", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { volver(); } });
    }

    private void volver() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Dashboard) {
            ((Dashboard) window).cargarPanel(new ProgramasPanel(usuarioActual));
        }
    }

    private void guardarPrograma() {
        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();
        
        if (codigo.isEmpty() || nombre.isEmpty() || nombre.startsWith("Ej.")) {
            JOptionPane.showMessageDialog(this, "Por favor complete los campos obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (programaEditar == null && (!cbPlanEstudios.isSelected() || !cbDocentesBase.isSelected())) {
            JOptionPane.showMessageDialog(this,
                "<html>Para registrar el programa debe confirmar los requisitos previos:<br><br>"
                + (!cbPlanEstudios.isSelected() ? "• Plan de estudios aprobado por comité<br>" : "")
                + (!cbDocentesBase.isSelected() ? "• Docentes base asignados" : "")
                + "</html>",
                "Requisitos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String estado = (String) comboEstado.getSelectedItem();
        EstadoPrograma estadoPrograma;
        if (estado.contains("Activo")) estadoPrograma = EstadoPrograma.ACTIVO;
        else if (estado.contains("Cerrado")) estadoPrograma = EstadoPrograma.CERRADO;
        else estadoPrograma = EstadoPrograma.EN_PAUSA;

        if (programaEditar != null) {
            // MODO EDICIÓN: actualizar programa existente
            programaEditar.setCodigo(codigo);
            programaEditar.setNombre(nombre);
            programaEditar.setDuracionSemestres((Integer) spinnerDuracion.getValue());
            programaEditar.setInscritos((Integer) spinnerProyeccion.getValue());
            programaEditar.setEstado(estadoPrograma);
            programaEditar.setIconoColor(txtColorHex.getText().trim());

            if (programaController.actualizarPrograma(programaEditar)) {
                JOptionPane.showMessageDialog(this, "Programa actualizado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                volver();
            } else {
                JOptionPane.showMessageDialog(this, "Error al actualizar el programa", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // MODO CREACIÓN: registrar nuevo programa
            Programa p = new Programa();
            p.setCodigo(codigo);
            p.setNombre(nombre);
            p.setDuracionSemestres((Integer) spinnerDuracion.getValue());
            p.setInscritos((Integer) spinnerProyeccion.getValue());
            p.setEstado(estadoPrograma);
            p.setIconoColor(txtColorHex.getText().trim());

            if (programaController.crearPrograma(p)) {
                JOptionPane.showMessageDialog(this, "Programa creado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                volver();
            } else {
                JOptionPane.showMessageDialog(this, "Error al crear el programa", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Inner Classes (Icons and UI) ---

    class RoundedPanel extends JPanel {
        private int radius;
        public RoundedPanel(int radius, Color bg) { this.radius = radius; setBackground(bg); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class SaveIcon implements Icon {
        private Color color;
        public SaveIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x + 2, y + 2, 14, 14, 2, 2);
            g2.drawRect(x + 5, y + 2, 8, 5);
            g2.drawLine(x + 5, y + 10, x + 13, y + 10);
            g2.drawLine(x + 5, y + 13, x + 13, y + 13);
            g2.dispose();
        }
    }

    class InfoIcon implements Icon {
        private Color color;
        public InfoIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x + 2, y + 2, 16, 16);
            g2.drawLine(x + 10, y + 8, x + 10, y + 14);
            g2.fillOval(x + 9, y + 5, 2, 2);
            g2.dispose();
        }
    }

    class MetricsIcon implements Icon {
        private Color color;
        public MetricsIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(x + 3, y + 3, 14, 14);
            g2.drawLine(x + 6, y + 14, x + 6, y + 10);
            g2.drawLine(x + 10, y + 14, x + 10, y + 6);
            g2.drawLine(x + 14, y + 14, x + 14, y + 8);
            g2.dispose();
        }
    }

    class SettingsIcon implements Icon {
        private Color color;
        public SettingsIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x + 6, y + 6, 8, 8);
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45);
                int x1 = (int) (x + 10 + 6 * Math.cos(angle));
                int y1 = (int) (y + 10 + 6 * Math.sin(angle));
                int x2 = (int) (x + 10 + 9 * Math.cos(angle));
                int y2 = (int) (y + 10 + 9 * Math.sin(angle));
                g2.drawLine(x1, y1, x2, y2);
            }
            g2.dispose();
        }
    }

    class CheckIcon implements Icon {
        private Color color;
        public CheckIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(x + 2, y + 2, 14, 14);
            g2.drawLine(x + 5, y + 9, x + 8, y + 12);
            g2.drawLine(x + 8, y + 12, x + 13, y + 6);
            g2.dispose();
        }
    }
}
