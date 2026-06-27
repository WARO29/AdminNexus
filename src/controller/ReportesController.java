package controller;

import config.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportesController {

    // ─── FINANCIERO ───────────────────────────────────────────────────────────

    public Map<String, Double> getResumenFinanciero(Date inicio, Date fin, String metodo, String modalidad) {
        Map<String, Double> result = new LinkedHashMap<>();
        String sqlPlan = "SELECT SUM(monto_final) AS esperado, SUM(saldo_pendiente) AS pendiente FROM planes_pago";
        String filtroMetodo   = metodo   != null ? " AND metodo_pago = ?" : "";
        String filtroModalidad = modalidad != null ? " AND modalidad = ?"  : "";
        String sqlPagos = "SELECT SUM(monto) AS recaudado FROM pagos_realizados " +
                "WHERE (anulado = FALSE OR anulado IS NULL) AND fecha BETWEEN ? AND ?" + filtroMetodo + filtroModalidad;
        try (Connection conn = Database.getConexion()) {
            double esperado = 0, pendiente = 0, recaudado = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlPlan);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    esperado  = rs.getDouble("esperado");
                    pendiente = rs.getDouble("pendiente");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlPagos)) {
                int idx = 1;
                ps.setTimestamp(idx++, new java.sql.Timestamp(inicio.getTime()));
                ps.setTimestamp(idx++, new java.sql.Timestamp(fin.getTime()));
                if (metodo    != null) ps.setString(idx++, metodo);
                if (modalidad != null) ps.setString(idx,   modalidad);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) recaudado = rs.getDouble("recaudado");
                }
            }
            result.put("total_esperado",     esperado);
            result.put("total_recaudado",    recaudado);
            result.put("total_pendiente",    pendiente);
            result.put("porcentaje_recaudo", esperado > 0 ? (recaudado / esperado) * 100 : 0);
        } catch (SQLException e) {
            System.err.println("Error getResumenFinanciero: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Integer> getDistribucionModalidades(Date inicio, Date fin, String metodo, String modalidad) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String filtroMetodo    = metodo    != null ? " AND metodo_pago = ?" : "";
        String filtroModalidad = modalidad != null ? " AND modalidad = ?"   : "";
        String sql = "SELECT modalidad, COUNT(*) AS cantidad FROM pagos_realizados " +
                "WHERE (anulado = FALSE OR anulado IS NULL) AND fecha BETWEEN ? AND ?" +
                filtroMetodo + filtroModalidad + " GROUP BY modalidad ORDER BY cantidad DESC";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setTimestamp(idx++, new java.sql.Timestamp(inicio.getTime()));
            ps.setTimestamp(idx++, new java.sql.Timestamp(fin.getTime()));
            if (metodo    != null) ps.setString(idx++, metodo);
            if (modalidad != null) ps.setString(idx,   modalidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(rs.getString("modalidad"), rs.getInt("cantidad"));
            }
        } catch (SQLException e) {
            System.err.println("Error getDistribucionModalidades: " + e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getPagosPorModalidad(Date inicio, Date fin, String modalidad, String metodo) {
        List<Map<String, Object>> list = new ArrayList<>();
        String filtroModalidad = (modalidad != null) ? " AND pr.modalidad = ?" : "";
        String filtroMetodo    = (metodo    != null) ? " AND pr.metodo_pago = ?" : "";
        String sql = "SELECT pr.fecha, CONCAT(e.nombre,' ',e.apellido) AS estudiante, e.codigo, " +
                "p.nombre AS programa, pr.monto, pr.modalidad, pr.metodo_pago, pr.nombre_usuario " +
                "FROM pagos_realizados pr " +
                "JOIN estudiantes e ON pr.id_estudiante = e.id_estudiante " +
                "JOIN programas p ON e.id_programa = p.id_programa " +
                "WHERE (pr.anulado = FALSE OR pr.anulado IS NULL) AND pr.fecha BETWEEN ? AND ?" +
                filtroModalidad + filtroMetodo + " ORDER BY pr.fecha DESC";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setTimestamp(idx++, new java.sql.Timestamp(inicio.getTime()));
            ps.setTimestamp(idx++, new java.sql.Timestamp(fin.getTime()));
            if (modalidad != null) ps.setString(idx++, modalidad);
            if (metodo    != null) ps.setString(idx,   metodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("fecha",          rs.getTimestamp("fecha"));
                    row.put("estudiante",     rs.getString("estudiante"));
                    row.put("codigo",         rs.getString("codigo"));
                    row.put("programa",       rs.getString("programa"));
                    row.put("monto",          rs.getDouble("monto"));
                    row.put("modalidad",      rs.getString("modalidad"));
                    row.put("metodo",         rs.getString("metodo_pago"));
                    row.put("nombre_usuario", rs.getString("nombre_usuario"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getPagosPorModalidad: " + e.getMessage());
        }
        return list;
    }

    public List<Map<String, Object>> getResumenPagosPorEstudiante(Date inicio, Date fin, String metodo, String modalidad) {
        List<Map<String, Object>> list = new ArrayList<>();
        String filtroMetodo    = metodo    != null ? " AND pr.metodo_pago = ?" : "";
        String filtroModalidad = modalidad != null ? " AND pr.modalidad = ?"   : "";
        String sql = "SELECT CONCAT(e.nombre,' ',e.apellido) AS estudiante, e.codigo, " +
                "p.nombre AS programa, " +
                "COUNT(pr.id_pago)   AS num_pagos, " +
                "SUM(pr.monto)       AS total_pagado, " +
                "MIN(pr.fecha)       AS primer_pago, " +
                "MAX(pr.fecha)       AS ultimo_pago " +
                "FROM pagos_realizados pr " +
                "JOIN estudiantes e ON pr.id_estudiante = e.id_estudiante " +
                "JOIN programas p   ON e.id_programa = p.id_programa " +
                "WHERE (pr.anulado = FALSE OR pr.anulado IS NULL) " +
                "AND pr.fecha BETWEEN ? AND ?" + filtroMetodo + filtroModalidad + " " +
                "GROUP BY pr.id_estudiante, e.nombre, e.apellido, e.codigo, p.nombre " +
                "ORDER BY num_pagos DESC, total_pagado DESC";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setTimestamp(idx++, new java.sql.Timestamp(inicio.getTime()));
            ps.setTimestamp(idx++, new java.sql.Timestamp(fin.getTime()));
            if (metodo    != null) ps.setString(idx++, metodo);
            if (modalidad != null) ps.setString(idx,   modalidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("estudiante",  rs.getString("estudiante"));
                    row.put("codigo",      rs.getString("codigo"));
                    row.put("programa",    rs.getString("programa"));
                    row.put("num_pagos",   rs.getInt("num_pagos"));
                    row.put("total_pagado",rs.getDouble("total_pagado"));
                    row.put("primer_pago", rs.getTimestamp("primer_pago"));
                    row.put("ultimo_pago", rs.getTimestamp("ultimo_pago"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getResumenPagosPorEstudiante: " + e.getMessage());
        }
        return list;
    }

    public Map<String, Double> getIngresosPorMes(Date inicio, Date fin, String metodo, String modalidad) {
        Map<String, Double> result = new LinkedHashMap<>();
        String filtroMetodo    = metodo    != null ? " AND metodo_pago = ?" : "";
        String filtroModalidad = modalidad != null ? " AND modalidad = ?"   : "";
        String sql = "SELECT DATE_FORMAT(fecha, '%b %Y') AS mes, SUM(monto) AS total " +
                "FROM pagos_realizados " +
                "WHERE (anulado = FALSE OR anulado IS NULL) AND fecha BETWEEN ? AND ?" +
                filtroMetodo + filtroModalidad + " " +
                "GROUP BY DATE_FORMAT(fecha, '%Y-%m') " +
                "ORDER BY MIN(fecha) ASC";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setTimestamp(idx++, new java.sql.Timestamp(inicio.getTime()));
            ps.setTimestamp(idx++, new java.sql.Timestamp(fin.getTime()));
            if (metodo    != null) ps.setString(idx++, metodo);
            if (modalidad != null) ps.setString(idx,   modalidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("mes"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getIngresosPorMes: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Integer> getDistribucionEstadoPago() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT UPPER(estado) AS estado, COUNT(*) AS cantidad FROM planes_pago GROUP BY UPPER(estado)";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("estado"), rs.getInt("cantidad"));
            }
        } catch (SQLException e) {
            System.err.println("Error getDistribucionEstadoPago: " + e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getUltimosPagos(Date inicio, Date fin, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT pr.monto, pr.fecha, pr.modalidad, pr.metodo_pago, pr.nombre_usuario, " +
                "CONCAT(e.nombre, ' ', e.apellido) AS estudiante " +
                "FROM pagos_realizados pr JOIN estudiantes e ON pr.id_estudiante = e.id_estudiante " +
                "WHERE (pr.anulado = FALSE OR pr.anulado IS NULL) AND pr.fecha BETWEEN ? AND ? " +
                "ORDER BY pr.fecha DESC LIMIT ?";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new java.sql.Timestamp(inicio.getTime()));
            ps.setTimestamp(2, new java.sql.Timestamp(fin.getTime()));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("estudiante",    rs.getString("estudiante"));
                    row.put("monto",         rs.getDouble("monto"));
                    row.put("fecha",         rs.getTimestamp("fecha"));
                    row.put("modalidad",     rs.getString("modalidad"));
                    row.put("metodo",        rs.getString("metodo_pago"));
                    row.put("nombre_usuario", rs.getString("nombre_usuario"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getUltimosPagos: " + e.getMessage());
        }
        return list;
    }

    // ─── ESTUDIANTES ─────────────────────────────────────────────────────────

    public Map<String, Integer> getEstudiantesPorEstado() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT UPPER(estado) AS estado, COUNT(*) AS cantidad FROM estudiantes GROUP BY UPPER(estado)";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("estado"), rs.getInt("cantidad"));
            }
        } catch (SQLException e) {
            System.err.println("Error getEstudiantesPorEstado: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Integer> getEstudiantesPorPrograma() {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT p.nombre, COUNT(e.id_estudiante) AS cantidad " +
                "FROM programas p LEFT JOIN estudiantes e ON p.id_programa = e.id_programa " +
                "GROUP BY p.id_programa, p.nombre ORDER BY cantidad DESC";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("nombre"), rs.getInt("cantidad"));
            }
        } catch (SQLException e) {
            System.err.println("Error getEstudiantesPorPrograma: " + e.getMessage());
        }
        return result;
    }

    public List<Map<String, Object>> getEstudiantesEnMora() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT CONCAT(e.nombre,' ',e.apellido) AS nombre, e.codigo, " +
                "p.nombre AS programa, pp.saldo_pendiente, pp.fecha_proximo_pago " +
                "FROM planes_pago pp " +
                "JOIN estudiantes e ON pp.id_estudiante = e.id_estudiante " +
                "JOIN programas p ON e.id_programa = p.id_programa " +
                "WHERE UPPER(pp.estado) = 'ATRASADO' AND pp.saldo_pendiente > 0 " +
                "ORDER BY pp.saldo_pendiente DESC";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("nombre",          rs.getString("nombre"));
                row.put("codigo",          rs.getString("codigo"));
                row.put("programa",        rs.getString("programa"));
                row.put("saldo_pendiente", rs.getDouble("saldo_pendiente"));
                row.put("fecha_proximo_pago", rs.getDate("fecha_proximo_pago"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error getEstudiantesEnMora: " + e.getMessage());
        }
        return list;
    }

    // ─── PROGRAMAS ────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getResumenProgramas() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.nombre, p.codigo, UPPER(p.estado) AS estado, p.duracion_semestres, " +
                "COUNT(e.id_estudiante) AS estudiantes_activos " +
                "FROM programas p " +
                "LEFT JOIN estudiantes e ON p.id_programa = e.id_programa AND UPPER(e.estado) = 'ACTIVO' " +
                "GROUP BY p.id_programa, p.nombre, p.codigo, p.estado, p.duracion_semestres " +
                "ORDER BY p.nombre";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("nombre",              rs.getString("nombre"));
                row.put("codigo",              rs.getString("codigo"));
                row.put("estado",              rs.getString("estado"));
                row.put("duracion_semestres",  rs.getInt("duracion_semestres"));
                row.put("estudiantes_activos", rs.getInt("estudiantes_activos"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error getResumenProgramas: " + e.getMessage());
        }
        return list;
    }

    // ─── AUDITORÍA ────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getActividadesAuditoria(Date inicio, Date fin) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT descripcion, tipo, fecha, nombre_usuario FROM actividades " +
                "WHERE fecha BETWEEN ? AND ? ORDER BY fecha DESC LIMIT 200";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new java.sql.Timestamp(inicio.getTime()));
            ps.setTimestamp(2, new java.sql.Timestamp(fin.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("descripcion",   rs.getString("descripcion"));
                    row.put("tipo",          rs.getString("tipo"));
                    row.put("fecha",         rs.getTimestamp("fecha"));
                    row.put("nombre_usuario", rs.getString("nombre_usuario"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getActividadesAuditoria: " + e.getMessage());
        }
        return list;
    }

    public List<Map<String, Object>> getLogsFinancieros(Date inicio, Date fin) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT l.accion, l.detalle, l.fecha, l.ip_dispositivo, u.nombre_admin AS nombre_usuario " +
                "FROM logs_auditoria_financiera l " +
                "LEFT JOIN usuarios u ON l.idusuario = u.idusuario " +
                "WHERE l.fecha BETWEEN ? AND ? ORDER BY l.fecha DESC LIMIT 200";
        try (Connection conn = Database.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new java.sql.Timestamp(inicio.getTime()));
            ps.setTimestamp(2, new java.sql.Timestamp(fin.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("accion",        rs.getString("accion"));
                    row.put("detalle",       rs.getString("detalle"));
                    row.put("fecha",         rs.getTimestamp("fecha"));
                    row.put("ip",            rs.getString("ip_dispositivo"));
                    row.put("nombre_usuario", rs.getString("nombre_usuario"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getLogsFinancieros: " + e.getMessage());
        }
        return list;
    }
}
