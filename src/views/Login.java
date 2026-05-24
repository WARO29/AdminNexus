package views;

import controller.AutenticacionController;
import model.Usuario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public class Login extends JFrame {

    private final Color COLOR_BG = new Color(249, 250, 251); // Fondo gris muy claro
    private final Color COLOR_PRIMARY = new Color(0, 86, 179); // Azul oscuro del botón
    private final Color COLOR_PRIMARY_LIGHT = new Color(225, 238, 255); // Azul claro del logo
    private final Color COLOR_TEXT_DARK = new Color(30, 41, 59); // Texto oscuro principal
    private final Color COLOR_TEXT_MUTED = new Color(100, 116, 139); // Gris para labels y placeholders
    private final Color COLOR_BORDER = new Color(148, 163, 184); // Gris oscuro para la linea de input
    
    private final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 11);
    
    private JTextField campoUsuario;
    private JPasswordField campoContrasena;
    private AutenticacionController autenticacionController;

    public Login() {
        setTitle("AdminNexus - Iniciar Sesión");
        setSize(460, 660);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
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
        
        // Inicializar controlador de autenticación
        autenticacionController = new AutenticacionController();

        try {
            com.formdev.flatlaf.FlatIntelliJLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Contenedor principal que centra la tarjeta
        JPanel mainContainer = new JPanel(new GridBagLayout());
        mainContainer.setBackground(COLOR_BG);
        
        // Tarjeta de Login
        CardPanel loginCard = new CardPanel();
        loginCard.setLayout(new BorderLayout());
        loginCard.setPreferredSize(new Dimension(420, 560));
        
        // 1. Contenido principal de la tarjeta (Arriba)
        JPanel cardContent = new JPanel();
        cardContent.setLayout(new BoxLayout(cardContent, BoxLayout.Y_AXIS));
        cardContent.setOpaque(false);
        cardContent.setBorder(new EmptyBorder(40, 40, 20, 40));
        
        // 1.1 Logo y Título
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        headerPanel.setOpaque(false);
        
        JLabel logoIcon = new JLabel();
        try {
            java.net.URL url = getClass().getResource("/assets/adminnexus.png");
            Image originalLogo = (url != null) ? new ImageIcon(url).getImage() : new ImageIcon("src/assets/adminnexus.png").getImage();
            
            int targetWidth = 240;
            int imgWidth = originalLogo.getWidth(null);
            int imgHeight = originalLogo.getHeight(null);
            int targetHeight = (imgWidth > 0) ? (int) (((double) targetWidth / imgWidth) * imgHeight) : 120;
            
            java.awt.image.BufferedImage scaledLogo = new java.awt.image.BufferedImage(targetWidth, targetHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2dLogo = scaledLogo.createGraphics();
            g2dLogo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2dLogo.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2dLogo.drawImage(originalLogo, 0, 0, targetWidth, targetHeight, null);
            g2dLogo.dispose();
            logoIcon.setIcon(new ImageIcon(scaledLogo));
        } catch (Exception ex) {
            logoIcon.setIcon(new LogoShieldIcon()); // Fallback
        }
        
        headerPanel.add(logoIcon);
        
        JLabel subtitleText = new JLabel("SISTEMA DE ADMINISTRACIÓN");
        subtitleText.setFont(new Font("Segoe UI", Font.BOLD, 10));
        subtitleText.setForeground(COLOR_TEXT_MUTED);
        subtitleText.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        
        cardContent.add(headerPanel);
        cardContent.add(Box.createVerticalStrut(10));
        cardContent.add(subtitleText);
        cardContent.add(Box.createVerticalStrut(40));

        // 1.2 Input Usuario
        cardContent.add(createLabelWrap("NOMBRE DE USUARIO"));
        cardContent.add(Box.createVerticalStrut(15));
        JPanel panelUsuario = createInputField(new UserIcon(), "ej. jdoe_admin", false);
        cardContent.add(panelUsuario);
        cardContent.add(Box.createVerticalStrut(25));
        
        // 1.3 Input Contraseña
        cardContent.add(createLabelWrap("CONTRASEÑA"));
        cardContent.add(Box.createVerticalStrut(15));
        JPanel panelContrasena = createInputField(new LockIcon(), "••••••••", true);
        cardContent.add(panelContrasena);
        cardContent.add(Box.createVerticalStrut(25));
        
        // 1.4 Opciones (Recordarme y Olvido)
        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setOpaque(false);
        optionsPanel.setMaximumSize(new Dimension(340, 30));
        
        JCheckBox rememberMe = new JCheckBox("Recordarme");
        rememberMe.setFont(FONT_REGULAR);
        rememberMe.setForeground(COLOR_TEXT_DARK);
        rememberMe.setOpaque(false);
        rememberMe.setFocusPainted(false);
        
        JLabel forgotPass = new JLabel("¿Olvidó su contraseña?");
        forgotPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        forgotPass.setForeground(COLOR_PRIMARY);
        forgotPass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        optionsPanel.add(rememberMe, BorderLayout.WEST);
        optionsPanel.add(forgotPass, BorderLayout.EAST);
        
        cardContent.add(optionsPanel);
        cardContent.add(Box.createVerticalStrut(30));
        
        // 1.5 Botón Ingresar
        JButton loginBtn = new RoundedButton("Ingresar al Sistema");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(340, 48));
        
        // Agregar evento para autenticar y abrir el Dashboard
        loginBtn.addActionListener(e -> {
            autenticarUsuario();
        });
        
        // Permitir login con Enter en los campos
        campoUsuario.addActionListener(e -> autenticarUsuario());
        campoContrasena.addActionListener(e -> autenticarUsuario());
        
        cardContent.add(loginBtn);

        // 2. Footer de la tarjeta (Fondo gris)
        JPanel cardFooter = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 20));
        cardFooter.setBackground(new Color(244, 245, 247));
        cardFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));
        
        JLabel shieldIcon = new JLabel(new SmallShieldIcon());
        JLabel footerText = new JLabel("ACCESO RESTRINGIDO A PERSONAL AUTORIZADO");
        footerText.setFont(new Font("Segoe UI", Font.BOLD, 10));
        footerText.setForeground(COLOR_TEXT_MUTED);
        
        cardFooter.add(shieldIcon);
        cardFooter.add(footerText);

        loginCard.add(cardContent, BorderLayout.CENTER);
        loginCard.add(cardFooter, BorderLayout.SOUTH);
        
        mainContainer.add(loginCard);
        
        // Footer General de la página (Versión y Enlaces)
        JPanel pageFooter = new JPanel(new BorderLayout());
        pageFooter.setOpaque(false);
        pageFooter.setBorder(new EmptyBorder(0, 20, 15, 20));
        
        JLabel versionText = new JLabel("v2.4.0-stable");
        versionText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        versionText.setForeground(new Color(156, 163, 175));
        
        JPanel linksPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        linksPanel.setOpaque(false);
        JLabel privText = new JLabel("Privacidad");
        JLabel sopText = new JLabel("Soporte");
        privText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        privText.setForeground(new Color(156, 163, 175));
        sopText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sopText.setForeground(new Color(156, 163, 175));
        
        linksPanel.add(privText);
        linksPanel.add(sopText);
        
        pageFooter.add(versionText, BorderLayout.WEST);
        pageFooter.add(linksPanel, BorderLayout.EAST);

        add(mainContainer, BorderLayout.CENTER);
        add(pageFooter, BorderLayout.SOUTH);
    }
    
    // Modificamos createLabel para que retorne el Wrapper y se alinee a la izquierda
    private JPanel createLabelWrap(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(COLOR_TEXT_MUTED);
        
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(340, 20));
        wrap.add(label);
        return wrap;
    }

    private JPanel createInputField(Icon icon, String placeholder, boolean isPassword) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(340, 40));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER)); // Linea inferior

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(new EmptyBorder(0, 5, 5, 0));
        panel.add(iconLabel, BorderLayout.WEST);

        JTextField field;
        if (isPassword) {
            field = new JPasswordField(20);
            campoContrasena = (JPasswordField) field;
        } else {
            field = new JTextField(20);
            campoUsuario = field;
        }
        
        field.setText(placeholder);
        field.setForeground(new Color(203, 213, 225)); // Color placeholder
        field.setFont(FONT_REGULAR);
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(0, 0, 5, 0));

        // Evento para borrar placeholder
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(COLOR_TEXT_DARK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(203, 213, 225));
                }
            }
        });

        panel.add(field, BorderLayout.CENTER);
        
        if (isPassword) {
            JLabel eyeIcon = new JLabel(new EyeIcon());
            eyeIcon.setBorder(new EmptyBorder(0, 0, 5, 5));
            eyeIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JPasswordField pwdField = (JPasswordField) field;
            final char defaultEchoChar = pwdField.getEchoChar();
            
            eyeIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                private boolean isVisible = false;
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    isVisible = !isVisible;
                    if (isVisible) {
                        pwdField.setEchoChar((char) 0); // Mostrar texto
                    } else {
                        pwdField.setEchoChar(defaultEchoChar != 0 ? defaultEchoChar : '•'); // Ocultar texto
                    }
                }
            });
            
            panel.add(eyeIcon, BorderLayout.EAST);
        }

        return panel;
    }
    
    /**
     * Autentica al usuario con las credenciales ingresadas.
     * Captura errores de conexión a la base de datos y los muestra al usuario.
     */
    private void autenticarUsuario() {
        String nombreUsuario = campoUsuario.getText().trim();
        String contrasena = new String(campoContrasena.getPassword());
        
        // Validar que no estén vacíos o con placeholder
        if (nombreUsuario.isEmpty() || nombreUsuario.equals("ej. jdoe_admin")) {
            JOptionPane.showMessageDialog(this, 
                "Por favor ingrese su nombre de usuario", 
                "Campo requerido", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (contrasena.isEmpty() || contrasena.equals("••••••••")) {
            JOptionPane.showMessageDialog(this, 
                "Por favor ingrese su contraseña", 
                "Campo requerido", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Verificar si existen usuarios en la base de datos
            if (!autenticacionController.existenUsuarios()) {
                JOptionPane.showMessageDialog(this, 
                    "No hay usuarios registrados en el sistema.\n\n" +
                    "Por favor, contacte al administrador para crear usuarios\n" +
                    "directamente en la base de datos.", 
                    "Sin usuarios registrados", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Autenticar
            Usuario usuario = autenticacionController.autenticar(nombreUsuario, contrasena);
            
            if (usuario != null) {
                // Autenticación exitosa
                this.dispose();
                new Dashboard(usuario).setVisible(true);
            } else {
                // Credenciales incorrectas
                JOptionPane.showMessageDialog(this, 
                    "Usuario o contraseña incorrectos", 
                    "Error de autenticación", 
                    JOptionPane.ERROR_MESSAGE);
                campoContrasena.setText("");
            }
            
        } catch (RuntimeException e) {
            // Error de conexión a la base de datos u otro error inesperado
            System.err.println("Error de conexión durante la autenticación: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "⚠  Error de conexión a la base de datos\n\n" +
                "No se pudo establecer comunicación con el servidor de base de datos.\n" +
                "Verifique que MySQL esté en ejecución y que la configuración sea correcta.\n\n" +
                "Detalle técnico:\n" + e.getMessage(),
                "Error de conexión",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= CLASES UI PERSONALIZADAS =================

    // Tarjeta con sombra suave (simulada con bordes)
    class CardPanel extends JPanel {
        public CardPanel() {
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Sombra (simulada simple)
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fillRoundRect(3, 3, getWidth()-6, getHeight()-6, 12, 12);
            g2.setColor(new Color(0, 0, 0, 5));
            g2.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
            
            // Fondo Blanco
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10);
            
            // Clip para que el footer no se salga de los bordes redondeados
            Shape clip = new RoundRectangle2D.Float(2, 2, getWidth()-4, getHeight()-4, 10, 10);
            g2.clip(clip);
            
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Botón azul redondeado con ícono
    class RoundedButton extends JButton {
        public RoundedButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (getModel().isPressed()) {
                g2.setColor(new Color(0, 66, 140)); // Azul más oscuro
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(0, 96, 190)); // Azul hover
            } else {
                g2.setColor(COLOR_PRIMARY); // Azul normal
            }
            
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            
            // Dibujar texto e icono manualmente para centrarlos
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(getText());
            int textHeight = fm.getAscent();
            
            int totalWidth = textWidth + 30; // 30 es el espacio + icono
            int startX = (getWidth() - totalWidth) / 2;
            int textY = (getHeight() + textHeight) / 2 - 3;
            
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), startX, textY);
            
            // Dibujar icono (flecha y puerta)
            int iconX = startX + textWidth + 10;
            int iconY = (getHeight() - 16) / 2;
            
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Flecha
            g2.drawLine(iconX, iconY + 8, iconX + 8, iconY + 8);
            g2.drawLine(iconX + 4, iconY + 4, iconX + 8, iconY + 8);
            g2.drawLine(iconX + 4, iconY + 12, iconX + 8, iconY + 8);
            // Puerta
            g2.drawLine(iconX + 8, iconY, iconX + 14, iconY);
            g2.drawLine(iconX + 14, iconY, iconX + 14, iconY + 16);
            g2.drawLine(iconX + 8, iconY + 16, iconX + 14, iconY + 16);

            g2.dispose();
        }
    }

    // ================= ICONOS VECTORIALES =================

    class LogoShieldIcon implements Icon {
        public int getIconWidth() { return 54; }
        public int getIconHeight() { return 54; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Caja Azul Clara
            g2.setColor(COLOR_PRIMARY_LIGHT);
            g2.fillRoundRect(x, y, 54, 54, 16, 16);
            
            // Escudo Azul Oscuro
            g2.setColor(COLOR_PRIMARY);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            Path2D shield = new Path2D.Double();
            shield.moveTo(x + 27, y + 14);
            shield.lineTo(x + 16, y + 17);
            shield.lineTo(x + 16, y + 28);
            shield.curveTo(x + 16, y + 36, x + 21, y + 42, x + 27, y + 44);
            shield.curveTo(x + 33, y + 42, x + 38, y + 36, x + 38, y + 28);
            shield.lineTo(x + 38, y + 17);
            shield.closePath();
            g2.draw(shield);
            
            // Cerradura (Candado pequeño) dentro del escudo
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(x + 23, y + 26, 8, 6, 2, 2);
            g2.drawArc(x + 24, y + 22, 6, 6, 0, 180);
            g2.drawLine(x + 27, y + 28, x + 27, y + 30);
            
            g2.dispose();
        }
    }

    class UserIcon implements Icon {
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(156, 163, 175)); // Gris claro
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            g2.drawOval(x + 6, y + 2, 8, 8);
            g2.drawArc(x + 2, y + 13, 16, 10, 0, 180);
            g2.drawLine(x + 2, y + 18, x + 18, y + 18); // Base plana
            
            g2.dispose();
        }
    }

    class LockIcon implements Icon {
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(156, 163, 175));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            g2.drawRoundRect(x + 3, y + 8, 14, 10, 3, 3);
            g2.drawArc(x + 6, y + 2, 8, 12, 0, 180);
            g2.drawLine(x + 10, y + 12, x + 10, y + 14); // Ojo cerradura
            
            g2.dispose();
        }
    }

    class EyeIcon implements Icon {
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(156, 163, 175));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            Path2D eye = new Path2D.Double();
            eye.moveTo(x, y + 10);
            eye.curveTo(x, y + 10, x + 5, y + 3, x + 10, y + 3);
            eye.curveTo(x + 15, y + 3, x + 20, y + 10, x + 20, y + 10);
            eye.curveTo(x + 20, y + 10, x + 15, y + 17, x + 10, y + 17);
            eye.curveTo(x + 5, y + 17, x, y + 10, x, y + 10);
            g2.draw(eye);
            
            g2.drawOval(x + 7, y + 7, 6, 6);
            
            g2.dispose();
        }
    }

    class SmallShieldIcon implements Icon {
        public int getIconWidth() { return 14; }
        public int getIconHeight() { return 14; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(new Color(185, 28, 28)); // Rojo escudo
            Path2D shield = new Path2D.Double();
            shield.moveTo(x + 7, y);
            shield.lineTo(x, y + 2);
            shield.lineTo(x, y + 8);
            shield.curveTo(x, y + 11, x + 3, y + 13, x + 7, y + 14);
            shield.curveTo(x + 11, y + 13, x + 14, y + 11, x + 14, y + 8);
            shield.lineTo(x + 14, y + 2);
            shield.closePath();
            g2.fill(shield);
            
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 6, y + 4, 2, 4);
            g2.fillRect(x + 6, y + 9, 2, 2);
            
            g2.dispose();
        }
    }

}