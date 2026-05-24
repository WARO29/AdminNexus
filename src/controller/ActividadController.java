package controller;

import config.Database;
import model.Actividad;
import model.Actividad.TipoActividad;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActividadController {

    public ActividadController() {
        crearTablaActividades();
    }

    /**
     * Crea la tabla de actividades si no existe
     */
    private void crearTablaActividades() {
        String sql = "CREATE TABLE IF NOT EXISTS actividades ("
                   + "id_actividad INT AUTO_INCREMENT PRIMARY KEY, "
                   + "descripcion VARCHAR(255) NOT NULL, "
                   + "tipo ENUM('PROGRAMA', 'ESTUDIANTE', 'SISTEMA', 'PAGO') NOT NULL, "
                   + "fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                   + ")";
                   
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            
            // Asegurar que el ENUM esté actualizado en bases de datos existentes
            try {
                stmt.execute("ALTER TABLE actividades MODIFY COLUMN tipo ENUM('PROGRAMA', 'ESTUDIANTE', 'SISTEMA', 'PAGO') NOT NULL");
            } catch (SQLException e) {
                // Silencioso si ya está actualizado
            }
        } catch (SQLException e) {
            System.err.println("Error al crear la tabla de actividades: " + e.getMessage());
        }
    }

    /**
     * Registra una nueva actividad en el sistema
     */
    public boolean registrarActividad(String descripcion, TipoActividad tipo) {
        String sql = "INSERT INTO actividades (descripcion, tipo) VALUES (?, ?)";
        
        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, descripcion);
            pstmt.setString(2, tipo.name());
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al registrar actividad: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene las actividades más recientes
     * @param limite Número máximo de actividades a retornar
     * @return Lista de actividades ordenadas por fecha descendente
     */
    public List<Actividad> obtenerActividadesRecientes(int limite) {
        List<Actividad> actividades = new ArrayList<>();
        String sql = "SELECT * FROM actividades ORDER BY fecha DESC LIMIT ?";
        
        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limite);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Actividad act = new Actividad();
                    act.setIdActividad(rs.getInt("id_actividad"));
                    act.setDescripcion(rs.getString("descripcion"));
                    act.setTipo(TipoActividad.valueOf(rs.getString("tipo")));
                    act.setFecha(rs.getTimestamp("fecha"));
                    actividades.add(act);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener actividades recientes: " + e.getMessage());
        }
        
        return actividades;
    }
}
