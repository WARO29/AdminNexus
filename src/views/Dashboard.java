package views;

import config.Database;
import controller.ActividadController;
import model.Actividad;
import model.Usuario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.text.SimpleDateFormat;
import java.util.List;
import controller.BusquedaController;
import model.ResultadoBusqueda;
import model.Estudiante;
import model.Programa;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.prefs.Preferences;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.awt.geom.Ellipse2D;

public class Dashboard extends JFrame {

    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG = new Color(249, 250, 251); // Fondo gris muy claro
    private Color COLOR_SIDEBAR = Color.WHITE;
    private Color COLOR_TEXT = new Color(107, 114, 128); // Gris inactivo
    private Color COLOR_TEXT_DARK = new Color(17, 24, 39);
    private Color COLOR_TEXT_DANGER = new Color(239, 68, 68);
    private Color COLOR_BORDER = new Color(229, 231, 235);
    private Color COLOR_CARD_BG = new Color(243, 244, 246);
    private Color COLOR_SELECTION = new Color(239, 246, 255);
    private Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 14);
    private Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    private Usuario usuarioActual;
    private JPanel menuPanel;
    private JPanel contentPanel; // Panel de contenido principal
    private long lastNotificationCloseTime = 0;
    private long lastProfileCloseTime = 0;
    private Preferences prefs;
    private JLabel topAvatar;
    private JLabel sidebarAvatar;

    public Dashboard(Usuario usuario) {
        this.usuarioActual = usuario;
        this.prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config")
                .node("User_" + usuario.getIdusuario());
        setTitle("AdminNexus - Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Manejamos el cierre manualmente
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);

        // Establecer el icono de la aplicación
        try {
            java.net.URL iconURL = getClass().getResource("/assets/adminnexus.png");
            if (iconURL != null) {
                setIconImage(new ImageIcon(iconURL).getImage());
            } else {
                setIconImage(new ImageIcon("src/assets/adminnexus.png").getImage());
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono de la aplicación");
        }

        try {
            if (prefs.get("app_mode", "light").equals("dark")) {
                com.formdev.flatlaf.FlatDarkLaf.setup();
            } else {
                com.formdev.flatlaf.FlatIntelliJLaf.setup();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // WindowListener: liberar recursos al cerrar la aplicación
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Cerrando AdminNexus — liberando recursos...");
                Database.cerrarPool(); // Cierra todas las conexiones del pool HikariCP
                dispose();
                System.exit(0);
            }
        });

        aplicarPreferencias();

        add(createSidebar(), BorderLayout.WEST);
        add(createMainContent(), BorderLayout.CENTER);
    }

    /**
     * Aplica las preferencias de usuario (Tema, Color, Densidad)
     */
    public void aplicarPreferencias() {
        // Cargar preferencias
        String mode = prefs.get("app_mode", "light");

        // Aplicar LookAndFeel de FlatLaf
        try {
            if (mode.equals("dark")) {
                com.formdev.flatlaf.FlatDarkLaf.setup();
            } else {
                com.formdev.flatlaf.FlatIntelliJLaf.setup();
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String colorTheme = prefs.get("app_color", "blue");
        String density = prefs.get("app_density", "relaxed");
        int fontSize = prefs.getInt("app_font_size", 14);
        String textColorTheme = prefs.get("app_text_color", "black");

        // Aplicar Color Primario
        switch (colorTheme) {
            case "green":
                COLOR_PRIMARY = new Color(16, 185, 129);
                break;
            case "gray":
                COLOR_PRIMARY = new Color(75, 85, 99);
                break;
            case "burgundy":
                COLOR_PRIMARY = new Color(153, 27, 27);
                break;
            default:
                COLOR_PRIMARY = new Color(26, 86, 219);
                break;
        }

        // Aplicar Color de Texto
        switch (textColorTheme) {
            case "gray":
                COLOR_TEXT_DARK = new Color(75, 85, 99);
                break;
            case "blue":
                COLOR_TEXT_DARK = new Color(30, 64, 175);
                break;
            case "navy":
                COLOR_TEXT_DARK = new Color(15, 23, 42);
                break;
            default:
                COLOR_TEXT_DARK = new Color(17, 24, 39);
                break;
        }

        // Aplicar Modo Claro/Oscuro
        if (mode.equals("dark")) {
            COLOR_BG = new Color(17, 24, 39);
            COLOR_SIDEBAR = new Color(31, 41, 55);
            COLOR_TEXT = new Color(209, 213, 219);
            COLOR_TEXT_DARK = Color.WHITE;
            COLOR_BORDER = new Color(55, 65, 81);
            COLOR_CARD_BG = new Color(55, 65, 81);
            COLOR_SELECTION = new Color(255, 255, 255, 20); // Blanco semi-transparente para modo oscuro
        } else {
            COLOR_BG = new Color(249, 250, 251);
            COLOR_SIDEBAR = Color.WHITE;
            COLOR_TEXT = new Color(107, 114, 128);
            COLOR_BORDER = new Color(229, 231, 235);
            COLOR_CARD_BG = new Color(243, 244, 246);
            COLOR_SELECTION = new Color(239, 246, 255); // Azul muy claro para modo claro
        }

        // Aplicar Densidad y Tamaño de Fuente
        int baseSize = fontSize;
        if (density.equals("compact"))
            baseSize -= 1;

        // Limitar tamaño para no romper diseño (10px a 20px)
        baseSize = Math.max(10, Math.min(20, baseSize));

        FONT_MAIN = new Font("Segoe UI", Font.PLAIN, baseSize);
        FONT_BOLD = new Font("Segoe UI", Font.BOLD, baseSize);

        // Refrescar componentes si ya existen
        if (menuPanel != null) {
            // Guardar el panel actual para restaurarlo después
            JPanel currentView = null;
            if (contentPanel != null && contentPanel.getComponentCount() > 0) {
                Component comp = contentPanel.getComponent(0);
                if (comp instanceof JPanel) {
                    currentView = (JPanel) comp;
                }
            }

            // Limpiar y reconstruir la interfaz base
            getContentPane().removeAll();
            getContentPane().setBackground(COLOR_BG);

            add(createSidebar(), BorderLayout.WEST);
            add(createMainContent(), BorderLayout.CENTER);

            // Restaurar la vista previa si era un panel de gestión
            if (currentView != null) {
                cargarPanel(currentView);
            }

            revalidate();
            repaint();
        }
    }

    // ================= SIDEBAR =================

    private JPanel createSidebar() {
        boolean hideLabels = prefs.getBoolean("app_hide_labels", false);
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(COLOR_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(hideLabels ? 80 : 260, getHeight()));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));

        // Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 20));
        logoPanel.setBackground(COLOR_SIDEBAR);
        logoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel logoIcon = new JLabel(new LogoIcon());
        logoIcon.setForeground(COLOR_PRIMARY); // Aplicar color del tema al logo si es posible

        if (!prefs.getBoolean("app_hide_labels", false)) {
            JLabel logoText = new JLabel("<html><b style='color:" + toHexString(COLOR_PRIMARY)
                    + "; font-size:16px;'>AdminNexus</b><br><span style='color:" + toHexString(COLOR_TEXT)
                    + "; font-size:9px; letter-spacing: 1px;'>ADMINISTRACIÓN<br>CENTRAL</span></html>");
            logoPanel.add(logoIcon);
            logoPanel.add(logoText);
        } else {
            logoPanel.add(logoIcon);
        }
        sidebar.add(logoPanel, BorderLayout.NORTH);

        // Menú
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(COLOR_SIDEBAR);
        menuPanel.setBorder(new EmptyBorder(0, 10, 0, 10));

        menuPanel
                .add(createMenuItem("Inicio", new DashboardIcon(COLOR_TEXT), false, false, () -> mostrarPanelInicio()));
        menuPanel.add(createMenuItem("Estudiantes", new UsersIcon(COLOR_TEXT), false, false, () -> abrirEstudiantes()));
        menuPanel.add(createMenuItem("Pagos", new PaymentIcon(COLOR_TEXT), false, false, () -> abrirPagos()));
        menuPanel.add(createMenuItem("Programas", new LayersIcon(COLOR_TEXT), false, false, () -> abrirProgramas()));
        menuPanel.add(createMenuItem("Academia", new AcademicIcon(COLOR_TEXT), false, false, null));
        menuPanel.add(createMenuItem("Reportes", new ChartIcon(COLOR_TEXT), false, false, null));
        menuPanel.add(createMenuItem("Ajustes", new GearIcon(COLOR_TEXT), false, false, () -> abrirAjustes()));

        // Opción de Gestión de Usuarios solo para Administradores
        if (usuarioActual.esAdministrador()) {
            menuPanel.add(Box.createVerticalStrut(10));
            menuPanel.add(createSeparator());
            menuPanel.add(Box.createVerticalStrut(10));
            menuPanel.add(createMenuItem("Gestión de Usuarios", new AdminIcon(COLOR_PRIMARY), false, false,
                    () -> abrirGestionUsuarios()));
        }

        menuPanel.add(Box.createVerticalStrut(30)); // Separador

        menuPanel.add(createMenuItem("Cerrar sesión", new LogoutIcon(COLOR_TEXT_DANGER), false, true,
                () -> cerrarSesion()));

        sidebar.add(menuPanel, BorderLayout.CENTER);

        // Perfil Inferior
        JPanel profileContainer = new JPanel(new BorderLayout());
        profileContainer.setBackground(COLOR_SIDEBAR);
        profileContainer.setBorder(new EmptyBorder(15, 20, 20, 20));

        RoundedPanel profileCard = new RoundedPanel(15, COLOR_CARD_BG);
        profileCard.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        profileCard.setPreferredSize(new Dimension(220, 65));

        sidebarAvatar = new JLabel();
        actualizarIconoAvatar(sidebarAvatar, 36,
                usuarioActual.esAdministrador() ? new Color(15, 118, 110) : new Color(59, 130, 246));

        JLabel profileInfo = new JLabel("<html><body style='font-family:Segoe UI;'><b style='color:"
                + toHexString(COLOR_TEXT_DARK) + "; font-size:12px;'>" +
                capitalizeFully(usuarioActual.getNombreAdmin()) +
                "</b><br><b style='color:" + toHexString(COLOR_TEXT_DARK) + "; font-size:10px;'>" +
                usuarioActual.getUser().toLowerCase() +
                "</b></body></html>");

        profileCard.add(sidebarAvatar);
        profileCard.add(profileInfo);

        profileContainer.add(profileCard, BorderLayout.CENTER);
        sidebar.add(profileContainer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createMenuItem(String text, MenuIcon iconObj, boolean isActive, boolean isDanger, Runnable action) {
        MenuItemPanel item = new MenuItemPanel(isActive);
        item.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 12));
        item.setMaximumSize(new Dimension(240, 45));

        if (isActive) {
            iconObj.setColor(COLOR_PRIMARY);
        } else if (isDanger) {
            iconObj.setColor(COLOR_TEXT_DANGER);
        }

        JLabel iconLabel = new JLabel(iconObj);
        iconLabel.setPreferredSize(new Dimension(24, 24));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel label = new JLabel(text);
        label.setFont(isActive ? FONT_BOLD : FONT_MAIN);

        if (isActive) {
            label.setForeground(COLOR_PRIMARY);
        } else if (isDanger) {
            label.setForeground(COLOR_TEXT_DANGER);
        } else {
            label.setForeground(COLOR_TEXT);
        }

        item.add(Box.createRigidArea(new Dimension(5, 0))); // Padding
        item.add(iconLabel);

        // Solo agregar etiqueta si no está activado "Ocultar etiquetas"
        if (!prefs.getBoolean("app_hide_labels", false)) {
            item.add(label);
            item.setMaximumSize(new Dimension(240, 45));
        } else {
            item.setMaximumSize(new Dimension(60, 45));
            item.setToolTipText(text);
        }

        // Agregar acción al hacer clic
        if (action != null) {
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        }

        return item;
    }

    /**
     * Crea un separador visual para el menú
     */
    private JPanel createSeparator() {
        JPanel separator = new JPanel();
        separator.setBackground(COLOR_BORDER);
        separator.setMaximumSize(new Dimension(240, 1));
        separator.setPreferredSize(new Dimension(240, 1));
        return separator;
    }

    /**
     * Abre la ventana de gestión de usuarios
     */
    public void abrirGestionUsuarios() {
        GestionUsuariosPanel panel = new GestionUsuariosPanel(usuarioActual);
        cargarPanel(panel);
    }

    /**
     * Abre el panel de programas académicos
     */
    public void abrirProgramas() {
        ProgramasPanel panel = new ProgramasPanel(usuarioActual);
        cargarPanel(panel);
    }

    /**
     * Abre el panel de pagos (Dashboard Financiero)
     */
    public void abrirPagos() {
        PagosPanel panel = new PagosPanel(usuarioActual);
        cargarPanel(panel);
    }

    /**
     * Abre el panel de estudiantes
     */
    public void abrirEstudiantes() {
        EstudiantesPanel panel = new EstudiantesPanel(usuarioActual);
        cargarPanel(panel);
    }

    /**
     * Abre el panel de configuración del sistema
     */
    public void abrirAjustes() {
        AjustesPanel panel = new AjustesPanel(usuarioActual);
        cargarPanel(panel);
    }

    /**
     * Cierra la sesión y vuelve al login
     */
    private void cerrarSesion() {
        String mensaje = "<html><body style='width: 260px; font-family: Segoe UI;'>"
                + "<b style='font-size: 14px; color: " + toHexString(COLOR_TEXT_DARK) + ";'>¿Finalizar sesión actual?</b><br><br>"
                + "<span style='font-size: 12px; color: " + toHexString(COLOR_TEXT) + ";'>"
                + "Está a punto de salir del sistema de administración AdminNexus. "
                + "Asegúrese de haber guardado todos sus cambios.<br><br>"
                + "¿Desea continuar y volver a la pantalla de acceso seguro?</span>"
                + "</body></html>";

        Object[] options = {"Sí, salir", "Cancelar"};
        int opcion = JOptionPane.showOptionDialog(this,
                mensaje,
                "Cierre de Sesión de Seguridad",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);

        if (opcion == JOptionPane.YES_OPTION) {
            this.dispose();
            new Login().setVisible(true);
        }
    }

    // ================= HEADER =================

    private JPanel createMainContent() {
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(COLOR_BG);

        // Header Top
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_SIDEBAR);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)),
                new EmptyBorder(20, 30, 20, 30)));

        // Buscador
        RoundedPanel searchBar = new RoundedPanel(10, new Color(243, 244, 246));
        searchBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 2));
        searchBar.setPreferredSize(new Dimension(280, 28));

        JLabel searchIcon = new JLabel(new SearchIcon());

        JTextField searchField = new JTextField("Buscar registros...", 18);
        searchField.setOpaque(false);
        searchField.setBorder(null);
        searchField.setForeground(new Color(156, 163, 175));
        searchField.setFont(FONT_MAIN);

        JPopupMenu searchPopup = new JPopupMenu();
        searchPopup.setBackground(Color.WHITE);
        searchPopup.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));

        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Buscar registros...")) {
                    searchField.setText("");
                    searchField.setForeground(new Color(17, 24, 39));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(new Color(156, 163, 175));
                    searchField.setText("Buscar registros...");
                    searchPopup.setVisible(false);
                }
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                buscar();
            }

            public void removeUpdate(DocumentEvent e) {
                buscar();
            }

            public void changedUpdate(DocumentEvent e) {
                buscar();
            }

            private void buscar() {
                String query = searchField.getText();
                if (query.equals("Buscar registros...") || query.trim().length() < 2) {
                    searchPopup.setVisible(false);
                    return;
                }

                BusquedaController bc = new BusquedaController();
                ResultadoBusqueda resultados = bc.buscarGlobal(query, 4);
                mostrarResultadosBusqueda(searchField, searchPopup, resultados);
            }
        });

        searchBar.add(searchIcon);
        searchBar.add(searchField);

        // Wrapper para asegurar el centrado vertical perfecto sin estirarse
        JPanel searchWrapper = new JPanel(new GridBagLayout());
        searchWrapper.setBackground(COLOR_SIDEBAR);
        searchWrapper.add(searchBar);

        header.add(searchWrapper, BorderLayout.WEST);

        // Perfil Superior Derecho
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 5));
        rightHeader.setBackground(COLOR_SIDEBAR);

        ActividadController ac = new ActividadController();
        List<Actividad> recientes = ac.obtenerActividadesRecientes(1);
        boolean hayNotificaciones = false;
        if (!recientes.isEmpty()) {
            int ultimaActividadId = recientes.get(0).getIdActividad();
            int ultimaVistaId = prefs.getInt("ultima_actividad_vista_" + usuarioActual.getIdusuario(), -1);
            if (ultimaActividadId > ultimaVistaId) {
                hayNotificaciones = true;
            }
        }

        JLabel bellIcon = new JLabel(new BellIcon(hayNotificaciones));
        bellIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bellIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (System.currentTimeMillis() - lastNotificationCloseTime > 200) {
                    mostrarNotificaciones(bellIcon);
                    // Quitar el punto rojo una vez que se abren las notificaciones
                    bellIcon.setIcon(new BellIcon(false));
                    bellIcon.repaint();

                    // Guardar como leída en las preferencias del usuario local
                    ActividadController controlAct = new ActividadController();
                    List<Actividad> rec = controlAct.obtenerActividadesRecientes(1);
                    if (!rec.isEmpty()) {
                        prefs.putInt("ultima_actividad_vista_" + usuarioActual.getIdusuario(),
                                rec.get(0).getIdActividad());
                    }
                }
            }
        });

        // Contenedor del perfil (Nombre + Avatar)
        JPanel profileBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        profileBox.setBackground(COLOR_SIDEBAR);
        profileBox.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel profileText = new JLabel("<html><div style='text-align:right;'><b style='color:"
                + toHexString(COLOR_TEXT_DARK) + "; font-size:13px;'>" +
                usuarioActual.getNombreAdmin() +
                "</b><br><b style='color:" + toHexString(COLOR_TEXT_DARK) + "; font-size:11px;'>" +
                usuarioActual.getRol().getNombre() +
                "</b></div></html>");
        // Panel para el avatar y el botón de cámara (usamos JLayeredPane para
        // superponer)
        JLayeredPane layeredAvatar = new JLayeredPane();
        layeredAvatar.setPreferredSize(new Dimension(36, 42));

        topAvatar = new JLabel();
        actualizarIconoAvatar(topAvatar, 36,
                usuarioActual.esAdministrador() ? new Color(17, 24, 39) : new Color(59, 130, 246));
        topAvatar.setBounds(0, 0, 36, 36);

        // Botón de cámara redondeado
        JButton btnCamara = new JButton(new CameraIcon(Color.WHITE));
        btnCamara.setBounds(15, 20, 22, 22); // Aumentado un poco el tamaño
        btnCamara.setBackground(COLOR_PRIMARY);
        btnCamara.setFocusPainted(false);
        btnCamara.setBorderPainted(false);
        btnCamara.setContentAreaFilled(false); // Importante para que no pinte el fondo cuadrado
        btnCamara.setOpaque(false);
        btnCamara.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCamara.setToolTipText("Cargar imagen de perfil");

        // Hacer el botón circular perfecto
        btnCamara.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo circular
                g2.setColor(c.getBackground());
                g2.fillOval(0, 0, c.getWidth() - 1, c.getHeight() - 1);

                // Borde blanco sutil para destacar
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                g2.drawOval(0, 0, c.getWidth() - 1, c.getHeight() - 1);

                super.paint(g, c);
                g2.dispose();
            }
        });

        btnCamara.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Seleccionar Imagen de Perfil");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Imágenes (JPG, PNG, JPEG)", "jpg", "png", "jpeg"));
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    File destDir = new File("uploads/profiles");
                    if (!destDir.exists())
                        destDir.mkdirs();

                    File destFile = new File(destDir, "perfil_" + usuarioActual.getIdusuario() + ".png");
                    Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    prefs.put("foto_perfil_" + usuarioActual.getIdusuario(), destFile.getAbsolutePath());

                    actualizarIconoAvatar(topAvatar, 36, null);
                    actualizarIconoAvatar(sidebarAvatar, 36, null);

                    try {
                        prefs.flush(); // Forzar escritura en disco
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    JOptionPane.showMessageDialog(this, "Imagen de perfil actualizada correctamente", "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error al guardar la imagen: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        layeredAvatar.add(topAvatar, JLayeredPane.DEFAULT_LAYER);
        layeredAvatar.add(btnCamara, JLayeredPane.PALETTE_LAYER);

        profileBox.add(profileText);
        profileBox.add(layeredAvatar);

        // Popup Menu del Perfil
        JPopupMenu profilePopup = new JPopupMenu();
        profilePopup.setBackground(Color.WHITE);
        profilePopup.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));

        profilePopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                lastProfileCloseTime = System.currentTimeMillis();
            }

            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                lastProfileCloseTime = System.currentTimeMillis();
            }
        });

        JMenuItem itemAjustes = new JMenuItem("Ajustes de Perfil");
        itemAjustes.setIcon(new GearIcon(COLOR_TEXT));
        itemAjustes.setBackground(Color.WHITE);
        itemAjustes.setFont(FONT_MAIN);
        itemAjustes.addActionListener(e -> JOptionPane.showMessageDialog(this, "Ajustes de perfil en desarrollo",
                "Ajustes", JOptionPane.INFORMATION_MESSAGE));

        JMenuItem itemLogout = new JMenuItem("Cerrar Sesión");
        itemLogout.setIcon(new LogoutIcon(COLOR_TEXT_DANGER));
        itemLogout.setBackground(Color.WHITE);
        itemLogout.setForeground(COLOR_TEXT_DANGER);
        itemLogout.setFont(FONT_MAIN);
        itemLogout.addActionListener(e -> cerrarSesion());

        profilePopup.add(itemAjustes);
        profilePopup.addSeparator();
        profilePopup.add(itemLogout);

        profileBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (System.currentTimeMillis() - lastProfileCloseTime > 200) {
                    profilePopup.show(profileBox, profileBox.getWidth() - profilePopup.getPreferredSize().width,
                            profileBox.getHeight() + 10);
                }
            }
        });

        rightHeader.add(bellIcon);
        rightHeader.add(profileBox);

        header.add(rightHeader, BorderLayout.EAST);
        mainContent.add(header, BorderLayout.NORTH);

        // Panel de contenido dinámico
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(COLOR_BG);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Mostrar panel de inicio por defecto
        mostrarPanelInicio();

        mainContent.add(contentPanel, BorderLayout.CENTER);

        return mainContent;
    }

    private void actualizarIconoAvatar(JLabel label, int size, Color fallbackColor) {
        String path = prefs.get("foto_perfil_" + usuarioActual.getIdusuario(), null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                Image original = icon.getImage();
                
                // Escalar de forma síncrona usando BufferedImage
                java.awt.image.BufferedImage scaledImage = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = scaledImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.drawImage(original, 0, 0, size, size, null);
                g2d.dispose();
                
                label.setIcon(new CircleAvatar(scaledImage));
                return;
            }
        }

        if (fallbackColor != null) {
            label.setIcon(new CircleAvatar(fallbackColor));
        } else {
            // Color por defecto si no hay nada
            label.setIcon(new CircleAvatar(new Color(75, 85, 99)));
        }
    }

    /**
     * Muestra el popup de notificaciones con las actividades recientes
     */
    private void mostrarNotificaciones(Component invoker) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));

        popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                lastNotificationCloseTime = System.currentTimeMillis();
            }

            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                lastNotificationCloseTime = System.currentTimeMillis();
            }
        });

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(249, 250, 251));
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel("Notificaciones Recientes");
        title.setFont(FONT_BOLD);
        headerPanel.add(title, BorderLayout.WEST);

        JLabel lblLimpiar = new JLabel("Limpiar todo");
        lblLimpiar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLimpiar.setForeground(COLOR_PRIMARY);
        lblLimpiar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblLimpiar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ActividadController ac = new ActividadController();
                List<Actividad> rec = ac.obtenerActividadesRecientes(1);
                if (!rec.isEmpty()) {
                    prefs.putInt("ultima_actividad_limpiada_" + usuarioActual.getIdusuario(),
                            rec.get(0).getIdActividad());
                }
                popup.setVisible(false);
            }
        });
        headerPanel.add(lblLimpiar, BorderLayout.EAST);

        popup.add(headerPanel);
        popup.addSeparator();

        int ultimaLimpiada = prefs.getInt("ultima_actividad_limpiada_" + usuarioActual.getIdusuario(), -1);
        ActividadController actividadController = new ActividadController();
        List<Actividad> todasActividades = actividadController.obtenerActividadesRecientes(15);
        todasActividades.removeIf(a -> a.getIdActividad() <= ultimaLimpiada);

        List<Actividad> actividades = todasActividades.size() > 5 ? todasActividades.subList(0, 5) : todasActividades;

        if (actividades.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("No hay notificaciones recientes");
            emptyItem.setEnabled(false);
            emptyItem.setBackground(Color.WHITE);
            emptyItem.setFont(FONT_MAIN);
            popup.add(emptyItem);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (Actividad act : actividades) {
                JPanel itemPanel = new JPanel(new BorderLayout(10, 5));
                itemPanel.setBackground(Color.WHITE);
                itemPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

                JLabel lblDesc = new JLabel(
                        "<html><div style='width:250px;'>" + act.getDescripcion() + "</div></html>");
                lblDesc.setFont(FONT_MAIN);

                JLabel lblFecha = new JLabel(sdf.format(act.getFecha()));
                lblFecha.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                lblFecha.setForeground(COLOR_TEXT);

                itemPanel.add(lblDesc, BorderLayout.CENTER);
                itemPanel.add(lblFecha, BorderLayout.SOUTH);

                popup.add(itemPanel);
                popup.addSeparator();
            }
        }

        // Mostrar debajo de la campanita alineado a la derecha
        popup.show(invoker, invoker.getWidth() - popup.getPreferredSize().width, invoker.getHeight() + 10);
    }

    /**
     * Muestra el popup con los resultados de la búsqueda global
     */
    private void mostrarResultadosBusqueda(JTextField searchField, JPopupMenu popup, ResultadoBusqueda resultados) {
        popup.removeAll();

        if (resultados.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("No se encontraron resultados");
            emptyItem.setEnabled(false);
            emptyItem.setBackground(Color.WHITE);
            emptyItem.setFont(FONT_MAIN);
            popup.add(emptyItem);
        } else {
            // Sección Estudiantes
            if (!resultados.getEstudiantes().isEmpty()) {
                JLabel lblEstudiantes = new JLabel("  🎓 Estudiantes");
                lblEstudiantes.setFont(FONT_BOLD);
                lblEstudiantes.setForeground(COLOR_TEXT);
                popup.add(lblEstudiantes);
                popup.addSeparator();

                for (Estudiante est : resultados.getEstudiantes()) {
                    JMenuItem item = new JMenuItem("<html><b>" + est.getNombre() + " " + est.getApellido()
                            + "</b><br><span style='font-size:10px; color:#6B7280;'>" + est.getCodigo() + " - "
                            + est.getNombrePrograma() + "</span></html>");
                    item.setBackground(Color.WHITE);
                    item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    item.addActionListener(e -> {
                        searchField.setText("");
                        searchField.transferFocus();
                        abrirEstudiantes();
                        // Idealmente aquí podríamos abrir el diálogo de detalles directamente
                    });
                    popup.add(item);
                }
            }

            // Sección Programas
            if (!resultados.getProgramas().isEmpty()) {
                if (!resultados.getEstudiantes().isEmpty()) {
                    popup.addSeparator();
                }
                JLabel lblProgramas = new JLabel("  📚 Programas");
                lblProgramas.setFont(FONT_BOLD);
                lblProgramas.setForeground(COLOR_TEXT);
                popup.add(lblProgramas);
                popup.addSeparator();

                for (Programa prog : resultados.getProgramas()) {
                    JMenuItem item = new JMenuItem(
                            "<html><b>" + prog.getNombre() + "</b><br><span style='font-size:10px; color:#6B7280;'>"
                                    + prog.getCodigo() + "</span></html>");
                    item.setBackground(Color.WHITE);
                    item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    item.addActionListener(e -> {
                        searchField.setText("");
                        searchField.transferFocus();
                        abrirProgramas();
                    });
                    popup.add(item);
                }
            }
        }

        popup.pack();
        if (!popup.isVisible()) {
            popup.show(searchField, 0, searchField.getHeight() + 5);
        }
        searchField.requestFocusInWindow();
    }

    /**
     * Muestra el panel de inicio (bienvenida)
     */
    private void mostrarPanelInicio() {
        contentPanel.removeAll();

        JPanel inicioPanel = new JPanel(new GridBagLayout());
        inicioPanel.setBackground(COLOR_BG);
        inicioPanel.setName("inicio"); // Identificador para refresco dinámico

        String titleColor = toHexString(COLOR_TEXT_DARK);
        String subtitleColor = toHexString(COLOR_TEXT);

        JLabel bienvenida = new JLabel("<html><div style='text-align:center; font-family:Segoe UI;'>" +
                "<h1 style='color:" + titleColor
                + "; font-size:36px; margin-bottom:10px; font-weight:bold;'>Bienvenido, " +
                usuarioActual.getNombreAdmin() + "</h1>" +
                "<p style='color:" + subtitleColor
                + "; font-size:18px; font-weight:500;'>Sistema de Administración AdminNexus</p>" +
                "</div></html>");
        bienvenida.setHorizontalAlignment(SwingConstants.CENTER);

        inicioPanel.add(bienvenida);

        contentPanel.add(inicioPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Carga un panel en el área de contenido principal
     */
    public void cargarPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ================= CLASES DE DIBUJO PERSONALIZADO =================

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

    class MenuItemPanel extends JPanel {
        private boolean active;
        private boolean isHover = false;

        public MenuItemPanel(boolean active) {
            this.active = active;
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            if (!active) {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        isHover = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        isHover = false;
                        repaint();
                    }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(COLOR_SELECTION);
                g2.fillRoundRect(5, 2, getWidth() - 15, getHeight() - 4, 15, 15);
                g2.setColor(COLOR_PRIMARY);
                g2.fillRoundRect(getWidth() - 8, getHeight() / 2 - 10, 4, 20, 4, 4);
            } else if (isHover) {
                g2.setColor(prefs.get("app_mode", "light").equals("dark") ? new Color(255, 255, 255, 10)
                        : new Color(243, 244, 246));
                g2.fillRoundRect(5, 2, getWidth() - 15, getHeight() - 4, 15, 15);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ================= ICONOS VECTORIALES (Sustituyen los Emojis)
    // =================

    abstract class MenuIcon implements Icon {
        protected Color color;

        public MenuIcon(Color c) {
            this.color = c;
        }

        public void setColor(Color c) {
            this.color = c;
        }

        public int getIconWidth() {
            return 24;
        }

        public int getIconHeight() {
            return 24;
        }
    }

    class DashboardIcon extends MenuIcon {
        public DashboardIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x + 3, y + 3, 7, 7, 2, 2);
            g2.drawRoundRect(x + 14, y + 3, 7, 7, 2, 2);
            g2.drawRoundRect(x + 3, y + 14, 7, 7, 2, 2);
            g2.drawRoundRect(x + 14, y + 14, 7, 7, 2, 2);
            g2.dispose();
        }
    }

    class UsersIcon extends MenuIcon {
        public UsersIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 5, y + 4, 5, 5);
            g2.drawArc(x + 2, y + 12, 11, 9, 0, 180);
            g2.drawOval(x + 14, y + 6, 4, 4);
            g2.drawArc(x + 12, y + 13, 9, 7, 0, 180);
            g2.dispose();
        }
    }

    class PaymentIcon extends MenuIcon {
        public PaymentIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x + 2, y + 5, 20, 14, 3, 3);
            g2.drawOval(x + 9, y + 9, 6, 6);
            g2.drawLine(x + 6, y + 12, x + 6, y + 12);
            g2.drawLine(x + 18, y + 12, x + 18, y + 12);
            g2.dispose();
        }
    }

    class LayersIcon extends MenuIcon {
        public LayersIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p1 = new Path2D.Double();
            p1.moveTo(x + 12, y + 4);
            p1.lineTo(x + 21, y + 9);
            p1.lineTo(x + 12, y + 14);
            p1.lineTo(x + 3, y + 9);
            p1.closePath();
            g2.draw(p1);
            Path2D p2 = new Path2D.Double();
            p2.moveTo(x + 3, y + 13);
            p2.lineTo(x + 12, y + 18);
            p2.lineTo(x + 21, y + 13);
            g2.draw(p2);
            Path2D p3 = new Path2D.Double();
            p3.moveTo(x + 3, y + 17);
            p3.lineTo(x + 12, y + 22);
            p3.lineTo(x + 21, y + 17);
            g2.draw(p3);
            g2.dispose();
        }
    }

    class AcademicIcon extends MenuIcon {
        public AcademicIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            Path2D cap = new Path2D.Double();
            cap.moveTo(x + 12, y + 5);
            cap.lineTo(x + 22, y + 10);
            cap.lineTo(x + 12, y + 15);
            cap.lineTo(x + 2, y + 10);
            cap.closePath();
            g2.draw(cap);
            
            g2.drawLine(x + 6, y + 13, x + 6, y + 18);
            g2.drawLine(x + 18, y + 13, x + 18, y + 18);
            g2.drawArc(x + 6, y + 16, 12, 5, 0, -180);
            
            g2.drawLine(x + 22, y + 10, x + 22, y + 15);
            g2.fillOval(x + 21, y + 15, 2, 2);
            
            g2.dispose();
        }
    }

    class ChartIcon extends MenuIcon {
        public ChartIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x + 3, y + 3, 18, 18, 4, 4);
            g2.drawLine(x + 7, y + 17, x + 7, y + 11);
            g2.drawLine(x + 12, y + 17, x + 12, y + 6);
            g2.drawLine(x + 17, y + 17, x + 17, y + 14);
            g2.dispose();
        }
    }

    class GearIcon extends MenuIcon {
        public GearIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 7, y + 7, 10, 10);
            for (int i = 0; i < 8; i++) {
                g2.drawLine(x + 12, y + 3, x + 12, y + 5);
                g2.rotate(Math.PI / 4.0, x + 12, y + 12);
            }
            g2.dispose();
        }
    }

    class LogoutIcon extends MenuIcon {
        public LogoutIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 10, y + 12, x + 20, y + 12);
            g2.drawLine(x + 16, y + 8, x + 20, y + 12);
            g2.drawLine(x + 16, y + 16, x + 20, y + 12);
            Path2D p = new Path2D.Double();
            p.moveTo(x + 12, y + 4);
            p.lineTo(x + 5, y + 4);
            p.lineTo(x + 5, y + 20);
            p.lineTo(x + 12, y + 20);
            g2.draw(p);
            g2.dispose();
        }
    }

    class AdminIcon extends MenuIcon {
        public AdminIcon(Color c) {
            super(c);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Escudo
            Path2D shield = new Path2D.Double();
            shield.moveTo(x + 12, y + 3);
            shield.lineTo(x + 4, y + 6);
            shield.lineTo(x + 4, y + 13);
            shield.curveTo(x + 4, y + 18, x + 7, y + 21, x + 12, y + 22);
            shield.curveTo(x + 17, y + 21, x + 20, y + 18, x + 20, y + 13);
            shield.lineTo(x + 20, y + 6);
            shield.closePath();
            g2.draw(shield);

            // Usuario dentro del escudo
            g2.drawOval(x + 9, y + 9, 6, 6);
            g2.drawArc(x + 7, y + 16, 10, 6, 0, 180);

            g2.dispose();
        }
    }

    class SearchIcon implements Icon {
        public int getIconWidth() {
            return 20;
        }

        public int getIconHeight() {
            return 20;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(156, 163, 175));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 2, y + 2, 10, 10);
            g2.drawLine(x + 9, y + 9, x + 16, y + 16);
            g2.dispose();
        }
    }

    class BellIcon implements Icon {
        private boolean showDot;

        public BellIcon(boolean showDot) {
            this.showDot = showDot;
        }

        public int getIconWidth() {
            return 28;
        }

        public int getIconHeight() {
            return 28;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLOR_TEXT);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D bell = new Path2D.Double();
            bell.moveTo(x + 14, y + 5);
            bell.curveTo(x + 14, y + 5, x + 8, y + 7, x + 8, y + 14);
            bell.lineTo(x + 6, y + 20);
            bell.lineTo(x + 22, y + 20);
            bell.lineTo(x + 20, y + 14);
            bell.curveTo(x + 20, y + 7, x + 14, y + 5, x + 14, y + 5);
            g2.draw(bell);
            g2.drawArc(x + 12, y + 20, 4, 4, 0, -180);

            if (showDot) {
                g2.setColor(new Color(220, 38, 38));
                g2.fillOval(x + 16, y + 3, 8, 8);
                g2.setColor(COLOR_SIDEBAR);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x + 16, y + 3, 8, 8);
            }
            g2.dispose();
        }
    }

    class LogoIcon implements Icon {
        public int getIconWidth() {
            return 36;
        }

        public int getIconHeight() {
            return 36;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLOR_PRIMARY);
            g2.fillRoundRect(x, y, 36, 36, 12, 12);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D cap = new Path2D.Double();
            cap.moveTo(x + 18, y + 10);
            cap.lineTo(x + 28, y + 15);
            cap.lineTo(x + 18, y + 20);
            cap.lineTo(x + 8, y + 15);
            cap.closePath();
            g2.fill(cap);
            g2.drawPolyline(new int[] { x + 12, x + 12, x + 18, x + 24, x + 24 },
                    new int[] { y + 18, y + 24, y + 26, y + 24, y + 18 }, 5);
            g2.drawLine(x + 28, y + 15, x + 28, y + 22);
            g2.dispose();
        }
    }

    class CircleAvatar implements Icon {
        private Color color;
        private Image image;

        public CircleAvatar(Color c) {
            this.color = c;
        }

        public CircleAvatar(Image img) {
            this.image = img;
        }

        public int getIconWidth() {
            return 36;
        }

        public int getIconHeight() {
            return 36;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Recorte circular
            Shape originalClip = g2.getClip();
            Ellipse2D circle = new Ellipse2D.Double(x, y, 36, 36);
            g2.setClip(circle);

            if (image != null) {
                g2.drawImage(image, x, y, 36, 36, null);
            } else {
                g2.setColor(color);
                g2.fillOval(x, y, 36, 36);
            }

            g2.setClip(originalClip);
            g2.dispose();
        }
    }

    class CameraIcon implements Icon {
        private Color color;

        public CameraIcon(Color c) {
            this.color = c;
        }

        public int getIconWidth() {
            return 12;
        }

        public int getIconHeight() {
            return 12;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Cuerpo
            g2.drawRoundRect(x + 1, y + 3, 10, 7, 1, 1);
            // Lente
            g2.drawOval(x + 4, y + 4, 4, 4);
            // Visor
            g2.drawRect(x + 3, y + 2, 3, 1);
            g2.dispose();
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String capitalizeFully(String text) {
        if (text == null || text.isEmpty())
            return text;
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

}