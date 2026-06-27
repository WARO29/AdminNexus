package views;

import controller.PagosController;
import model.Usuario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Diálogo para anular un pago registrado de un estudiante.
 */
public class AnularPagoDialog extends JDialog {

    private final Color COLOR_BG = Color.WHITE;
    private final Color COLOR_TEXT = new Color(31, 41, 55);
    private final Color COLOR_PRIMARY = new Color(26, 86, 219);
    private final Color COLOR_DANGER = new Color(220, 38, 38);
    private final Color COLOR_BORDER = new Color(229, 231, 235);

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final int idEstudiante;
    private final Usuario usuarioActual;
    private final PagosController pagosController;

    private JTable tablaPagos;
    private DefaultTableModel modeloTabla;
    private JTextArea txtMotivo;
    private JButton btnConfirmar;

    private boolean success = false;

    public AnularPagoDialog(Window owner, Usuario usuario, int idEstudiante) {
        super(owner, "Anular Pago", Dialog.ModalityType.APPLICATION_MODAL);
        this.idEstudiante = idEstudiante;
        this.usuarioActual = usuario;
        this.pagosController = new PagosController();

        initComponents();
        cargarPagos();

        setSize(620, 500);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(COLOR_BG);
        main.setBorder(new EmptyBorder(24, 28, 20, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Anular Pago");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(COLOR_DANGER);

        JLabel lblSub = new JLabel("Seleccione el pago a revertir e indique el motivo.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(107, 114, 128));

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 3));
        headerText.setOpaque(false);
        headerText.add(lblTitle);
        headerText.add(lblSub);
        header.add(headerText, BorderLayout.CENTER);

        // Tabla de pagos
        String[] cols = {"#", "Fecha", "Monto", "Modalidad", "Método", "Concepto"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaPagos = new JTable(modeloTabla);
        tablaPagos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaPagos.setRowHeight(32);
        tablaPagos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaPagos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPagos.setFillsViewportHeight(true);
        tablaPagos.setGridColor(COLOR_BORDER);
        tablaPagos.setShowGrid(true);

        // Anchos de columna
        tablaPagos.getColumnModel().getColumn(0).setMaxWidth(40);
        tablaPagos.getColumnModel().getColumn(1).setPreferredWidth(120);
        tablaPagos.getColumnModel().getColumn(2).setPreferredWidth(100);
        tablaPagos.getColumnModel().getColumn(3).setPreferredWidth(110);
        tablaPagos.getColumnModel().getColumn(4).setPreferredWidth(100);
        tablaPagos.getColumnModel().getColumn(5).setPreferredWidth(90);

        JScrollPane scrollTabla = new JScrollPane(tablaPagos);
        scrollTabla.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        scrollTabla.setPreferredSize(new Dimension(0, 180));

        // Motivo
        JLabel lblMotivo = new JLabel("MOTIVO DE ANULACIÓN (obligatorio)");
        lblMotivo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblMotivo.setForeground(COLOR_PRIMARY);

        txtMotivo = new JTextArea(3, 0);
        txtMotivo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtMotivo.setLineWrap(true);
        txtMotivo.setWrapStyleWord(true);
        txtMotivo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            new EmptyBorder(6, 10, 6, 10)
        ));

        JScrollPane scrollMotivo = new JScrollPane(txtMotivo);
        scrollMotivo.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        JPanel motivoPanel = new JPanel(new BorderLayout(0, 6));
        motivoPanel.setOpaque(false);
        motivoPanel.add(lblMotivo, BorderLayout.NORTH);
        motivoPanel.add(scrollMotivo, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(scrollTabla, BorderLayout.CENTER);
        center.add(motivoPanel, BorderLayout.SOUTH);

        // Aviso de advertencia
        JPanel aviso = new JPanel(new BorderLayout());
        aviso.setBackground(new Color(254, 242, 242));
        aviso.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(252, 165, 165)),
            new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel lblAviso = new JLabel("<html><b>Atención:</b> Esta acción revertirá el saldo del estudiante y reducirá las cuotas pagadas. Esta operación queda registrada en auditoría.</html>");
        lblAviso.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblAviso.setForeground(new Color(185, 28, 28));
        aviso.add(lblAviso);

        // Botones
        btnConfirmar = new JButton("Confirmar Anulación");
        btnConfirmar.setBackground(COLOR_DANGER);
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirmar.setPreferredSize(new Dimension(180, 38));
        btnConfirmar.setEnabled(false);
        btnConfirmar.addActionListener(e -> confirmarAnulacion());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancelar.setPreferredSize(new Dimension(100, 38));
        btnCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnConfirmar);

