import java.sql.*;
import config.Database;

public class UpdateModalidadEnumNoSpaces {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Updating ENUM in pagos_realizados (NO SPACES)...");
            
            // SIN ESPACIOS después de las comas
            String sql = "ALTER TABLE pagos_realizados MODIFY COLUMN modalidad " +
                         "ENUM('ABONO','ABONO_SABADO','ABONO_SEMANA','CUOTA_MENSUAL','CARRERA_TOTAL','MEDIA_CARRERA') NOT NULL";
            
            stmt.executeUpdate(sql);
            System.out.println("Database updated successfully without spaces!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
