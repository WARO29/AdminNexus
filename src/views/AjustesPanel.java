package views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;

public class AjustesPanel extends JPanel {

    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG = new Color(249, 250, 251);
    private Color COLOR_CARD = Color.WHITE;
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    private Color COLOR_TEXT_DARK = new Color(17, 24, 39);
    private Color COLOR_BORDER = new Color(229, 231, 235);
    private Color COLOR_SELECTION = new Color(239, 246, 255);
    
    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 14);
    private final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 11);
    
    private Preferences prefs;
    private model.Usuario usuarioActual;
    
    private String selectedMode;
    private String selectedColor;
    private String selectedDensity;
    private int selectedFontSize;
    private String selectedTextColor;
    private boolean hideLabels;
    
    private ModeButton btnLight, btnDark;
    private ColorSwatch swatchBlue, swatchGreen, swatchGray, swatchBurgundy;
    private ColorSwatch textBlack, textGray, textBlue, textNavy;
    private JSlider sliderFont;
    private DensityItem itemRelaxed, itemCompact;
    private ToggleSwitch switchLabels;

    public AjustesPanel(model.Usuario usuario) {
        this.usuarioActual = usuario;
        this.prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuario.getIdusuario());
        
        selectedMode = prefs.get("app_mode", "light");
        selectedColor = prefs.get("app_color", "blue");
        selectedDensity = prefs.get("app_density", "relaxed");
        selectedFontSize = prefs.getInt("app_font_size", 14);
        selectedTextColor = prefs.get("app_text_color", "black");
        hideLabels = prefs.getBoolean("app_hide_labels", false);
        
        cargarConfiguracion();

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(30, 40, 40, 40));

        // Header
        add(crearHeader(), BorderLayout.NORTH);

        // Content Area
        JPanel contentGrid = new JPanel(new GridBagLayout());
        contentGrid.setBackground(COLOR_BG);
        contentGrid.setBorder(new EmptyBorder(0, 0, 20, 0)); // Extra space at bottom
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        
        // Left Column (Appearance)
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setBackground(COLOR_BG);
        leftColumn.add(crearCardApariencia());
        leftColumn.add(Box.createVerticalStrut(25));
        leftColumn.add(crearCardTipografia());
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.6;
        gbc.insets = new Insets(0, 0, 0, 25);
        contentGrid.add(leftColumn, gbc);

        // Right Column (Preferences)
        gbc.gridx = 1;
        gbc.weightx = 0.4;
        gbc.insets = new Insets(0, 0, 0, 0);
        contentGrid.add(crearCardPreferencias(), gbc);

        // Wrap in JScrollPane
        JScrollPane scroll = new JScrollPane(contentGrid);
        scroll.setBorder(null);
        scroll.setBackground(COLOR_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_BG);
        header.setBorder(new EmptyBorder(0, 0, 35, 0));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(COLOR_BG);
        
        JLabel title = new JLabel("Configuración del Sistema");
        title.setFont(FONT_TITLE);
        title.setForeground(COLOR_TEXT_DARK);
        
        JLabel subtitle = new JLabel("<html>Gestiona las preferencias visuales, comportamiento de la interfaz y<br>configuraciones globales de la plataforma administrativa.</html>");
        subtitle.setFont(FONT_SUBTITLE);
        subtitle.setForeground(COLOR_TEXT_MUTED);
        
        left.add(title);
        left.add(Box.createVerticalStrut(10));
        left.add(subtitle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        right.setBackground(COLOR_BG);

        JButton btnReset = new JButton("Restablecer Valores");
        estilizarBotonSecundario(btnReset);
        btnReset.addActionListener(e -> resetSettings());

        JButton btnSave = new JButton("Guardar Cambios");
        estilizarBotonPrimario(btnSave);
        btnSave.addActionListener(e -> saveSettings());

        right.add(btnReset);
        right.add(btnSave);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JPanel crearCardApariencia() {
        RoundedPanel card = new RoundedPanel(15, COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(30, 35, 35, 35));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(new JLabel(new PaletteIcon(COLOR_PRIMARY)));
        JLabel lblTitle = new JLabel("Apariencia");
        lblTitle.setFont(FONT_SECTION);
        lblTitle.setForeground(COLOR_TEXT_DARK);
        titlePanel.add(lblTitle);
        
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 15, 0);
        body.add(titlePanel, gbc);
        
        gbc.gridy = 1; body.add(new JSeparator(), gbc);

        // View Mode
        gbc.gridy = 2; gbc.insets = new Insets(30, 0, 15, 0);
        JLabel lblMode = new JLabel("MODO DE VISUALIZACIÓN");
        lblMode.setFont(FONT_LABEL);
        lblMode.setForeground(COLOR_TEXT_MUTED);
        body.add(lblMode, gbc);

        JPanel modesPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        modesPanel.setOpaque(false);
        modesPanel.setPreferredSize(new Dimension(0, 180)); // Much taller
        
        btnLight = new ModeButton("Modo Claro", true, selectedMode.equals("light"));
        btnDark = new ModeButton("Modo Oscuro", false, selectedMode.equals("dark"));
        
        btnLight.addActionListener(e -> setMode("light"));
        btnDark.addActionListener(e -> setMode("dark"));
        
        modesPanel.add(btnLight);
        modesPanel.add(btnDark);
        
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 35, 0);
        body.add(modesPanel, gbc);
        
        gbc.gridy = 4; body.add(new JSeparator(), gbc);

        // Color Themes
        gbc.gridy = 5; gbc.insets = new Insets(30, 0, 15, 0);
        JLabel lblTheme = new JLabel("TEMAS DE COLOR");
        lblTheme.setFont(FONT_LABEL);
        lblTheme.setForeground(COLOR_TEXT_MUTED);
        body.add(lblTheme, gbc);

        JPanel themesPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        themesPanel.setOpaque(false);
        themesPanel.setPreferredSize(new Dimension(0, 130)); // Fixed height for color grid
        
        swatchBlue = new ColorSwatch("Azul Corporativo", new Color(26, 86, 219), selectedColor.equals("blue"));
        swatchGreen = new ColorSwatch("Verde Esmeralda", new Color(16, 185, 129), selectedColor.equals("green"));
        swatchGray = new ColorSwatch("Gris Profesional", new Color(75, 85, 99), selectedColor.equals("gray"));
        swatchBurgundy = new ColorSwatch("Borgoña", new Color(153, 27, 27), selectedColor.equals("burgundy"));

        swatchBlue.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setColor("blue"); } });
        swatchGreen.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setColor("green"); } });
        swatchGray.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setColor("gray"); } });
        swatchBurgundy.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setColor("burgundy"); } });

        themesPanel.add(swatchBlue);
        themesPanel.add(swatchGreen);
        themesPanel.add(swatchGray);
        themesPanel.add(swatchBurgundy);
        
        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 10, 0);
        body.add(themesPanel, gbc);

        card.add(body, BorderLayout.NORTH); // Use North to prevent stretching empty space
        return card;
    }

    private JPanel crearCardTipografia() {
        RoundedPanel card = new RoundedPanel(15, COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(30, 35, 35, 35));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(new JLabel(new TextIcon(COLOR_PRIMARY)));
        JLabel lblTitle = new JLabel("Tipografía y Lectura");
        lblTitle.setFont(FONT_SECTION);
        lblTitle.setForeground(COLOR_TEXT_DARK);
        titlePanel.add(lblTitle);
        
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 15, 0);
        body.add(titlePanel, gbc);
        gbc.gridy = 1; body.add(new JSeparator(), gbc);

        // Font Size Slider
        gbc.gridy = 2; gbc.insets = new Insets(25, 0, 10, 0);
        JLabel lblSize = new JLabel("TAMAÑO DE LA FUENTE (" + selectedFontSize + "px)");
        lblSize.setFont(FONT_LABEL);
        lblSize.setForeground(COLOR_TEXT_MUTED);
        body.add(lblSize, gbc);

        sliderFont = new JSlider(11, 18, selectedFontSize);
        sliderFont.setBackground(COLOR_CARD);
        sliderFont.setPaintTicks(true);
        sliderFont.setMajorTickSpacing(1);
        sliderFont.setSnapToTicks(true);
        sliderFont.addChangeListener(e -> {
            selectedFontSize = sliderFont.getValue();
            lblSize.setText("TAMAÑO DE LA FUENTE (" + selectedFontSize + "px)");
        });
        
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 25, 0);
        body.add(sliderFont, gbc);

        body.add(new JSeparator(), gbc);

        // Text Color Selection
        gbc.gridy = 5; gbc.insets = new Insets(25, 0, 15, 0);
        JLabel lblTextColor = new JLabel("COLOR DEL TEXTO PRINCIPAL");
        lblTextColor.setFont(FONT_LABEL);
        lblTextColor.setForeground(COLOR_TEXT_MUTED);
        body.add(lblTextColor, gbc);

        JPanel colorsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        colorsPanel.setOpaque(false);
        colorsPanel.setPreferredSize(new Dimension(0, 50));
        
        textBlack = new ColorSwatch("Negro", new Color(17, 24, 39), selectedTextColor.equals("black"));
        textGray = new ColorSwatch("Gris", new Color(75, 85, 99), selectedTextColor.equals("gray"));
        textBlue = new ColorSwatch("Azul", new Color(30, 64, 175), selectedTextColor.equals("blue"));
        textNavy = new ColorSwatch("Navy", new Color(15, 23, 42), selectedTextColor.equals("navy"));

        textBlack.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setTextColor("black"); } });
        textGray.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setTextColor("gray"); } });
        textBlue.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setTextColor("blue"); } });
        textNavy.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setTextColor("navy"); } });

        colorsPanel.add(textBlack);
        colorsPanel.add(textGray);
        colorsPanel.add(textBlue);
        colorsPanel.add(textNavy);

        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 0, 0);
        body.add(colorsPanel, gbc);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearCardPreferencias() {
        RoundedPanel card = new RoundedPanel(15, COLOR_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(new JLabel(new SettingsIcon(COLOR_PRIMARY)));
        JLabel lblTitle = new JLabel("Preferencias de Interfaz");
        lblTitle.setFont(FONT_SECTION);
        lblTitle.setForeground(COLOR_TEXT_DARK);
        titlePanel.add(lblTitle);
        
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 15, 0);
        body.add(titlePanel, gbc);
        
        gbc.gridy = 1; body.add(new JSeparator(), gbc);

        // Information Density
        gbc.gridy = 2; gbc.insets = new Insets(30, 0, 20, 0);
        JLabel lblDensity = new JLabel("DENSIDAD DE INFORMACIÓN");
        lblDensity.setFont(FONT_LABEL);
        lblDensity.setForeground(COLOR_TEXT_MUTED);
        body.add(lblDensity, gbc);

        itemRelaxed = new DensityItem("Relajada", "Mayor espaciado y legibilidad", selectedDensity.equals("relaxed"));
        itemCompact = new DensityItem("Compacta", "Más datos por pantalla", selectedDensity.equals("compact"));
        
        itemRelaxed.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setDensity("relaxed"); } });
        itemCompact.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { setDensity("compact"); } });

        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 12, 0);
        body.add(itemRelaxed, gbc);
        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 30, 0);
        body.add(itemCompact, gbc);

        gbc.gridy = 5; body.add(new JSeparator(), gbc);

        // Sidebar Navigation
        gbc.gridy = 6; gbc.insets = new Insets(30, 0, 20, 0);
        JLabel lblNav = new JLabel("NAVEGACIÓN LATERAL");
        lblNav.setFont(FONT_LABEL);
        lblNav.setForeground(COLOR_TEXT_MUTED);
        body.add(lblNav, gbc);

        switchLabels = new ToggleSwitch("Ocultar etiquetas de texto", hideLabels);
        switchLabels.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                hideLabels = !hideLabels;
                switchLabels.setSelected(hideLabels);
            }
        });
        
        gbc.gridy = 7; gbc.insets = new Insets(0, 0, 15, 0);
        body.add(switchLabels, gbc);
        
        gbc.gridy = 8; gbc.insets = new Insets(10, 0, 0, 0);
        JLabel info = new JLabel("<html><p style='width:240px; color:#6B7280; font-size:10.5px;'>Al activar esta opción, el menú lateral se colapsará mostrando únicamente los iconos para maximizar el espacio.</p></html>");
        body.add(info, gbc);

        card.add(body, BorderLayout.NORTH);
        return card;
    }

    private void setMode(String mode) {
        selectedMode = mode;
        btnLight.setSelected(mode.equals("light"));
        btnDark.setSelected(mode.equals("dark"));
    }

    private void setColor(String color) {
        selectedColor = color;
        swatchBlue.setSelected(color.equals("blue"));
        swatchGreen.setSelected(color.equals("green"));
        swatchGray.setSelected(color.equals("gray"));
        swatchBurgundy.setSelected(color.equals("burgundy"));
    }

    private void setDensity(String density) {
        selectedDensity = density;
        itemRelaxed.setSelected(density.equals("relaxed"));
        itemCompact.setSelected(density.equals("compact"));
    }

    private void setTextColor(String color) {
        selectedTextColor = color;
        textBlack.setSelected(color.equals("black"));
        textGray.setSelected(color.equals("gray"));
        textBlue.setSelected(color.equals("blue"));
        textNavy.setSelected(color.equals("navy"));
    }

    private void resetSettings() {
        setMode("light");
        setColor("blue");
        setDensity("relaxed");
        selectedFontSize = 14;
        sliderFont.setValue(14);
        setTextColor("black");
        hideLabels = false;
        switchLabels.setSelected(false);
    }

    private void saveSettings() {
        prefs.put("app_mode", selectedMode);
        prefs.put("app_color", selectedColor);
        prefs.put("app_density", selectedDensity);
        prefs.putInt("app_font_size", selectedFontSize);
        prefs.put("app_text_color", selectedTextColor);
        prefs.putBoolean("app_hide_labels", hideLabels);
        
        try {
            prefs.flush(); // Forzar escritura en disco
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        JOptionPane.showMessageDialog(this, "Ajustes aplicados correctamente.", "Configuración", JOptionPane.INFORMATION_MESSAGE);
        
        // Refrescar el Dashboard inmediatamente
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof Dashboard) {
            ((Dashboard) window).aplicarPreferencias();
        }
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
            COLOR_TEXT_DARK = Color.WHITE;
            COLOR_TEXT_MUTED = new Color(156, 163, 175);
            COLOR_BORDER = new Color(55, 65, 81);
            COLOR_SELECTION = new Color(59, 130, 246, 40);
        } else {
            COLOR_BG = new Color(249, 250, 251);
            COLOR_CARD = Color.WHITE;
            COLOR_TEXT_DARK = new Color(17, 24, 39);
            COLOR_TEXT_MUTED = new Color(107, 114, 128);
            COLOR_BORDER = new Color(229, 231, 235);
            COLOR_SELECTION = new Color(239, 246, 255);
        }
    }

    // --- Custom Components ---

    class ModeButton extends JButton {
        private boolean isLight, selected;
        public ModeButton(String text, boolean isLight, boolean selected) {
            this.isLight = isLight; this.selected = selected;
            setText(text);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(150, 180));
        }
        public void setSelected(boolean s) { this.selected = s; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Outer Frame
            g2.setColor(selected ? COLOR_SELECTION : COLOR_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(selected ? COLOR_PRIMARY : COLOR_BORDER);
            g2.setStroke(new BasicStroke(selected ? 2.5f : 1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            // Interface Simulation Thumbnail
            int tw = getWidth() - 40, th = 90;
            int tx = 20, ty = 20;
            g2.setColor(isLight ? Color.WHITE : new Color(31, 41, 55));
            g2.fillRoundRect(tx, ty, tw, th, 6, 6);
            g2.setColor(COLOR_BORDER);
            g2.drawRoundRect(tx, ty, tw, th, 6, 6);
            
            // Sidebar simulation
            g2.setColor(isLight ? new Color(243, 244, 246) : new Color(17, 24, 39));
            g2.fillRect(tx, ty, 30, th);
            
            // Content Card simulation
            g2.setColor(isLight ? new Color(249, 250, 251) : new Color(55, 65, 81));
            g2.fillRoundRect(tx + 40, ty + 15, tw - 55, th - 35, 4, 4);

            // Label and Icon at bottom
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(selected ? COLOR_PRIMARY : COLOR_TEXT_DARK);
            FontMetrics fm = g2.getFontMetrics();
            int fullW = fm.stringWidth(getText()) + 25;
            int startX = (getWidth() - fullW) / 2;
            
            if (isLight) {
                g2.drawOval(startX, getHeight() - 32, 14, 14); // Sun
                for(int i=0; i<360; i+=45) {
                    double rad = Math.toRadians(i);
                    g2.drawLine(startX+7+(int)(Math.cos(rad)*7), getHeight()-25+(int)(Math.sin(rad)*7),
                               startX+7+(int)(Math.cos(rad)*10), getHeight()-25+(int)(Math.sin(rad)*10));
                }
            } else {
                g2.drawArc(startX, getHeight() - 32, 14, 14, 45, 240); // Moon
            }
            g2.drawString(getText(), startX + 25, getHeight() - 20);
            g2.dispose();
        }
    }

    class ColorSwatch extends JPanel {
        private Color color; private boolean selected; private String name;
        public ColorSwatch(String name, Color c, boolean selected) {
            this.name = name; this.color = c; this.selected = selected;
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 55));
        }
        public void setSelected(boolean s) { this.selected = s; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(selected ? COLOR_SELECTION : COLOR_CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(selected ? COLOR_PRIMARY : COLOR_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            
            g2.setColor(color);
            g2.fillOval(15, (getHeight() - 24) / 2, 24, 24);
            
            if (selected) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(22, getHeight()/2, 26, getHeight()/2 + 4);
                g2.drawLine(26, getHeight()/2 + 4, 32, getHeight()/2 - 4);
            }
            
            g2.setColor(COLOR_TEXT_DARK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.drawString(name, 55, (getHeight() + 10) / 2);
            g2.dispose();
        }
    }

    class DensityItem extends JPanel {
        private String title, desc; private boolean selected;
        public DensityItem(String title, String desc, boolean selected) {
            this.title = title; this.desc = desc; this.selected = selected;
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 65));
        }
        public void setSelected(boolean s) { this.selected = s; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(selected ? COLOR_SELECTION : COLOR_CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(selected ? COLOR_PRIMARY : COLOR_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            
            // Radio button
            g2.setColor(COLOR_BORDER);
            g2.drawOval(20, (getHeight() - 20) / 2, 20, 20);
            if (selected) {
                g2.setColor(COLOR_PRIMARY);
                g2.fillOval(25, (getHeight() - 10) / 2, 10, 10);
            }
            
            g2.setColor(COLOR_TEXT_DARK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString(title, 55, (getHeight() - 5) / 2);
            g2.setColor(COLOR_TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString(desc, 55, (getHeight() + 25) / 2);
            
            g2.dispose();
        }
    }

    class ToggleSwitch extends JPanel {
        private boolean selected; private String text;
        public ToggleSwitch(String text, boolean selected) {
            this.text = text; this.selected = selected;
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 45));
        }
        public void setSelected(boolean s) { this.selected = s; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(selected ? COLOR_SELECTION : COLOR_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            
            g2.setColor(COLOR_TEXT_DARK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString(text, 15, (getHeight() + 10) / 2);
            
            // Switch
            int sw = 45, sh = 24;
            int sx = getWidth() - 60, sy = (getHeight() - sh) / 2;
            g2.setColor(selected ? COLOR_PRIMARY : new Color(209, 213, 219));
            g2.fillRoundRect(sx, sy, sw, sh, sh, sh);
            
            g2.setColor(Color.WHITE);
            int kSize = sh - 6;
            int kx = selected ? sx + sw - kSize - 3 : sx + 3;
            g2.fillOval(kx, sy + 3, kSize, kSize);
            
            g2.dispose();
        }
    }

    // --- Helper UI Methods ---
    
    private void estilizarBotonPrimario(JButton btn) {
        btn.setPreferredSize(new Dimension(170, 42));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void estilizarBotonSecundario(JButton btn) {
        btn.setPreferredSize(new Dimension(170, 42));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(COLOR_CARD);
        btn.setForeground(COLOR_TEXT_DARK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    class TextIcon implements Icon {
        private Color color;
        public TextIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 22; }
        public int getIconHeight() { return 22; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setFont(new Font("Serif", Font.BOLD, 18));
            g2.drawString("A", x + 2, y + 16);
            g2.setFont(new Font("Serif", Font.PLAIN, 12));
            g2.drawString("a", x + 12, y + 16);
            g2.dispose();
        }
    }

    class PaletteIcon implements Icon {
        private Color color;
        public PaletteIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 22; }
        public int getIconHeight() { return 22; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color != null ? color : new Color(26, 86, 219));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(x+2, y+2, 18, 18);
            g2.fillOval(x+6, y+6, 4, 4);
            g2.fillOval(x+12, y+7, 3, 3);
            g2.fillOval(x+11, y+13, 4, 4);
            g2.dispose();
        }
    }

    class SettingsIcon implements Icon {
        private Color color;
        public SettingsIcon(Color c) { this.color = c; }
        public int getIconWidth() { return 22; }
        public int getIconHeight() { return 22; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color != null ? color : new Color(26, 86, 219));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRect(x+3, y+3, 16, 16);
            g2.drawLine(x+3, y+11, x+19, y+11);
            g2.drawLine(x+11, y+3, x+11, y+19);
            g2.dispose();
        }
    }

    class RoundedPanel extends JPanel {
        private int radius;
        public RoundedPanel(int radius, Color bg) { this.radius = radius; setBackground(bg); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }
}
