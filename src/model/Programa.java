package model;

/**
 * Modelo de Programa Académico
 */
public class Programa {
    private int idPrograma;
    private String codigo;
    private String nombre;
    private int duracionSemestres;
    private int inscritos;
    private EstadoPrograma estado;
    private String iconoColor; // Color del icono en formato hex
    
    /**
     * Enum para los estados del programa
     */
    public enum EstadoPrograma {
        ACTIVO("Activo"),
        CERRADO("Cerrado"),
        EN_PAUSA("En Pausa");
        
        private final String nombre;
        
        EstadoPrograma(String nombre) {
            this.nombre = nombre;
        }
        
        public String getNombre() {
            return nombre;
        }
    }
    
    // Constructor vacío
    public Programa() {
    }
    
    // Constructor completo
    public Programa(int idPrograma, String codigo, String nombre, 
                   int duracionSemestres, int inscritos, 
                   EstadoPrograma estado, String iconoColor) {
        this.idPrograma = idPrograma;
        this.codigo = codigo;
        this.nombre = nombre;
        this.duracionSemestres = duracionSemestres;
        this.inscritos = inscritos;
        this.estado = estado;
        this.iconoColor = iconoColor;
    }
    
    // Getters y Setters
    public int getIdPrograma() {
        return idPrograma;
    }
    
    public void setIdPrograma(int idPrograma) {
        this.idPrograma = idPrograma;
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
    
    public int getDuracionSemestres() {
        return duracionSemestres;
    }
    
    public void setDuracionSemestres(int duracionSemestres) {
        this.duracionSemestres = duracionSemestres;
    }
    

    
    public int getInscritos() {
        return inscritos;
    }
    
    public void setInscritos(int inscritos) {
        this.inscritos = inscritos;
    }
    
    public EstadoPrograma getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoPrograma estado) {
        this.estado = estado;
    }
    
    public String getIconoColor() {
        return iconoColor;
    }
    
    public void setIconoColor(String iconoColor) {
        this.iconoColor = iconoColor;
    }
    
    @Override
    public String toString() {
        return "Programa{" +
                "idPrograma=" + idPrograma +
                ", codigo='" + codigo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", duracionSemestres=" + duracionSemestres +
                ", inscritos=" + inscritos +
                ", estado=" + estado.getNombre() +
                '}';
    }
}
