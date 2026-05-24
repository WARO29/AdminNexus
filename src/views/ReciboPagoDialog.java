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

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReciboPagoDialog(Window owner, String nombreEstudiante, String programa, PagoRealizado pago) {
        super(owner, "Recibo de Pago Premium", Dialog.ModalityType.APPLICATION_MODAL);
        setSize(500, 750);
        setLocationRelativeTo(owner);
        setResizable(false);

        // Obtener datos adicionales del plan
        PagosController ctrl = new PagosController();
        Map<String, Object> dataPlan = ctrl.obtenerDetalleFinancieroEstudiante(pago.getEstudianteId());

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(COLOR_BG);
        main.setBorder(new EmptyBorder(25, 35, 25, 35));

        // Header (Logotipo y Título)
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 5));
        header.setOpaque(false);
        
        JLabel lblLogo = new JLabel("ADMIN NEXUS", SwingConstants.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblLogo.setForeground(new Color(26, 86, 219));
        
        JLabel lblTitle = new JLabel("COMPROBANTE ELECTRÓNICO DE PAGO", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(Color.GRAY);
        
        header.add(lblLogo);
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
        content.add(new JSeparator());
        content.add(Box.createVerticalStrut(15));

        // Sección Detalle del Pago Actual
        content.add(crearSeccionHeader("DETALLE DEL PAGO"));
        content.add(crearFila("Concepto:", pago.getComprobante()));
        content.add(crearFila("Método:", pago.getMetodoPago()));
        content.add(crearFila("Modalidad:", pago.getModalidad() != null ? pago.getModalidad().getNombre() : "N/A"));
        
        JPanel montoPanel = new JPanel(new BorderLayout());
        montoPanel.setOpaque(false);
        montoPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
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
            int pagadas = (int) dataPlan.get("cuotas_pagadas");
            int totales = (int) dataPlan.get("cuotas_totales");
            double montoTotal = (double) dataPlan.get("monto_total");
            double montoCuota = montoTotal / totales;
            
            JPanel tablaCuotas = new JPanel(new GridLayout(0, 1));
            tablaCuotas.setOpaque(false);
            
            // Header de tabla (4 columnas)
            JPanel th = new JPanel(new GridLayout(1, 4));
            th.setBackground(new Color(243, 244, 246));
            th.add(crearCeldaHeader("CUOTA"));
            th.add(crearCeldaHeader("VALOR"));
            th.add(crearCeldaHeader("SALDO REST."));
            th.add(crearCeldaHeader("ESTADO"));
            tablaCuotas.add(th);
 
            double saldoAcumulado = montoTotal;
            // Generar filas
            for (int i = 1; i <= totales; i++) {
                JPanel tr = new JPanel(new GridLayout(1, 4));
                tr.setOpaque(false);
                tr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(243, 244, 246)));
                
                tr.add(crearCelda("Cuota #" + i));
                tr.add(crearCelda(currencyFormat.format(montoCuota)));
                
                saldoAcumulado -= montoCuota;
                if (saldoAcumulado < 0) saldoAcumulado = 0;
                tr.add(crearCelda(currencyFormat.format(saldoAcumulado)));
                
                String estado = (i <= pagadas) ? "PAGADA" : "PENDIENTE";
                JLabel lblEst = crearCelda(estado);
                if (estado.equals("PAGADA")) {
                    lblEst.setForeground(new Color(22, 163, 74));
                    lblEst.setFont(new Font("Segoe UI", Font.BOLD, 10));
                } else {
                    lblEst.setForeground(new Color(220, 38, 38));
                    lblEst.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                }
                tr.add(lblEst);
                
                tablaCuotas.add(tr);
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
            content.add(discPanel);
        }

        content.add(Box.createVerticalStrut(20));
        content.add(crearFila("Saldo Restante Final:", currencyFormat.format(pago.getSaldoRestante())));
        
        content.add(Box.createVerticalGlue());
        
        JLabel lblFooter = new JLabel("<html><center>AdminNexus ERP - Sistema de Gestión Institucional<br>Copia para el Estudiante - Validez legal institucional</center></html>", SwingConstants.CENTER);
        lblFooter.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblFooter.setForeground(Color.GRAY);
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
        
        JButton btnClose = new JButton("Cerrar");
        btnClose.setPreferredSize(new Dimension(100, 38));
        btnClose.addActionListener(e -> dispose());
        
        buttons.add(btnClose);
        buttons.add(btnPrint);

        main.add(header, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        main.add(buttons, BorderLayout.SOUTH);

        add(main);
    }

    private JLabel crearSeccionHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(107, 114, 128));
        l.setBorder(new EmptyBorder(10, 0, 5, 0));
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
        p.setMaximumSize(new Dimension(500, 30));
        
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
