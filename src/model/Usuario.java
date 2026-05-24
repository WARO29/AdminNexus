package model;

/**
 * Modelo de Usuario del sistema AdminNexus.
 * Representa un usuario con sus credenciales y rol.
 */
public class Usuario {
    private int idusuario;
    private String nombreAdmin;
    private String user;
    private String password;
    private RolUsuario rol;
    
    /**
     * Enum para los roles de usuario en el sistema
     */
    public enum RolUsuario {
        ADMINISTRADOR("Administrador"),
        USUARIO("Usuario");
        
        private final String nombre;
        
        RolUsuario(String nombre) {
            this.nombre = nombre;
        }
        
        public String getNombre() {
            return nombre;
        }
    }
    
    // Constructor vacío
    public Usuario() {
    }
    
    // Constructor completo
    public Usuario(int idusuario, String nombreAdmin, String user, String password, RolUsuario rol) {
        this.idusuario = idusuario;
        this.nombreAdmin = nombreAdmin;
        this.user = user;
        this.password = password;
        this.rol = rol;
    }
    
    // Getters y Setters
    public int getIdusuario() {
        return idusuario;
    }
    
    public void setIdusuario(int idusuario) {
        this.idusuario = idusuario;
    }
    
    public String getNombreAdmin() {
        return nombreAdmin;
    }
    
    public void setNombreAdmin(String nombreAdmin) {
        this.nombreAdmin = nombreAdmin;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public RolUsuario getRol() {
        return rol;
    }
    
    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }
    
    /**
     * Verifica si el usuario es administrador
     */
    public boolean esAdministrador() {
        return this.rol == RolUsuario.ADMINISTRADOR;
    }
    
    /**
     * Verifica si el usuario es un usuario regular
     */
    public boolean esUsuario() {
        return this.rol == RolUsuario.USUARIO;
    }
    
    @Override
    public String toString() {
        return "Usuario{" +
                "idusuario=" + idusuario +
                ", nombreAdmin='" + nombreAdmin + '\'' +
                ", user='" + user + '\'' +
                ", rol=" + rol.getNombre() +
                '}';
    }
}
