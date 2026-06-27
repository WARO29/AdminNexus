package controller;

import config.Database;
import model.Programa;
import model.Programa.EstadoPrograma;
import model.Actividad;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controlador para la gestión de programas académicos.
 * Implementa caché en memoria para reducir consultas repetitivas a la BD.
 */
public class ProgramaController {

    // =====================================================================
    // Caché de Programas Académicos
    // Los programas no cambian frecuentemente → caché con TTL de 5 minutos
    // =====================================================================
    private static volatile List<Programa> cachePrograms   = null;
    private static volatile long           cacheTimestamp  = 0L;
    private static final    long           CACHE_TTL_MS    = 5 * 60 * 1000; // 5 minutos

    public static final int REGISTROS_POR_PAGINA = 10;
    
    private ActividadController actividadController;
    private String nombreUsuarioActual = null;

    public void setNombreUsuario(String nombre) { this.nombreUsuarioActual = nombre; }

    public ProgramaController() {
        actividadController = new ActividadController();
        // La tabla se verifica al primer uso, no en el constructor
        crearTablaProgramas();
    }

    /**
     * Invalida el caché para forzar recarga en la próxima consulta.
     */
    private static synchronized void invalidarCache() {
        cachePrograms  = null;
        cacheTimestamp = 0L;
    }

    /**
     * Indica si el caché es válido (existe y no ha expirado).
     */
    private static boolean cacheValido() {
        return cachePrograms != null
                && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS;
    }

    // =====================================================================
    // Operaciones CRUD
    // =====================================================================

