package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Clase centralizada para la gestión de conexiones a la base de datos.
 * Utiliza HikariCP como connection pool para mayor rendimiento y fiabilidad.
 */
public class Database {

    // Configuración de la base de datos
    private static final String URL      = "jdbc:mysql://localhost/adminnexus?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USUARIO  = "root";
    private static final String CONTRASENA = "";

    // Pool de conexiones HikariCP
    private static HikariDataSource dataSource = null;

    /**
     * Constructor privado para evitar instanciación
     */
    private Database() {}

    /**
     * Inicializa el pool de conexiones HikariCP (solo se ejecuta una vez).
     */
    private static synchronized void inicializarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USUARIO);
        config.setPassword(CONTRASENA);

        // Tamaño del pool
        config.setMaximumPoolSize(10);          // Máximo 10 conexiones simultáneas
        config.setMinimumIdle(2);               // Mantener 2 conexiones listas
        config.setConnectionTimeout(30_000);    // 30s para obtener conexión del pool
        config.setIdleTimeout(600_000);         // 10min antes de cerrar conexión ociosa
        config.setMaxLifetime(1_800_000);       // 30min de vida máxima por conexión
        config.setKeepaliveTime(60_000);        // Ping cada 1min para detectar conexiones muertas
        config.setConnectionTestQuery("SELECT 1"); // Verificar conexión viva

        // Nombre del pool (útil para logs)
        config.setPoolName("AdminNexus-Pool");

        try {
            dataSource = new HikariDataSource(config);
            System.out.println("✓ Pool HikariCP inicializado correctamente [max=" + config.getMaximumPoolSize() + " conexiones]");
        } catch (Exception e) {
            System.err.println("✗ Error al inicializar pool HikariCP: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("No se pudo establecer conexión con la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene una conexión del pool.
     * IMPORTANTE: Siempre usar con try-with-resources para devolver la conexión al pool.
     *
     * @return Connection objeto de conexión del pool
     * @throws RuntimeException si no se puede obtener conexión
     */
    public static Connection getConexion() {
        if (dataSource == null || dataSource.isClosed()) {
            inicializarPool(); // Puede lanzar RuntimeException si el servidor no está disponible
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            System.err.println("✗ Error al obtener conexión del pool: " + e.getMessage());
            throw new RuntimeException("No se pudo obtener conexión de la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Compatibilidad con código existente — alias de getConexion().
     * @deprecated Usar getConexion() directamente con try-with-resources
     */
    @Deprecated
    public static Connection conectarBaseDatos() {
        return getConexion();
    }

    /**
     * Cierra el pool de conexiones HikariCP.
     * Llamar al cerrar la aplicación (WindowListener del Dashboard).
     */
    public static synchronized void cerrarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            System.out.println("✓ Pool HikariCP cerrado correctamente — todos los recursos liberados");
        }
    }

    /**
     * Verifica si el pool está activo y tiene conexiones disponibles.
     *
     * @return true si el pool está operativo
     */
    public static boolean isConectado() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Prueba la conexión a la base de datos.
     *
     * @return true si la conexión es exitosa
     */
    public static boolean probarConexion() {
        try (Connection conn = getConexion()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✓ Prueba de conexión exitosa");
                return true;
            }
        } catch (Exception e) {
            System.err.println("✗ Prueba de conexión fallida: " + e.getMessage());
        }
        return false;
    }
}
