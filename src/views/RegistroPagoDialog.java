package views;

import controller.PagosController;
import controller.PagoService;
import model.PagoRealizado;
import model.Usuario;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.io.File;

/**
 * Diálogo premium para registrar pagos con lógica de cuotas y descuentos.
 */
public class RegistroPagoDialog extends JDialog {

    private Color COLOR_BG = Color.WHITE;
    private Color COLOR_TEXT = new Color(31, 41, 55);
    private Color COLOR_PRIMARY = new Color(26, 86, 219);
    private Color COLOR_BORDER = new Color(229, 231, 235);

    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
    
    private JTextField txtMonto;
    private JComboBox<PagoRealizado.ModalidadPago> cbModalidad;
    private JComboBox<String> cbMetodo;
    private JTextField txtCuota;
    private JLabel lblNombreArchivo;
    private JLabel lblAdjunto;
    private JButton btnSubir;
    private File archivoOrden;
    
    private final int idEstudiante;
    private final Usuario usuarioActual;
    private final PagosController pagosController;
    private Map<String, Object> dataEstudiante;
    private boolean success = false;
    private boolean esPreferencial = false;

    public RegistroPagoDialog(Window owner, Usuario usuario, int idEstudiante) {
        this(owner, usuario, idEstudiante, false);
    }

    public RegistroPagoDialog(Window owner, Usuario usuario, int idEstudiante, boolean esPreferencial) {
        super(owner, esPreferencial ? "Pago Preferencial" : "Registro de Pago Seguro", Dialog.ModalityType.APPLICATION_MODAL);
        this.idEstudiante = idEstudiante;
        this.usuarioActual = usuario;
        this.esPreferencial = esPreferencial;
        this.pagosController = new PagosController();
        
        cargarConfiguracion();
        initData();
        initComponents();
        
        setSize(440, esPreferencial ? 565 : 590);
        setLocationRelativeTo(owner);
    }

