package model;

import java.sql.Timestamp;

public class Actividad {
    
    public enum TipoActividad {
        PROGRAMA,
        ESTUDIANTE,
        SISTEMA,
        PAGO
    }
    
    private int idActividad;
    private String descripcion;
    private TipoActividad tipo;
    private Timestamp fecha;
    private String nombreUsuario;

    public Actividad() {
    }

    public Actividad(String descripcion, TipoActividad tipo) {
        this.descripcion = descripcion;
        this.tipo = tipo;
    }

    public int getIdActividad() {
        return idActividad;
    }

    public void setIdActividad(int idActividad) {
        this.idActividad = idActividad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoActividad getTipo() {
        return tipo;
    }

    public void setTipo(TipoActividad tipo) {
        this.tipo = tipo;
    }

    public Timestamp getFecha() {
        return fecha;
    }

    public void setFecha(Timestamp fecha) {
        this.fecha = fecha;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}
