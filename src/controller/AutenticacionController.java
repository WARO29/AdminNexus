package controller;

import config.Database;
import model.Usuario;
import model.Usuario.RolUsuario;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador para la autenticación y gestión de usuarios.
 * Usa try-with-resources en cada operación para gestión correcta de recursos.
 */
public class AutenticacionController {

    public AutenticacionController() {
        // No se almacena conexión en instancia — cada método obtiene la suya del pool
    }

    /**
     * Autentica un usuario con sus credenciales.
     *
     * @return Usuario si las credenciales son correctas, null en caso contrario
     */
    public Usuario autenticar(String user, String password) {
        String sql = "SELECT * FROM usuarios WHERE user = ? AND password = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();
                    usuario.setIdusuario(rs.getInt("idusuario"));
                    usuario.setNombreAdmin(rs.getString("nombre_admin"));
                    usuario.setUser(rs.getString("user"));
                    usuario.setPassword(rs.getString("password"));
                    usuario.setRol(RolUsuario.valueOf(rs.getString("rol").toUpperCase()));
                    return usuario;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al autenticar usuario: " + e.getMessage());
        }

        return null;
    }

    /**
     * Verifica si existe al menos un usuario en el sistema.
     *
     * @return true si hay usuarios, false si la tabla está vacía
     */
    public boolean existenUsuarios() {
        String sql = "SELECT COUNT(*) FROM usuarios";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar usuarios: " + e.getMessage());
        }

        return false;
    }

    /**
     * Crea un nuevo usuario en el sistema.
     *
     * @return true si se creó exitosamente, false en caso contrario
     */
    public boolean crearUsuario(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre_admin, user, password, rol) VALUES (?, ?, ?, ?)";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getNombreAdmin());
            pstmt.setString(2, usuario.getUser());
            pstmt.setString(3, usuario.getPassword());
            pstmt.setString(4, usuario.getRol().name().toLowerCase());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al crear usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene todos los usuarios del sistema.
     *
     * @return Lista de todos los usuarios
     */
    public List<Usuario> obtenerTodosLosUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY idusuario DESC";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setIdusuario(rs.getInt("idusuario"));
                usuario.setNombreAdmin(rs.getString("nombre_admin"));
                usuario.setUser(rs.getString("user"));
                usuario.setPassword(rs.getString("password"));
                usuario.setRol(RolUsuario.valueOf(rs.getString("rol").toUpperCase()));
                usuarios.add(usuario);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuarios: " + e.getMessage());
        }

        return usuarios;
    }

    /**
     * Actualiza un usuario existente.
     *
     * @return true si se actualizó exitosamente
     */
    public boolean actualizarUsuario(Usuario usuario) {
        String sql = "UPDATE usuarios SET nombre_admin = ?, user = ?, password = ?, rol = ? WHERE idusuario = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getNombreAdmin());
            pstmt.setString(2, usuario.getUser());
            pstmt.setString(3, usuario.getPassword());
            pstmt.setString(4, usuario.getRol().name().toLowerCase());
            pstmt.setInt(5, usuario.getIdusuario());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un usuario del sistema.
     *
     * @return true si se eliminó exitosamente
     */
    public boolean eliminarUsuario(int idusuario) {
        String sql = "DELETE FROM usuarios WHERE idusuario = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idusuario);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            return false;
        }
    }
}
