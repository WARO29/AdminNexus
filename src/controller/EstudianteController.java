package controller;

import config.Database;
import model.Estudiante;
import model.Estudiante.EstadoMatricula;
import model.Programa;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador para la gestión de estudiantes.
 * Usa try-with-resources en cada operación para gestión correcta de recursos.
 * Incluye soporte de paginación real con LIMIT/OFFSET.
 */
public class EstudianteController {

    /** Número de registros por página para paginación. */
    public static final int REGISTROS_POR_PAGINA = 10;
    
    private ActividadController actividadController;
    private String nombreUsuarioActual = null;

    public void setNombreUsuario(String nombre) { this.nombreUsuarioActual = nombre; }

    public EstudianteController() {
        actividadController = new ActividadController();
        // No se almacena conexión en instancia — cada método obtiene la suya del pool
        crearTablaEstudiantes();
    }

    /**
     * Crea la tabla de estudiantes si no existe.
     */
    private void crearTablaEstudiantes() {
        String sql = "CREATE TABLE IF NOT EXISTS estudiantes ("
                + "id_estudiante INT AUTO_INCREMENT PRIMARY KEY,"
                + "codigo VARCHAR(20) UNIQUE NOT NULL,"
                + "nombre VARCHAR(100) NOT NULL,"
                + "apellido VARCHAR(100) NOT NULL,"
                + "email VARCHAR(150) UNIQUE NOT NULL,"
                + "telefono VARCHAR(20),"
                + "direccion VARCHAR(255),"
                + "fecha_nacimiento DATE,"
                + "id_programa INT,"
                + "fecha_matricula DATE NOT NULL,"
                + "estado ENUM('activo', 'inactivo', 'graduado', 'retirado') NOT NULL DEFAULT 'activo',"
                + "avatar_url VARCHAR(255),"
                + "fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (id_programa) REFERENCES programas(id_programa) ON DELETE SET NULL"
                + ")";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla estudiantes verificada/creada");
        } catch (SQLException e) {
            System.err.println("Error al crear tabla estudiantes: " + e.getMessage());
        }
    }

    // =====================================================================
    // PAGINACIÓN REAL CON LIMIT/OFFSET
    // =====================================================================

    /**
     * Obtiene una página de estudiantes usando LIMIT/OFFSET.
     *
     * @param pagina          Número de página (1-based)
     * @param registrosPagina Cantidad de registros por página
     * @return Lista de estudiantes en esa página
     */
    public List<Estudiante> obtenerEstudiantesPaginados(int pagina, int registrosPagina) {
        List<Estudiante> estudiantes = new ArrayList<>();
        int offset = (pagina - 1) * registrosPagina;

        String sql = "SELECT e.*, p.nombre as nombre_programa "
                + "FROM estudiantes e "
                + "LEFT JOIN programas p ON e.id_programa = p.id_programa "
                + "ORDER BY e.id_estudiante DESC "
                + "LIMIT ? OFFSET ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, registrosPagina);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    estudiantes.add(mapearEstudiante(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener estudiantes paginados: " + e.getMessage());
        }

        return estudiantes;
    }

    /**
     * Obtiene una página de estudiantes con filtros opcionales.
     *
     * @param pagina          Número de página (1-based)
     * @param registrosPagina Cantidad de registros por página
     * @param filtroPrograma  Nombre del programa (null o "" para todos)
     * @param filtroEstado    Estado del estudiante (null o "" para todos)
     * @return Lista de estudiantes filtrados y paginados
     */
    public List<Estudiante> obtenerEstudiantesFiltradosPaginados(
            int pagina, int registrosPagina, String filtroPrograma, String filtroEstado, String filtroBusqueda) {

        List<Estudiante> estudiantes = new ArrayList<>();
        int offset = (pagina - 1) * registrosPagina;

        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT e.*, p.nombre as nombre_programa "
                + "FROM estudiantes e "
                + "LEFT JOIN programas p ON e.id_programa = p.id_programa "
                + "WHERE 1=1 ");

        boolean filtrarPrograma = filtroPrograma != null && !filtroPrograma.isEmpty()
                && !filtroPrograma.equals("Todos los Programas");
        boolean filtrarEstado = filtroEstado != null && !filtroEstado.isEmpty()
                && !filtroEstado.equals("Cualquier Estado");
        boolean filtrarBusqueda = filtroBusqueda != null && !filtroBusqueda.trim().isEmpty();

        if (filtrarPrograma) sqlBuilder.append("AND p.nombre = ? ");
        if (filtrarEstado)   sqlBuilder.append("AND e.estado = ? ");
        if (filtrarBusqueda) sqlBuilder.append("AND (e.nombre LIKE ? OR e.apellido LIKE ? OR e.codigo LIKE ? OR e.email LIKE ? OR e.telefono LIKE ?) ");

        sqlBuilder.append("ORDER BY e.id_estudiante DESC LIMIT ? OFFSET ?");

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIdx = 1;
            if (filtrarPrograma) pstmt.setString(paramIdx++, filtroPrograma);
            if (filtrarEstado)   pstmt.setString(paramIdx++, filtroEstado.toLowerCase());
            if (filtrarBusqueda) {
                String term = "%" + filtroBusqueda.trim() + "%";
                for (int i = 0; i < 5; i++) pstmt.setString(paramIdx++, term);
            }
            pstmt.setInt(paramIdx++, registrosPagina);
            pstmt.setInt(paramIdx,   offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    estudiantes.add(mapearEstudiante(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener estudiantes filtrados paginados: " + e.getMessage());
        }

        return estudiantes;
    }

    /**
     * Cuenta el total de estudiantes que cumplen un filtro.
     *
     * @param filtroPrograma Nombre del programa (null o "" para todos)
     * @param filtroEstado   Estado del estudiante (null o "" para todos)
     * @return Total de estudiantes que cumplen el filtro
     */
    public int contarEstudiantesConFiltro(String filtroPrograma, String filtroEstado, String filtroBusqueda) {
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT COUNT(*) "
                + "FROM estudiantes e "
                + "LEFT JOIN programas p ON e.id_programa = p.id_programa "
                + "WHERE 1=1 ");

        boolean filtrarPrograma = filtroPrograma != null && !filtroPrograma.isEmpty()
                && !filtroPrograma.equals("Todos los Programas");
        boolean filtrarEstado = filtroEstado != null && !filtroEstado.isEmpty()
                && !filtroEstado.equals("Cualquier Estado");
        boolean filtrarBusqueda = filtroBusqueda != null && !filtroBusqueda.trim().isEmpty();

        if (filtrarPrograma) sqlBuilder.append("AND p.nombre = ? ");
        if (filtrarEstado)   sqlBuilder.append("AND e.estado = ? ");
        if (filtrarBusqueda) sqlBuilder.append("AND (e.nombre LIKE ? OR e.apellido LIKE ? OR e.codigo LIKE ? OR e.email LIKE ? OR e.telefono LIKE ?) ");

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIdx = 1;
            if (filtrarPrograma) pstmt.setString(paramIdx++, filtroPrograma);
            if (filtrarEstado)   pstmt.setString(paramIdx++,   filtroEstado.toLowerCase());
            if (filtrarBusqueda) {
                String term = "%" + filtroBusqueda.trim() + "%";
                for (int i = 0; i < 5; i++) pstmt.setString(paramIdx++, term);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al contar estudiantes con filtro: " + e.getMessage());
        }

        return 0;
    }

    // =====================================================================
    // CRUD BÁSICO
    // =====================================================================

    /**
     * Crea un nuevo estudiante.
     *
     * @return true si se creó exitosamente
     */
    public boolean crearEstudiante(Estudiante estudiante) {
        String sql = "INSERT INTO estudiantes (codigo, nombre, apellido, email, telefono, "
                + "direccion, fecha_nacimiento, id_programa, fecha_matricula, estado, avatar_url) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, estudiante.getCodigo());
            pstmt.setString(2, estudiante.getNombre());
            pstmt.setString(3, estudiante.getApellido());
            pstmt.setString(4, estudiante.getEmail());
            pstmt.setString(5, estudiante.getTelefono());
            pstmt.setString(6, estudiante.getDireccion());
            pstmt.setDate(7, estudiante.getFechaNacimiento() != null
                    ? Date.valueOf(estudiante.getFechaNacimiento()) : null);
            pstmt.setInt(8, estudiante.getIdPrograma());
            pstmt.setDate(9, Date.valueOf(estudiante.getFechaMatricula()));
            pstmt.setString(10, estudiante.getEstado().name().toLowerCase());
            pstmt.setString(11, estudiante.getAvatarUrl());

            boolean exito = pstmt.executeUpdate() > 0;
            if (exito) {
                // Post-procesamiento aislado: un fallo aquí no debe revertir el éxito del INSERT
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int idGenerado = generatedKeys.getInt(1);
                        try {
                            new PagosController().inicializarPlanEstudiante(idGenerado);
                        } catch (Exception ex) {
                            System.err.println("Error al inicializar plan de pago: " + ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error al obtener ID generado: " + ex.getMessage());
                }
                try {
                    actividadController.registrarActividad(
                        "Se registró el estudiante: " + estudiante.getNombre() + " " + estudiante.getApellido() + " (" + estudiante.getCodigo() + ")",
                        model.Actividad.TipoActividad.ESTUDIANTE,
                        nombreUsuarioActual
                    );
                } catch (Exception ex) {
                    System.err.println("Error al registrar actividad: " + ex.getMessage());
                }
            }
            return exito;
        } catch (SQLException e) {
            System.err.println("Error al crear estudiante: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene todos los estudiantes (sin paginación).
     * Usar {@link #obtenerEstudiantesPaginados} para listas grandes.
     *
     * @return Lista completa de estudiantes
     */
    public List<Estudiante> obtenerTodosLosEstudiantes() {
        List<Estudiante> estudiantes = new ArrayList<>();
        String sql = "SELECT e.*, p.nombre as nombre_programa "
                + "FROM estudiantes e "
                + "LEFT JOIN programas p ON e.id_programa = p.id_programa "
                + "ORDER BY e.id_estudiante DESC";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                estudiantes.add(mapearEstudiante(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener estudiantes: " + e.getMessage());
        }

        return estudiantes;
    }

    /**
     * Actualiza un estudiante existente.
     *
     * @return true si se actualizó exitosamente
     */
    public boolean actualizarEstudiante(Estudiante estudiante) {
        String sql = "UPDATE estudiantes SET codigo = ?, nombre = ?, apellido = ?, "
                + "email = ?, telefono = ?, direccion = ?, fecha_nacimiento = ?, "
                + "id_programa = ?, fecha_matricula = ?, estado = ?, avatar_url = ? "
                + "WHERE id_estudiante = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, estudiante.getCodigo());
            pstmt.setString(2, estudiante.getNombre());
            pstmt.setString(3, estudiante.getApellido());
            pstmt.setString(4, estudiante.getEmail());
            pstmt.setString(5, estudiante.getTelefono());
            pstmt.setString(6, estudiante.getDireccion());
            pstmt.setDate(7, estudiante.getFechaNacimiento() != null
                    ? Date.valueOf(estudiante.getFechaNacimiento()) : null);
            pstmt.setInt(8, estudiante.getIdPrograma());
            pstmt.setDate(9, Date.valueOf(estudiante.getFechaMatricula()));
            pstmt.setString(10, estudiante.getEstado().name().toLowerCase());
            pstmt.setString(11, estudiante.getAvatarUrl());
            pstmt.setInt(12, estudiante.getIdEstudiante());

            boolean exito = pstmt.executeUpdate() > 0;
            if (exito) {
                actividadController.registrarActividad(
                    "Se actualizó el estudiante: " + estudiante.getNombre() + " " + estudiante.getApellido() + " (" + estudiante.getCodigo() + ")",
                    model.Actividad.TipoActividad.ESTUDIANTE,
                    nombreUsuarioActual
                );
            }
            return exito;
        } catch (SQLException e) {
            System.err.println("Error al actualizar estudiante: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un estudiante por su ID.
     *
     * @return true si se eliminó exitosamente
     */
    public boolean eliminarEstudiante(int idEstudiante) {
        String sql = "DELETE FROM estudiantes WHERE id_estudiante = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEstudiante);
            boolean exito = pstmt.executeUpdate() > 0;
            if (exito) {
                actividadController.registrarActividad(
                    "Se eliminó un estudiante (ID: " + idEstudiante + ")",
                    model.Actividad.TipoActividad.ESTUDIANTE,
                    nombreUsuarioActual
                );
            }
            return exito;
        } catch (SQLException e) {
            System.err.println("Error al eliminar estudiante: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cuenta el total de estudiantes en la base de datos.
     *
     * @return Número total de estudiantes
     */
    public int contarEstudiantes() {
        String sql = "SELECT COUNT(*) FROM estudiantes";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al contar estudiantes: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Obtiene todos los programas disponibles (delega al ProgramaController con caché).
     *
     * @return Lista de programas disponibles
     */
    public List<Programa> obtenerProgramasDisponibles() {
        return new ProgramaController().obtenerTodosLosProgramas();
    }

    /**
     * Genera el siguiente código de estudiante.
     *
     * @return Código generado en formato AN-AÑO-NNN
     */
    public String generarCodigoEstudiante() {
        String sql = "SELECT codigo FROM estudiantes ORDER BY id_estudiante DESC LIMIT 1";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String ultimoCodigo = rs.getString("codigo");
                String[] partes = ultimoCodigo.split("-");
                int numero = Integer.parseInt(partes[2]) + 1;
                return String.format("AN-%s-%03d", java.time.Year.now().getValue(), numero);
            } else {
                return String.format("AN-%s-001", java.time.Year.now().getValue());
            }
        } catch (SQLException e) {
            System.err.println("Error al generar código: " + e.getMessage());
            return String.format("AN-%s-001", java.time.Year.now().getValue());
        }
    }

    // =====================================================================
    // MÉTODO AUXILIAR DE MAPEO
    // =====================================================================

    /**
     * Mapea una fila del ResultSet a un objeto Estudiante.
     */
    private Estudiante mapearEstudiante(ResultSet rs) throws SQLException {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdEstudiante(rs.getInt("id_estudiante"));
        estudiante.setCodigo(rs.getString("codigo"));
        estudiante.setNombre(rs.getString("nombre"));
        estudiante.setApellido(rs.getString("apellido"));
        estudiante.setEmail(rs.getString("email"));
        estudiante.setTelefono(rs.getString("telefono"));
        estudiante.setDireccion(rs.getString("direccion"));

        Date fechaNac = rs.getDate("fecha_nacimiento");
        if (fechaNac != null) {
            estudiante.setFechaNacimiento(fechaNac.toLocalDate());
        }

        estudiante.setIdPrograma(rs.getInt("id_programa"));
        estudiante.setNombrePrograma(rs.getString("nombre_programa"));
        estudiante.setFechaMatricula(rs.getDate("fecha_matricula").toLocalDate());

        String estadoStr = rs.getString("estado");
        switch (estadoStr) {
            case "activo":    estudiante.setEstado(EstadoMatricula.ACTIVO);   break;
            case "inactivo":  estudiante.setEstado(EstadoMatricula.INACTIVO); break;
            case "graduado":  estudiante.setEstado(EstadoMatricula.GRADUADO); break;
            default:          estudiante.setEstado(EstadoMatricula.RETIRADO); break;
        }

        estudiante.setAvatarUrl(rs.getString("avatar_url"));
        return estudiante;
    }
}
