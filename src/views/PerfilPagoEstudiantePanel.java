package views;

import controller.PagosController;
import controller.PagoService;
import model.Usuario;
import model.PagoRealizado;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vista de perfil de pago tipo red social.
 */
public class PerfilPagoEstudiantePanel extends JPanel {

    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BG = new Color(243, 244, 246);
    private Color COLOR_CARD = Color.WHITE;
    private Color COLOR_TEXT = new Color(17, 24, 39);
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);
    private Color COLOR_BORDER = new Color(229, 231, 235);
    
    private Font FONT_NAME = new Font("Segoe UI", Font.BOLD, 28);
    private Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 16);
    private Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 14);
    private Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    private Usuario usuarioActual;
    private int idEstudiante;
    private PagosController pagosController;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd 'de' MMMM, yyyy", new Locale("es", "ES"));

    public PerfilPagoEstudiantePanel(Usuario usuario, int idEstudiante) {
        this.usuarioActual = usuario;
        this.idEstudiante = idEstudiante;
        this.pagosController = new PagosController();

        cargarConfiguracion();
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        // Fetch Data
        Map<String, Object> data = pagosController.obtenerDetalleFinancieroEstudiante(idEstudiante);
        if (data == null) {
            add(new JLabel("No se encontró información del estudiante"), BorderLayout.CENTER);
            return;
        }

        // Main Scrollable Area
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        mainContent.add(crearHeaderSocial(data));
        mainContent.add(Box.createVerticalStrut(20));
        mainContent.add(crearCuerpoSocial(data));

        JScrollPane scroll = new JScrollPane(mainContent);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private void cargarConfiguracion() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuarioActual.getIdusuario());
        if (prefs.get("app_mode", "light").equals("dark")) {
            COLOR_BG = new Color(17, 24, 39);
            COLOR_CARD = new Color(31, 41, 55);
            COLOR_TEXT = Color.WHITE;
            COLOR_TEXT_MUTED = new Color(156, 163, 175);
            COLOR_BORDER = new Color(55, 65, 81);
        }
    }

    private JPanel crearHeaderSocial(Map<String, Object> data) {
        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(0, 350));
        header.setBackground(COLOR_CARD);

        // Portada (Cover Image Gradient)
        JPanel cover = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, COLOR_PRIMARY, getWidth(), 0, COLOR_PRIMARY.darker());
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        cover.setBounds(0, 0, 2000, 200);

        // Avatar Circular
        JLabel avatar = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(229, 231, 235));
                g2.setStroke(new BasicStroke(4));
                g2.drawOval(2, 2, getWidth()-4, getHeight()-4);
                
                // Iniciales o icono
                g2.setColor(COLOR_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 60));
                String nombre = data.get("nombre") != null ? data.get("nombre").toString() : "E";
                String inicial = nombre.isEmpty() ? "E" : nombre.substring(0, 1);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(inicial, (getWidth()-fm.stringWidth(inicial))/2, (getHeight()+fm.getAscent())/2 - 10);
                g2.dispose();
            }
        };
        avatar.setBounds(50, 120, 160, 160);

        // Nombre e Info
        JLabel name = new JLabel(data.get("nombre") != null ? data.get("nombre").toString() : "Estudiante");
        name.setFont(FONT_NAME);
        name.setForeground(COLOR_TEXT);
        name.setBounds(230, 210, 600, 40);

        JLabel subInfo = new JLabel(data.get("programa") + " • Código: " + data.get("codigo"));
        subInfo.setFont(FONT_MAIN);
        subInfo.setForeground(COLOR_TEXT_MUTED);
        subInfo.setBounds(230, 250, 600, 20);

        // Botón Atrás
        JButton btnBack = new JButton("← Volver a gestión de pago");
        btnBack.setBounds(30, 30, 220, 30);
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof Dashboard) ((Dashboard) w).abrirPagos();
        });

        header.add(btnBack);
        header.add(avatar);
        header.add(name);
        header.add(subInfo);
        header.add(cover);

        return header;
    }

    private JPanel crearCuerpoSocial(Map<String, Object> data) {
        JPanel body = new JPanel(new BorderLayout(30, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 50, 50, 50));

        // Columna Izquierda: Estadísticas (Widgets)
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setPreferredSize(new Dimension(350, 0));
        leftCol.setOpaque(false);

        leftCol.add(crearWidgetInfo("Resumen Financiero", data));
        leftCol.add(Box.createVerticalStrut(20));
        leftCol.add(crearWidgetAcciones(data));

        // Columna Derecha: Timeline (Muro de pagos)
        JPanel rightCol = new JPanel(new BorderLayout());
        rightCol.setOpaque(false);
        
        JLabel lblTimeline = new JLabel("Historial de Transacciones");
        lblTimeline.setFont(FONT_SECTION);
        lblTimeline.setBorder(new EmptyBorder(0, 0, 15, 0));
        lblTimeline.setForeground(COLOR_TEXT);
        
        JPanel timelineContainer = new JPanel();
        timelineContainer.setLayout(new BoxLayout(timelineContainer, BoxLayout.Y_AXIS));
        timelineContainer.setOpaque(false);

        List<Map<String, Object>> historial = pagosController.obtenerHistorialEstudiante(idEstudiante);
        if (historial.isEmpty()) {
            timelineContainer.add(new JLabel("No hay pagos registrados aún."));
        } else {
            for (Map<String, Object> pago : historial) {
                timelineContainer.add(crearTimelineItem(pago));
                timelineContainer.add(Box.createVerticalStrut(15));
            }
        }

        rightCol.add(lblTimeline, BorderLayout.NORTH);
        rightCol.add(timelineContainer, BorderLayout.CENTER);

        body.add(leftCol, BorderLayout.WEST);
        body.add(rightCol, BorderLayout.CENTER);

        return body;
    }

    private JPanel crearWidgetInfo(String title, Map<String, Object> data) {
        JPanel widget = new JPanel(new GridLayout(0, 1, 0, 10));
        widget.setBackground(COLOR_CARD);
        widget.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_SECTION);
        lblTitle.setForeground(COLOR_TEXT);
        widget.add(lblTitle);

        Object montoTotal = data.get("monto_total");
        Object saldo = data.get("saldo");
        Object estado = data.get("estado");
        Object cuotasPagadas = data.get("cuotas_pagadas");
        Object cuotasTotales = data.get("cuotas_totales");
        Object proximoPago = data.get("proximo_pago");
        Object ultimoPago = data.get("ultimo_pago");
        
        double total = montoTotal != null ? (double) montoTotal : 0;
        double pendiente = saldo != null ? (double) saldo : 0;
        double pagado = total - pendiente;

        String estadoStr = estado != null ? estado.toString() : "SIN PLAN";
        String proximoStr = proximoPago != null ? dateFormat.format(proximoPago) : "N/A";

        if (pendiente <= 0.001) {
            proximoStr = "No procede";
        }

        widget.add(crearInfoRow("Monto Total:", currencyFormat.format(total)));
        widget.add(crearInfoRow("Total Pagado:", currencyFormat.format(pagado)));
        widget.add(crearInfoRow("Saldo Pendiente:", currencyFormat.format(pendiente)));
        widget.add(crearInfoRow("Estado:", estadoStr));
        widget.add(crearInfoRow("Último Pago:", ultimoPago != null ? dateFormat.format(ultimoPago) : "N/A"));
        widget.add(crearInfoRow("Próximo Pago:", proximoStr));
        widget.add(crearInfoRow("Cuotas:", (cuotasPagadas != null ? cuotasPagadas : "0") + " de " + (cuotasTotales != null ? cuotasTotales : "0")));

        return widget;
    }

    private JPanel crearInfoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_MAIN);
        lbl.setForeground(COLOR_TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(FONT_BOLD);
        val.setForeground(COLOR_TEXT);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JPanel crearWidgetAcciones(Map<String, Object> data) {
        JPanel widget = new JPanel(new GridLayout(0, 1, 0, 10));
        widget.setBackground(COLOR_CARD);
        widget.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JButton btnPago = new JButton("+ Registrar Nuevo Pago");
        btnPago.setBackground(COLOR_PRIMARY);
        btnPago.setForeground(Color.WHITE);
        btnPago.setFont(FONT_BOLD);
        btnPago.setFocusPainted(false);
        btnPago.setPreferredSize(new Dimension(0, 40));
        btnPago.addActionListener(e -> abrirDialogoRegistroPago());
        
        Object saldoObj = data.get("saldo");
        boolean isPagado = false;
        if (saldoObj instanceof Number) {
            isPagado = ((Number) saldoObj).doubleValue() <= 0.001;
        }
        
        btnPago.setEnabled(!isPagado);
        if (isPagado) {
            btnPago.setBackground(Color.GRAY);
            btnPago.setText("Pagos Completados");
        }

        widget.add(btnPago);
        return widget;
    }

    private JPanel crearTimelineItem(Map<String, Object> pago) {
        JPanel item = new JPanel(new BorderLayout(15, 0));
        item.setBackground(COLOR_CARD);
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Icono circular a la izquierda
        JPanel dotPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PRIMARY);
                g2.fillOval(10, 0, 12, 12);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(16, 12, 16, 100);
                g2.dispose();
            }
        };
        dotPanel.setPreferredSize(new Dimension(30, 0));
        dotPanel.setOpaque(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);

        JLabel lblMonto = new JLabel("Pago Recibido: " + currencyFormat.format(pago.get("monto")));
        lblMonto.setFont(FONT_SECTION);
        lblMonto.setForeground(COLOR_PRIMARY);

        JLabel lblFecha = new JLabel(dateFormat.format(pago.get("fecha")));
        lblFecha.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFecha.setForeground(COLOR_TEXT_MUTED);

        String detalle = "Modalidad: " + pago.get("modalidad") + " | Método: " + pago.get("metodo");
        JLabel lblDetalle = new JLabel(detalle);
        lblDetalle.setFont(FONT_MAIN);
        lblDetalle.setForeground(COLOR_TEXT);

        content.add(lblMonto, BorderLayout.NORTH);
        content.add(lblFecha, BorderLayout.SOUTH);
        content.add(lblDetalle, BorderLayout.CENTER);

        item.add(dotPanel, BorderLayout.WEST);
        item.add(content, BorderLayout.CENTER);

        return item;
    }

    private void abrirDialogoRegistroPago() {
        RegistroPagoDialog dlg = new RegistroPagoDialog(SwingUtilities.getWindowAncestor(this), usuarioActual, idEstudiante);
        dlg.setVisible(true);
        
        if (dlg.isSuccess()) {
            // Recargar Panel para mostrar el nuevo pago en el timeline y actualizar balance
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof Dashboard) {
                ((Dashboard) w).cargarPanel(new PerfilPagoEstudiantePanel(usuarioActual, idEstudiante));
            }
        }
    }
}
