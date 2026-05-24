import java.sql.*;
import config.Database;

public class CheckPlanesPago {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE planes_pago")) {
            if (rs.next()) {
                System.out.println("Create Table PlanesPago: " + rs.getString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
