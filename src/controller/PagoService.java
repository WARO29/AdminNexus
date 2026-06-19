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
            double montoFinal = 0;
            int cuotasTotales = 0;
            java.sql.Date fechaProximoPagoActual = null;
            String sqlPlan = "SELECT saldo_pendiente, cuotas_pagadas, monto_final, cuotas_totales, fecha_proximo_pago FROM planes_pago WHERE id_estudiante = ?";
            try (PreparedStatement psPlan = conn.prepareStatement(sqlPlan)) {
                psPlan.setInt(1, pago.getEstudianteId());
                try (ResultSet rs = psPlan.executeQuery()) {
                    if (rs.next()) {
                        saldoActual = rs.getDouble("saldo_pendiente");
                        cuotasPagadasActuales = rs.getInt("cuotas_pagadas");
                        montoFinal = rs.getDouble("monto_final");
                        cuotasTotales = rs.getInt("cuotas_totales");
                        fechaProximoPagoActual = rs.getDate("fecha_proximo_pago");
                    } else {
                        throw new SQLException("No se encontró el plan de pago para el estudiante especificado.");
                    }
                }
            }

            // 2. Calcular nuevo saldo y estado
            double nuevoSaldo = saldoActual - pago.getMonto();
            if (nuevoSaldo < 0) nuevoSaldo = 0; 
            
            pago.setSaldoRestante(nuevoSaldo);
            
            double valorCuota = cuotasTotales > 0 ? (montoFinal / cuotasTotales) : 100000.0;
            
            // 3. Determinar cuotas pagadas según modalidad
            if (pago.getModalidad() == PagoRealizado.ModalidadPago.CARRERA_TOTAL) {
                cuotasPagadasActuales = cuotasTotales;
            } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.MEDIA_CARRERA) {
                cuotasPagadasActuales = Math.max(cuotasPagadasActuales, (int) Math.ceil(cuotasTotales / 2.0));
            } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.CUOTA_MENSUAL) {
                // Si es cuota mensual, intentamos obtener el número de cuota desde el comprobante (ej. "Cuota #2" -> 2)
                int cuotaNum = 0;
                try {
                    String comp = pago.getComprobante();
                    if (comp != null && comp.startsWith("Cuota #")) {
                        cuotaNum = Integer.parseInt(comp.replace("Cuota #", "").trim());
                    }
                } catch (NumberFormatException e) {
                    // Silencioso
                }
                if (cuotaNum > 0) {
                    cuotasPagadasActuales = Math.max(cuotasPagadasActuales, cuotaNum);
                } else {
                    cuotasPagadasActuales = Math.min(cuotasPagadasActuales + 1, cuotasTotales);
                }
            } else {
                // Cálculo proporcional basado en dinero real aportado
                double totalPagadoReal = montoFinal - nuevoSaldo;
                if (valorCuota > 0) {
                    cuotasPagadasActuales = (int) Math.round(totalPagadoReal / valorCuota);
                } else {
                    cuotasPagadasActuales = cuotasTotales;
                }
            }
            if (cuotasPagadasActuales > cuotasTotales) {
                cuotasPagadasActuales = cuotasTotales;
            }
            
            java.time.LocalDate proximaFecha = null;
            java.time.LocalDate hoy = java.time.LocalDate.now();
            boolean tieneProrroga = false;
            
            // Determinar si es un abono parcial (prórroga - Opción B)
            boolean esAbonoParcial = (pago.getModalidad() == PagoRealizado.ModalidadPago.ABONO_SABADO ||
                                      pago.getModalidad() == PagoRealizado.ModalidadPago.ABONO_SEMANA ||
                                      (pago.getModalidad() == PagoRealizado.ModalidadPago.CUOTA_MENSUAL && pago.getMonto() < valorCuota - 0.01));
            
            if (nuevoSaldo <= 0.001) {
                proximaFecha = null; // Paz y salvo, no hay más pagos
            } else if (esAbonoParcial) {
                // Si es un abono parcial y todavía hay saldo, se le otorga una prórroga corta de 15 días laborales (Opción B)
                proximaFecha = PagosController.calcularFechaVencimiento(hoy, 15);
                tieneProrroga = true;
            } else {
                // Pago de cuota completa o más
                if (pago.getModalidad() == PagoRealizado.ModalidadPago.CUOTA_MENSUAL) {
                    if (fechaProximoPagoActual != null) {
                        java.time.LocalDate vencimientoActual = fechaProximoPagoActual.toLocalDate();
                        // Si ya venció, empezamos desde hoy. Si no, desde el vencimiento.
                        java.time.LocalDate inicio = vencimientoActual.isBefore(hoy) ? hoy : vencimientoActual;
                        proximaFecha = PagosController.calcularFechaVencimiento(inicio, 30);
                    } else {
                        proximaFecha = PagosController.calcularFechaVencimiento(hoy, 30);
                    }
                } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.MEDIA_CARRERA) {
                    proximaFecha = PagosController.calcularFechaVencimiento(hoy, 60);
                } else {
                    // Fallback para abonos completos o cualquier otro tipo
                    proximaFecha = PagosController.calcularFechaVencimiento(hoy, 30);
                }
            }
            
            // Determinar estado inmediato del plan de pago
            String nuevoEstado = "AL_DIA";
            if (nuevoSaldo > 0.001) {
                if (tieneProrroga) {
                    nuevoEstado = "CON_SALDO"; // Estado dinámico solicitado por el usuario
                } else if (proximaFecha != null) {
                    if (proximaFecha.isBefore(hoy)) {
                        nuevoEstado = "ATRASADO";
                    } else if (java.time.temporal.ChronoUnit.DAYS.between(hoy, proximaFecha) <= 2) {
                        nuevoEstado = "POR_VENCER";
                    } else {
                        nuevoEstado = "CON_SALDO"; // Si tiene saldo pero no está por vencer ni vencido
                    }
                } else {
                    nuevoEstado = "CON_SALDO";
                }
            } else {
                nuevoEstado = "AL_DIA";
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
                
                if (proximaFecha != null) {
                    psUpdatePlan.setDate(5, java.sql.Date.valueOf(proximaFecha));
                } else {
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
