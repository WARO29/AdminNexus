package views;

import controller.EstudianteController;
import controller.ProgramaController;
import model.Estudiante;
import model.Estudiante.EstadoMatricula;
import model.Programa;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class NuevoEstudiantePanel extends JPanel {

    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG      = new Color(249, 250, 251);
    private Color COLOR_CARD    = Color.WHITE;
    private Color COLOR_TEXT    = new Color(17, 24, 39);
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    private Color COLOR_BORDER  = new Color(229, 231, 235);
    private Color COLOR_SELECTION = new Color(239, 246, 255);
    
    private java.util.prefs.Preferences prefs;
    private model.Usuario usuarioActual;
    
    private final Font FONT_BREADCRUMB = new Font("Segoe UI", Font.BOLD, 10);
    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    private final Font FONT_FIELD = new Font("Segoe UI", Font.PLAIN, 14);
    
    private EstudianteController estudianteController;
    private ProgramaController programaController;
    private JTextField txtCodigo, txtNombre, txtApellido, txtEmail, txtTelefono, txtDireccion;
    private ModernDatePicker dpFechaNac, dpFechaMatricula;
    private JComboBox<String> comboPrograma, comboEstado;

    // Modo edición
    private Estudiante estudianteEditar = null;

    public NuevoEstudiantePanel(model.Usuario usuario) {
        this(usuario, null);
    }

    /**
     * Constructor en modo edición. Recibe el estudiante a editar y pre-llena los campos.
     */
    public NuevoEstudiantePanel(model.Usuario usuario, Estudiante estudiante) {
        this.usuarioActual = usuario;
        this.estudianteEditar = estudiante;
        this.prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuario.getIdusuario());
        this.estudianteController = new EstudianteController();
        this.programaController = new ProgramaController();
        
        cargarConfiguracion();
        
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(10, 20, 20, 20));

        // Breadcrumbs and Title
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(COLOR_BG);
        
        JPanel breadcrumbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        breadcrumbPanel.setBackground(COLOR_BG);
        breadcrumbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lblEstudiantes = new JLabel("ESTUDIANTES");
        lblEstudiantes.setFont(FONT_BREADCRUMB);
        lblEstudiantes.setForeground(COLOR_PRIMARY);
        lblEstudiantes.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblEstudiantes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { volver(); }
            @Override
            public void mouseEntered(MouseEvent e) {
                lblEstudiantes.setText("<html><u>ESTUDIANTES</u></html>");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                lblEstudiantes.setText("ESTUDIANTES");
            }
        });
        
        boolean modoEdicion = (estudianteEditar != null);
        JLabel lblSeparator = new JLabel(modoEdicion ? "  ›  EDITAR REGISTRO" : "  ›  NUEVO REGISTRO");
        lblSeparator.setFont(FONT_BREADCRUMB);
        lblSeparator.setForeground(COLOR_TEXT_MUTED);
        
        breadcrumbPanel.add(lblEstudiantes);
        breadcrumbPanel.add(lblSeparator);
        
        JLabel lblTitle = new JLabel(modoEdicion ? "Editar Estudiante" : "Nuevo Estudiante");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(COLOR_TEXT);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        header.add(breadcrumbPanel);
        header.add(Box.createVerticalStrut(10));
        header.add(lblTitle);
        header.add(Box.createVerticalStrut(20));
        
        add(header, BorderLayout.NORTH);

        // Main Content (Center) wrapped in ScrollPane
        JPanel mainContent = new JPanel(new BorderLayout(30, 0));
        mainContent.setBackground(COLOR_BG);

        // Form Section (Left)
        mainContent.add(crearSeccionFormulario(), BorderLayout.CENTER);

        // Sidebar Section (Right)
        mainContent.add(crearSidebar(), BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(COLOR_BG);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);

        // Si es edición, pre-llenar campos
        if (estudianteEditar != null) {
            precargarDatosEdicion();
        }

        // Setup Keyboard Shortcuts
        setupShortcuts();
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
    
    private void setupShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Ctrl + S: Guardar
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "guardar");
        am.put("guardar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { guardarEstudiante(); }
        });

        // Esc: Limpiar
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "limpiar");
        am.put("limpiar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { limpiarCampos(); }
        });
    }

    private void limpiarCampos() {
        txtNombre.setText("");
        txtApellido.setText("");
        txtEmail.setText("");
        txtTelefono.setText("");
        txtDireccion.setText("");
        dpFechaNac.setDate(null);
        comboPrograma.setSelectedIndex(0);
        dpFechaMatricula.setDate(LocalDate.now());
        comboEstado.setSelectedIndex(0);
        txtNombre.requestFocus();
    }

    private JPanel crearSeccionFormulario() {
        RoundedPanel card = new RoundedPanel(15, COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        // Header of the card
        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        cardHeader.setBackground(COLOR_CARD);
        cardHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        
        JLabel iconLabel = new JLabel(new UserPlusIcon(COLOR_PRIMARY));
        JLabel titleLabel = new JLabel("Detalles del Estudiante");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(COLOR_TEXT);
        
        cardHeader.add(iconLabel);
        cardHeader.add(titleLabel);
        card.add(cardHeader, BorderLayout.NORTH);

        // Form Body
        JPanel formBody = new JPanel(new GridBagLayout());
        formBody.setBackground(COLOR_CARD);
        formBody.setBorder(new EmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Initialize fields
        txtCodigo = crearTextField(estudianteController.generarCodigoEstudiante(), false);
        txtNombre = crearTextField("", true);
        txtApellido = crearTextField("", true);
        txtEmail = crearTextField("", true);
        txtTelefono = crearTextField("", true);
        txtDireccion = crearTextField("", true);

        dpFechaNac = new ModernDatePicker();
        dpFechaMatricula = new ModernDatePicker();
        dpFechaMatricula.setDate(LocalDate.now());

        comboPrograma = new JComboBox<>();
        comboPrograma.addItem("Seleccione un programa");
        cargarProgramasEnCombo(comboPrograma);
        estilizarCombo(comboPrograma);

        comboEstado = new JComboBox<>(new String[]{"Activo", "Inactivo", "Graduado", "Retirado"});
        estilizarCombo(comboEstado);

        // Add fields to form
        int row = 0;
        addFormField(formBody, "Código:", txtCodigo, gbc, row++);
        addFormField(formBody, "Nombre:", txtNombre, gbc, row++);
        addFormField(formBody, "Apellido:", txtApellido, gbc, row++);
        addFormField(formBody, "Email:", txtEmail, gbc, row++);
        addFormField(formBody, "Teléfono:", txtTelefono, gbc, row++);
        addFormField(formBody, "Dirección:", txtDireccion, gbc, row++);
        addFormField(formBody, "Fecha de Nacimiento:", dpFechaNac, gbc, row++);
        addFormField(formBody, "Programa Académico:", comboPrograma, gbc, row++);
        addFormField(formBody, "Fecha de Matrícula:", dpFechaMatricula, gbc, row++);
        addFormField(formBody, "Estado:", comboEstado, gbc, row++);

        // Actions (Buttons)
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        actionsPanel.setBackground(COLOR_CARD);
        actionsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER));

        JButton btnCancelar = new JButton("Cancelar");
        estilizarBoton(btnCancelar, false);
        btnCancelar.addActionListener(e -> volver());

        JButton btnGuardar = new JButton(estudianteEditar != null ? "Guardar Cambios" : "Guardar");
        estilizarBoton(btnGuardar, true);
        btnGuardar.addActionListener(e -> guardarEstudiante());

        actionsPanel.add(btnCancelar);
        actionsPanel.add(btnGuardar);

        card.add(formBody, BorderLayout.CENTER);
        card.add(actionsPanel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_BG);
        sidebar.setPreferredSize(new Dimension(280, 0));

        // Requirements Card
        sidebar.add(crearTarjetaRequisitos());
        sidebar.add(Box.createVerticalStrut(15));
        
        // Recent Activity Card
        sidebar.add(crearTarjetaActividad());
        sidebar.add(Box.createVerticalStrut(15));
        
        // Shortcuts Card
        sidebar.add(crearTarjetaAtajos());
        
        // Espaciador inferior para el scroll
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JPanel crearTarjetaRequisitos() {
        RoundedPanel card = new RoundedPanel(15, new Color(219, 234, 254));
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(new Color(191, 219, 254)));
        card.setMaximumSize(new Dimension(300, 180));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        header.setOpaque(false);
        JLabel icon = new JLabel(new CheckIcon(new Color(30, 64, 175)));
        JLabel title = new JLabel("REQUISITOS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(new Color(30, 64, 175));
        header.add(icon);
        header.add(title);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 15, 15, 15));

        String[][] requisitos = {
            {"Copia de identificación oficial", "ID"},
            {"Certificado de estudios previos", "CERT"},
            {"Comprobante de domicilio", "HOME"},
            {"Fotografías tamaño carnet", "PHOTO"}
        };

        for (String[] req : requisitos) {
            Icon iconReq;
            switch(req[1]) {
                case "ID":    iconReq = new IDIcon(new Color(30, 64, 175)); break;
                case "CERT":  iconReq = new CertIcon(new Color(30, 64, 175)); break;
                case "HOME":  iconReq = new HomeIcon(new Color(30, 64, 175)); break;
                default:      iconReq = new PhotoIcon(new Color(30, 64, 175)); break;
            }
            
            JLabel lbl = new JLabel(req[0]);
            lbl.setIcon(iconReq);
            lbl.setIconTextGap(10);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(new Color(30, 64, 175));
            lbl.setBorder(new EmptyBorder(5, 0, 5, 0));
            body.add(lbl);
        }
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearTarjetaActividad() {
        RoundedPanel card = new RoundedPanel(15, COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        card.setMaximumSize(new Dimension(300, 220));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        header.setOpaque(false);
        JLabel title = new JLabel("ACTIVIDAD RECIENTE");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(COLOR_TEXT_MUTED);
        header.add(title);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 15, 15, 15));

        // Obtener últimos 3 estudiantes
        List<Estudiante> recientes = estudianteController.obtenerEstudiantesPaginados(1, 3);
        Color[] bgColors = {new Color(219, 234, 254), new Color(220, 252, 231), new Color(239, 246, 255)};
        Color[] fgColors = {new Color(30, 64, 175), new Color(22, 101, 52), new Color(59, 130, 246)};

        if (recientes.isEmpty()) {
            JLabel lblEmpty = new JLabel("No hay registros recientes");
            lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblEmpty.setForeground(COLOR_TEXT_MUTED);
            body.add(lblEmpty);
        } else {
            for (int i = 0; i < recientes.size(); i++) {
                Estudiante est = recientes.get(i);
                String iniciales = (est.getNombre().substring(0, 1) + est.getApellido().substring(0, 1)).toUpperCase();
                body.add(crearItemActividad(iniciales, est.getNombreCompleto(), "Registro reciente", 
                                          bgColors[i % 3], fgColors[i % 3]));
                if (i < recientes.size() - 1) body.add(Box.createVerticalStrut(10));
            }
        }

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearItemActividad(String initial, String name, String time, Color bg, Color fg) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setOpaque(false);

        JLabel avatar = new JLabel(initial, SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(32, 32));
        avatar.setOpaque(true);
        avatar.setBackground(bg);
        avatar.setForeground(fg);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 10));
        avatar.setBorder(BorderFactory.createEmptyBorder());
        // Circular avatar
        avatar.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillOval(0, 0, c.getWidth(), c.getHeight());
                super.paint(g, c);
                g2.dispose();
            }
        });

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

        item.add(avatar, BorderLayout.WEST);
        item.add(info, BorderLayout.CENTER);
        return item;
    }

    private JPanel crearTarjetaAtajos() {
        RoundedPanel card = new RoundedPanel(15, COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        card.setMaximumSize(new Dimension(300, 180));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        header.setOpaque(false);
        JLabel title = new JLabel("AYUDA RÁPIDA / ATAJOS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(COLOR_TEXT);
        header.add(title);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 15, 15, 15));

        body.add(crearItemAtajo("Guardar Formulario", "Ctrl + S"));
        body.add(Box.createVerticalStrut(10));
        body.add(crearItemAtajo("Limpiar Campos", "Esc"));
        body.add(Box.createVerticalStrut(15));
        
        JLabel lblManual = new JLabel("<html><span style='color:#1d4ed8;'>📖 Ver Manual de Usuario</span></html>");
        lblManual.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblManual.setCursor(new Cursor(Cursor.HAND_CURSOR));
        body.add(lblManual);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearItemAtajo(String label, String key) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(COLOR_TEXT_MUTED);
        
        JLabel k = new JLabel(key);
        k.setFont(new Font("Monospaced", Font.BOLD, 10));
        k.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            new EmptyBorder(2, 5, 2, 5)
        ));
        k.setBackground(COLOR_BG);
        k.setForeground(COLOR_TEXT);
        k.setOpaque(true);
        
        p.add(lbl, BorderLayout.WEST);
        p.add(k, BorderLayout.EAST);
        return p;
    }

    private void addFormField(JPanel panel, String labelText, JComponent field, GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        
        // Label
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        JLabel label = new JLabel(labelText);
        label.setFont(FONT_LABEL);
        label.setForeground(COLOR_TEXT);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setBorder(new EmptyBorder(0, 0, 0, 20));
        panel.add(label, gbc);

        // Field
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    private JTextField crearTextField(String text, boolean editable) {
        JTextField field = new JTextField(text);
        field.setPreferredSize(new Dimension(0, 36));
        field.setFont(FONT_FIELD);
        field.setEditable(editable);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            new EmptyBorder(0, 10, 0, 10)
        ));
        if (!editable) {
            field.setBackground(COLOR_BG);
            field.setForeground(COLOR_TEXT_MUTED);
        } else {
            field.setBackground(COLOR_CARD);
            field.setForeground(COLOR_TEXT);
        }
        return field;
    }

    private void estilizarCombo(JComboBox<String> combo) {
        combo.setPreferredSize(new Dimension(0, 36));
        combo.setFont(FONT_FIELD);
        combo.setBackground(COLOR_CARD);
        combo.setForeground(COLOR_TEXT);
    }

    private void estilizarBoton(JButton btn, boolean primary) {
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (primary) {
            btn.setBackground(COLOR_PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setBorder(null);
        } else {
            btn.setBackground(COLOR_BG);
            btn.setForeground(COLOR_TEXT);
            btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        }
    }

    private void cargarProgramasEnCombo(JComboBox<String> combo) {
        ProgramaController pc = new ProgramaController();
        List<Programa> programas = pc.obtenerTodosLosProgramas();
        for (Programa p : programas) {
            combo.addItem(p.getNombre());
        }
    }

    private void volver() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Dashboard) {
            ((Dashboard) window).cargarPanel(new EstudiantesPanel(usuarioActual));
        }
    }

    private void guardarEstudiante() {
        try {
            // Validaciones básicas
            String nombre = txtNombre.getText().trim();
            String apellido = txtApellido.getText().trim();
            String email = txtEmail.getText().trim();
            
            if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nombre, Apellido y Email son campos obligatorios", 
                                            "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!email.contains("@")) {
                JOptionPane.showMessageDialog(this, "Ingrese un formato de email válido", 
                                            "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String prog = (String) comboPrograma.getSelectedItem();
            if (prog == null || prog.equals("Seleccione un programa")) {
                JOptionPane.showMessageDialog(this, "Seleccione un programa académico", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Resolver idPrograma
            int idPrograma = -1;
            List<Programa> programas = estudianteController.obtenerProgramasDisponibles();
            for (Programa p : programas) {
                if (p.getNombre().equals(prog)) {
                    idPrograma = p.getIdPrograma();
                    break;
                }
            }

            String est = (String) comboEstado.getSelectedItem();
            EstadoMatricula estadoMatricula = EstadoMatricula.ACTIVO;
            if (est != null) estadoMatricula = EstadoMatricula.valueOf(est.toUpperCase());

            if (estudianteEditar != null) {
                // MODO EDICIÓN: actualizar estudiante existente
                estudianteEditar.setNombre(nombre);
                estudianteEditar.setApellido(apellido);
                estudianteEditar.setEmail(email);
                estudianteEditar.setTelefono(txtTelefono.getText().trim());
                estudianteEditar.setDireccion(txtDireccion.getText().trim());
                estudianteEditar.setFechaNacimiento(dpFechaNac.getDate());
                estudianteEditar.setIdPrograma(idPrograma);
                estudianteEditar.setFechaMatricula(dpFechaMatricula.getDate());
                estudianteEditar.setEstado(estadoMatricula);

                if (estudianteController.actualizarEstudiante(estudianteEditar)) {
                    JOptionPane.showMessageDialog(this, "Estudiante actualizado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    volver();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al actualizar estudiante", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // MODO CREACIÓN: registrar nuevo estudiante
                Estudiante e = new Estudiante();
                e.setCodigo(txtCodigo.getText().trim());
                e.setNombre(nombre);
                e.setApellido(apellido);
                e.setEmail(email);
                e.setTelefono(txtTelefono.getText().trim());
                e.setDireccion(txtDireccion.getText().trim());
                e.setFechaNacimiento(dpFechaNac.getDate());
                e.setIdPrograma(idPrograma);
                e.setFechaMatricula(dpFechaMatricula.getDate());
                e.setEstado(estadoMatricula);
                e.setAvatarUrl("");

                if (estudianteController.crearEstudiante(e)) {
                    JOptionPane.showMessageDialog(this, "Estudiante registrado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    volver();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al registrar estudiante", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Pre-carga los datos del estudiante en el formulario cuando está en modo edición.
     * Se llama después de construir la UI.
     */
    private void precargarDatosEdicion() {
        txtCodigo.setText(estudianteEditar.getCodigo());
        txtNombre.setText(estudianteEditar.getNombre());
        txtApellido.setText(estudianteEditar.getApellido());
        txtEmail.setText(estudianteEditar.getEmail());
        txtTelefono.setText(estudianteEditar.getTelefono() != null ? estudianteEditar.getTelefono() : "");
        txtDireccion.setText(estudianteEditar.getDireccion() != null ? estudianteEditar.getDireccion() : "");

        // Fechas
        dpFechaNac.setDate(estudianteEditar.getFechaNacimiento());
        dpFechaMatricula.setDate(estudianteEditar.getFechaMatricula());

        // Programa
        String nombrePrograma = estudianteEditar.getNombrePrograma();
        if (nombrePrograma != null) {
            for (int i = 0; i < comboPrograma.getItemCount(); i++) {
                if (comboPrograma.getItemAt(i).equals(nombrePrograma)) {
                    comboPrograma.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Estado
        if (estudianteEditar.getEstado() != null) {
            String estadoNombre = estudianteEditar.getEstado().name();
            for (int i = 0; i < comboEstado.getItemCount(); i++) {
                if (comboEstado.getItemAt(i).toUpperCase().equals(estadoNombre)) {
                    comboEstado.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    // --- Helper Components ---

    class RoundedPanel extends JPanel {
        private int radius;
        public RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            setBackground(bg);
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class UserPlusIcon implements Icon {
        private Color color;
        public UserPlusIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 24; }
        public int getIconHeight() { return 24; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 4, y + 4, 8, 8);
            g2.drawArc(x + 2, y + 14, 12, 8, 0, 180);
            g2.drawLine(x + 18, y + 8, x + 18, y + 14);
            g2.drawLine(x + 15, y + 11, x + 21, y + 11);
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
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 2, y + 2, 14, 14);
            g2.drawLine(x + 5, y + 9, x + 8, y + 12);
            g2.drawLine(x + 8, y + 12, x + 13, y + 6);
            g2.dispose();
        }
    }

    class CalendarIcon implements Icon {
        private Color color;
        public CalendarIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x + 2, y + 3, 14, 13, 2, 2);
            g2.drawLine(x + 2, y + 7, x + 16, y + 7);
            g2.drawLine(x + 5, y + 2, x + 5, y + 5);
            g2.drawLine(x + 13, y + 2, x + 13, y + 5);
            g2.dispose();
        }
    }

    class ModernDatePicker extends JPanel {
        private JTextField txtDate;
        private JButton btnCalendar;
        private LocalDate selectedDate;
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        public ModernDatePicker() {
            setLayout(new BorderLayout());
            setBackground(COLOR_CARD);
            setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
            setPreferredSize(new Dimension(0, 36));

            txtDate = new JTextField();
            txtDate.setBorder(new EmptyBorder(0, 10, 0, 0));
            txtDate.setEditable(false);
            txtDate.setCursor(new Cursor(Cursor.HAND_CURSOR));
            txtDate.setBackground(COLOR_CARD);
            txtDate.setForeground(COLOR_TEXT);
            txtDate.setFont(FONT_FIELD);

            btnCalendar = new JButton(new CalendarIcon(COLOR_TEXT_MUTED));
            btnCalendar.setBorder(new EmptyBorder(0, 5, 0, 10));
            btnCalendar.setContentAreaFilled(false);
            btnCalendar.setFocusPainted(false);
            btnCalendar.setCursor(new Cursor(Cursor.HAND_CURSOR));

            add(txtDate, BorderLayout.CENTER);
            add(btnCalendar, BorderLayout.EAST);

            MouseAdapter showPopup = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showCalendarPopup();
                }
            };
            txtDate.addMouseListener(showPopup);
            btnCalendar.addMouseListener(showPopup);
        }

        public void setDate(LocalDate date) {
            this.selectedDate = date;
            if (date != null) {
                txtDate.setText(date.format(formatter));
            } else {
                txtDate.setText("mm/dd/yyyy");
            }
        }

        public LocalDate getDate() {
            return selectedDate;
        }

        private void showCalendarPopup() {
            JPopupMenu popup = new JPopupMenu();
            popup.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
            CalendarPanel calendarPanel = new CalendarPanel(selectedDate != null ? selectedDate : LocalDate.now(), date -> {
                setDate(date);
                popup.setVisible(false);
            });
            popup.add(calendarPanel);
            popup.show(this, 0, getHeight());
        }
    }

    class CalendarPanel extends JPanel {
        private enum CalendarMode { DAYS, MONTHS, YEARS }
        private CalendarMode mode = CalendarMode.DAYS;
        private LocalDate displayDate;
        private Consumer<LocalDate> onDateSelected;
        private JPanel mainGrid;
        private JLabel lblHeader;
        private int yearStart;

        public CalendarPanel(LocalDate initialDate, Consumer<LocalDate> onDateSelected) {
            this.displayDate = initialDate;
            this.onDateSelected = onDateSelected;
            this.yearStart = (displayDate.getYear() / 12) * 12;
            
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(250, 280));
            setBackground(COLOR_CARD);
            setBorder(new EmptyBorder(10, 10, 10, 10));

            // Header
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            lblHeader = new JLabel("", SwingConstants.CENTER);
            lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblHeader.setForeground(COLOR_TEXT);
            lblHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lblHeader.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (mode == CalendarMode.DAYS) {
                        mode = CalendarMode.MONTHS;
                    } else if (mode == CalendarMode.MONTHS) {
                        mode = CalendarMode.YEARS;
                    } else {
                        mode = CalendarMode.DAYS;
                    }
                    updateView();
                }
            });

            JButton btnPrev = new JButton("‹");
            JButton btnNext = new JButton("›");
            estilizarNavBtn(btnPrev);
            estilizarNavBtn(btnNext);

            btnPrev.addActionListener(e -> {
                if (mode == CalendarMode.DAYS) displayDate = displayDate.minusMonths(1);
                else if (mode == CalendarMode.MONTHS) displayDate = displayDate.minusYears(1);
                else { yearStart -= 12; displayDate = displayDate.withYear(yearStart); }
                updateView();
            });
            btnNext.addActionListener(e -> {
                if (mode == CalendarMode.DAYS) displayDate = displayDate.plusMonths(1);
                else if (mode == CalendarMode.MONTHS) displayDate = displayDate.plusYears(1);
                else { yearStart += 12; displayDate = displayDate.withYear(yearStart); }
                updateView();
            });

            header.add(btnPrev, BorderLayout.WEST);
            header.add(lblHeader, BorderLayout.CENTER);
            header.add(btnNext, BorderLayout.EAST);

            add(header, BorderLayout.NORTH);

            // Main Grid
            mainGrid = new JPanel();
            mainGrid.setOpaque(false);
            add(mainGrid, BorderLayout.CENTER);

            updateView();
        }

        private void estilizarNavBtn(JButton btn) {
            btn.setContentAreaFilled(false);
            btn.setBorder(null);
            btn.setFocusPainted(false);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setForeground(COLOR_PRIMARY);
        }

        private void updateView() {
            mainGrid.removeAll();
            switch (mode) {
                case DAYS:   showDays();   break;
                case MONTHS: showMonths(); break;
                case YEARS:  showYears();  break;
            }
            mainGrid.revalidate();
            mainGrid.repaint();
        }

        private void showDays() {
            mainGrid.setLayout(new GridLayout(0, 7, 2, 2));
            String month = displayDate.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            lblHeader.setText(month.substring(0, 1).toUpperCase() + month.substring(1) + " " + displayDate.getYear());

            String[] dayNames = {"Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sá"};
            for (String name : dayNames) {
                JLabel lbl = new JLabel(name, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
                lbl.setForeground(COLOR_TEXT_MUTED);
                mainGrid.add(lbl);
            }

            LocalDate firstOfMonth = displayDate.withDayOfMonth(1);
            int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
            for (int i = 0; i < dayOfWeek; i++) mainGrid.add(new JLabel(""));

            int daysInMonth = displayDate.lengthOfMonth();
            for (int i = 1; i <= daysInMonth; i++) {
                final int day = i;
                JButton btnDay = crearGridBtn(String.valueOf(i));
                if (displayDate.withDayOfMonth(day).equals(LocalDate.now())) {
                    btnDay.setForeground(COLOR_PRIMARY);
                    btnDay.setFont(new Font("Segoe UI", Font.BOLD, 11));
                }
                btnDay.addActionListener(e -> onDateSelected.accept(displayDate.withDayOfMonth(day)));
                mainGrid.add(btnDay);
            }
        }

        private void showMonths() {
            mainGrid.setLayout(new GridLayout(4, 3, 5, 5));
            lblHeader.setText("Seleccionar Mes (" + displayDate.getYear() + ")");
            String[] months = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            for (int i = 0; i < months.length; i++) {
                final int monthIdx = i + 1;
                JButton btn = crearGridBtn(months[i]);
                if (displayDate.getMonthValue() == monthIdx) btn.setForeground(COLOR_PRIMARY);
                btn.addActionListener(e -> {
                    displayDate = displayDate.withMonth(monthIdx);
                    mode = CalendarMode.DAYS;
                    updateView();
                });
                mainGrid.add(btn);
            }
        }

        private void showYears() {
            mainGrid.setLayout(new GridLayout(4, 3, 5, 5));
            lblHeader.setText(yearStart + " - " + (yearStart + 11));
            for (int i = 0; i < 12; i++) {
                final int year = yearStart + i;
                JButton btn = crearGridBtn(String.valueOf(year));
                if (displayDate.getYear() == year) btn.setForeground(COLOR_PRIMARY);
                btn.addActionListener(e -> {
                    displayDate = displayDate.withYear(year);
                    mode = CalendarMode.MONTHS;
                    updateView();
                });
                mainGrid.add(btn);
            }
        }

        private JButton crearGridBtn(String text) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorder(null);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { btn.setOpaque(true); btn.setBackground(new Color(239, 246, 255)); btn.repaint(); }
                @Override
                public void mouseExited(MouseEvent e) { btn.setOpaque(false); btn.repaint(); }
            });
            return btn;
        }
    }

    class IDIcon implements Icon {
        private Color color;
        public IDIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x + 1, y + 3, 16, 12, 2, 2);
            g2.drawOval(x + 4, y + 6, 4, 4);
            g2.drawLine(x + 10, y + 7, x + 14, y + 7);
            g2.drawLine(x + 10, y + 10, x + 14, y + 10);
            g2.dispose();
        }
    }

    class CertIcon implements Icon {
        private Color color;
        public CertIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(x + 3, y + 2, 12, 14);
            g2.drawLine(x + 6, y + 6, x + 12, y + 6);
            g2.drawLine(x + 6, y + 9, x + 12, y + 9);
            g2.drawLine(x + 6, y + 12, x + 12, y + 12);
            g2.dispose();
        }
    }

    class HomeIcon implements Icon {
        private Color color;
        public HomeIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            Path2D home = new Path2D.Double();
            home.moveTo(x + 9, y + 3);
            home.lineTo(x + 3, y + 8);
            home.lineTo(x + 3, y + 15);
            home.lineTo(x + 15, y + 15);
            home.lineTo(x + 15, y + 8);
            home.closePath();
            g2.draw(home);
            g2.drawRect(x + 8, y + 11, 2, 4);
            g2.dispose();
        }
    }

    class PhotoIcon implements Icon {
        private Color color;
        public PhotoIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x + 2, y + 3, 14, 12, 2, 2);
            g2.drawOval(x + 7, y + 7, 4, 4);
            g2.fillOval(x + 13, y + 5, 2, 2);
            g2.dispose();
        }
    }
}
