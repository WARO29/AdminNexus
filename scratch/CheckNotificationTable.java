import java.sql.*;
import config.Database;

public class CheckNotificationTable {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            String[] tables = {"notificaciones", "alertas", "actividades", "actividad_reciente"};
            
            for (String tableName : tables) {
                System.out.println("Checking table: " + tableName);
                try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
                    boolean exists = false;
                    while (rs.next()) {
                        exists = true;
                        System.out.println("  " + rs.getString("COLUMN_NAME") + " (" + rs.getString("TYPE_NAME") + ")");
                    }
                    if (!exists) System.out.println("  Table not found.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
