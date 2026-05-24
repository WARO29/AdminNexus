package model;

import java.time.LocalDateTime;

/**
 * Modelo para el registro de auditoría de seguridad
 */
public class LogAuditoria {
    private int id;
    private int usuarioId;
    private String accion;
    private String detalle;
    private LocalDateTime fecha;
    private String ipDispositivo;

    public LogAuditoria() {
    }

    public LogAuditoria(int id, int usuarioId, String accion, String detalle, LocalDateTime fecha, String ipDispositivo) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.detalle = detalle;
        this.fecha = fecha;
        this.ipDispositivo = ipDispositivo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getIpDispositivo() {
        return ipDispositivo;
    }

    public void setIpDispositivo(String ipDispositivo) {
        this.ipDispositivo = ipDispositivo;
    }
}
