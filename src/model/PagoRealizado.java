package model;

import java.time.LocalDateTime;

/**
 * Modelo para el historial de Pagos Realizados
 */
public class PagoRealizado {
    private int id;
    private int estudianteId;
    private double monto;
    private LocalDateTime fecha;
    private ModalidadPago modalidad;
    private String metodoPago;
    private String comprobante;
    private double saldoRestante;
    private String comprobanteRuta;

    public enum ModalidadPago {
        ABONO_SABADO("Abono Sábado"),
        ABONO_SEMANA("Abono Semana"),
        CUOTA_MENSUAL("Cuota Mensual"),
        CARRERA_TOTAL("Carrera Total"),
        MEDIA_CARRERA("Media Carrera");

        private final String nombre;

        ModalidadPago(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return nombre;
        }
    }

    public PagoRealizado() {
    }

    public PagoRealizado(int id, int estudianteId, double monto, LocalDateTime fecha, ModalidadPago modalidad, String metodoPago, String comprobante, double saldoRestante) {
        this.id = id;
        this.estudianteId = estudianteId;
        this.monto = monto;
        this.fecha = fecha;
        this.modalidad = modalidad;
        this.metodoPago = metodoPago;
        this.comprobante = comprobante;
        this.saldoRestante = saldoRestante;
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

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public ModalidadPago getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadPago modalidad) {
        this.modalidad = modalidad;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getComprobante() {
        return comprobante;
    }

    public void setComprobante(String comprobante) {
        this.comprobante = comprobante;
    }

    public double getSaldoRestante() {
        return saldoRestante;
    }

    public void setSaldoRestante(double saldoRestante) {
        this.saldoRestante = saldoRestante;
    }

    public String getComprobanteRuta() {
        return comprobanteRuta;
    }

    public void setComprobanteRuta(String comprobanteRuta) {
        this.comprobanteRuta = comprobanteRuta;
    }
}
