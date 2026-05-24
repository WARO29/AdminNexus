import model.PagoRealizado;
import controller.PagoService;
import model.Usuario;

public class TestPayment {
    public static void main(String[] args) {
        try {
            PagoService service = new PagoService();
            PagoRealizado pago = new PagoRealizado();
            pago.setEstudianteId(1); // Andres Romero should be id 1 based on AN-2026-001
            pago.setMonto(500000);
            pago.setModalidad(PagoRealizado.ModalidadPago.CARRERA_TOTAL);
            pago.setMetodoPago("Efectivo");
            pago.setComprobante("Test Carrera Total");
            
            boolean success = service.registrarPagoTransaccional(pago, 1, "Localhost");
            System.out.println("Payment Success: " + success);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
