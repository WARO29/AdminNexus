package model;

import java.util.List;

public class ResultadoBusqueda {
    private List<Estudiante> estudiantes;
    private List<Programa> programas;

    public ResultadoBusqueda(List<Estudiante> estudiantes, List<Programa> programas) {
        this.estudiantes = estudiantes;
        this.programas = programas;
    }

    public List<Estudiante> getEstudiantes() {
        return estudiantes;
    }

    public void setEstudiantes(List<Estudiante> estudiantes) {
        this.estudiantes = estudiantes;
    }

    public List<Programa> getProgramas() {
        return programas;
    }

    public void setProgramas(List<Programa> programas) {
        this.programas = programas;
    }
    
    public boolean isEmpty() {
        return (estudiantes == null || estudiantes.isEmpty()) && 
               (programas == null || programas.isEmpty());
    }
}
