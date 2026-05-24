package views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Diálogo para generar la Orden de Pago (Pre-recibo) antes de registrar el pago.
 */
public class OrdenPagoDialog extends JDialog {

    private final Color COLOR_BG = Color.WHITE;
    private final Color COLOR_PRIMARY = new Color(26, 86, 219);
    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    private final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    private final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 12);

    private final Map<String, Object> dataEstudiante;
    private JComboBox<Integer> comboCuota;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public OrdenPagoDialog(Window owner, Map<String, Object> data) {
        super(owner, "Generar Orden de Pago", ModalityType.APPLICATION_MODAL);
        this.dataEstudiante = data;
        
        setSize(500, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);

        add(crearPanelConfiguracion(), BorderLayout.NORTH);
        add(new JScrollPane(crearPanelVistaPrevia()), BorderLayout.CENTER);
        add(crearPanelAcciones(), BorderLayout.SOUTH);
    }

    private JPanel crearPanelConfiguracion() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        p.add(new JLabel("Seleccione Cuota a Pagar:"));
        
        int totalCuotas = (int) dataEstudiante.getOrDefault("cuotas_totales", 5);
        int pagadas = (int) dataEstudiante.getOrDefault("cuotas_pagadas", 0);
        
        Integer[] cuotas = new Integer[totalCuotas - pagadas];
        for (int i = 0; i < cuotas.length; i++) {
            cuotas[i] = pagadas + i + 1;
        }
        
        comboCuota = new JComboBox<>(cuotas);
        comboCuota.setPreferredSize(new Dimension(80, 30));
        p.add(comboCuota);

        return p;
    }

    private JPanel crearPanelVistaPrevia() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                dibujarOrden(g, getWidth(), getHeight(), false);
            }
        };
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(450, 550));
        return p;
    }

    private void dibujarOrden(Graphics g, int w, int h, boolean forPrint) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int margin = 40;
        int y = margin + 20;

        // Logo y Cabecera
        g2.setColor(COLOR_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.drawString("ADMIN NEXUS", margin, y);
        
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        y += 20;
        g2.drawString("ORDEN DE PAGO INSTITUCIONAL", margin, y);
        
        y += 30;
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString("FECHA DE EMISIÓN: " + LocalDateTime.now().format(dateFormat), margin, y);
        
        y += 40;
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(margin, y, w - margin, y);
        
        // Datos Estudiante
        y += 30;
        g2.setFont(FONT_BOLD);
        g2.drawString("ESTUDIANTE:", margin, y);
        g2.setFont(FONT_NORMAL);
        g2.drawString(dataEstudiante.get("nombre").toString(), margin + 100, y);
        
        y += 20;
        g2.setFont(FONT_BOLD);
        g2.drawString("PROGRAMA:", margin, y);
        g2.setFont(FONT_NORMAL);
        g2.drawString(dataEstudiante.get("programa").toString(), margin + 100, y);
        
        y += 40;
        g2.setFont(FONT_BOLD);
        g2.drawString("CONCEPTO DE PAGO:", margin, y);
        y += 20;
        g2.setFont(FONT_NORMAL);
        int cuotaSeleccionada = (Integer) comboCuota.getSelectedItem();
        g2.drawString("Pago de Cuota Académica #" + cuotaSeleccionada, margin + 20, y);
        
        y += 40;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString("TOTAL A PAGAR:", margin, y);
        
        double montoTotal = (double) dataEstudiante.get("monto_total");
        int totalCuotas = (int) dataEstudiante.get("cuotas_totales");
        double valorCuota = montoTotal / totalCuotas;
        
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.setColor(COLOR_PRIMARY);
        g2.drawString(currencyFormat.format(valorCuota), margin + 150, y);
        
        y += 60;
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        g2.drawString("Nota: Presente este documento en caja o cárguelo al sistema una vez realizado el pago.", margin, y);
        
        y += 100;
        g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f}, 0f));
        g2.drawLine(margin, y, w - margin, y);
        y += 20;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        g2.drawString("ID Orden: " + System.currentTimeMillis(), margin, y);
    }

    private JPanel crearPanelAcciones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnGenerar = new JButton("Generar PDF / Imprimir");
        btnGenerar.setBackground(COLOR_PRIMARY);
        btnGenerar.setForeground(Color.WHITE);
        btnGenerar.setFont(FONT_BOLD);
        btnGenerar.setPreferredSize(new Dimension(180, 35));
        btnGenerar.addActionListener(e -> imprimirOrden());

        p.add(btnCancelar);
        p.add(btnGenerar);
        return p;
    }

    private void imprimirOrden() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Orden de Pago - " + dataEstudiante.get("nombre"));

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            
            dibujarOrden(graphics, (int) pageFormat.getImageableWidth(), (int) pageFormat.getImageableHeight(), true);
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Orden generada con éxito.");
                dispose();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Error al generar la orden: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
