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

            try {
                stmt.execute("ALTER TABLE actividades MODIFY COLUMN tipo ENUM('PROGRAMA', 'ESTUDIANTE', 'SISTEMA', 'PAGO') NOT NULL");
            } catch (SQLException e) { /* ya actualizado */ }

            // Migrar columna nombre_usuario si no existe
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "actividades", "nombre_usuario")) {
                if (!rs.next()) {
                    stmt.execute("ALTER TABLE actividades ADD COLUMN nombre_usuario VARCHAR(100) NULL");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al crear la tabla de actividades: " + e.getMessage());
        }
    }

    /** Registra actividad sin usuario (compatibilidad hacia atrás) */
    public boolean registrarActividad(String descripcion, TipoActividad tipo) {
        return registrarActividad(descripcion, tipo, null);
    }

    /** Registra actividad con el nombre del usuario que la ejecutó */
    public boolean registrarActividad(String descripcion, TipoActividad tipo, String nombreUsuario) {
        String sql = "INSERT INTO actividades (descripcion, tipo, nombre_usuario) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, descripcion);
            pstmt.setString(2, tipo.name());
            pstmt.setString(3, nombreUsuario);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al registrar actividad: " + e.getMessage());
            return false;
        }
    }

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
                    act.setNombreUsuario(rs.getString("nombre_usuario"));
                    actividades.add(act);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener actividades recientes: " + e.getMessage());
        }

        return actividades;
    }
}