    private void cargarConfiguracion() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("AdminNexus").node("Config").node("User_" + usuarioActual.getIdusuario());
        if (prefs.get("app_mode", "light").equals("dark")) {
            COLOR_BG = new Color(17, 24, 39);
            COLOR_TEXT = Color.WHITE;
            COLOR_BORDER = new Color(55, 65, 81);
        }
    }

    private void initData() {
        dataEstudiante = pagosController.obtenerDetalleFinancieroEstudiante(idEstudiante);
    }

    private void initComponents() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(COLOR_BG);
        main.setBorder(new EmptyBorder(18, 28, 18, 28));

        // Header
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 5));
        header.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Registrar Transacción");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_PRIMARY);
        
        JLabel lblSub = new JLabel("Estudiante: " + (dataEstudiante != null ? dataEstudiante.get("nombre") : "Desconocido"));
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT);
        
        header.add(lblTitle);
        header.add(lblSub);
        header.add(Box.createVerticalStrut(6));

        // Formulario
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.setOpaque(false);
        form.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        // Lógica de cuotas sugerida
        double saldoPendiente = dataEstudiante != null ? (double) dataEstudiante.get("saldo") : 0;
        double cuotaSugerida = calcularCuotaSugerida();
        
        txtMonto = crearCampoTexto(String.valueOf((int)cuotaSugerida), "Monto a Pagar ($)");
        
        cbModalidad = new JComboBox<>(PagoRealizado.ModalidadPago.values());
        estilizarCombo(cbModalidad, "Modalidad de Cobro");

        String[] metodos = {"Bancolombia", "Nequi", "Efectivo"};
        cbMetodo = new JComboBox<>(metodos);
        estilizarCombo(cbMetodo, "Método de Pago");

        int cuotasPagadas = dataEstudiante != null ? (int) dataEstudiante.get("cuotas_pagadas") : 0;
        int cuotasTotales = dataEstudiante != null ? (int) dataEstudiante.get("cuotas_totales") : 5;
        int siguienteCuota = Math.min(cuotasPagadas + 1, cuotasTotales);
        txtCuota = crearCampoTexto(String.valueOf(siguienteCuota), "Número de Cuota");

        // Panel de datos bancarios (visible según método seleccionado)
        JPanel panelInfoBanco = new JPanel(new BorderLayout());
        panelInfoBanco.setBackground(new Color(239, 246, 255));
        panelInfoBanco.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(191, 219, 254)),
            new EmptyBorder(10, 14, 10, 14)
        ));
        JLabel lblInfoBanco = new JLabel();
        lblInfoBanco.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfoBanco.setForeground(new Color(30, 64, 175));
        panelInfoBanco.add(lblInfoBanco, BorderLayout.CENTER);

        Runnable actualizarInfoBanco = () -> {
            String metodo = cbMetodo.getSelectedItem() != null ? cbMetodo.getSelectedItem().toString() : "";
            switch (metodo) {
                case "Bancolombia":
                    lblInfoBanco.setText("<html><b>Cuenta Bancolombia</b><br>"
                        + "Entidad Técnica al Servicio Técnico de Formación<br>"
                        + "N° de cuenta: <b>526-000079-88</b></html>");
                    panelInfoBanco.setVisible(true);
                    break;
                case "Nequi":
                    lblInfoBanco.setText("<html><b>Nequi</b><br>"
                        + "Yulibeth Illidge<br>"
                        + "Número: <b>301 347 4891</b></html>");
                    panelInfoBanco.setVisible(true);
                    break;
                default:
                    panelInfoBanco.setVisible(false);
                    break;
            }
        };
        actualizarInfoBanco.run();
        cbMetodo.addActionListener(e -> actualizarInfoBanco.run());

        form.add(crearLabel("MONTO A PAGAR"));
        form.add(txtMonto);
        form.add(crearLabel("MODALIDAD"));
        form.add(cbModalidad);
        form.add(crearLabel("MÉTODO DE PAGO"));
        form.add(cbMetodo);
        form.add(crearLabel("NÚMERO DE CUOTA A PAGAR"));
        form.add(txtCuota);

        // Sección de Adjunto (Solo si no es preferencial)
        JPanel filePanel = new JPanel(new BorderLayout(10, 0));
        filePanel.setOpaque(false);
        if (!esPreferencial) {
            lblAdjunto = crearLabel("ADJUNTAR COMPROBANTE DE TRANSACCIÓN (Requerido)");
            form.add(lblAdjunto);
            
            // Restringir modalidades para registro normal (Excluir preferenciales)
            cbModalidad.setModel(new DefaultComboBoxModel<>(new PagoRealizado.ModalidadPago[]{
                PagoRealizado.ModalidadPago.ABONO_SABADO,
                PagoRealizado.ModalidadPago.ABONO_SEMANA,
                PagoRealizado.ModalidadPago.CUOTA_MENSUAL
            }));
            
            btnSubir = new JButton("Seleccionar Archivo (PDF, Imagen)");
            btnSubir.setPreferredSize(new Dimension(200, 34));
            btnSubir.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnSubir.addActionListener(e -> seleccionarArchivo());
            
            lblNombreArchivo = new JLabel("Ningún archivo seleccionado");
            lblNombreArchivo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblNombreArchivo.setForeground(COLOR_TEXT);
            
            filePanel.add(btnSubir, BorderLayout.WEST);
            filePanel.add(lblNombreArchivo, BorderLayout.CENTER);
            form.add(filePanel);

            // Listener reactivo para habilitar/deshabilitar carga de archivos
            cbMetodo.addActionListener(e -> {
                boolean esEfectivo = "Efectivo".equals(cbMetodo.getSelectedItem().toString());
                if (esEfectivo) {
                    lblAdjunto.setText("ADJUNTAR COMPROBANTE (No requerido para Efectivo)");
                    lblNombreArchivo.setText("No requerido para Efectivo");
                    lblNombreArchivo.setForeground(Color.GRAY);
                    btnSubir.setEnabled(false);
                    archivoOrden = null;
                } else {
                    lblAdjunto.setText("ADJUNTAR COMPROBANTE DE TRANSACCIÓN (Requerido)");
                    btnSubir.setEnabled(true);
                    if (archivoOrden == null) {
                        lblNombreArchivo.setText("Ningún archivo seleccionado");
                        lblNombreArchivo.setForeground(COLOR_TEXT);
                    } else {
                        lblNombreArchivo.setText("✓ " + archivoOrden.getName());
                        lblNombreArchivo.setForeground(new Color(34, 197, 94));
                    }
                }
            });

            // Lógica de habilitación inicial
            boolean esEfectivo = "Efectivo".equals(cbMetodo.getSelectedItem().toString());
            if (esEfectivo) {
                lblAdjunto.setText("ADJUNTAR COMPROBANTE (No requerido para Efectivo)");
                lblNombreArchivo.setText("No requerido para Efectivo");
                lblNombreArchivo.setForeground(Color.GRAY);
                btnSubir.setEnabled(false);
            }
        } else {
            // Lógica especial para preferencial
            lblTitle.setText("Pago Preferencial");
            
            // Restringir opciones de modalidad
            cbModalidad.setModel(new DefaultComboBoxModel<>(new PagoRealizado.ModalidadPago[]{
                PagoRealizado.ModalidadPago.CARRERA_TOTAL,
                PagoRealizado.ModalidadPago.MEDIA_CARRERA
            }));
            
            double total = (double) dataEstudiante.get("monto_total");
            txtMonto.setText(String.valueOf((int)total));
            cbModalidad.setSelectedItem(PagoRealizado.ModalidadPago.CARRERA_TOTAL);
            txtCuota.setText("0");
            
            cbModalidad.addActionListener(e -> {
                if (cbModalidad.getSelectedItem() == PagoRealizado.ModalidadPago.CARRERA_TOTAL) {
                    txtMonto.setText(String.valueOf((int)total));
                    txtCuota.setText("0");
                } else if (cbModalidad.getSelectedItem() == PagoRealizado.ModalidadPago.MEDIA_CARRERA) {
                    txtMonto.setText(String.valueOf((int)(total / 2)));
                    txtCuota.setText("1/2");
                }
            });
        }

        // Footer Informativo
        JPanel infoFooter = new JPanel(new BorderLayout());
        infoFooter.setOpaque(false);
        infoFooter.setBorder(new EmptyBorder(10, 0, 0, 0));
        JLabel lblSaldo = new JLabel("Saldo pendiente tras este pago: Calculando...");
        lblSaldo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblSaldo.setForeground(COLOR_TEXT);
        infoFooter.add(lblSaldo);

        final double finalSaldo = saldoPendiente;
        Runnable actualizarSaldo = () -> {
            try {
                String text = txtMonto.getText().trim();
                if (text.isEmpty()) {
                    lblSaldo.setText("Saldo pendiente tras este pago: " + currencyFormat.format(finalSaldo));
                    return;
                }
                double m = Double.parseDouble(text);
                lblSaldo.setText("Saldo pendiente tras este pago: " + currencyFormat.format(finalSaldo - m));
            } catch (Exception ex) {
                lblSaldo.setText("Monto inválido");
            }
        };

        txtMonto.getDocument().addDocumentListener(new SaldoDocumentListener(actualizarSaldo));

        // Inicializar el saldo de inmediato
        actualizarSaldo.run();

        // Botones
        JPanel buttons = new JPanel(new GridLayout(1, 2, 15, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(14, 0, 0, 0));
        
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        
        JButton btnConfirmar = new JButton("Confirmar Pago");
        btnConfirmar.setBackground(COLOR_PRIMARY);
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirmar.addActionListener(e -> procesarPago());

        buttons.add(btnCancelar);
        buttons.add(btnConfirmar);

        // Wrapper BoxLayout: form + panelInfoBanco
        // BoxLayout respeta visibilidad: panelInfoBanco no ocupa espacio cuando está oculto
        panelInfoBanco.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panelInfoBanco.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JPanel centerWrapper = new JPanel();
        centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.Y_AXIS));
        centerWrapper.setOpaque(false);
        centerWrapper.add(form);
        centerWrapper.add(panelInfoBanco);

        main.add(header, BorderLayout.NORTH);
        main.add(centerWrapper, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(infoFooter, BorderLayout.NORTH);
        southPanel.add(buttons, BorderLayout.CENTER);
        main.add(southPanel, BorderLayout.SOUTH);

        add(main);
    }

    private double calcularCuotaSugerida() {
        if (dataEstudiante == null) return 100000;
        
        double montoTotal = (double) dataEstudiante.get("monto_total");
        int cuotasTotales = (int) dataEstudiante.get("cuotas_totales");
        if (cuotasTotales <= 0) cuotasTotales = 5;
        
        // Si no hay descuento, el monto total debería ser 500,000 (según reglas de negocio previas)
        // Pero usamos el valor real almacenado en el plan de pago.
        return montoTotal / cuotasTotales;
    }

    private JTextField crearCampoTexto(String value, String hint) {
        JTextField tf = new JTextField(value);
        tf.setPreferredSize(new Dimension(0, 34));
        tf.setBackground(COLOR_BG);
        tf.setForeground(COLOR_TEXT);
        tf.setCaretColor(COLOR_PRIMARY);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1),
            new EmptyBorder(0, 10, 0, 10)
        ));
        return tf;
    }

    private void estilizarCombo(JComboBox<?> cb, String title) {
        cb.setPreferredSize(new Dimension(0, 34));
        cb.setBackground(COLOR_BG);
        cb.setForeground(COLOR_TEXT);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void seleccionarArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar Comprobante de Transacción");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF e Imágenes", "pdf", "jpg", "jpeg", "png"));
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            archivoOrden = chooser.getSelectedFile();
            lblNombreArchivo.setText("✓ " + archivoOrden.getName());
            lblNombreArchivo.setForeground(new Color(34, 197, 94));
        }
    }

    private JLabel crearLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(COLOR_PRIMARY);
        return l;
    }

    private void procesarPago() {
        try {
            String metodo = cbMetodo.getSelectedItem().toString();
            boolean requiereArchivo = !esPreferencial && !"Efectivo".equals(metodo);
            
            if (requiereArchivo && archivoOrden == null) {
                throw new Exception("Es obligatorio adjuntar el comprobante de transferencia para proceder.");
            }
            
            double monto = Double.parseDouble(txtMonto.getText());
            if (monto <= 0) throw new Exception("El monto debe ser mayor a cero.");

            PagoRealizado pago = new PagoRealizado();
            pago.setEstudianteId(idEstudiante);
            pago.setMonto(monto);
            pago.setModalidad((PagoRealizado.ModalidadPago) cbModalidad.getSelectedItem());
            pago.setMetodoPago(metodo);
            pago.setComprobante("Cuota #" + txtCuota.getText());
            pago.setFecha(java.time.LocalDateTime.now());

            // Procesar y copiar el archivo si corresponde
            if (requiereArchivo && archivoOrden != null) {
                File folder = new File("uploads/comprobantes");
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                
                String ext = "";
                String name = archivoOrden.getName();
                int lastDot = name.lastIndexOf('.');
                if (lastDot > 0) {
                    ext = name.substring(lastDot);
                }
                
                String nuevoNombre = "comprobante_" + idEstudiante + "_" + System.currentTimeMillis() + ext;
                File destino = new File(folder, nuevoNombre);
                
                // Copiar archivo físico
                java.nio.file.Files.copy(archivoOrden.toPath(), destino.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // Guardar la ruta relativa en el objeto PagoRealizado
                pago.setComprobanteRuta("uploads/comprobantes/" + nuevoNombre);
            }

            PagoService service = new PagoService();
            if (service.registrarPagoTransaccional(pago, usuarioActual.getIdusuario(), "Localhost", usuarioActual.getNombreAdmin())) {
                success = true;
                
                // Mostrar recibo
                Map<String, Object> updatedData = pagosController.obtenerDetalleFinancieroEstudiante(idEstudiante);
                pago.setSaldoRestante((double) updatedData.get("saldo"));
                
                Window owner = SwingUtilities.getWindowAncestor(this);
                dispose();
                
                ReciboPagoDialog recibo = new ReciboPagoDialog(owner, 
                                                               (String) updatedData.get("nombre"), 
                                                               (String) updatedData.get("programa"), 
                                                               pago);
                recibo.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Error al procesar el pago en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese un monto válido.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    private static class SaldoDocumentListener implements javax.swing.event.DocumentListener {
        private final Runnable callback;
        SaldoDocumentListener(Runnable cb) { this.callback = cb; }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
    }
}
