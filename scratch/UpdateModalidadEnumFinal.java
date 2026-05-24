import java.sql.*;
import config.Database;

public class UpdateModalidadEnumFinal {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Updating ENUM in pagos_realizados (Final Attempt)...");
            
            // Incluimos todos los valores previos detectados + los nuevos
            String sql = "ALTER TABLE pagos_realizados MODIFY COLUMN modalidad " +
                         "ENUM('ABONO', 'ABONO_SABADO', 'ABONO_SEMANA', 'CUOTA_MENSUAL', 'CARRERA_TOTAL', 'MEDIA_CARRERA') NOT NULL";
            
            stmt.executeUpdate(sql);
            System.out.println("Database updated successfully with all values!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
