package controller;

import config.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import model.LogAuditoria;
import model.PagoRealizado;
import model.PlanPago;

/**
 * Servicio centralizado para manejar la lógica de pagos con transacciones seguras.
 */
public class PagoService {

    /**
     * Registra un pago y actualiza el saldo de forma transaccional.
     * 
     * @param pago El pago a registrar
     * @param idUsuario El usuario del sistema que registra el pago (idusuario)
     * @param ipDispositivo IP o nombre del dispositivo para auditoría
     * @return true si la transacción fue exitosa, false en caso contrario
     */
    public boolean registrarPagoTransaccional(PagoRealizado pago, int idUsuario, String ipDispositivo) {
        Connection conn = null;
        try {
            conn = Database.getConexion();
            // Deshabilitar autocommit para iniciar transacción
            conn.setAutoCommit(false);

            // 1. Obtener saldo actual del Plan de Pago (id_estudiante)
            double saldoActual = 0;
            int cuotasPagadasActuales = 0;
            String sqlPlan = "SELECT saldo_pendiente, cuotas_pagadas FROM planes_pago WHERE id_estudiante = ?";
            try (PreparedStatement psPlan = conn.prepareStatement(sqlPlan)) {
                psPlan.setInt(1, pago.getEstudianteId());
                try (ResultSet rs = psPlan.executeQuery()) {
                    if (rs.next()) {
                        saldoActual = rs.getDouble("saldo_pendiente");
                        cuotasPagadasActuales = rs.getInt("cuotas_pagadas");
                    } else {
                        throw new SQLException("No se encontró el plan de pago para el estudiante especificado.");
                    }
                }
            }

            // 2. Calcular nuevo saldo y estado
            double nuevoSaldo = saldoActual - pago.getMonto();
            if (nuevoSaldo < 0) nuevoSaldo = 0; 
            
            pago.setSaldoRestante(nuevoSaldo);
            
            // 3. Determinar cuotas pagadas según modalidad
            if (pago.getModalidad() == PagoRealizado.ModalidadPago.CARRERA_TOTAL) {
                // Obtener totales para setear al máximo
                String sqlTot = "SELECT cuotas_totales FROM planes_pago WHERE id_estudiante = ?";
                try (PreparedStatement psTot = conn.prepareStatement(sqlTot)) {
                    psTot.setInt(1, pago.getEstudianteId());
                    try (ResultSet rs = psTot.executeQuery()) {
                        if (rs.next()) cuotasPagadasActuales = rs.getInt("cuotas_totales");
                    }
                }
            } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.MEDIA_CARRERA) {
                String sqlTot = "SELECT cuotas_totales FROM planes_pago WHERE id_estudiante = ?";
                try (PreparedStatement psTot = conn.prepareStatement(sqlTot)) {
                    psTot.setInt(1, pago.getEstudianteId());
                    try (ResultSet rs = psTot.executeQuery()) {
                        if (rs.next()) {
                            int total = rs.getInt("cuotas_totales");
                            // Se registra al menos la mitad de las cuotas totales
                            cuotasPagadasActuales = Math.max(cuotasPagadasActuales + 1, (int) Math.ceil(total / 2.0));
                        }
                    }
                }
            } else {
                cuotasPagadasActuales++;
            }
            
            String nuevoEstado = "AL_DIA";
            java.time.LocalDate proximaFecha = null;
            java.time.LocalDate hoy = java.time.LocalDate.now();
            
            if (pago.getModalidad() == PagoRealizado.ModalidadPago.CUOTA_MENSUAL) {
                // Obtener la fecha de vencimiento actual para sumarle 30 días laborales
                String sqlFechaActual = "SELECT fecha_proximo_pago FROM planes_pago WHERE id_estudiante = ?";
                try (PreparedStatement psFecha = conn.prepareStatement(sqlFechaActual)) {
                    psFecha.setInt(1, pago.getEstudianteId());
                    try (ResultSet rs = psFecha.executeQuery()) {
                        if (rs.next() && rs.getDate("fecha_proximo_pago") != null) {
                            java.time.LocalDate vencimientoActual = rs.getDate("fecha_proximo_pago").toLocalDate();
                            // Si ya venció, empezamos desde hoy. Si no, desde el vencimiento.
                            java.time.LocalDate inicio = vencimientoActual.isBefore(hoy) ? hoy : vencimientoActual;
                            proximaFecha = PagosController.calcularFechaVencimiento(inicio, 30);
                        } else {
                            proximaFecha = PagosController.calcularFechaVencimiento(hoy, 30);
                        }
                    }
                }
            } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.CARRERA_TOTAL) {
                proximaFecha = null; // No hay más fechas de pago
            } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.MEDIA_CARRERA) {
                // Por defecto, le damos 60 días para el próximo pago si paga la mitad
                proximaFecha = PagosController.calcularFechaVencimiento(hoy, 60);
            }

            // 3. Insertar el Pago Realizado (id_estudiante)
            String sqlInsertPago = "INSERT INTO pagos_realizados (id_estudiante, monto, modalidad, metodo_pago, comprobante, saldo_restante, comprobante_ruta) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psPago = conn.prepareStatement(sqlInsertPago)) {
                psPago.setInt(1, pago.getEstudianteId());
                psPago.setDouble(2, pago.getMonto());
                psPago.setString(3, pago.getModalidad().name());
                psPago.setString(4, pago.getMetodoPago());
                psPago.setString(5, pago.getComprobante());
                psPago.setDouble(6, nuevoSaldo);
                psPago.setString(7, pago.getComprobanteRuta());
                psPago.executeUpdate();
            }

            // 5. Actualizar el Plan de Pago (id_estudiante)
            String sqlUpdatePlan = "UPDATE planes_pago SET saldo_pendiente = ?, cuotas_pagadas = ?, estado = ?, " +
                                   "fecha_ultimo_pago = ?, fecha_proximo_pago = ? " +
                                   "WHERE id_estudiante = ?";
            try (PreparedStatement psUpdatePlan = conn.prepareStatement(sqlUpdatePlan)) {
                psUpdatePlan.setDouble(1, nuevoSaldo);
                psUpdatePlan.setInt(2, cuotasPagadasActuales);
                psUpdatePlan.setString(3, nuevoEstado);
                psUpdatePlan.setDate(4, java.sql.Date.valueOf(hoy));
                
                if (proximaFecha != null && nuevoSaldo > 0.001) {
                    psUpdatePlan.setDate(5, java.sql.Date.valueOf(proximaFecha));
                } else {
                    // Si es Carrera Total o el saldo está totalmente pagado, eliminamos la fecha de próximo pago
                    psUpdatePlan.setNull(5, java.sql.Types.DATE);
                }
                psUpdatePlan.setInt(6, pago.getEstudianteId());
                psUpdatePlan.executeUpdate();
            }

            // 5. Registrar Auditoría (idusuario)
            String sqlAuditoria = "INSERT INTO logs_auditoria_financiera (idusuario, accion, detalle, ip_dispositivo) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psAudit = conn.prepareStatement(sqlAuditoria)) {
                psAudit.setInt(1, idUsuario);
                psAudit.setString(2, "REGISTRO_PAGO");
                psAudit.setString(3, "Pago de $" + pago.getMonto() + " registrado. Modalidad: " + pago.getModalidad().getNombre() + ". Saldo restante: $" + nuevoSaldo);
                psAudit.setString(4, ipDispositivo);
                psAudit.executeUpdate();
            }

            conn.commit();
            
            // 6. Registrar Actividad para Notificaciones (Campanita)
            new ActividadController().registrarActividad(
                "Pago recibido: " + java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "CO")).format(pago.getMonto()) + 
                " - Modalidad: " + pago.getModalidad().getNombre(),
                model.Actividad.TipoActividad.PAGO
            );
            
            return true;

        } catch (SQLException e) {
            System.err.println("Error en transacción de pago: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public boolean crearPlanPagoInicial(int idEstudiante, double descuentoPorcentaje) {
        double montoBase = 500000.00;
        double montoFinal = montoBase - (montoBase * (descuentoPorcentaje / 100));
        
        String sql = "INSERT INTO planes_pago (id_estudiante, monto_base, descuento_porcentaje, monto_final, saldo_pendiente, cuotas_totales) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEstudiante);
            ps.setDouble(2, montoBase);
            ps.setDouble(3, descuentoPorcentaje);
            ps.setDouble(4, montoFinal);
            ps.setDouble(5, montoFinal);
            ps.setInt(6, 5);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
