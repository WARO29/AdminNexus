import java.sql.*;
import config.Database;

public class CheckPlanes {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_estudiante, saldo_pendiente, cuotas_pagadas, cuotas_totales FROM planes_pago")) {
            
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id_estudiante") + 
                                   " | Saldo: " + rs.getDouble("saldo_pendiente") + 
                                   " | Cuotas: " + rs.getInt("cuotas_pagadas") + "/" + rs.getInt("cuotas_totales"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
