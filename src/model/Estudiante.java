package model;

import java.time.LocalDate;

/**
 * Modelo de Estudiante
 */
public class Estudiante {
    private int idEstudiante;
    private String codigo; // AN-2024-001
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String direccion;
    private LocalDate fechaNacimiento;
    private int idPrograma; // FK a programas
    private String nombrePrograma; // Para mostrar en la tabla
    private LocalDate fechaMatricula;
    private EstadoMatricula estado;
    private String avatarUrl; // URL o path del avatar
    
    /**
     * Enum para los estados de matrícula
     */
    public enum EstadoMatricula {
        ACTIVO("Activo"),
        INACTIVO("Inactivo"),
        GRADUADO("Graduado"),
        RETIRADO("Retirado");
        
        private final String nombre;
        
        EstadoMatricula(String nombre) {
            this.nombre = nombre;
        }
        
        public String getNombre() {
            return nombre;
        }
    }
    
    // Constructor vacío
    public Estudiante() {
    }
    
    // Constructor completo
    public Estudiante(int idEstudiante, String codigo, String nombre, String apellido,
                     String email, String telefono, String direccion, LocalDate fechaNacimiento,
                     int idPrograma, String nombrePrograma, LocalDate fechaMatricula,
                     EstadoMatricula estado, String avatarUrl) {
        this.idEstudiante = idEstudiante;
        this.codigo = codigo;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.fechaNacimiento = fechaNacimiento;
        this.idPrograma = idPrograma;
        this.nombrePrograma = nombrePrograma;
        this.fechaMatricula = fechaMatricula;
        this.estado = estado;
        this.avatarUrl = avatarUrl;
    }
    
    // Getters y Setters
    public int getIdEstudiante() {
        return idEstudiante;
    }
    
    public void setIdEstudiante(int idEstudiante) {
        this.idEstudiante = idEstudiante;
    }
    
    public String getCodigo() {
        return codigo;
    }
    
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getApellido() {
        return apellido;
    }
    
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
    
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getTelefono() {
        return telefono;
    }
    
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    
    public String getDireccion() {
        return direccion;
    }
    
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }
    
    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }
    
    public int getIdPrograma() {
        return idPrograma;
    }
    
    public void setIdPrograma(int idPrograma) {
        this.idPrograma = idPrograma;
    }
    
    public String getNombrePrograma() {
        return nombrePrograma;
    }
    
    public void setNombrePrograma(String nombrePrograma) {
        this.nombrePrograma = nombrePrograma;
    }
    
    public LocalDate getFechaMatricula() {
        return fechaMatricula;
    }
    
    public void setFechaMatricula(LocalDate fechaMatricula) {
        this.fechaMatricula = fechaMatricula;
    }
    
    public EstadoMatricula getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoMatricula estado) {
        this.estado = estado;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    @Override
    public String toString() {
        return "Estudiante{" +
                "idEstudiante=" + idEstudiante +
                ", codigo='" + codigo + '\'' +
                ", nombre='" + getNombreCompleto() + '\'' +
                ", email='" + email + '\'' +
                ", programa='" + nombrePrograma + '\'' +
                ", estado=" + estado.getNombre() +
                '}';
    }
}
