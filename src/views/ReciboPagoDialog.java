package views;

import model.PagoRealizado;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.Dialog;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.print.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import controller.PagosController;

/**
 * Diálogo para visualizar y generar la constancia de pago.
 */
public class ReciboPagoDialog extends JDialog {

    private final Color COLOR_BG = Color.WHITE;
    private final Color COLOR_TEXT = new Color(31, 41, 55);
    private final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 20);
    private final Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReciboPagoDialog(Window owner, String nombreEstudiante, String programa, PagoRealizado pago) {
        super(owner, "Recibo de Pago Premium", Dialog.ModalityType.APPLICATION_MODAL);
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int dialogW = Math.min(540, screen.width  - 100);
        int dialogH = Math.min(820, screen.height - 80);
        setMinimumSize(new Dimension(400, 460));
        setMaximumSize(new Dimension(720, 960));
        setSize(dialogW, dialogH);
        setLocationRelativeTo(owner);
        setResizable(true);

        // Obtener datos adicionales del plan
        PagosController ctrl = new PagosController();
        Map<String, Object> dataPlan = ctrl.obtenerDetalleFinancieroEstudiante(pago.getEstudianteId());

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(COLOR_BG);
        main.setBorder(new EmptyBorder(25, 35, 25, 35));

        // Header (Logotipo y Título)
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel lblLogo = new JLabel("ADMIN NEXUS", SwingConstants.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblLogo.setForeground(new Color(26, 86, 219));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("COMPROBANTE ELECTRÓNICO DE PAGO", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(Color.GRAY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(lblLogo);
        header.add(Box.createVerticalStrut(4));
        header.add(lblTitle);
        header.add(Box.createVerticalStrut(20));

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Sección Estudiante
        content.add(crearSeccionHeader("DATOS DEL ESTUDIANTE"));
        content.add(crearFila("Nombre Completo:", nombreEstudiante));
        content.add(crearFila("Programa Académico:", programa));
        content.add(crearFila("Fecha de Pago:", pago.getFecha() != null ? pago.getFecha().format(dateFormat) : "N/A"));
        
        content.add(Box.createVerticalStrut(15));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sep);
        content.add(Box.createVerticalStrut(15));

        // Sección Detalle del Pago Actual
        content.add(crearSeccionHeader("DETALLE DEL PAGO"));
        content.add(crearFila("Concepto:", pago.getComprobante()));
        content.add(crearFila("Método:", pago.getMetodoPago()));
        content.add(crearFila("Modalidad:", pago.getModalidad() != null ? pago.getModalidad().getNombre() : "N/A"));
        
        JPanel montoPanel = new JPanel(new BorderLayout());
        montoPanel.setOpaque(false);
        montoPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        montoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblMontoTxt = new JLabel("VALOR RECIBIDO:");
        lblMontoTxt.setFont(FONT_BOLD);
        JLabel lblMontoVal = new JLabel(currencyFormat.format(pago.getMonto()));
        lblMontoVal.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblMontoVal.setForeground(new Color(22, 163, 74));
        montoPanel.add(lblMontoTxt, BorderLayout.WEST);
        montoPanel.add(lblMontoVal, BorderLayout.EAST);
        content.add(montoPanel);

        content.add(Box.createVerticalStrut(10));
        
        // TABLA DE CUOTAS (Relación de Pagos)
        content.add(crearSeccionHeader("ESTADO DE CUOTAS (PLAN DE PAGO)"));
        if (dataPlan != null) {
            int totales = (int) dataPlan.get("cuotas_totales");
            double montoTotal = (double) dataPlan.get("monto_total");
            double saldoPendientePlan = (double) dataPlan.get("saldo");

            java.util.List<Map<String, Object>> historial = ctrl.obtenerHistorialEstudiante(pago.getEstudianteId());
            // Invertir el historial para que quede en orden cronológico (antiguo a reciente)
            java.util.Collections.reverse(historial);
            int paidCount = historial.size();

            JPanel tablaCuotas = new JPanel(new GridLayout(0, 1));
            tablaCuotas.setOpaque(false);
            tablaCuotas.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Header de tabla (4 columnas)
            JPanel th = new JPanel(new GridLayout(1, 4));
            th.setBackground(new Color(243, 244, 246));
            th.add(crearCeldaHeader("CUOTA"));
            th.add(crearCeldaHeader("VALOR"));
            th.add(crearCeldaHeader("SALDO REST."));
            th.add(crearCeldaHeader("ESTADO"));
            tablaCuotas.add(th);
 
            // 1. Mostrar las cuotas pagadas a partir del historial real
            for (int i = 0; i < paidCount; i++) {
                Map<String, Object> p = historial.get(i);
                double valorPagado = (double) p.get("monto");
                double saldoRestante = (double) p.get("saldo_despues");
                String concepto = p.get("comprobante") != null ? p.get("comprobante").toString() : "Cuota #" + (i + 1);

                JPanel tr = new JPanel(new GridLayout(1, 4));
                tr.setOpaque(false);
                tr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(243, 244, 246)));
                
                tr.add(crearCelda(concepto));
                tr.add(crearCelda(currencyFormat.format(valorPagado)));
                tr.add(crearCelda(currencyFormat.format(saldoRestante)));
                
                JLabel lblEst = crearCelda("PAGADA");
                lblEst.setForeground(new Color(22, 163, 74));
                lblEst.setFont(new Font("Segoe UI", Font.BOLD, 10));
                tr.add(lblEst);
                
                tablaCuotas.add(tr);
            }

            // 2. Mostrar las cuotas pendientes restantes si el historial es menor al total de cuotas y hay saldo
            int pendingCount = totales - paidCount;
            if (pendingCount > 0 && saldoPendientePlan > 0.001) {
                double valorCuotaPendiente = saldoPendientePlan / pendingCount;
                double saldoAcumulado = saldoPendientePlan;

                for (int i = 1; i <= pendingCount; i++) {
                    JPanel tr = new JPanel(new GridLayout(1, 4));
                    tr.setOpaque(false);
                    tr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(243, 244, 246)));
                    
                    tr.add(crearCelda("Cuota #" + (paidCount + i)));
                    tr.add(crearCelda(currencyFormat.format(valorCuotaPendiente)));
                    
                    saldoAcumulado -= valorCuotaPendiente;
                    if (saldoAcumulado < 0) saldoAcumulado = 0;
                    tr.add(crearCelda(currencyFormat.format(saldoAcumulado)));
                    
                    JLabel lblEst = crearCelda("PENDIENTE");
                    lblEst.setForeground(new Color(220, 38, 38));
                    lblEst.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                    tr.add(lblEst);
                    
                    tablaCuotas.add(tr);
                }
            }
            content.add(tablaCuotas);
        }

        content.add(Box.createVerticalStrut(15));
        
        // Sección de Descuento
        if (dataPlan != null && (double) dataPlan.get("monto_total") < 500000) {
            JPanel discPanel = new JPanel(new BorderLayout());
            discPanel.setBackground(new Color(239, 246, 255));
            discPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254)),
                new EmptyBorder(8, 12, 8, 12)
            ));
            JLabel lblDisc = new JLabel("✨ Beneficio de Descuento Aplicado al Programa");
            lblDisc.setFont(new Font("Segoe UI", Font.BOLD | Font.ITALIC, 11));
            lblDisc.setForeground(new Color(29, 78, 216));
            discPanel.add(lblDisc);
            discPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(discPanel);
        }

        content.add(Box.createVerticalStrut(20));
        content.add(crearFila("Saldo Restante Final:", currencyFormat.format(pago.getSaldoRestante())));

        content.add(Box.createVerticalStrut(25));

        JLabel lblFooter = new JLabel("<html><center>AdminNexus ERP - Sistema de Gestión Institucional<br>Copia para el Estudiante - Validez legal institucional</center></html>", SwingConstants.CENTER);
        lblFooter.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblFooter.setForeground(Color.GRAY);
        lblFooter.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblFooter);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        JButton btnPrint = new JButton("Imprimir PDF");
        btnPrint.setBackground(new Color(26, 86, 219));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(FONT_BOLD);
        btnPrint.setPreferredSize(new Dimension(140, 38));
        btnPrint.addActionListener(e -> imprimirRecibo(nombreEstudiante, content));
        
        JButton btnClose = new JButton("Cerrar");
        btnClose.setPreferredSize(new Dimension(100, 38));
        btnClose.addActionListener(e -> dispose());
        
        buttons.add(btnClose);
        buttons.add(btnPrint);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(COLOR_BG);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        main.add(header, BorderLayout.NORTH);
        main.add(scrollPane, BorderLayout.CENTER);
        main.add(buttons, BorderLayout.SOUTH);

        add(main);
    }

    private void imprimirRecibo(String nombreEstudiante, JPanel panelAImprimir) {
        // Forzar layout completo antes de medir
        panelAImprimir.revalidate();
        panelAImprimir.doLayout();
        java.awt.Dimension preferred = panelAImprimir.getPreferredSize();
        final int panelW = preferred.width > 0 ? preferred.width : Math.max(panelAImprimir.getWidth(), 400);
        final int panelH = preferred.height > 0 ? preferred.height : Math.max(panelAImprimir.getHeight(), 600);

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Recibo de Pago - " + nombreEstudiante);

        // Usar el formato de página predeterminado del sistema (A4 o Carta según la región)
        PageFormat pf = job.defaultPage();

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            double imgW = pageFormat.getImageableWidth();
            double imgH = pageFormat.getImageableHeight();
            double scale = imgW / panelW;
            int totalPages = (int) Math.ceil((panelH * scale) / imgH);

            if (pageIndex >= totalPages) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.scale(scale, scale);

            // Desplazar verticalmente para la página actual
            double offsetY = pageIndex * (imgH / scale);
            g2.translate(0, -offsetY);

            // Recortar para no sangrar contenido de otras páginas
            g2.setClip(0, (int) offsetY, panelW, (int) Math.ceil(imgH / scale));

            panelAImprimir.printAll(g2);

            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Recibo enviado a la cola de impresión con éxito.", "Impresión Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Error al imprimir el recibo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JLabel crearSeccionHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(107, 114, 128));
        l.setBorder(new EmptyBorder(10, 0, 5, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel crearCeldaHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(new EmptyBorder(5, 5, 5, 5));
        return l;
    }

    private JLabel crearCelda(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(new EmptyBorder(5, 5, 5, 5));
        return l;
    }

    private JPanel crearFila(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_MAIN);
        lbl.setForeground(Color.GRAY);
        
        JLabel val = new JLabel(value != null ? value : "");
        val.setFont(FONT_BOLD);
        val.setForeground(COLOR_TEXT);
        
        p.add(lbl, BorderLayout.WEST);
        p.add(val, BorderLayout.EAST);
        return p;
    }
}
