import java.sql.*;
import config.Database;

public class CheckSchema {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion();
             ResultSet rs = conn.getMetaData().getColumns(null, null, "pagos_realizados", "modalidad")) {
            
            if (rs.next()) {
                System.out.println("Column: " + rs.getString("COLUMN_NAME"));
                System.out.println("Type Name: " + rs.getString("TYPE_NAME"));
                // Note: Get more details if possible
            }
            
            // Try to get the full create table if MySQL
            try (Statement stmt = conn.createStatement();
                 ResultSet rs2 = stmt.executeQuery("SHOW CREATE TABLE pagos_realizados")) {
                if (rs2.next()) {
                    System.out.println("Create Table: " + rs2.getString(2));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
