import java.sql.*;
import config.Database;

public class CheckAuditoria {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE logs_auditoria_financiera")) {
            if (rs.next()) {
                System.out.println("Create Table Auditoria: " + rs.getString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
