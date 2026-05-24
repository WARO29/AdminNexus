package main;

import views.Login;

/**
 * Punto de entrada principal de la aplicación AdminNexus.
 */
public class Main {
    public static void main(String[] args) {
        // Ejecutar la interfaz gráfica en el hilo de eventos de Swing
        java.awt.EventQueue.invokeLater(() -> {
            Login loginView = new Login();
            loginView.setVisible(true);
        });
    }
}
