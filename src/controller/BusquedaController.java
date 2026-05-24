package controller;

import config.Database;
import model.Estudiante;
import model.Programa;
import model.ResultadoBusqueda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class BusquedaController {

    /**
     * Realiza una búsqueda global en estudiantes y programas.
     * 
     * @param query El término de búsqueda
     * @param limitePorCategoria Número máximo de resultados por categoría (ej. top 5)
     * @return Objeto con las listas de resultados
     */
    public ResultadoBusqueda buscarGlobal(String query, int limitePorCategoria) {
        if (query == null || query.trim().isEmpty()) {
            return new ResultadoBusqueda(new ArrayList<>(), new ArrayList<>());
        }
        
        String cleanQuery = "%" + query.trim() + "%";
        
        List<Estudiante> estudiantes = buscarEstudiantes(cleanQuery, limitePorCategoria);
        List<Programa> programas = buscarProgramas(cleanQuery, limitePorCategoria);
        
        return new ResultadoBusqueda(estudiantes, programas);
    }
    
    private List<Estudiante> buscarEstudiantes(String query, int limite) {
        List<Estudiante> resultados = new ArrayList<>();
        String sql = "SELECT e.*, p.nombre as nombre_programa "
                   + "FROM estudiantes e "
                   + "LEFT JOIN programas p ON e.id_programa = p.id_programa "
                   + "WHERE e.nombre LIKE ? OR e.apellido LIKE ? OR e.codigo LIKE ? OR e.email LIKE ? OR e.telefono LIKE ? "
                   + "ORDER BY e.id_estudiante DESC LIMIT ?";
                   
        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, query);
            pstmt.setString(2, query);
            pstmt.setString(3, query);
            pstmt.setString(4, query);
            pstmt.setString(5, query);
            pstmt.setInt(6, limite);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Estudiante est = new Estudiante();
                    est.setIdEstudiante(rs.getInt("id_estudiante"));
                    est.setCodigo(rs.getString("codigo"));
                    est.setNombre(rs.getString("nombre"));
                    est.setApellido(rs.getString("apellido"));
                    est.setEmail(rs.getString("email"));
                    est.setTelefono(rs.getString("telefono"));
                    est.setDireccion(rs.getString("direccion"));
                    
                    Date fechaNac = rs.getDate("fecha_nacimiento");
                    if (fechaNac != null) est.setFechaNacimiento(fechaNac.toLocalDate());
                    
                    est.setIdPrograma(rs.getInt("id_programa"));
                    est.setNombrePrograma(rs.getString("nombre_programa"));
                    est.setFechaMatricula(rs.getDate("fecha_matricula").toLocalDate());
                    
                    String estadoStr = rs.getString("estado");
                    switch (estadoStr) {
                        case "activo":    est.setEstado(Estudiante.EstadoMatricula.ACTIVO);   break;
                        case "inactivo":  est.setEstado(Estudiante.EstadoMatricula.INACTIVO); break;
                        case "graduado":  est.setEstado(Estudiante.EstadoMatricula.GRADUADO); break;
                        default:          est.setEstado(Estudiante.EstadoMatricula.RETIRADO); break;
                    }
                    
                    est.setAvatarUrl(rs.getString("avatar_url"));
                    resultados.add(est);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda de estudiantes: " + e.getMessage());
        }
        
        return resultados;
    }
    
    private List<Programa> buscarProgramas(String query, int limite) {
        List<Programa> resultados = new ArrayList<>();
        String sql = "SELECT * FROM programas "
                   + "WHERE nombre LIKE ? OR codigo LIKE ? "
                   + "ORDER BY id_programa DESC LIMIT ?";
                   
        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, query);
            pstmt.setString(2, query);
            pstmt.setInt(3, limite);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Programa prog = new Programa();
                    prog.setIdPrograma(rs.getInt("id_programa"));
                    prog.setCodigo(rs.getString("codigo"));
                    prog.setNombre(rs.getString("nombre"));
                    prog.setDuracionSemestres(rs.getInt("duracion_semestres"));
                    prog.setInscritos(rs.getInt("inscritos"));
                    
                    String estadoStr = rs.getString("estado");
                    switch (estadoStr) {
                        case "activo":   prog.setEstado(Programa.EstadoPrograma.ACTIVO);   break;
                        case "cerrado":  prog.setEstado(Programa.EstadoPrograma.CERRADO);  break;
                        default:         prog.setEstado(Programa.EstadoPrograma.EN_PAUSA); break;
                    }
                    
                    prog.setIconoColor(rs.getString("icono_color"));
                    resultados.add(prog);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda de programas: " + e.getMessage());
        }
        
        return resultados;
    }
}