    /**
     * Crea la tabla de programas si no existe.
     */
    private void crearTablaProgramas() {
        String sql = "CREATE TABLE IF NOT EXISTS programas ("
                + "id_programa INT AUTO_INCREMENT PRIMARY KEY,"
                + "codigo VARCHAR(20) UNIQUE NOT NULL,"
                + "nombre VARCHAR(100) NOT NULL,"
                + "duracion_semestres INT NOT NULL,"
                + "inscritos INT DEFAULT 0,"
                + "estado ENUM('activo', 'cerrado', 'en_pausa') NOT NULL,"
                + "icono_color VARCHAR(7) NOT NULL,"
                + "fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabla programas verificada/creada");
        } catch (SQLException e) {
            System.err.println("Error al crear tabla programas: " + e.getMessage());
        }
    }

    /**
     * Crea un nuevo programa e invalida el caché.
     *
     * @return true si se creó exitosamente
     */
    public boolean crearPrograma(Programa programa) {
        String sql = "INSERT INTO programas (codigo, nombre, duracion_semestres, "
                + "inscritos, estado, icono_color) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, programa.getCodigo());
            pstmt.setString(2, programa.getNombre());
            pstmt.setInt(3, programa.getDuracionSemestres());
            pstmt.setInt(4, programa.getInscritos());
            pstmt.setString(5, programa.getEstado().name().toLowerCase());
            pstmt.setString(6, programa.getIconoColor());

            boolean exito = pstmt.executeUpdate() > 0;
            if (exito) {
                invalidarCache(); // El caché ya no es válido
                actividadController.registrarActividad(
                    "Se creó el programa: " + programa.getNombre() + " (" + programa.getCodigo() + ")",
                    Actividad.TipoActividad.PROGRAMA,
                    nombreUsuarioActual
                );
            }
            return exito;
        } catch (SQLException e) {
            System.err.println("Error al crear programa: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene todos los programas. Usa caché si los datos son recientes.
     *
     * @return Lista de programas (puede venir del caché)
     */
    public List<Programa> obtenerTodosLosProgramas() {
        // Devolver caché si es válido
        if (cacheValido()) {
            System.out.println("✓ Programas obtenidos desde caché");
            return Collections.unmodifiableList(cachePrograms);
        }

        // Consultar la base de datos
        List<Programa> programas = new ArrayList<>();
        String sql = "SELECT * FROM programas ORDER BY id_programa DESC";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Programa programa = new Programa();
                programa.setIdPrograma(rs.getInt("id_programa"));
                programa.setCodigo(rs.getString("codigo"));
                programa.setNombre(rs.getString("nombre"));
                programa.setDuracionSemestres(rs.getInt("duracion_semestres"));
                programa.setInscritos(rs.getInt("inscritos"));

                String estadoStr = rs.getString("estado");
                switch (estadoStr) {
                    case "activo":
                        programa.setEstado(EstadoPrograma.ACTIVO);
                        break;
                    case "cerrado":
                        programa.setEstado(EstadoPrograma.CERRADO);
                        break;
                    default:
                        programa.setEstado(EstadoPrograma.EN_PAUSA);
                        break;
                }

                programa.setIconoColor(rs.getString("icono_color"));
                programas.add(programa);
            }

            // Actualizar caché
            synchronized (ProgramaController.class) {
                cachePrograms  = new ArrayList<>(programas);
                cacheTimestamp = System.currentTimeMillis();
            }
            System.out.println("✓ Programas cargados desde BD y guardados en caché [" + programas.size() + " registros]");

        } catch (SQLException e) {
            System.err.println("Error al obtener programas: " + e.getMessage());
        }

        return programas;
    }

    /**
     * Actualiza un programa existente e invalida el caché.
     *
     * @return true si se actualizó exitosamente
     */
    public boolean actualizarPrograma(Programa programa) {
        String sql = "UPDATE programas SET codigo = ?, nombre = ?, "
                + "duracion_semestres = ?, inscritos = ?, "
                + "estado = ?, icono_color = ? WHERE id_programa = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, programa.getCodigo());
            pstmt.setString(2, programa.getNombre());
            pstmt.setInt(3, programa.getDuracionSemestres());
            pstmt.setInt(4, programa.getInscritos());
            pstmt.setString(5, programa.getEstado().name().toLowerCase());
            pstmt.setString(6, programa.getIconoColor());
            pstmt.setInt(7, programa.getIdPrograma());

            boolean exito = pstmt.executeUpdate() > 0;
            if (exito) {
                invalidarCache();
                actividadController.registrarActividad(
                    "Se actualizó el programa: " + programa.getNombre() + " (" + programa.getCodigo() + ")",
                    Actividad.TipoActividad.PROGRAMA,
                    nombreUsuarioActual
                );
            }
            return exito;
        } catch (SQLException e) {
            System.err.println("Error al actualizar programa: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un programa e invalida el caché.
     *
     * @return true si se eliminó exitosamente
     */
    public boolean eliminarPrograma(int idPrograma) {
        String sql = "DELETE FROM programas WHERE id_programa = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idPrograma);
            boolean exito = pstmt.executeUpdate() > 0;
            if (exito) {
                invalidarCache();
                actividadController.registrarActividad(
                    "Se eliminó un programa (ID: " + idPrograma + ")",
                    Actividad.TipoActividad.PROGRAMA,
                    nombreUsuarioActual
                );
            }
            return exito;
        } catch (SQLException e) {
            System.err.println("Error al eliminar programa: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cuenta el total de programas (puede usar el caché si está disponible).
     *
     * @return número total de programas
     */
    public int contarProgramas() {
        // Si el caché es válido, usar su tamaño sin ir a la BD
        if (cacheValido()) {
            return cachePrograms.size();
        }

        String sql = "SELECT COUNT(*) FROM programas";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al contar programas: " + e.getMessage());
        }

        return 0;
    }

    // =====================================================================
    // PAGINACIÓN REAL CON LIMIT/OFFSET
    // =====================================================================

    /**
     * Obtiene una página de programas con filtros opcionales.
     *
     * @param pagina          Número de página (1-based)
     * @param registrosPagina Cantidad de registros por página
     * @param filtroEstado    Estado del programa (null o "Cualquier estado" para todos)
     * @param filtroDuracion  Rango de duración (null o "Cualquier duración" para todos)
     * @return Lista de programas filtrados y paginados
     */
    public List<Programa> obtenerProgramasFiltradosPaginados(
            int pagina, int registrosPagina, String filtroEstado, String filtroDuracion, String filtroBusqueda) {

        List<Programa> programas = new ArrayList<>();
        int offset = (pagina - 1) * registrosPagina;

        boolean filtrarEstado    = filtroEstado   != null && !filtroEstado.isEmpty()
                && !filtroEstado.equals("Cualquier estado");
        boolean filtrarDuracion  = filtroDuracion != null && !filtroDuracion.isEmpty()
                && !filtroDuracion.equals("Cualquier duración");
        boolean filtrarBusqueda  = filtroBusqueda != null && !filtroBusqueda.trim().isEmpty();

        // Traducir rango de duración a SQL
        String condicionDuracion = calcularCondicionDuracionSQL(filtroDuracion);

        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT * FROM programas WHERE 1=1 ");

        if (filtrarEstado)   sqlBuilder.append("AND estado = ? ");
        if (filtrarDuracion && condicionDuracion != null)
                             sqlBuilder.append("AND ").append(condicionDuracion).append(" ");
        if (filtrarBusqueda) sqlBuilder.append("AND (nombre LIKE ? OR codigo LIKE ?) ");

        sqlBuilder.append("ORDER BY id_programa DESC LIMIT ? OFFSET ?");

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIdx = 1;
            if (filtrarEstado) pstmt.setString(paramIdx++, filtroEstado.toLowerCase().replace(" ", "_"));
            if (filtrarBusqueda) {
                pstmt.setString(paramIdx++, "%" + filtroBusqueda.trim() + "%");
                pstmt.setString(paramIdx++, "%" + filtroBusqueda.trim() + "%");
            }
            pstmt.setInt(paramIdx++, registrosPagina);
            pstmt.setInt(paramIdx,   offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    programas.add(mapearPrograma(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener programas paginados: " + e.getMessage());
        }

        return programas;
    }

    /**
     * Cuenta el total de programas que cumplen un filtro.
     *
     * @param filtroEstado   Estado del programa (null o "Cualquier estado" para todos)
     * @param filtroDuracion Rango de duración (null o "Cualquier duración" para todos)
     * @return Total de programas que cumplen el filtro
     */
    public int contarProgramasConFiltro(String filtroEstado, String filtroDuracion, String filtroBusqueda) {
        boolean filtrarEstado   = filtroEstado   != null && !filtroEstado.isEmpty()
                && !filtroEstado.equals("Cualquier estado");
        boolean filtrarDuracion = filtroDuracion != null && !filtroDuracion.isEmpty()
                && !filtroDuracion.equals("Cualquier duración");
        boolean filtrarBusqueda = filtroBusqueda != null && !filtroBusqueda.trim().isEmpty();

        String condicionDuracion = calcularCondicionDuracionSQL(filtroDuracion);

        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT COUNT(*) FROM programas WHERE 1=1 ");

        if (filtrarEstado)   sqlBuilder.append("AND estado = ? ");
        if (filtrarDuracion && condicionDuracion != null)
                             sqlBuilder.append("AND ").append(condicionDuracion).append(" ");
        if (filtrarBusqueda) sqlBuilder.append("AND (nombre LIKE ? OR codigo LIKE ?) ");

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIdx = 1;
            if (filtrarEstado) pstmt.setString(paramIdx++, filtroEstado.toLowerCase().replace(" ", "_"));
            if (filtrarBusqueda) {
                pstmt.setString(paramIdx++, "%" + filtroBusqueda.trim() + "%");
                pstmt.setString(paramIdx++, "%" + filtroBusqueda.trim() + "%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al contar programas con filtro: " + e.getMessage());
        }

        return 0;
    }

    // =====================================================================
    // MÉTODO AUXILIAR DE MAPEO
    // =====================================================================

    /**
     * Mapea una fila del ResultSet a un objeto Programa.
     */
    private Programa mapearPrograma(ResultSet rs) throws SQLException {
        Programa programa = new Programa();
        programa.setIdPrograma(rs.getInt("id_programa"));
        programa.setCodigo(rs.getString("codigo"));
        programa.setNombre(rs.getString("nombre"));
        programa.setDuracionSemestres(rs.getInt("duracion_semestres"));
        programa.setInscritos(rs.getInt("inscritos"));

        String estadoStr = rs.getString("estado");
        switch (estadoStr) {
            case "activo":   programa.setEstado(EstadoPrograma.ACTIVO);   break;
            case "cerrado":  programa.setEstado(EstadoPrograma.CERRADO);  break;
            default:         programa.setEstado(EstadoPrograma.EN_PAUSA); break;
        }

        programa.setIconoColor(rs.getString("icono_color"));
        return programa;
    }

    /**
     * Traduce un rango de duración en texto a una condición SQL.
     */
    private String calcularCondicionDuracionSQL(String rango) {
        if (rango == null) return null;
        switch (rango) {
            case "1-4 Semestres":          return "duracion_semestres BETWEEN 1 AND 4";
            case "5-8 Semestres":          return "duracion_semestres BETWEEN 5 AND 8";
            case "9-12 Semestres":         return "duracion_semestres BETWEEN 9 AND 12";
            case "Más de 12 Semestres":    return "duracion_semestres > 12";
            default:                        return null;
        }
    }

    /**
     * Genera el siguiente código de programa siguiendo el patrón #PRG-00X.
     *
     * @return Código autogenerado
     */
    public String generarCodigoPrograma() {
        String sql = "SELECT COUNT(*) FROM programas";
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int nextId = rs.getInt(1) + 1;
                return String.format("#PRG-%03d", nextId);
            }
        } catch (SQLException e) {
            System.err.println("Error al generar código de programa: " + e.getMessage());
        }
        return "#PRG-001";
    }
}