        JPanel south = new JPanel(new BorderLayout(0, 12));
        south.setOpaque(false);
        south.add(aviso, BorderLayout.NORTH);
        south.add(botones, BorderLayout.SOUTH);

        main.add(header, BorderLayout.NORTH);
        main.add(center, BorderLayout.CENTER);
        main.add(south, BorderLayout.SOUTH);

        add(main);

        // Habilitar botón solo cuando hay fila seleccionada Y motivo escrito
        tablaPagos.getSelectionModel().addListSelectionListener(e -> actualizarEstadoBoton());
        txtMotivo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizarEstadoBoton(); }
            @Override public void removeUpdate(DocumentEvent e) { actualizarEstadoBoton(); }
            @Override public void changedUpdate(DocumentEvent e) { actualizarEstadoBoton(); }
        });
    }

    private void cargarPagos() {
        modeloTabla.setRowCount(0);
        List<Map<String, Object>> pagos = pagosController.obtenerPagosAnulables(idEstudiante);

        if (pagos.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Este estudiante no tiene pagos registrados para anular.",
                "Sin pagos", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        int num = 1;
        for (Map<String, Object> p : pagos) {
            modeloTabla.addRow(new Object[]{
                num++,
                p.get("fecha") != null ? dateFormat.format(p.get("fecha")) : "N/A",
                currencyFormat.format(p.get("monto")),
                formatearModalidad((String) p.get("modalidad")),
                p.getOrDefault("metodo", "N/A"),
                p.getOrDefault("comprobante", "-")
            });
        }
    }

    private void actualizarEstadoBoton() {
        boolean filaSeleccionada = tablaPagos.getSelectedRow() >= 0;
        boolean tieneMotivo = !txtMotivo.getText().trim().isEmpty();
        btnConfirmar.setEnabled(filaSeleccionada && tieneMotivo);
    }

    private void confirmarAnulacion() {
        int fila = tablaPagos.getSelectedRow();
        if (fila < 0) return;

        String motivo = txtMotivo.getText().trim();
        if (motivo.isEmpty()) return;

        // Obtener el id_pago real de la lista
        List<Map<String, Object>> pagos = pagosController.obtenerPagosAnulables(idEstudiante);
        if (fila >= pagos.size()) return;

        Map<String, Object> pagoSeleccionado = pagos.get(fila);
        int idPago = (int) pagoSeleccionado.get("id_pago");
        String montoStr = currencyFormat.format(pagoSeleccionado.get("monto"));

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html>¿Confirma la anulación del pago de <b>" + montoStr + "</b>?<br>" +
            "Esta acción revertirá el saldo y las cuotas del estudiante.</html>",
            "Confirmar Anulación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = pagosController.anularPago(idPago, idEstudiante, usuarioActual.getIdusuario(), motivo, "Localhost", usuarioActual.getNombreAdmin());
        if (ok) {
            success = true;
            JOptionPane.showMessageDialog(this,
                "Pago anulado correctamente. El saldo del estudiante ha sido restaurado.",
                "Anulación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                "No se pudo anular el pago. Intente nuevamente.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatearModalidad(String modalidad) {
        if (modalidad == null) return "N/A";
        switch (modalidad) {
            case "CUOTA_MENSUAL":  return "Cuota Mensual";
            case "ABONO_SABADO":   return "Abono Sábado";
            case "ABONO_SEMANA":   return "Abono Semana";
            case "CARRERA_TOTAL":  return "Carrera Total";
            case "MEDIA_CARRERA":  return "Media Carrera";
            default:               return modalidad;
        }
    }

    public boolean isSuccess() {
        return success;
    }
}
