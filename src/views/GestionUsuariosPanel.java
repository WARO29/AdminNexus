package views;

import controller.AutenticacionController;
import model.Usuario;
import model.Usuario.RolUsuario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.prefs.Preferences;

public class GestionUsuariosPanel extends JPanel {
    
    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG = new Color(249, 250, 251);
    private Color COLOR_CARD_BG = Color.WHITE;
    private Color COLOR_TEXT_DARK = new Color(17, 24, 39);
    private Color COLOR_BORDER = new Color(229, 231, 235);
    private Color COLOR_TABLE_HEADER = new Color(243, 244, 246);
    private Color COLOR_SELECTION = new Color(239, 246, 255);
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    
    private final Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    
    private Usuario usuarioActual;
    private AutenticacionController autenticacionController;
    private DefaultTableModel modeloTabla;
    private JTable tablaUsuarios;
    
    public GestionUsuariosPanel(Usuario usuario) {
        this.usuarioActual = usuario;
        this.autenticacionController = new AutenticacionController();
        
        cargarConfiguracion();
        
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);
        
        add(crearHeader(), BorderLayout.NORTH);
        add(crearContenidoPrincipal(), BorderLayout.CENTER);
        
        cargarUsuarios();
    }
    
    private void cargarConfiguracion() {
        Preferences prefs = Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuarioActual.getIdusuario());
        String mode = prefs.get("app_mode", "light");
        
        if (mode.equals("dark")) {
            COLOR_BG = new Color(17, 24, 39);
            COLOR_CARD_BG = new Color(31, 41, 55);
            COLOR_TEXT_DARK = Color.WHITE;
            COLOR_BORDER = new Color(55, 65, 81);
            COLOR_TABLE_HEADER = new Color(31, 41, 55);
            COLOR_SELECTION = new Color(55, 65, 81);
            COLOR_TEXT_MUTED = new Color(156, 163, 175);
        }
        
        String colorTheme = prefs.get("app_color", "blue");
        switch (colorTheme) {
            case "green": COLOR_PRIMARY = new Color(16, 185, 129); break;
            case "gray": COLOR_PRIMARY = new Color(75, 85, 99); break;
            case "burgundy": COLOR_PRIMARY = new Color(153, 27, 27); break;
            default: COLOR_PRIMARY = new Color(26, 86, 219); break;
        }
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_CARD_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
            new EmptyBorder(20, 30, 20, 30)
        ));
        
        JLabel titulo = new JLabel("Gestión de Usuarios");
        titulo.setFont(FONT_TITLE);
        titulo.setForeground(COLOR_TEXT_DARK);
        
        JButton btnNuevoUsuario = new JButton("+ Nuevo Usuario");
        btnNuevoUsuario.setFont(FONT_BOLD);
        btnNuevoUsuario.setForeground(Color.WHITE);
        btnNuevoUsuario.setBackground(COLOR_PRIMARY);
        btnNuevoUsuario.setFocusPainted(false);
        btnNuevoUsuario.setBorderPainted(false);
        btnNuevoUsuario.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNuevoUsuario.setPreferredSize(new Dimension(150, 40));
        btnNuevoUsuario.addActionListener(e -> mostrarDialogoNuevoUsuario());
        
        header.add(titulo, BorderLayout.WEST);
        header.add(btnNuevoUsuario, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel crearContenidoPrincipal() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 30, 30, 30));
        
        // Tabla de usuarios
        String[] columnas = {"ID", "Nombre Completo", "Usuario", "Rol"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tablaUsuarios = new JTable(modeloTabla);
        
        // Estilos modernos (FlatLaf inspired)
        tablaUsuarios.setRowHeight(45);
        tablaUsuarios.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaUsuarios.setBackground(COLOR_CARD_BG);
        tablaUsuarios.setForeground(COLOR_TEXT_DARK);
        tablaUsuarios.setSelectionBackground(COLOR_SELECTION);
        tablaUsuarios.setSelectionForeground(COLOR_TEXT_DARK);
        
        tablaUsuarios.setShowVerticalLines(false);
        tablaUsuarios.setShowHorizontalLines(true);
        tablaUsuarios.setGridColor(COLOR_BORDER);
        tablaUsuarios.setIntercellSpacing(new Dimension(0, 1));
        tablaUsuarios.setFillsViewportHeight(true);
        tablaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Header con estilo moderno
        tablaUsuarios.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaUsuarios.getTableHeader().setBackground(COLOR_BG);
        tablaUsuarios.getTableHeader().setForeground(COLOR_TEXT_MUTED);
        tablaUsuarios.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tablaUsuarios.getTableHeader().setReorderingAllowed(false);
        tablaUsuarios.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_BORDER));
        ((javax.swing.table.DefaultTableCellRenderer)tablaUsuarios.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        
        // Renderers y padding
        javax.swing.table.DefaultTableCellRenderer dataRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setBackground(isSelected ? COLOR_SELECTION : COLOR_CARD_BG);
                c.setForeground(COLOR_TEXT_DARK);
                setBorder(new EmptyBorder(0, 15, 0, 15));
                return c;
            }
        };
        dataRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        for (int i = 0; i < tablaUsuarios.getColumnCount(); i++) {
            tablaUsuarios.getColumnModel().getColumn(i).setCellRenderer(dataRenderer);
        }
        
        // Configuración de anchos
        tablaUsuarios.getColumnModel().getColumn(0).setPreferredWidth(50);
        tablaUsuarios.getColumnModel().getColumn(1).setPreferredWidth(250);
        tablaUsuarios.getColumnModel().getColumn(2).setPreferredWidth(150);
        tablaUsuarios.getColumnModel().getColumn(3).setPreferredWidth(150);
        
        JScrollPane scrollPane = new JScrollPane(tablaUsuarios);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(COLOR_CARD_BG);
        
        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelBotones.setBackground(COLOR_BG);
        
        JButton btnEditar = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnActualizar = new JButton("Actualizar");
        
        estilizarBoton(btnEditar, new Color(59, 130, 246));
        estilizarBoton(btnEliminar, new Color(239, 68, 68));
        estilizarBoton(btnActualizar, new Color(34, 197, 94));
        
        btnEditar.addActionListener(e -> editarUsuarioSeleccionado());
        btnEliminar.addActionListener(e -> eliminarUsuarioSeleccionado());
        btnActualizar.addActionListener(e -> cargarUsuarios());
        
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void estilizarBoton(JButton boton, Color color) {
        boton.setFont(FONT_BOLD);
        boton.setForeground(Color.WHITE);
        boton.setBackground(color);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(120, 35));
    }
    
    private void cargarUsuarios() {
        modeloTabla.setRowCount(0);
        List<Usuario> usuarios = autenticacionController.obtenerTodosLosUsuarios();
        
        for (Usuario usuario : usuarios) {
            Object[] fila = {
                usuario.getIdusuario(),
                usuario.getNombreAdmin(),
                usuario.getUser(),
                usuario.getRol().getNombre()
            };
            modeloTabla.addRow(fila);
        }
    }
    
    private void mostrarDialogoNuevoUsuario() {
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialogo = new JDialog(window, "Nuevo Usuario", Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setSize(450, 400);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Campos del formulario
        JTextField txtNombreAdmin = new JTextField(20);
        JTextField txtUser = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        JComboBox<String> comboRol = new JComboBox<>(new String[]{"usuario", "administrador"});
        
        // Agregar componentes
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nombre Completo:"), gbc);
        gbc.gridx = 1;
        panel.add(txtNombreAdmin, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Nombre de Usuario:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUser, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Rol:"), gbc);
        gbc.gridx = 1;
        panel.add(comboRol, gbc);
        
        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        
        estilizarBoton(btnGuardar, COLOR_PRIMARY);
        estilizarBoton(btnCancelar, new Color(107, 114, 128));
        
        btnGuardar.addActionListener(e -> {
            if (validarCampos(txtNombreAdmin, txtUser, txtPassword)) {
                Usuario nuevoUsuario = new Usuario();
                nuevoUsuario.setNombreAdmin(txtNombreAdmin.getText().trim());
                nuevoUsuario.setUser(txtUser.getText().trim());
                nuevoUsuario.setPassword(new String(txtPassword.getPassword()));
                String rolSeleccionado = (String) comboRol.getSelectedItem();
                nuevoUsuario.setRol(rolSeleccionado.equals("administrador") ? RolUsuario.ADMINISTRADOR : RolUsuario.USUARIO);
                
                if (autenticacionController.crearUsuario(nuevoUsuario)) {
                    JOptionPane.showMessageDialog(dialogo, 
                        "Usuario creado exitosamente", 
                        "Éxito", 
                        JOptionPane.INFORMATION_MESSAGE);
                    cargarUsuarios();
                    dialogo.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialogo, 
                        "Error al crear usuario. El nombre de usuario puede estar en uso.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        btnCancelar.addActionListener(e -> dialogo.dispose());
        
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        
        dialogo.add(panel, BorderLayout.CENTER);
        dialogo.add(panelBotones, BorderLayout.SOUTH);
        dialogo.setVisible(true);
    }
    
    private void editarUsuarioSeleccionado() {
        int filaSeleccionada = tablaUsuarios.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, 
                "Por favor seleccione un usuario para editar", 
                "Advertencia", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int idUsuario = (int) modeloTabla.getValueAt(filaSeleccionada, 0);
        String nombreAdmin = (String) modeloTabla.getValueAt(filaSeleccionada, 1);
        String user = (String) modeloTabla.getValueAt(filaSeleccionada, 2);
        String rol = (String) modeloTabla.getValueAt(filaSeleccionada, 3);
        
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialogo = new JDialog(window, "Editar Usuario", Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setSize(450, 400);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField txtNombreAdmin = new JTextField(nombreAdmin, 20);
        JTextField txtUser = new JTextField(user, 20);
        JPasswordField txtPassword = new JPasswordField(20);
        JComboBox<String> comboRol = new JComboBox<>(new String[]{"usuario", "administrador"});
        comboRol.setSelectedItem(rol.equals("Administrador") ? "administrador" : "usuario");
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nombre Completo:"), gbc);
        gbc.gridx = 1;
        panel.add(txtNombreAdmin, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Nombre de Usuario:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUser, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Nueva Contraseña (obligatorio):"), gbc);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Rol:"), gbc);
        gbc.gridx = 1;
        panel.add(comboRol, gbc);
        
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        
        estilizarBoton(btnGuardar, COLOR_PRIMARY);
        estilizarBoton(btnCancelar, new Color(107, 114, 128));
        
        btnGuardar.addActionListener(e -> {
            if (txtNombreAdmin.getText().trim().isEmpty() || txtUser.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialogo, 
                    "Por favor complete todos los campos obligatorios", 
                    "Campos incompletos", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String nuevaPassword = new String(txtPassword.getPassword());
            if (nuevaPassword.isEmpty()) {
                JOptionPane.showMessageDialog(dialogo, 
                    "Por favor ingrese la contraseña", 
                    "Campo requerido", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Usuario usuarioEditado = new Usuario();
            usuarioEditado.setIdusuario(idUsuario);
            usuarioEditado.setNombreAdmin(txtNombreAdmin.getText().trim());
            usuarioEditado.setUser(txtUser.getText().trim());
            usuarioEditado.setPassword(nuevaPassword);
            String rolSeleccionado = (String) comboRol.getSelectedItem();
            usuarioEditado.setRol(rolSeleccionado.equals("administrador") ? RolUsuario.ADMINISTRADOR : RolUsuario.USUARIO);
            
            if (autenticacionController.actualizarUsuario(usuarioEditado)) {
                JOptionPane.showMessageDialog(dialogo, 
                    "Usuario actualizado exitosamente", 
                    "Éxito", 
                    JOptionPane.INFORMATION_MESSAGE);
                cargarUsuarios();
                dialogo.dispose();
            } else {
                JOptionPane.showMessageDialog(dialogo, 
                    "Error al actualizar usuario", 
                    "Error", 
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
    
    private void eliminarUsuarioSeleccionado() {
        int filaSeleccionada = tablaUsuarios.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, 
                "Por favor seleccione un usuario para eliminar", 
                "Advertencia", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int idUsuario = (int) modeloTabla.getValueAt(filaSeleccionada, 0);
        String user = (String) modeloTabla.getValueAt(filaSeleccionada, 2);
        
        // No permitir eliminar el usuario actual
        if (idUsuario == usuarioActual.getIdusuario()) {
            JOptionPane.showMessageDialog(this, 
                "No puede eliminar su propio usuario", 
                "Operación no permitida", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirmacion = JOptionPane.showConfirmDialog(this, 
            "¿Está seguro que desea eliminar al usuario '" + user + "'?", 
            "Confirmar eliminación", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.WARNING_MESSAGE);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            if (autenticacionController.eliminarUsuario(idUsuario)) {
                JOptionPane.showMessageDialog(this, 
                    "Usuario eliminado exitosamente", 
                    "Éxito", 
                    JOptionPane.INFORMATION_MESSAGE);
                cargarUsuarios();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Error al eliminar usuario", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private boolean validarCampos(JTextField txtNombreAdmin, JTextField txtUser, JPasswordField txtPassword) {
        if (txtNombreAdmin.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "El nombre completo es obligatorio", 
                "Campo requerido", 
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        if (txtUser.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "El nombre de usuario es obligatorio", 
                "Campo requerido", 
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        if (txtPassword.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, 
                "La contraseña es obligatoria", 
                "Campo requerido", 
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        return true;
    }
}
