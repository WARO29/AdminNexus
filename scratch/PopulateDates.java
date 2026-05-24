import java.sql.*;
import config.Database;
import java.time.LocalDate;
import controller.PagosController;

public class PopulateDates {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion()) {
            String sqlSelect = "SELECT pp.id_plan_pago, e.fecha_matricula FROM planes_pago pp " +
                               "JOIN estudiantes e ON pp.id_estudiante = e.id_estudiante " +
                               "WHERE pp.fecha_proximo_pago IS NULL";
            
            String sqlUpdate = "UPDATE planes_pago SET fecha_proximo_pago = ? WHERE id_plan_pago = ?";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlSelect);
                 PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                
                int count = 0;
                while (rs.next()) {
                    Date matriculaSql = rs.getDate("fecha_matricula");
                    if (matriculaSql == null) continue;
                    
                    LocalDate matricula = matriculaSql.toLocalDate();
                    LocalDate vencimiento = PagosController.calcularFechaVencimiento(matricula, 30);
                    pstmt.setDate(1, java.sql.Date.valueOf(vencimiento));
                    pstmt.setInt(2, rs.getInt("id_plan_pago"));
                    pstmt.addBatch();
                    count++;
                }
                pstmt.executeBatch();
                System.out.println("Updated " + count + " students with new due dates.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
