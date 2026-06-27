package views;

import controller.PagosController;
import model.Usuario;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
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
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.of("es", "ES"));

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
        double saldoHeader = data.get("saldo") != null ? (double) data.get("saldo") : 0;
        boolean headerPagado = saldoHeader <= 0.001;

        // Constantes de layout
        final int COVER_H  = 155;
        final int AVT_SIZE = 110;
        final int AVT_X    = 30;
        final int AVT_TOP  = COVER_H - AVT_SIZE / 2;   // mitad del avatar sobre la portada
        final int TEXT_X   = AVT_X + AVT_SIZE + 16;
        final int HEADER_H = AVT_TOP + AVT_SIZE + 50;  // espacio bajo el avatar

        // Portada (degradado) — se agrega AL FINAL para quedar detrás de todo
        JPanel cover = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, COLOR_PRIMARY, getWidth(), 0, COLOR_PRIMARY.darker()));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        // Avatar circular
        JLabel avatar = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(COLOR_PRIMARY.brighter());
                g2.setStroke(new BasicStroke(4));
                g2.drawOval(2, 2, getWidth()-4, getHeight()-4);
                g2.setColor(COLOR_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 44));
                String nombre = data.get("nombre") != null ? data.get("nombre").toString() : "E";
                String inicial = nombre.isEmpty() ? "E" : nombre.substring(0, 1).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(inicial, (getWidth()-fm.stringWidth(inicial))/2,
                              (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };

        // Nombre y subinfo
        JLabel name = new JLabel(data.get("nombre") != null ? data.get("nombre").toString() : "Estudiante");
        name.setFont(new Font("Segoe UI", Font.BOLD, 22));
        name.setForeground(COLOR_TEXT);

        JLabel subInfo = new JLabel(data.get("programa") + "  •  Cód: " + data.get("codigo"));
        subInfo.setFont(FONT_MAIN);
        subInfo.setForeground(COLOR_TEXT_MUTED);

        // Botones — anclados a la IZQUIERDA del cover para evitar cálculos negativos
        JButton btnBack = new JButton("← Volver");
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof Dashboard) ((Dashboard) w).abrirPagos();
        });

        JButton btnPagarHeader = new JButton("+ Registrar Pago");
        btnPagarHeader.setBackground(headerPagado ? new Color(107,114,128) : COLOR_PRIMARY);
        btnPagarHeader.setForeground(Color.WHITE);
        btnPagarHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPagarHeader.setFocusPainted(false);
        btnPagarHeader.setOpaque(true);
        btnPagarHeader.setBorderPainted(false);
        btnPagarHeader.setEnabled(!headerPagado);
        btnPagarHeader.addActionListener(e -> abrirDialogoRegistroPago(false));

        JButton btnPreferencialHeader = new JButton("Preferencial");
        btnPreferencialHeader.setBackground(headerPagado ? new Color(107,114,128) : new Color(139, 92, 246));
        btnPreferencialHeader.setForeground(Color.WHITE);
        btnPreferencialHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPreferencialHeader.setFocusPainted(false);
        btnPreferencialHeader.setOpaque(true);
        btnPreferencialHeader.setBorderPainted(false);
        btnPreferencialHeader.setEnabled(!headerPagado);
        btnPreferencialHeader.addActionListener(e -> abrirDialogoRegistroPago(true));

        // Panel null-layout con doLayout() para que el cover llene el ancho siempre
        JPanel header = new JPanel(null) {
            @Override public void doLayout() {
                int w = getWidth();
                if (w < 10) return;
                // Cover ocupa todo el ancho
                cover.setBounds(0, 0, w, COVER_H);
                // Avatar solapado sobre el borde inferior del cover
                avatar.setBounds(AVT_X, AVT_TOP, AVT_SIZE, AVT_SIZE);
                // Nombre y subinfo a la derecha del avatar, alineados con su centro inferior
                int nameY = AVT_TOP + AVT_SIZE - 44;
                name.setBounds(TEXT_X, nameY, Math.max(w - TEXT_X - 10, 80), 30);
                subInfo.setBounds(TEXT_X, nameY + 32, Math.max(w - TEXT_X - 10, 80), 20);
                // Botones en la esquina superior izquierda del cover
                btnBack.setBounds(16, 12, 110, 30);
                btnPagarHeader.setBounds(136, 12, 145, 30);
                btnPreferencialHeader.setBounds(289, 12, 140, 30);
            }
        };
        header.setPreferredSize(new Dimension(0, HEADER_H));
        header.setBackground(COLOR_CARD);

        // ORDEN IMPORTANTE EN SWING (null layout):
        // El primer componente agregado queda DELANTE (pintado último).
        // → Botones y avatar primero (al frente), cover último (detrás).
        header.add(btnBack);
        header.add(btnPagarHeader);
        header.add(btnPreferencialHeader);
        header.add(subInfo);
        header.add(name);
        header.add(avatar);
        header.add(cover);   // ← añadido ÚLTIMO = pintado PRIMERO = detrás de todo

        return header;
    }

    private JPanel crearCuerpoSocial(Map<String, Object> data) {
        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 20, 30, 20));

        // Columna Izquierda: Estadísticas (Widgets)
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setPreferredSize(new Dimension(270, 0));
        leftCol.setMinimumSize(new Dimension(200, 0));
        leftCol.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));
        leftCol.setOpaque(false);

        leftCol.add(crearWidgetInfo("Resumen Financiero", data));

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
            JLabel lblVacio = new JLabel("No hay pagos registrados aún.");
            lblVacio.setFont(FONT_MAIN);
            lblVacio.setForeground(COLOR_TEXT_MUTED);
            lblVacio.setAlignmentX(Component.CENTER_ALIGNMENT);
            timelineContainer.add(lblVacio);
        } else {
            for (Map<String, Object> pago : historial) {
                timelineContainer.add(crearTimelineItem(pago));
                timelineContainer.add(Box.createVerticalStrut(12));
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

        Object saldoObj = data.get("saldo");
        boolean isPagado = saldoObj instanceof Number && ((Number) saldoObj).doubleValue() <= 0.001;

        JLabel lblAcciones = new JLabel("Acciones Rápidas");
        lblAcciones.setFont(FONT_SECTION);
        lblAcciones.setForeground(COLOR_TEXT);
        widget.add(lblAcciones);

        JButton btnPago = new JButton("+ Registrar Pago");
        btnPago.setBackground(isPagado ? Color.GRAY : COLOR_PRIMARY);
        btnPago.setForeground(Color.WHITE);
        btnPago.setFont(FONT_BOLD);
        btnPago.setFocusPainted(false);
        btnPago.setPreferredSize(new Dimension(0, 40));
        btnPago.setEnabled(!isPagado);
        btnPago.addActionListener(e -> abrirDialogoRegistroPago(false));

        JButton btnPreferencial = new JButton("Pago Preferencial", crearIconoEstrella());
        btnPreferencial.setBackground(isPagado ? Color.GRAY : new Color(139, 92, 246));
        btnPreferencial.setForeground(Color.WHITE);
        btnPreferencial.setFont(FONT_BOLD);
        btnPreferencial.setFocusPainted(false);
        btnPreferencial.setPreferredSize(new Dimension(0, 40));
        btnPreferencial.setEnabled(!isPagado);
        btnPreferencial.addActionListener(e -> abrirDialogoRegistroPago(true));

        if (isPagado) {
            JLabel lblPaz = new JLabel("✓ Pagos completados");
            lblPaz.setFont(FONT_BOLD);
            lblPaz.setForeground(new Color(22, 163, 74));
            widget.add(lblPaz);
        } else {
            widget.add(btnPago);
            widget.add(btnPreferencial);
        }

        return widget;
    }

    private JPanel crearTimelineItem(Map<String, Object> pago) {
        JPanel item = new JPanel(new BorderLayout(12, 0));
        item.setBackground(COLOR_CARD);
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(14, 16, 14, 16)
        ));
        item.setMaximumSize(new Dimension(780, 200));
        item.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Indicador de línea de tiempo a la izquierda
        JPanel dotPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PRIMARY);
                g2.fillOval(5, 4, 12, 12);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(11, 16, 11, getHeight());
                g2.dispose();
            }
        };
        dotPanel.setPreferredSize(new Dimension(24, 0));
        dotPanel.setOpaque(false);

        // Contenido principal
        JPanel content = new JPanel(new BorderLayout(0, 3));
        content.setOpaque(false);

        // Fila superior: monto + saldo restante
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblMonto = new JLabel("Pago: " + currencyFormat.format(pago.get("monto")));
        lblMonto.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblMonto.setForeground(new Color(22, 163, 74));

        Object saldoRest = pago.get("saldo_despues");
        JLabel lblSaldo = new JLabel("Saldo: " + currencyFormat.format(saldoRest != null ? saldoRest : 0));
        lblSaldo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSaldo.setForeground(COLOR_TEXT_MUTED);
        lblSaldo.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

        topRow.add(lblMonto, BorderLayout.WEST);
        topRow.add(lblSaldo, BorderLayout.EAST);

        // Fila media: modalidad y método (con HTML para que envuelva en pantallas pequeñas)
        String modalidadTexto = pago.get("modalidad") != null ? pago.get("modalidad").toString() : "";
        String metodoTexto    = pago.get("metodo")    != null ? pago.get("metodo").toString()    : "";
        JLabel lblDetalle = new JLabel("<html><b>Modalidad:</b> " + modalidadTexto +
                                       "&nbsp;&nbsp;<b>Método:</b> " + metodoTexto + "</html>");
        lblDetalle.setFont(FONT_MAIN);
        lblDetalle.setForeground(COLOR_TEXT);

        // Fila inferior: fecha + usuario
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);

        JLabel lblFecha = new JLabel(dateFormat.format(pago.get("fecha")));
        lblFecha.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblFecha.setForeground(COLOR_TEXT_MUTED);
        bottomRow.add(lblFecha, BorderLayout.WEST);

        String nombreUsuario = (String) pago.get("nombre_usuario");
        if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
            JLabel lblUsuario = new JLabel("Por: " + nombreUsuario);
            lblUsuario.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblUsuario.setForeground(COLOR_PRIMARY);
            lblUsuario.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            bottomRow.add(lblUsuario, BorderLayout.EAST);
        }

        content.add(topRow,    BorderLayout.NORTH);
        content.add(lblDetalle, BorderLayout.CENTER);
        content.add(bottomRow, BorderLayout.SOUTH);

        item.add(dotPanel, BorderLayout.WEST);
        item.add(content,  BorderLayout.CENTER);

        return item;
    }

    private Icon crearIconoEstrella() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.translate(x, y);
                int[] xp = {14, 18, 25, 19, 21, 14, 7, 9, 3, 10};
                int[] yp = {4, 12, 12, 17, 24, 19, 24, 17, 12, 12};
                g2.fillPolygon(xp, yp, 10);
                g2.dispose();
            }
            @Override public int getIconWidth()  { return 28; }
            @Override public int getIconHeight() { return 28; }
        };
    }

    private void abrirDialogoRegistroPago(boolean preferencial) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        RegistroPagoDialog dlg = new RegistroPagoDialog(owner, usuarioActual, idEstudiante, preferencial);
        dlg.setVisible(true);

        if (dlg.isSuccess()) {
            if (owner instanceof Dashboard) {
                ((Dashboard) owner).cargarPanel(new PerfilPagoEstudiantePanel(usuarioActual, idEstudiante));
            }
        }
    }
}
