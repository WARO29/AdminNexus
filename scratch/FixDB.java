import java.sql.*;
import config.Database;

public class FixDB {
    public static void main(String[] args) {
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Checking columns...");
            
            try {
                stmt.execute("ALTER TABLE planes_pago ADD COLUMN fecha_ultimo_pago DATE AFTER cuotas_pagadas");
                System.out.println("Added fecha_ultimo_pago");
            } catch (SQLException e) {
                System.out.println("fecha_ultimo_pago already exists or error: " + e.getMessage());
            }

            try {
                stmt.execute("ALTER TABLE planes_pago ADD COLUMN fecha_proximo_pago DATE AFTER estado");
                System.out.println("Added fecha_proximo_pago");
            } catch (SQLException e) {
                System.out.println("fecha_proximo_pago already exists or error: " + e.getMessage());
            }
            
            System.out.println("Done.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
