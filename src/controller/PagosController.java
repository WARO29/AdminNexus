package controller;

import config.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.DayOfWeek;
import controller.ProgramaController;

/**
 * Controlador para la lógica del Dashboard de Pagos.
 * Proporciona datos agregados para gráficas y tablas de estado financiero.
 */
public class PagosController {

    public static final int REGISTROS_POR_PAGINA = 10;

    public PagosController() {
        verificarColumnas();
    }

    private void verificarColumnas() {
        try (Connection conn = Database.getConexion()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 1. Verificar descuento_aplicado
            try (ResultSet rs = metaData.getColumns(null, null, "planes_pago", "descuento_aplicado")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE planes_pago ADD COLUMN descuento_aplicado BOOLEAN DEFAULT FALSE AFTER descuento_porcentaje");
                    }
                }
            }
            
            // 2. Verificar fecha_proximo_pago
            try (ResultSet rs = metaData.getColumns(null, null, "planes_pago", "fecha_proximo_pago")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE planes_pago ADD COLUMN fecha_proximo_pago DATE AFTER estado");
                    }
                }
            }
            
            // 3. Verificar fecha_ultimo_pago
            try (ResultSet rs = metaData.getColumns(null, null, "planes_pago", "fecha_ultimo_pago")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE planes_pago ADD COLUMN fecha_ultimo_pago DATE AFTER cuotas_pagadas");
                    }
                }
            }

            // 4. Actualizar tipos de modalidad en pagos_realizados para soportar nuevos tipos
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE pagos_realizados MODIFY COLUMN modalidad ENUM('ABONO', 'ABONO_SABADO', 'ABONO_SEMANA', 'CUOTA_MENSUAL') NOT NULL");
            } catch (SQLException e) {
                // Manejo silencioso si falla el MODIFY
            }

            // 5. Verificar comprobante_ruta en pagos_realizados
            try (ResultSet rs = metaData.getColumns(null, null, "pagos_realizados", "comprobante_ruta")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE pagos_realizados ADD COLUMN comprobante_ruta VARCHAR(255) AFTER comprobante");
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error verificando columnas: " + e.getMessage());
        }
    }

    /**
     * Obtiene un resumen general de las finanzas.
     */
    public Map<String, Double> obtenerResumenGeneral() {
        Map<String, Double> resumen = new HashMap<>();
        String sql = "SELECT " +
                     "SUM(monto_final) as total_esperado, " +
                     "SUM(monto_final - saldo_pendiente) as total_recaudado, " +
                     "SUM(saldo_pendiente) as total_pendiente " +
                     "FROM planes_pago";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                resumen.put("total_esperado", rs.getDouble("total_esperado"));
                resumen.put("total_recaudado", rs.getDouble("total_recaudado"));
                resumen.put("total_pendiente", rs.getDouble("total_pendiente"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener resumen financiero: " + e.getMessage());
        }
        return resumen;
    }

    /**
     * Obtiene el conteo de estudiantes por estado de pago.
     */
    public Map<String, Integer> obtenerConteoPorEstado() {
        Map<String, Integer> conteo = new HashMap<>();
        String sql = "SELECT estado, COUNT(*) as cantidad FROM planes_pago GROUP BY estado";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                conteo.put(rs.getString("estado"), rs.getInt("cantidad"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener conteo por estado: " + e.getMessage());
        }
        return conteo;
    }

    /**
     * Obtiene los ingresos totales por mes de los últimos 6 meses.
     */
    public Map<String, Double> obtenerIngresosMensuales() {
        Map<String, Double> ingresos = new java.util.LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(fecha, '%b %Y') as mes, SUM(monto) as total " +
                     "FROM pagos_realizados " +
                     "GROUP BY mes " +
                     "ORDER BY MIN(fecha) DESC LIMIT 6";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ingresos.put(rs.getString("mes"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener ingresos mensuales: " + e.getMessage());
        }
        return ingresos;
    }

    /**
     * Obtiene los últimos pagos realizados para mostrar en un feed o tabla pequeña.
     */
    public List<Map<String, Object>> obtenerUltimosPagos(int limite) {
        List<Map<String, Object>> pagos = new ArrayList<>();
        String sql = "SELECT p.*, CONCAT(e.nombre, ' ', e.apellido) as nombre_completo " +
                     "FROM pagos_realizados p " +
                     "JOIN estudiantes e ON p.id_estudiante = e.id_estudiante " +
                     "ORDER BY p.fecha DESC LIMIT ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limite);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> pago = new HashMap<>();
                    pago.put("id_pago", rs.getInt("id_pago"));
                    pago.put("estudiante", rs.getString("nombre_completo"));
                    pago.put("monto", rs.getDouble("monto"));
                    pago.put("fecha", rs.getTimestamp("fecha"));
                    pago.put("modalidad", rs.getString("modalidad"));
                    pago.put("metodo", rs.getString("metodo_pago"));
                    pagos.add(pago);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener últimos pagos: " + e.getMessage());
        }
        return pagos;
    }

    /**
     * Obtiene la lista de estudiantes filtrada por búsqueda, programa y paginada.
     */
    public List<Map<String, Object>> obtenerEstadoPagosEstudiantesPaginado(int pagina, int registrosPorPagina, String filtroBusqueda, String programaNombre) {
        actualizarEstadosAutomaticos();

        List<Map<String, Object>> lista = new ArrayList<>();
        int offset = (pagina - 1) * registrosPorPagina;

        StringBuilder sql = new StringBuilder(
            "SELECT e.id_estudiante, CONCAT(e.nombre, ' ', e.apellido) as nombre_completo, e.codigo, e.fecha_matricula, " +
            "pp.monto_final, pp.saldo_pendiente, pp.estado, pp.cuotas_pagadas, pp.cuotas_totales, " +
            "pp.descuento_porcentaje, pp.descuento_aplicado, pp.fecha_ultimo_pago, pp.fecha_proximo_pago, p.nombre as programa " +
            "FROM estudiantes e " +
            "LEFT JOIN planes_pago pp ON e.id_estudiante = pp.id_estudiante " +
            "LEFT JOIN programas p ON e.id_programa = p.id_programa " +
            "WHERE (CONCAT(e.nombre, ' ', e.apellido) LIKE ? OR e.codigo LIKE ?) "
        );

        if (programaNombre != null && !programaNombre.isEmpty() && !programaNombre.equals("Todos los programas")) {
            sql.append("AND p.nombre = ? ");
        }

        sql.append("ORDER BY pp.estado DESC, nombre_completo ASC LIMIT ? OFFSET ?");

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            String pattern = "%" + (filtroBusqueda != null ? filtroBusqueda : "") + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            
            int paramIndex = 3;
            if (programaNombre != null && !programaNombre.isEmpty() && !programaNombre.equals("Todos los programas")) {
                pstmt.setString(paramIndex++, programaNombre);
            }
            pstmt.setInt(paramIndex++, registrosPorPagina);
            pstmt.setInt(paramIndex++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_estudiante", rs.getInt("id_estudiante"));
                    row.put("codigo", rs.getString("codigo"));
                    row.put("nombre", rs.getString("nombre_completo"));
                    row.put("monto_total", rs.getDouble("monto_final"));
                    row.put("saldo_pendiente", rs.getDouble("saldo_pendiente"));
                    row.put("estado", rs.getString("estado"));
                    row.put("progreso", rs.getInt("cuotas_pagadas") + "/" + rs.getInt("cuotas_totales"));
                    row.put("descuento_porcentaje", rs.getDouble("descuento_porcentaje"));
                    row.put("descuento_aplicado", rs.getBoolean("descuento_aplicado"));
                    row.put("fecha_ultimo_pago", rs.getDate("fecha_ultimo_pago"));
                    row.put("fecha_proximo_pago", rs.getDate("fecha_proximo_pago"));
                    row.put("fecha_matricula", rs.getDate("fecha_matricula"));
                    row.put("programa", rs.getString("programa"));
                    lista.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener estado de pagos paginado: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Cuenta cuántos estudiantes cumplen con los filtros aplicados.
     */
    public int contarEstudiantesConFiltro(String filtroBusqueda, String programaNombre) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM estudiantes e " +
            "LEFT JOIN programas p ON e.id_programa = p.id_programa " +
            "WHERE (CONCAT(e.nombre, ' ', e.apellido) LIKE ? OR e.codigo LIKE ?) "
        );

        if (programaNombre != null && !programaNombre.isEmpty() && !programaNombre.equals("Todos los programas")) {
            sql.append("AND p.nombre = ? ");
        }

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            String pattern = "%" + (filtroBusqueda != null ? filtroBusqueda : "") + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            
            if (programaNombre != null && !programaNombre.isEmpty() && !programaNombre.equals("Todos los programas")) {
                pstmt.setString(3, programaNombre);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al contar estudiantes con filtro: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Actualiza automáticamente los estados de los planes de pago según la fecha actual.
     * ✅ Al día: Fecha futura > 2 días
     * ⚠️ Por vencer: Fecha <= 2 días y >= hoy
     * 🔴 Atrasado: Fecha < hoy
     */
    public void actualizarEstadosAutomaticos() {
        String sql = "UPDATE planes_pago SET estado = CASE " +
                     "WHEN fecha_proximo_pago IS NULL THEN 'AL_DIA' " +
                     "WHEN fecha_proximo_pago < CURRENT_DATE THEN 'ATRASADO' " +
                     "WHEN DATEDIFF(fecha_proximo_pago, CURRENT_DATE) <= 2 THEN 'POR_VENCER' " +
                     "ELSE 'AL_DIA' END " +
                     "WHERE saldo_pendiente > 0";

        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error en actualización automática de estados: " + e.getMessage());
        }
    }
    /**
     * Obtiene el detalle financiero de un estudiante específico.
     */
    public Map<String, Object> obtenerDetalleFinancieroEstudiante(int idEstudiante) {
        String sql = "SELECT e.*, CONCAT(e.nombre, ' ', e.apellido) as nombre_completo, p.nombre as programa_nombre, " +
                     "pp.monto_final, pp.saldo_pendiente, pp.estado, pp.cuotas_totales, pp.cuotas_pagadas, pp.fecha_proximo_pago, pp.fecha_ultimo_pago " +
                     "FROM estudiantes e " +
                     "LEFT JOIN programas p ON e.id_programa = p.id_programa " +
                     "LEFT JOIN planes_pago pp ON e.id_estudiante = pp.id_estudiante " +
                     "WHERE e.id_estudiante = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idEstudiante);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("nombre", rs.getString("nombre_completo"));
                    data.put("codigo", rs.getString("codigo"));
                    data.put("programa", rs.getString("programa_nombre"));
                    data.put("monto_total", rs.getDouble("monto_final"));
                    data.put("saldo", rs.getDouble("saldo_pendiente"));
                    data.put("estado", rs.getString("estado"));
                    data.put("cuotas_totales", rs.getInt("cuotas_totales"));
                    data.put("cuotas_pagadas", rs.getInt("cuotas_pagadas"));
                    data.put("proximo_pago", rs.getDate("fecha_proximo_pago"));
                    data.put("ultimo_pago", rs.getDate("fecha_ultimo_pago"));
                    return data;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener detalle estudiante: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene todo el historial de pagos de un estudiante.
     */
    public List<Map<String, Object>> obtenerHistorialEstudiante(int idEstudiante) {
        List<Map<String, Object>> historial = new ArrayList<>();
        String sql = "SELECT * FROM pagos_realizados WHERE id_estudiante = ? ORDER BY fecha DESC";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idEstudiante);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> h = new HashMap<>();
                    h.put("monto", rs.getDouble("monto"));
                    h.put("fecha", rs.getTimestamp("fecha"));
                    h.put("modalidad", rs.getString("modalidad"));
                    h.put("metodo", rs.getString("metodo_pago"));
                    h.put("comprobante", rs.getString("comprobante"));
                    h.put("comprobante_ruta", rs.getString("comprobante_ruta"));
                    h.put("saldo_despues", rs.getDouble("saldo_restante"));
                    historial.add(h);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener historial: " + e.getMessage());
        }
        return historial;
    }
    /**
     * Cuenta cuántos estudiantes no tienen un plan de pago asignado.
     */
    public int contarEstudiantesSinPlan() {
        String sql = "SELECT COUNT(*) FROM estudiantes e " +
                     "LEFT JOIN planes_pago pp ON e.id_estudiante = pp.id_estudiante " +
                     "WHERE pp.id_plan_pago IS NULL";
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error al contar estudiantes sin plan: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Sincroniza todos los estudiantes sin plan al módulo de pagos.
     * @param descuentoPorcentaje Descuento a aplicar (0-100)
     * @return Cantidad de estudiantes sincronizados
     */
    public int sincronizarEstudiantes(double descuentoPorcentaje) {
        double montoBase = 500000.00;
        double factor = (100.0 - descuentoPorcentaje) / 100.0;
        double montoFinal = montoBase * factor;
        
        String sql = "INSERT INTO planes_pago (id_estudiante, monto_base, descuento_porcentaje, monto_final, saldo_pendiente, cuotas_totales, fecha_proximo_pago) " +
                     "SELECT e.id_estudiante, ?, ?, ?, ?, 5, NULL " +
                     "FROM estudiantes e " +
                     "LEFT JOIN planes_pago pp ON e.id_estudiante = pp.id_estudiante " +
                     "WHERE pp.id_plan_pago IS NULL";
        
        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, montoBase);
            pstmt.setDouble(2, descuentoPorcentaje);
            pstmt.setDouble(3, montoFinal);
            pstmt.setDouble(4, montoFinal);
            
            int rows = pstmt.executeUpdate();
            
            // Actualizar fechas iniciales basadas en fecha_matricula
            actualizarFechasIniciales();
            
            return rows;
        } catch (SQLException e) {
            System.err.println("Error en sincronización masiva: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Inicializa un plan de pago básico para un estudiante recién registrado.
     */
    public boolean inicializarPlanEstudiante(int idEstudiante) {
        double montoBase = 500000.00;
        
        // 1. Obtener fecha_matricula del estudiante
        LocalDate fechaMatricula = null;
        String sqlEst = "SELECT fecha_matricula FROM estudiantes WHERE id_estudiante = ?";
        try (Connection conn = Database.getConexion();
             PreparedStatement psEst = conn.prepareStatement(sqlEst)) {
            psEst.setInt(1, idEstudiante);
            try (ResultSet rs = psEst.executeQuery()) {
                if (rs.next() && rs.getDate("fecha_matricula") != null) {
                    fechaMatricula = rs.getDate("fecha_matricula").toLocalDate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener fecha de matrícula: " + e.getMessage());
        }

        // Si no se encuentra la fecha de matrícula, usar el día de hoy como fallback
        if (fechaMatricula == null) {
            fechaMatricula = LocalDate.now();
        }

        // 2. Calcular la fecha del próximo pago (30 días laborales después de la matrícula)
        LocalDate proximaFecha = calcularFechaVencimiento(fechaMatricula, 30);

        // 3. Insertar el plan de pago con la fecha calculada
        String sql = "INSERT INTO planes_pago (id_estudiante, monto_base, monto_final, saldo_pendiente, cuotas_totales, estado, fecha_proximo_pago) " +
                     "VALUES (?, ?, ?, ?, 5, 'AL_DIA', ?)";
        
        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idEstudiante);
            pstmt.setDouble(2, montoBase);
            pstmt.setDouble(3, montoBase);
            pstmt.setDouble(4, montoBase);
            pstmt.setDate(5, java.sql.Date.valueOf(proximaFecha));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al inicializar plan estudiante: " + e.getMessage());
            return false;
        }
    }

    public boolean aplicarDescuento(int idEstudiante, double porcentaje) {
        double montoBase = 500000.00;
        double totalPagado = 0.0;

        // 1. Obtener el monto_base actual del plan de pago del estudiante
        String sqlSelect = "SELECT monto_base FROM planes_pago WHERE id_estudiante = ?";
        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {
            pstmt.setInt(1, idEstudiante);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    montoBase = rs.getDouble("monto_base");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar monto base: " + e.getMessage());
        }

        // 2. Consultar el total pagado por el estudiante (suma de pagos realizados)
        String sqlPagos = "SELECT COALESCE(SUM(monto), 0) as total_pagado FROM pagos_realizados WHERE id_estudiante = ?";
        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sqlPagos)) {
            pstmt.setInt(1, idEstudiante);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalPagado = rs.getDouble("total_pagado");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar total pagado: " + e.getMessage());
        }

        // 3. Calcular los nuevos valores según el nuevo descuento
        double factor = (100.0 - porcentaje) / 100.0;
        double montoFinal = montoBase * factor;
        double saldoPendiente = montoFinal - totalPagado;
        if (saldoPendiente < 0) {
            saldoPendiente = 0;
        }

        boolean descuentoAplicado = porcentaje > 0.001;

        // 4. Actualizar el plan de pago
        String sqlUpdate = "UPDATE planes_pago SET " +
              "descuento_porcentaje = ?, " +
              "descuento_aplicado = ?, " +
              "monto_final = ?, " +
              "saldo_pendiente = ?, " +
              "estado = CASE WHEN ? <= 0.001 THEN 'AL_DIA' ELSE estado END, " +
              "fecha_proximo_pago = CASE WHEN ? <= 0.001 THEN NULL ELSE fecha_proximo_pago END " +
              "WHERE id_estudiante = ?";

        try (Connection conn = Database.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
            
            pstmt.setDouble(1, porcentaje);
            pstmt.setBoolean(2, descuentoAplicado);
            pstmt.setDouble(3, montoFinal);
            pstmt.setDouble(4, saldoPendiente);
            pstmt.setDouble(5, saldoPendiente);
            pstmt.setDouble(6, saldoPendiente);
            pstmt.setInt(7, idEstudiante);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al aplicar/modificar descuento: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcula la fecha sumando N días laborales (excluyendo domingos).
     */
    public static LocalDate calcularFechaVencimiento(LocalDate inicio, int diasLaborales) {
        LocalDate fecha = inicio;
        int cont = 0;
        while (cont < diasLaborales) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                cont++;
            }
        }
        return fecha;
    }

    private void actualizarFechasIniciales() {
        String sqlSelect = "SELECT pp.id_plan_pago, e.fecha_matricula FROM planes_pago pp " +
                           "JOIN estudiantes e ON pp.id_estudiante = e.id_estudiante " +
                           "WHERE pp.fecha_proximo_pago IS NULL";
        
        String sqlUpdate = "UPDATE planes_pago SET fecha_proximo_pago = ? WHERE id_plan_pago = ?";
        
        try (Connection conn = Database.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSelect);
             PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
            
            while (rs.next()) {
                Date matriculaSql = rs.getDate("fecha_matricula");
                if (matriculaSql == null) continue;
                
                LocalDate matricula = matriculaSql.toLocalDate();
                LocalDate vencimiento = calcularFechaVencimiento(matricula, 30);
                pstmt.setDate(1, java.sql.Date.valueOf(vencimiento));
                pstmt.setInt(2, rs.getInt("id_plan_pago"));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("Error al actualizar fechas iniciales: " + e.getMessage());
        }
    }
}

