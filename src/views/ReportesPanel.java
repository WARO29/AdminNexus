package views;

import controller.ReportesController;
import model.Usuario;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.Date;

public class ReportesPanel extends JPanel {

    // ─── Colores (mutables por tema) ─────────────────────────────────────────
    private Color COLOR_BG         = new Color(249, 250, 251);
    private Color COLOR_CARD       = Color.WHITE;
    private Color COLOR_PRIMARY    = new Color(26, 86, 219);
    private Color COLOR_SUCCESS    = new Color(34, 197, 94);
    private Color COLOR_WARNING    = new Color(245, 158, 11);
    private Color COLOR_DANGER     = new Color(239, 68, 68);
    private Color COLOR_BORDER     = new Color(229, 231, 235);
    private Color COLOR_TEXT       = new Color(17, 24, 39);
    private Color COLOR_TEXT_MUTED = new Color(107, 114, 128);

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private final SimpleDateFormat sdfCorto   = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat sdfLargo   = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ─── Estado ───────────────────────────────────────────────────────────────
    private final ReportesController controller = new ReportesController();
    private final model.Usuario usuarioActual;
    private JTabbedPane tabbedPane;
    private JSpinner spinnerInicio, spinnerFin;
    private JComboBox<String> comboMetodo;
    private JComboBox<String> comboModalidadFiltro;

    // KPI labels – Financiero
    private JLabel lblTotalEsperado   = new JLabel("$0");
    private JLabel lblTotalRecaudado  = new JLabel("$0");
    private JLabel lblSaldoPendiente  = new JLabel("$0");
    private JLabel lblPorcentaje      = new JLabel("0%");

    // KPI labels – Estudiantes
    private JLabel lblTotalEstudiantes = new JLabel("0");
    private JLabel lblActivos          = new JLabel("0");
    private JLabel lblEnMora           = new JLabel("0");

    // KPI labels – Programas
    private JLabel lblTotalProgramas   = new JLabel("0");
    private JLabel lblProgramasActivos = new JLabel("0");
    private JLabel lblProgramasCerrados = new JLabel("0");

    // Gráficas
    private DonutChart donutEstadoPago;
    private BarChart   barIngresosMes;
    private DonutChart donutEstudiantes;
    private BarChart   barPorPrograma;

    // Tablas – Financiero
    private DefaultTableModel modeloTodosPagos;
    private DefaultTableModel modeloAbonoSabado;
    private DefaultTableModel modeloAbonoSemana;
    private DefaultTableModel modeloCuotaMensual;
    private DefaultTableModel modeloCarreraTotal;
    private DefaultTableModel modeloMediaCarrera;

    // Tablas – Otros
    private DefaultTableModel modeloMora;
    private DefaultTableModel modeloProgramas;
    private DefaultTableModel modeloActividades;
    private DefaultTableModel modeloLogs;

    public ReportesPanel(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
        cargarConfiguracion();

        setLayout(new BorderLayout(0, 0));
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        add(crearHeader(), BorderLayout.NORTH);
        add(crearTabs(), BorderLayout.CENTER);

        cargarDatos();
    }

    private void cargarConfiguracion() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot()
                .node("AdminNexus").node("Config").node("User_" + usuarioActual.getIdusuario());

        String mode  = prefs.get("app_mode",  "light");
        String color = prefs.get("app_color", "blue");

        switch (color) {
            case "green":    COLOR_PRIMARY = new Color(16, 185, 129);  break;
            case "gray":     COLOR_PRIMARY = new Color(75, 85, 99);    break;
            case "burgundy": COLOR_PRIMARY = new Color(153, 27, 27);   break;
            default:         COLOR_PRIMARY = new Color(26, 86, 219);   break;
        }

