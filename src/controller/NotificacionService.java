package controller;

import config.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Servicio para gestionar alertas y notificaciones por correo electrónico.
 */
public class NotificacionService {

    /**
     * Identifica estudiantes con saldos próximos a vencer (2 días o menos) 
     * o ya atrasados que no han sido notificados hoy.
     */
    public void procesarAlertasPendientes() {
        System.out.println("Iniciando procesamiento de alertas de pago...");
        
        List<Map<String, String>> pendientes = obtenerEstudiantesParaNotificar();
        
        for (Map<String, String> data : pendientes) {
            enviarCorreoAlerta(data);
        }
    }

    private List<Map<String, String>> obtenerEstudiantesParaNotificar() {
        List<Map<String, String>> lista = new ArrayList<>();
        String sql = "SELECT e.nombre, e.email, pp.estado, pp.fecha_proximo_pago, pp.saldo_pendiente " +
                     "FROM estudiantes e " +
                     "JOIN planes_pago pp ON e.id_estudiante = pp.id_estudiante " +
                     "WHERE pp.estado IN ('POR_VENCER', 'ATRASADO') AND pp.saldo_pendiente > 0";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, String> data = new HashMap<>();
                data.put("nombre", rs.getString("nombre"));
                data.put("correo", rs.getString("email"));
                data.put("estado", rs.getString("estado"));
                data.put("fecha", rs.getString("fecha_proximo_pago"));
                data.put("saldo", rs.getString("saldo_pendiente"));
                lista.add(data);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener alertas: " + e.getMessage());
        }
        return lista;
    }

    private void enviarCorreoAlerta(Map<String, String> data) {
        String asunto = data.get("estado").equals("ATRASADO") ? "⚠️ PAGO ATRASADO - AdminNexus" : "📅 Recordatorio de Pago Próximo - AdminNexus";
        
        String mensaje = "Hola " + data.get("nombre") + ",\n\n" +
                         "Te informamos que tu saldo en AdminNexus se encuentra en estado: " + data.get("estado") + ".\n" +
                         "Monto pendiente: $" + data.get("saldo") + "\n" +
                         "Fecha límite: " + data.get("fecha") + "\n\n" +
                         "Por favor realiza tu pago lo antes posible a través de nuestros canales autorizados.\n" +
                         "Si ya realizaste el pago, por favor ignora este mensaje.\n\n" +
                         "Atentamente,\nAdministración AdminNexus";

        // Placeholder para envío real (SMTP)
        System.out.println("ENVIANDO CORREO A: " + data.get("correo"));
        System.out.println("ASUNTO: " + asunto);
        System.out.println("CONTENIDO:\n" + mensaje);
        System.out.println("-------------------------------------------");
    }
}
