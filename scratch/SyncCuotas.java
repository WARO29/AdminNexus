import java.sql.*;
import config.Database;

public class SyncCuotas {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement()) {
            
            // Actualizar cuotas_pagadas contando los registros en pagos_realizados
            String sql = "UPDATE planes_pago pp SET cuotas_pagadas = (" +
                         "SELECT COUNT(*) FROM pagos_realizados pr WHERE pr.id_estudiante = pp.id_estudiante" +
                         ")";
            
            int rows = stmt.executeUpdate(sql);
            System.out.println("Synchronized " + rows + " payment plans.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