        if ("dark".equals(mode)) {
            COLOR_BG         = new Color(17, 24, 39);
            COLOR_CARD       = new Color(31, 41, 55);
            COLOR_TEXT       = Color.WHITE;
            COLOR_TEXT_MUTED = new Color(156, 163, 175);
            COLOR_BORDER     = new Color(55, 65, 81);
        } else {
            COLOR_BG         = new Color(249, 250, 251);
            COLOR_CARD       = Color.WHITE;
            COLOR_TEXT       = new Color(17, 24, 39);
            COLOR_TEXT_MUTED = new Color(107, 114, 128);
            COLOR_BORDER     = new Color(229, 231, 235);
        }
    }

    // ─── Header con filtro de fechas ─────────────────────────────────────────

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel lblTitulo = new JLabel("Reportes");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(COLOR_TEXT);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setOpaque(false);

        // Rango por defecto: primer día del mes actual → hoy
        Calendar cal = Calendar.getInstance();
        Date hoy = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date primerDiaMes = cal.getTime();

        SpinnerDateModel mdlInicio = new SpinnerDateModel(primerDiaMes, null, null, Calendar.DAY_OF_MONTH);
        SpinnerDateModel mdlFin    = new SpinnerDateModel(hoy, null, null, Calendar.DAY_OF_MONTH);
        spinnerInicio = new JSpinner(mdlInicio);
        spinnerFin    = new JSpinner(mdlFin);

        JSpinner.DateEditor edInicio = new JSpinner.DateEditor(spinnerInicio, "dd/MM/yyyy");
        JSpinner.DateEditor edFin    = new JSpinner.DateEditor(spinnerFin,    "dd/MM/yyyy");
        spinnerInicio.setEditor(edInicio);
        spinnerFin.setEditor(edFin);
        spinnerInicio.setPreferredSize(new Dimension(110, 30));
        spinnerFin.setPreferredSize(new Dimension(110, 30));

        JButton btnAplicar = new JButton("Aplicar");
        btnAplicar.setBackground(COLOR_PRIMARY);
        btnAplicar.setForeground(Color.WHITE);
        btnAplicar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAplicar.setPreferredSize(new Dimension(85, 30));
        btnAplicar.setBorderPainted(false);
        btnAplicar.setFocusPainted(false);
        btnAplicar.addActionListener(e -> cargarDatos());

        JButton btnPDF = new JButton("Exportar PDF");
        btnPDF.setBackground(COLOR_DANGER);
        btnPDF.setForeground(Color.WHITE);
        btnPDF.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPDF.setPreferredSize(new Dimension(120, 30));
        btnPDF.setBorderPainted(false);
        btnPDF.setFocusPainted(false);
        btnPDF.addActionListener(e -> exportarPDF());

        comboMetodo = new JComboBox<>(new String[]{"Todos", "Bancolombia", "Nequi", "Efectivo"});
        comboMetodo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboMetodo.setPreferredSize(new Dimension(120, 30));

        comboModalidadFiltro = new JComboBox<>(new String[]{
            "Todas", "Abono Sábado", "Abono Semana", "Cuota Mensual", "Carrera Total", "Media Carrera"
        });
        comboModalidadFiltro.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboModalidadFiltro.setPreferredSize(new Dimension(140, 30));

        filtros.add(crearLabel("Desde:"));
        filtros.add(spinnerInicio);
        filtros.add(crearLabel("Hasta:"));
        filtros.add(spinnerFin);
        filtros.add(crearLabel("Modalidad:"));
        filtros.add(comboModalidadFiltro);
        filtros.add(crearLabel("Método:"));
        filtros.add(comboMetodo);
        filtros.add(btnAplicar);
        filtros.add(Box.createHorizontalStrut(10));
        filtros.add(btnPDF);

        header.add(lblTitulo, BorderLayout.NORTH);
        header.add(filtros,   BorderLayout.SOUTH);
        return header;
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COLOR_TEXT_MUTED);
        return lbl;
    }

    // ─── Pestañas ─────────────────────────────────────────────────────────────

    private JTabbedPane crearTabs() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabbedPane.setBackground(COLOR_BG);

        tabbedPane.addTab("Financiero",  crearTabFinanciero());
        tabbedPane.addTab("Estudiantes", crearTabEstudiantes());
        tabbedPane.addTab("Programas",   crearTabProgramas());
        tabbedPane.addTab("Auditoría",   crearTabAuditoria());

        return tabbedPane;
    }

    // ─── TAB FINANCIERO ───────────────────────────────────────────────────────

    private JPanel crearTabFinanciero() {
        // Contenido scrollable
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(COLOR_BG);
        inner.setBorder(new EmptyBorder(16, 0, 8, 0));

        // KPI cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        cards.add(crearStatCard("Total Esperado",  lblTotalEsperado,  COLOR_PRIMARY));
        cards.add(crearStatCard("Total Recaudado", lblTotalRecaudado, COLOR_SUCCESS));
        cards.add(crearStatCard("Saldo Pendiente", lblSaldoPendiente, COLOR_WARNING));
        cards.add(crearStatCard("% Recaudo",       lblPorcentaje,     COLOR_DANGER));
        inner.add(cards);
        inner.add(Box.createVerticalStrut(16));

        // Gráficas
        JPanel graficas = new JPanel(new GridLayout(1, 2, 16, 0));
        graficas.setOpaque(false);
        graficas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        graficas.setPreferredSize(new Dimension(0, 280));

        donutEstadoPago = new DonutChart("Modalidades en el período");
        barIngresosMes  = new BarChart("Recaudación por Mes");

        graficas.add(envolverEnCard(donutEstadoPago, "Distribución por Modalidad"));
        graficas.add(envolverEnCard(barIngresosMes,  "Recaudación por Mes"));
        inner.add(graficas);
        inner.add(Box.createVerticalStrut(16));

        // Sección de tablas de desglose
        JLabel lblDetalle = new JLabel("Detalle de Movimientos");
        lblDetalle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDetalle.setForeground(COLOR_TEXT);
        lblDetalle.setAlignmentX(LEFT_ALIGNMENT);
        inner.add(lblDetalle);
        inner.add(Box.createVerticalStrut(8));

        JTabbedPane tabsDetalle = new JTabbedPane();
        tabsDetalle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabsDetalle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        tabsDetalle.setPreferredSize(new Dimension(0, 300));

        // Todos los pagos
        String[] colsTodos = {"Fecha", "Estudiante", "Código", "Programa", "Monto", "Modalidad", "Método", "Registrado por"};
        modeloTodosPagos = crearModelo(colsTodos);
        tabsDetalle.addTab("Todos los pagos", crearScrollTabla(modeloTodosPagos));

        // Abono Sábado
        String[] colsAbono = {"Fecha", "Estudiante", "Código", "Programa", "Monto", "Método", "Registrado por"};
        modeloAbonoSabado = crearModelo(colsAbono);
        tabsDetalle.addTab("Abono Sábado", crearScrollTabla(modeloAbonoSabado));

        // Abono Semana
        modeloAbonoSemana = crearModelo(colsAbono);
        tabsDetalle.addTab("Abono Semana", crearScrollTabla(modeloAbonoSemana));

        // Resumen por estudiante (todos los pagos agrupados)
        String[] colsCuota = {"Estudiante", "Código", "Programa", "N° Pagos", "Total Pagado", "Primer Pago", "Último Pago"};
        modeloCuotaMensual = crearModelo(colsCuota);
        tabsDetalle.addTab("Resumen por Estudiante", crearScrollTabla(modeloCuotaMensual));

        // Carrera Total
        modeloCarreraTotal = crearModelo(colsAbono);
        tabsDetalle.addTab("Carrera Total", crearScrollTabla(modeloCarreraTotal));

        // Media Carrera
        modeloMediaCarrera = crearModelo(colsAbono);
        tabsDetalle.addTab("Media Carrera", crearScrollTabla(modeloMediaCarrera));

        inner.add(tabsDetalle);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setBackground(COLOR_BG);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_BG);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private DefaultTableModel crearModelo(String[] columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JScrollPane crearScrollTabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? COLOR_CARD : filaAlternada());
                    c.setForeground(COLOR_TEXT);
                }
                return c;
            }
        };
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.setRowHeight(28);
        tabla.setBackground(COLOR_CARD);
        tabla.setForeground(COLOR_TEXT);
        tabla.setGridColor(COLOR_BORDER);
        tabla.setShowGrid(true);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(COLOR_PRIMARY);
        tabla.setSelectionForeground(Color.WHITE);

        // Header
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(COLOR_BORDER);
        tabla.getTableHeader().setForeground(COLOR_TEXT);
        tabla.getTableHeader().setReorderingAllowed(false);

        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(null);
        sp.getViewport().setBackground(COLOR_CARD);
        return sp;
    }

    private Color filaAlternada() {
        // Fila impar ligeramente más oscura que el card
        Color c = COLOR_CARD;
        int delta = isDarkMode() ? 8 : -6;
        return new Color(
            Math.max(0, Math.min(255, c.getRed()   + delta)),
            Math.max(0, Math.min(255, c.getGreen() + delta)),
            Math.max(0, Math.min(255, c.getBlue()  + delta))
        );
    }

    private boolean isDarkMode() {
        return COLOR_BG.getRed() < 50; // fondo oscuro si R < 50
    }

    // ─── TAB ESTUDIANTES ─────────────────────────────────────────────────────

    private JPanel crearTabEstudiantes() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        // KPI cards
        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        cards.add(crearStatCard("Total Estudiantes", lblTotalEstudiantes, COLOR_PRIMARY));
        cards.add(crearStatCard("Activos",           lblActivos,          COLOR_SUCCESS));
        cards.add(crearStatCard("En Mora",           lblEnMora,           COLOR_DANGER));
        panel.add(cards);
        panel.add(Box.createVerticalStrut(16));

        // Contenido principal
        JPanel content = new JPanel(new GridLayout(1, 2, 16, 0));
        content.setOpaque(false);

        donutEstudiantes = new DonutChart("Estudiantes por Estado");
        content.add(envolverEnCard(donutEstudiantes, "Estudiantes por Estado"));

        // Tabla en mora
        String[] colsMora = {"Nombre", "Código", "Programa", "Saldo Pendiente", "Próximo Pago"};
        modeloMora = new DefaultTableModel(colsMora, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        content.add(envolverTablaEnCard(modeloMora, "Estudiantes en Mora"));

        panel.add(content);
        return panel;
    }

    // ─── TAB PROGRAMAS ───────────────────────────────────────────────────────

    private JPanel crearTabProgramas() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        // KPI cards
        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        cards.add(crearStatCard("Total Programas",   lblTotalProgramas,   COLOR_PRIMARY));
        cards.add(crearStatCard("Activos",           lblProgramasActivos, COLOR_SUCCESS));
        cards.add(crearStatCard("Cerrados / Pausa",  lblProgramasCerrados, COLOR_WARNING));
        panel.add(cards);
        panel.add(Box.createVerticalStrut(16));

        // Gráfica + Tabla
        JPanel content = new JPanel(new GridLayout(1, 2, 16, 0));
        content.setOpaque(false);

        barPorPrograma = new BarChart("Estudiantes Activos por Programa");
        content.add(envolverEnCard(barPorPrograma, "Estudiantes Activos por Programa"));

        String[] colsProg = {"Nombre", "Código", "Estado", "Duración (sem.)", "Est. Activos"};
        modeloProgramas = new DefaultTableModel(colsProg, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        content.add(envolverTablaEnCard(modeloProgramas, "Detalle de Programas"));

        panel.add(content);
        return panel;
    }

    // ─── TAB AUDITORÍA ───────────────────────────────────────────────────────

    private JPanel crearTabAuditoria() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JTabbedPane subTabs = new JTabbedPane();
        subTabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Sub-tab Actividades
        String[] colsAct = {"Fecha", "Tipo", "Descripción", "Usuario"};
        modeloActividades = new DefaultTableModel(colsAct, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        subTabs.addTab("Actividades del Sistema", envolverTablaEnCard(modeloActividades, null));

        // Sub-tab Logs Financieros
        String[] colsLog = {"Fecha", "Acción", "Detalle", "IP", "Usuario"};
        modeloLogs = new DefaultTableModel(colsLog, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        subTabs.addTab("Logs Financieros", envolverTablaEnCard(modeloLogs, null));

        panel.add(subTabs, BorderLayout.CENTER);
        return panel;
    }

    // ─── Helpers UI ──────────────────────────────────────────────────────────

    private JPanel crearStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(15, 20, 15, 20)));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(COLOR_TEXT);

        JPanel accentBar = new JPanel();
        accentBar.setPreferredSize(new Dimension(4, 0));
        accentBar.setBackground(accentColor);

        card.add(lblTitle,   BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(accentBar,  BorderLayout.WEST);
        return card;
    }

    private JPanel envolverEnCard(JComponent comp, String titulo) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(12, 12, 12, 12)));
        if (titulo != null) {
            JLabel lbl = new JLabel(titulo);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lbl.setForeground(COLOR_TEXT);
            card.add(lbl, BorderLayout.NORTH);
        }
        card.add(comp, BorderLayout.CENTER);
        return card;
    }

    private JPanel envolverTablaEnCard(DefaultTableModel modelo, String titulo) {
        JTable tabla = new JTable(modelo) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? COLOR_CARD : filaAlternada());
                    c.setForeground(COLOR_TEXT);
                }
                return c;
            }
        };
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.setRowHeight(28);
        tabla.setBackground(COLOR_CARD);
        tabla.setForeground(COLOR_TEXT);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(COLOR_BORDER);
        tabla.getTableHeader().setForeground(COLOR_TEXT);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setGridColor(COLOR_BORDER);
        tabla.setShowGrid(true);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(COLOR_PRIMARY);
        tabla.setSelectionForeground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        scroll.getViewport().setBackground(COLOR_CARD);

        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(12, 12, 12, 12)));
        if (titulo != null) {
            JLabel lbl = new JLabel(titulo);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lbl.setForeground(COLOR_TEXT);
            card.add(lbl, BorderLayout.NORTH);
        }
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ─── Carga de datos ──────────────────────────────────────────────────────

    private void cargarDatos() {
        Date inicio = (Date) spinnerInicio.getValue();
        Date fin    = (Date) spinnerFin.getValue();

        // Ajustar fin al final del día
        Calendar cal = Calendar.getInstance();
        cal.setTime(fin);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date finDia = cal.getTime();

        cargarFinanciero(inicio, finDia);
        cargarEstudiantes(inicio, finDia);
        cargarProgramas();
        cargarAuditoria(inicio, finDia);
    }

    private void cargarFinanciero(Date inicio, Date fin) {
        String metodo    = "Todos".equals(comboMetodo.getSelectedItem())           ? null : (String) comboMetodo.getSelectedItem();
        String modalidad = "Todas".equals(comboModalidadFiltro.getSelectedItem())  ? null : modalidadClave((String) comboModalidadFiltro.getSelectedItem());

        Map<String, Double> resumen = controller.getResumenFinanciero(inicio, fin, metodo, modalidad);
        lblTotalEsperado.setText(currencyFormat.format(resumen.getOrDefault("total_esperado", 0.0)));
        lblTotalRecaudado.setText(currencyFormat.format(resumen.getOrDefault("total_recaudado", 0.0)));
        lblSaldoPendiente.setText(currencyFormat.format(resumen.getOrDefault("total_pendiente", 0.0)));
        lblPorcentaje.setText(String.format("%.1f%%", resumen.getOrDefault("porcentaje_recaudo", 0.0)));

        // Donut por modalidad
        Map<String, Integer> modalidades = controller.getDistribucionModalidades(inicio, fin, metodo, modalidad);
        Map<String, Integer> donutModalidad = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : modalidades.entrySet()) {
            donutModalidad.put(formatModalidad(e.getKey()), e.getValue());
        }
        donutEstadoPago.setData(donutModalidad);

        // Barra de ingresos por mes
        barIngresosMes.setData(controller.getIngresosPorMes(inicio, fin, metodo, modalidad));

        // Tabla: Todos los pagos (respeta filtro global de modalidad)
        llenarTablaPagos(modeloTodosPagos, controller.getPagosPorModalidad(inicio, fin, modalidad, metodo), true);

        // Tablas por modalidad específica (si el filtro global restringe, las otras quedan vacías)
        llenarTablaPagos(modeloAbonoSabado,  controller.getPagosPorModalidad(inicio, fin, combinar(modalidad, "ABONO_SABADO"),  metodo), false);
        llenarTablaPagos(modeloAbonoSemana,  controller.getPagosPorModalidad(inicio, fin, combinar(modalidad, "ABONO_SEMANA"),  metodo), false);
        llenarTablaPagos(modeloCarreraTotal, controller.getPagosPorModalidad(inicio, fin, combinar(modalidad, "CARRERA_TOTAL"), metodo), false);
        llenarTablaPagos(modeloMediaCarrera, controller.getPagosPorModalidad(inicio, fin, combinar(modalidad, "MEDIA_CARRERA"), metodo), false);

        // Resumen por estudiante
        modeloCuotaMensual.setRowCount(0);
        for (Map<String, Object> row : controller.getResumenPagosPorEstudiante(inicio, fin, metodo, modalidad)) {
            modeloCuotaMensual.addRow(new Object[]{
                row.get("estudiante"),
                row.get("codigo"),
                row.get("programa"),
                row.get("num_pagos"),
                currencyFormat.format(row.get("total_pagado")),
                row.get("primer_pago") != null ? sdfLargo.format(row.get("primer_pago")) : "-",
                row.get("ultimo_pago") != null ? sdfLargo.format(row.get("ultimo_pago"))  : "-"
            });
        }
    }

    /** Convierte la etiqueta del combo a la clave ENUM de la BD */
    private String modalidadClave(String label) {
        switch (label) {
            case "Abono Sábado":  return "ABONO_SABADO";
            case "Abono Semana":  return "ABONO_SEMANA";
            case "Cuota Mensual": return "CUOTA_MENSUAL";
            case "Carrera Total": return "CARRERA_TOTAL";
            case "Media Carrera": return "MEDIA_CARRERA";
            default:              return null;
        }
    }

    /** Si ya hay un filtro global de modalidad, solo retorna ese; si el tab coincide devuelve el tab, si no null */
    private String combinar(String filtroGlobal, String modalidadTab) {
        if (filtroGlobal == null) return modalidadTab;
        return filtroGlobal.equals(modalidadTab) ? modalidadTab : null; // tab vacío si no coincide
    }

    private void llenarTablaPagos(DefaultTableModel modelo, List<Map<String, Object>> pagos, boolean incluyeModalidad) {
        modelo.setRowCount(0);
        for (Map<String, Object> p : pagos) {
            if (incluyeModalidad) {
                modelo.addRow(new Object[]{
                    p.get("fecha") != null ? sdfLargo.format(p.get("fecha")) : "-",
                    p.get("estudiante"),
                    p.get("codigo"),
                    p.get("programa"),
                    currencyFormat.format(p.get("monto")),
                    formatModalidad((String) p.get("modalidad")),
                    p.getOrDefault("metodo", "-"),
                    p.getOrDefault("nombre_usuario", "-")
                });
            } else {
                modelo.addRow(new Object[]{
                    p.get("fecha") != null ? sdfLargo.format(p.get("fecha")) : "-",
                    p.get("estudiante"),
                    p.get("codigo"),
                    p.get("programa"),
                    currencyFormat.format(p.get("monto")),
                    p.getOrDefault("metodo", "-"),
                    p.getOrDefault("nombre_usuario", "-")
                });
            }
        }
    }

    private String formatModalidad(String m) {
        if (m == null) return "-";
        switch (m) {
            case "ABONO_SABADO":  return "Abono Sábado";
            case "ABONO_SEMANA":  return "Abono Semana";
            case "CUOTA_MENSUAL": return "Cuota Mensual";
            case "CARRERA_TOTAL": return "Carrera Total";
            case "MEDIA_CARRERA": return "Media Carrera";
            default:              return m;
        }
    }

    private void cargarEstudiantes(Date inicio, Date fin) {
        Map<String, Integer> porEstado = controller.getEstudiantesPorEstado();
        int total   = porEstado.values().stream().mapToInt(Integer::intValue).sum();
        int activos = porEstado.getOrDefault("ACTIVO", 0);

        List<Map<String, Object>> mora = controller.getEstudiantesEnMora();
        lblTotalEstudiantes.setText(String.valueOf(total));
        lblActivos.setText(String.valueOf(activos));
        lblEnMora.setText(String.valueOf(mora.size()));

        // Dona con estados de estudiantes
        Map<String, Integer> donutData = new LinkedHashMap<>();
        if (porEstado.getOrDefault("ACTIVO", 0)    > 0) donutData.put("ACTIVO",    porEstado.get("ACTIVO"));
        if (porEstado.getOrDefault("INACTIVO", 0)  > 0) donutData.put("INACTIVO",  porEstado.get("INACTIVO"));
        if (porEstado.getOrDefault("GRADUADO", 0)  > 0) donutData.put("GRADUADO",  porEstado.get("GRADUADO"));
        if (porEstado.getOrDefault("RETIRADO", 0)  > 0) donutData.put("RETIRADO",  porEstado.get("RETIRADO"));
        donutEstudiantes.setData(donutData);

        // Tabla mora
        modeloMora.setRowCount(0);
        for (Map<String, Object> row : mora) {
            modeloMora.addRow(new Object[]{
                row.get("nombre"),
                row.get("codigo"),
                row.get("programa"),
                currencyFormat.format(row.get("saldo_pendiente")),
                row.get("fecha_proximo_pago") != null ? sdfCorto.format(row.get("fecha_proximo_pago")) : "-"
            });
        }
    }

    private void cargarProgramas() {
        List<Map<String, Object>> programas = controller.getResumenProgramas();
        // estado ya llega en UPPER desde el controller
        long activos  = programas.stream().filter(p -> "ACTIVO".equals(p.get("estado"))).count();
        long cerrados = programas.stream().filter(p -> !"ACTIVO".equals(p.get("estado"))).count();

        lblTotalProgramas.setText(String.valueOf(programas.size()));
        lblProgramasActivos.setText(String.valueOf(activos));
        lblProgramasCerrados.setText(String.valueOf(cerrados));

        // BarChart con estudiantes activos reales por programa
        Map<String, Double> barData = new LinkedHashMap<>();
        for (Map<String, Object> p : programas) {
            barData.put((String) p.get("nombre"), ((Number) p.get("estudiantes_activos")).doubleValue());
        }
        barPorPrograma.setData(barData);

        // Tabla sin columna "Inscritos" (proyectado) — muestra estudiantes activos reales
        modeloProgramas.setRowCount(0);
        for (Map<String, Object> p : programas) {
            modeloProgramas.addRow(new Object[]{
                p.get("nombre"),
                p.get("codigo"),
                formatEstado((String) p.get("estado")),
                p.get("duracion_semestres") + " sem.",
                p.get("estudiantes_activos")
            });
        }
    }

    private void cargarAuditoria(Date inicio, Date fin) {
        List<Map<String, Object>> actividades = controller.getActividadesAuditoria(inicio, fin);
        modeloActividades.setRowCount(0);
        for (Map<String, Object> a : actividades) {
            modeloActividades.addRow(new Object[]{
                a.get("fecha") != null ? sdfLargo.format(a.get("fecha")) : "-",
                a.get("tipo"),
                a.get("descripcion"),
                a.getOrDefault("nombre_usuario", "-")
            });
        }

        List<Map<String, Object>> logs = controller.getLogsFinancieros(inicio, fin);
        modeloLogs.setRowCount(0);
        for (Map<String, Object> l : logs) {
            modeloLogs.addRow(new Object[]{
                l.get("fecha") != null ? sdfLargo.format(l.get("fecha")) : "-",
                l.get("accion"),
                l.get("detalle"),
                l.getOrDefault("ip", "-"),
                l.getOrDefault("nombre_usuario", "-")
            });
        }
    }

    private String formatEstado(String estado) {
        if (estado == null) return "-";
        switch (estado) {
            case "ACTIVO":   return "Activo";
            case "CERRADO":  return "Cerrado";
            case "EN_PAUSA": return "En Pausa";
            default:         return estado;
        }
    }

    // ─── Exportar PDF ─────────────────────────────────────────────────────────

    private void exportarPDF() {
        // Solo aplica el diálogo de selección en la pestaña Financiero
        if (tabbedPane.getSelectedIndex() == 0) {
            exportarPDFFinanciero();
        } else {
            exportarPDFSimple();
        }
    }

    private void exportarPDFFinanciero() {
        Date inicio = (Date) spinnerInicio.getValue();
        Date fin    = (Date) spinnerFin.getValue();
        String metodoSel    = "Todos".equals(comboMetodo.getSelectedItem())          ? "Todos"  : (String) comboMetodo.getSelectedItem();
        String modalidadSel = "Todas".equals(comboModalidadFiltro.getSelectedItem()) ? "Todas"  : (String) comboModalidadFiltro.getSelectedItem();

        // ── Diálogo de selección ──────────────────────────────────────────────
        JDialog dlg = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), "Configurar exportación PDF", true);
        dlg.setLayout(new BorderLayout(0, 0));
        dlg.setSize(420, 400);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(20, 24, 12, 24));
        body.setBackground(COLOR_CARD);

        // Título
        JLabel lblTit = new JLabel("Selecciona las secciones a incluir");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTit.setForeground(COLOR_TEXT);
        body.add(lblTit);
        body.add(Box.createVerticalStrut(4));

        // Info de filtros actuales
        JLabel lblFiltros = new JLabel("<html><font color='gray'>Período: " + sdfCorto.format(inicio) +
                " – " + sdfCorto.format(fin) + "&nbsp;&nbsp;|&nbsp;&nbsp;" +
                "Modalidad: " + modalidadSel + "&nbsp;&nbsp;|&nbsp;&nbsp;Método: " + metodoSel + "</font></html>");
        lblFiltros.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        body.add(lblFiltros);
        body.add(Box.createVerticalStrut(16));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        body.add(sep);
        body.add(Box.createVerticalStrut(14));

        // Checkboxes
        JCheckBox chkTodos     = new JCheckBox("Todos los pagos (filtrados)",    true);
        JCheckBox chkSabado    = new JCheckBox("Abono Sábado",                   true);
        JCheckBox chkSemana    = new JCheckBox("Abono Semana",                   true);
        JCheckBox chkResumen   = new JCheckBox("Resumen por estudiante",         true);
        JCheckBox chkCarrera   = new JCheckBox("Carrera Completa",               true);
        JCheckBox chkMedia     = new JCheckBox("Media Carrera",                  true);

        for (JCheckBox chk : new JCheckBox[]{chkTodos, chkSabado, chkSemana, chkResumen, chkCarrera, chkMedia}) {
            chk.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            chk.setForeground(COLOR_TEXT);
            chk.setBackground(COLOR_CARD);
            chk.setAlignmentX(LEFT_ALIGNMENT);
            body.add(chk);
            body.add(Box.createVerticalStrut(4));
        }

        body.add(Box.createVerticalStrut(8));
        JSeparator sep2 = new JSeparator();
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        body.add(sep2);

        // Botón "Seleccionar / Deseleccionar todos"
        JButton btnToggle = new JButton("Deseleccionar todos");
        btnToggle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnToggle.setFocusPainted(false);
        btnToggle.setBorderPainted(false);
        btnToggle.setBackground(COLOR_BG);
        btnToggle.setForeground(COLOR_PRIMARY);
        btnToggle.setAlignmentX(LEFT_ALIGNMENT);
        btnToggle.addActionListener(e -> {
            boolean nuevoEstado = !chkTodos.isSelected();
            for (JCheckBox chk : new JCheckBox[]{chkTodos, chkSabado, chkSemana, chkResumen, chkCarrera, chkMedia})
                chk.setSelected(nuevoEstado);
            btnToggle.setText(nuevoEstado ? "Deseleccionar todos" : "Seleccionar todos");
        });
        body.add(Box.createVerticalStrut(6));
        body.add(btnToggle);

        dlg.add(body, BorderLayout.CENTER);

        // Botones
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        botones.setBackground(COLOR_CARD);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelar.addActionListener(e -> dlg.dispose());

        JButton btnGenerar = new JButton("Generar PDF");
        btnGenerar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGenerar.setBackground(COLOR_PRIMARY);
        btnGenerar.setForeground(Color.WHITE);
        btnGenerar.setFocusPainted(false);
        btnGenerar.setBorderPainted(false);
        btnGenerar.addActionListener(e -> {
            dlg.dispose();
            // Construir mapa de secciones seleccionadas (en orden)
            java.util.LinkedHashMap<String, DefaultTableModel> secciones = new java.util.LinkedHashMap<>();
            if (chkTodos.isSelected()   && modeloTodosPagos.getRowCount()  > 0) secciones.put("Todos los pagos",        modeloTodosPagos);
            if (chkSabado.isSelected()  && modeloAbonoSabado.getRowCount() > 0) secciones.put("Abono Sábado",           modeloAbonoSabado);
            if (chkSemana.isSelected()  && modeloAbonoSemana.getRowCount() > 0) secciones.put("Abono Semana",           modeloAbonoSemana);
            if (chkResumen.isSelected() && modeloCuotaMensual.getRowCount()> 0) secciones.put("Resumen por Estudiante", modeloCuotaMensual);
            if (chkCarrera.isSelected() && modeloCarreraTotal.getRowCount()> 0) secciones.put("Carrera Completa",       modeloCarreraTotal);
            if (chkMedia.isSelected()   && modeloMediaCarrera.getRowCount()> 0) secciones.put("Media Carrera",          modeloMediaCarrera);

            if (secciones.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay datos para exportar con los filtros y secciones seleccionadas.",
                        "Sin datos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            generarPDFFinanciero(inicio, fin, metodoSel, modalidadSel, secciones);
        });

        botones.add(btnCancelar);
        botones.add(btnGenerar);
        dlg.add(botones, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void generarPDFFinanciero(Date inicio, Date fin, String metodo, String modalidad,
                                       java.util.LinkedHashMap<String, DefaultTableModel> secciones) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Reporte_Financiero_" + LocalDate.now() + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 36); // horizontal para tablas anchas
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(fc.getSelectedFile()));
            writer.setPageEvent(new PiePaginaEvento());
            doc.open();

            com.lowagie.text.Font fTitulo  = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD,   new Color(COLOR_PRIMARY.getRed(), COLOR_PRIMARY.getGreen(), COLOR_PRIMARY.getBlue()));
            com.lowagie.text.Font fMeta    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,  com.lowagie.text.Font.NORMAL,  new Color(107, 114, 128));
            com.lowagie.text.Font fSeccion = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD,    new Color(COLOR_PRIMARY.getRed(), COLOR_PRIMARY.getGreen(), COLOR_PRIMARY.getBlue()));
            com.lowagie.text.Font fHead    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,  com.lowagie.text.Font.BOLD,    Color.WHITE);
            com.lowagie.text.Font fBody    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8,  com.lowagie.text.Font.NORMAL,  new Color(17, 24, 39));

            // Cabecera del documento
            Paragraph pTitulo = new Paragraph("AdminNexus — Reporte Financiero", fTitulo);
            pTitulo.setAlignment(Element.ALIGN_LEFT);
            doc.add(pTitulo);

            String filtrosStr = "Período: " + sdfCorto.format(inicio) + " al " + sdfCorto.format(fin)
                    + "   |   Modalidad: " + modalidad
                    + "   |   Método: "    + metodo
                    + "   |   Generado: "  + sdfLargo.format(new Date());
            Paragraph pMeta = new Paragraph(filtrosStr, fMeta);
            pMeta.setSpacingAfter(16);
            doc.add(pMeta);

            // KPI resumen (una línea con los 4 valores)
            String kpiStr = "Total Esperado: " + lblTotalEsperado.getText()
                    + "     Recaudado: "  + lblTotalRecaudado.getText()
                    + "     Pendiente: "  + lblSaldoPendiente.getText()
                    + "     % Recaudo: "  + lblPorcentaje.getText();
            com.lowagie.text.Font fKpi = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, new Color(17, 24, 39));
            Paragraph pKpi = new Paragraph(kpiStr, fKpi);
            pKpi.setSpacingAfter(20);
            doc.add(pKpi);

            // Secciones seleccionadas
            boolean primera = true;
            for (java.util.Map.Entry<String, DefaultTableModel> entry : secciones.entrySet()) {
                if (!primera) doc.add(new com.lowagie.text.Chunk(com.lowagie.text.Chunk.NEXTPAGE));
                primera = false;

                Paragraph pSec = new Paragraph(entry.getKey() + "  (" + entry.getValue().getRowCount() + " registros)", fSeccion);
                pSec.setSpacingBefore(8);
                pSec.setSpacingAfter(6);
                doc.add(pSec);

                agregarTablaPDF(doc, entry.getValue(), fHead, fBody);
            }

            doc.close();
            JOptionPane.showMessageDialog(this,
                    "PDF generado correctamente:\n" + fc.getSelectedFile().getAbsolutePath(),
                    "Exportación exitosa", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarTablaPDF(Document doc, DefaultTableModel modelo,
                                  com.lowagie.text.Font fHead, com.lowagie.text.Font fBody) throws Exception {
        int cols = modelo.getColumnCount();
        PdfPTable tabla = new PdfPTable(cols);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(10);

        for (int c = 0; c < cols; c++) {
            PdfPCell celda = new PdfPCell(new Phrase(modelo.getColumnName(c), fHead));
            celda.setBackgroundColor(new Color(COLOR_PRIMARY.getRed(), COLOR_PRIMARY.getGreen(), COLOR_PRIMARY.getBlue()));
            celda.setPadding(5);
            celda.setBorderColor(Color.WHITE);
            tabla.addCell(celda);
        }
        for (int r = 0; r < modelo.getRowCount(); r++) {
            Color fondo = (r % 2 == 0) ? Color.WHITE : new Color(243, 244, 246);
            for (int c = 0; c < cols; c++) {
                Object val = modelo.getValueAt(r, c);
                PdfPCell celda = new PdfPCell(new Phrase(val != null ? val.toString() : "", fBody));
                celda.setBackgroundColor(fondo);
                celda.setPadding(4);
                celda.setBorderColor(new Color(229, 231, 235));
                tabla.addCell(celda);
            }
        }
        doc.add(tabla);
    }

    private void exportarPDFSimple() {
        String tabNombre = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
        Date inicio = (Date) spinnerInicio.getValue();
        Date fin    = (Date) spinnerFin.getValue();

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Reporte_" + tabNombre + "_" + LocalDate.now() + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        DefaultTableModel modelo;
        switch (tabbedPane.getSelectedIndex()) {
            case 1:  modelo = modeloMora;        break;
            case 2:  modelo = modeloProgramas;   break;
            case 3:  modelo = modeloActividades; break;
            default: modelo = null;
        }

        try {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(fc.getSelectedFile()));
            writer.setPageEvent(new PiePaginaEvento());
            doc.open();

            com.lowagie.text.Font fTitulo = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD,  new Color(COLOR_PRIMARY.getRed(), COLOR_PRIMARY.getGreen(), COLOR_PRIMARY.getBlue()));
            com.lowagie.text.Font fMeta   = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,  com.lowagie.text.Font.NORMAL, new Color(107, 114, 128));
            com.lowagie.text.Font fHead   = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,  com.lowagie.text.Font.BOLD,   Color.WHITE);
            com.lowagie.text.Font fBody   = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,  com.lowagie.text.Font.NORMAL, new Color(17, 24, 39));

            Paragraph pTitulo = new Paragraph("AdminNexus — Reporte de " + tabNombre, fTitulo);
            pTitulo.setAlignment(Element.ALIGN_LEFT);
            doc.add(pTitulo);

            Paragraph pMeta = new Paragraph(
                    "Período: " + sdfCorto.format(inicio) + " al " + sdfCorto.format(fin) +
                    "   |   Generado: " + sdfLargo.format(new Date()), fMeta);
            pMeta.setSpacingAfter(16);
            doc.add(pMeta);

            if (modelo != null && modelo.getRowCount() > 0) {
                agregarTablaPDF(doc, modelo, fHead, fBody);
            }

            doc.close();
            JOptionPane.showMessageDialog(this,
                    "PDF generado correctamente:\n" + fc.getSelectedFile().getAbsolutePath(),
                    "Exportación exitosa", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class PiePaginaEvento extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            com.lowagie.text.Font f = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL, new Color(107, 114, 128));
            Phrase pie = new Phrase("Página " + writer.getPageNumber() + " — AdminNexus", f);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, pie,
                (document.left() + document.right()) / 2, document.bottom() - 10, 0);
        }
    }

    // ─── Gráfica de Dona ─────────────────────────────────────────────────────

    class DonutChart extends JComponent {
        private Map<String, Integer> data;

        DonutChart(String titulo) {
        }

        public void setData(Map<String, Integer> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth() - 10, getHeight() - 10) - 30;
            int cx   = getWidth() / 2;
            int cy   = getHeight() / 2;
            int x    = cx - size / 2;
            int y    = cy - size / 2;

            if (data == null || data.isEmpty()) {
                g2.setColor(COLOR_BORDER);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x, y, size, size);
                g2.dispose();
                return;
            }

            double total = data.values().stream().mapToInt(Integer::intValue).sum();
            if (total == 0) total = 1;

            int startAngle = 90;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int angle = (int) Math.round((entry.getValue() / total) * 360.0);
                g2.setColor(getColorForKey(entry.getKey()));
                g2.fillArc(x, y, size, size, startAngle, angle);
                startAngle += angle;
            }

            // Agujero
            g2.setColor(COLOR_CARD);
            int holeSize = (int) (size * 0.58);
            g2.fillOval(cx - holeSize / 2, cy - holeSize / 2, holeSize, holeSize);

            // Leyenda
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int ly = 16;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                g2.setColor(getColorForKey(entry.getKey()));
                g2.fillRect(6, ly - 8, 8, 8);
                g2.setColor(COLOR_TEXT);
                g2.drawString(formatLeyenda(entry.getKey()) + ": " + entry.getValue(), 18, ly);
                ly += 16;
            }

            g2.dispose();
        }

        private final Color[] PALETTE = {
            COLOR_PRIMARY, COLOR_SUCCESS, COLOR_WARNING, COLOR_DANGER,
            new Color(139, 92, 246), new Color(6, 182, 212), new Color(251, 146, 60)
        };

        private Color getColorForKey(String key) {
            // Estados de pago
            switch (key) {
                case "AL_DIA": case "ACTIVO":             return COLOR_SUCCESS;
                case "CON_SALDO": case "GRADUADO":        return new Color(59, 130, 246);
                case "POR_VENCER": case "INACTIVO":       return COLOR_WARNING;
                case "ATRASADO": case "RETIRADO":         return COLOR_DANGER;
                // Modalidades (ya formateadas)
                case "Abono Sábado":  return COLOR_PRIMARY;
                case "Abono Semana":  return COLOR_SUCCESS;
                case "Cuota Mensual": return COLOR_WARNING;
                case "Carrera Total": return new Color(139, 92, 246);
                case "Media Carrera": return COLOR_DANGER;
                default:
                    // Color basado en hash para claves desconocidas
                    int idx = Math.abs(key.hashCode()) % PALETTE.length;
                    return PALETTE[idx];
            }
        }

        private String formatLeyenda(String key) {
            return key; // ya viene formateado desde cargarFinanciero
        }
    }

    // ─── Gráfica de Barras ───────────────────────────────────────────────────

    class BarChart extends JComponent {
        private Map<String, Double> data;
        private int hoveredBarIndex = -1;
        private final List<Rectangle> barBounds = new ArrayList<>();

        BarChart(String titulo) {
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int prev = hoveredBarIndex;
                    hoveredBarIndex = -1;
                    for (int i = 0; i < barBounds.size(); i++) {
                        if (barBounds.get(i).contains(e.getPoint())) { hoveredBarIndex = i; break; }
                    }
                    if (prev != hoveredBarIndex) repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) {
                    hoveredBarIndex = -1; repaint();
                }
            });
        }

        public void setData(Map<String, Double> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (data == null || data.isEmpty()) {
                g2.setColor(COLOR_TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.drawString("Sin datos", 20, getHeight() / 2);
                g2.dispose();
                return;
            }

            double max  = data.values().stream().max(Double::compare).orElse(1.0);
            if (max == 0) max = 1;
            double yMax = Math.ceil(max / 1000.0) * 1000.0;
            if (yMax == 0) yMax = 1000.0;

            int lM = 60, rM = 10, tM = 30, bM = 40;
            int chartW = getWidth()  - lM - rM;
            int chartH = getHeight() - tM - bM;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();

            // Cuadrícula
            for (int i = 0; i <= 4; i++) {
                int yLine = tM + chartH - (i * chartH / 4);
                double val = (yMax / 4) * i;
                g2.setColor(COLOR_BORDER);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f}, 0f));
                g2.drawLine(lM, yLine, getWidth() - rM, yLine);
                g2.setColor(COLOR_TEXT_MUTED);
                String lbl = val >= 1000 ? String.format("%.0fk", val / 1000) : String.format("%.0f", val);
                g2.drawString(lbl, lM - fm.stringWidth(lbl) - 6, yLine + 4);
            }

            int barSpacing = 12;
            int barWidth   = Math.min(50, (chartW / Math.max(1, data.size())) - barSpacing);
            int totalW     = data.size() * (barWidth + barSpacing) - barSpacing;
            int startX     = lM + (chartW - totalW) / 2;

            barBounds.clear();
            int idx = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double val = entry.getValue();
                int barH   = (int) ((val / yMax) * chartH);
                if (val > 0 && barH < 4) barH = 4;
                int bx = startX + idx * (barWidth + barSpacing);
                int by = tM + chartH - barH;

                barBounds.add(new Rectangle(bx, by, barWidth, barH));
                boolean hovered = (idx == hoveredBarIndex);

                Color base = hovered ? COLOR_PRIMARY.brighter() : COLOR_PRIMARY;
                GradientPaint gp = new GradientPaint(bx, by,
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), 180),
                        bx, by + barH, base);
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(1f));
                g2.fillRoundRect(bx, by, barWidth, barH + 8, 8, 8);
                g2.fillRect(bx, by + barH - 4, barWidth, 4);

                // Etiqueta eje X
                g2.setColor(hovered ? COLOR_TEXT : COLOR_TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", hovered ? Font.BOLD : Font.PLAIN, 9));
                String key = entry.getKey();
                // Truncar si es muy largo
                if (key.length() > 12) key = key.substring(0, 10) + "..";
                g2.drawString(key, bx + (barWidth - g2.getFontMetrics().stringWidth(key)) / 2, tM + chartH + 18);

                // Valor sobre barra
                if (hovered || val > 0) {
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    String valStr = val >= 1000 ? String.format("%.0fk", val / 1000) : String.format("%.0f", val);
                    g2.setColor(hovered ? COLOR_PRIMARY : COLOR_TEXT_MUTED);
                    g2.drawString(valStr, bx + (barWidth - g2.getFontMetrics().stringWidth(valStr)) / 2, by - 5);
                }
                idx++;
            }

            // Eje X base
            g2.setColor(COLOR_BORDER);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(lM, tM + chartH, getWidth() - rM, tM + chartH);
            g2.dispose();
        }
    }
}
