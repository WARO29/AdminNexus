package model;

import java.time.LocalDateTime;

/**
 * Modelo para el Plan de Pago de un estudiante
 */
public class PlanPago {
    private int id;
    private int estudianteId;
    private double montoBase; // Siempre 500000.00 por defecto
    private double descuentoPorcentaje;
    private boolean descuentoAplicado;
    private double montoFinal;
    private double saldoPendiente;
    private int cuotasTotales;
    private int cuotasPagadas;
    private EstadoPago estado;
    private java.time.LocalDate fechaProximoPago;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public enum EstadoPago {
        AL_DIA("Al día"),
        POR_VENCER("Por vencer"),
        ATRASADO("Atrasado");

        private final String nombre;

        EstadoPago(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return nombre;
        }
    }

    public PlanPago() {
    }

    public PlanPago(int id, int estudianteId, double montoBase, double descuentoPorcentaje, boolean descuentoAplicado, double montoFinal, double saldoPendiente, int cuotasTotales, int cuotasPagadas, EstadoPago estado, java.time.LocalDate fechaProximoPago, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.estudianteId = estudianteId;
        this.montoBase = montoBase;
        this.descuentoPorcentaje = descuentoPorcentaje;
        this.descuentoAplicado = descuentoAplicado;
        this.montoFinal = montoFinal;
        this.saldoPendiente = saldoPendiente;
        this.cuotasTotales = cuotasTotales;
        this.cuotasPagadas = cuotasPagadas;
        this.estado = estado;
        this.fechaProximoPago = fechaProximoPago;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEstudianteId() {
        return estudianteId;
    }

    public void setEstudianteId(int estudianteId) {
        this.estudianteId = estudianteId;
    }

    public double getMontoBase() {
        return montoBase;
    }

    public void setMontoBase(double montoBase) {
        this.montoBase = montoBase;
    }

    public double getDescuentoPorcentaje() {
        return descuentoPorcentaje;
    }

    public void setDescuentoPorcentaje(double descuentoPorcentaje) {
        this.descuentoPorcentaje = descuentoPorcentaje;
    }

    public boolean isDescuentoAplicado() {
        return descuentoAplicado;
    }

    public void setDescuentoAplicado(boolean descuentoAplicado) {
        this.descuentoAplicado = descuentoAplicado;
    }

    public double getMontoFinal() {
        return montoFinal;
    }

    public void setMontoFinal(double montoFinal) {
        this.montoFinal = montoFinal;
    }

    public double getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(double saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public int getCuotasTotales() {
        return cuotasTotales;
    }

    public void setCuotasTotales(int cuotasTotales) {
        this.cuotasTotales = cuotasTotales;
    }

    public int getCuotasPagadas() {
        return cuotasPagadas;
    }

    public void setCuotasPagadas(int cuotasPagadas) {
        this.cuotasPagadas = cuotasPagadas;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public java.time.LocalDate getFechaProximoPago() {
        return fechaProximoPago;
    }

    public void setFechaProximoPago(java.time.LocalDate fechaProximoPago) {
        this.fechaProximoPago = fechaProximoPago;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
