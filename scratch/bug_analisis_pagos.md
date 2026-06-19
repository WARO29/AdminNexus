# Análisis y Verificación de Bug: Abonos Parciales y Cuotas en AdminNexus

Este documento contiene el análisis, verificación técnica y propuestas de solución para el comportamiento inconsistente del sistema de pagos al realizar abonos parciales en la última cuota.

---

## 🔍 Verificación Técnica en el Código

Hemos verificado el estado del código base clonado y confirmamos que **los 4 puntos reportados en el análisis del bug son 100% correctos y actualmente están presentes en el sistema**.

A continuación, detallamos la ubicación exacta y la línea de código afectada para cada punto:

### 1. Inconsistencia Visual y Lógica en la Base de Datos (5/5 con Deuda)
*   **Archivo afectado:** [PagoService.java](file:///c:/Users/Estudiante/Documents/NetBeansProjects/AdminNexus/src/controller/PagoService.java#L77-L79)
*   **Código actual:**
    ```java
    } else {
        cuotasPagadasActuales++;
    }
    ```
*   **Verificación:** Cuando la modalidad de cobro es un abono ordinario (`CUOTA_MENSUAL`, `ABONO_SABADO`, `ABONO_SEMANA`), el sistema realiza un incremento ciego de `cuotasPagadasActuales` sin validar el monto. Si un estudiante tiene 4 cuotas de 5 y hace un pago de tan solo $1, el sistema registra `cuotas_pagadas = 5` en `planes_pago`, mostrando visualmente un progreso de `5/5` (completado) en el perfil del estudiante, aunque todavía exista un saldo pendiente en la base de datos.

### 2. Descuadre en el "Número de Cuota" en UI
*   **Archivo afectado:** [RegistroPagoDialog.java](file:///c:/Users/Estudiante/Documents/NetBeansProjects/AdminNexus/src/views/RegistroPagoDialog.java#L113-L114)
*   **Código actual:**
    ```java
    int siguienteCuota = dataEstudiante != null ? ((int) dataEstudiante.get("cuotas_pagadas") + 1) : 1;
    txtCuota = crearCampoTexto(String.valueOf(siguienteCuota), "Número de Cuota");
    ```
*   **Verificación:** Como consecuencia del incremento ciego del punto 1, si un estudiante con 5 cuotas totales tiene `cuotas_pagadas = 5` debido a un abono parcial previo, el cuadro de diálogo calculará `siguienteCuota = 6`. Al intentar saldar la deuda restante, el administrador verá la sugerencia de pagar la **"Cuota #6"**, lo cual es confuso e incorrecto para planes estructurados de 5 cuotas.

### 3. Pérdida de la "Fecha del Próximo Pago" (NULL)
*   **Archivo afectado:** [PagoService.java](file:///c:/Users/Estudiante/Documents/NetBeansProjects/AdminNexus/src/controller/PagoService.java#L81-L106) y [L131-L136](file:///c:/Users/Estudiante/Documents/NetBeansProjects/AdminNexus/src/controller/PagoService.java#L131-L136)
*   **Código actual:**
    ```java
    java.time.LocalDate proximaFecha = null;
    // ... solo se calcula para CUOTA_MENSUAL y MEDIA_CARRERA ...
    
    if (proximaFecha != null && nuevoSaldo > 0.001) {
        psUpdatePlan.setDate(5, java.sql.Date.valueOf(proximaFecha));
    } else {
        psUpdatePlan.setNull(5, java.sql.Types.DATE);
    }
    ```
*   **Verificación:** Para las modalidades `ABONO_SABADO` and `ABONO_SEMANA`, la variable `proximaFecha` queda con valor `null`. En la actualización del plan de pago, si `proximaFecha` es `null`, la consulta ejecuta `psUpdatePlan.setNull(5, java.sql.Types.DATE)`, lo que **borra la fecha límite del próximo pago** en la base de datos. Como resultado, el sistema dejará de generar alertas o recordatorios para este estudiante.

### 4. Estado Inconsistentemente "Al Día"
*   **Archivo afectado:** [PagoService.java](file:///c:/Users/Estudiante/Documents/NetBeansProjects/AdminNexus/src/controller/PagoService.java#L81) y [PagosController.java](file:///c:/Users/Estudiante/Documents/NetBeansProjects/AdminNexus/src/controller/PagosController.java#L282-L296)
*   **Código actual:**
    ```java
    String nuevoEstado = "AL_DIA";
    ```
*   **Verificación:** Al registrar una transacción, se establece el estado a `"AL_DIA"` por defecto. Además, cuando el sistema ejecuta la actualización automática de estados (`actualizarEstadosAutomaticos` en `PagosController.java`), al estar la fecha de vencimiento en `NULL` (debido al bug 3), la base de datos evalúa:
    ```sql
    WHEN fecha_proximo_pago IS NULL THEN 'AL_DIA'
    ```
    Esto consolida al estudiante con deuda en el estado `"AL_DIA"`, ocultándolo de la vista de deudores y morosos.

---

## 🛠️ Solución Recomendada (Implementación Técnica)

Para resolver estos cuatro problemas de manera definitiva y robusta sin alterar el flujo natural del programa, proponemos las siguientes modificaciones:

### Modificación 1: Cálculo Proporcional Matemático de Cuotas
En lugar de un incremento ciego, obtenemos el `monto_final` y las `cuotas_totales` del plan de pago para calcular cuántas cuotas completas ha cubierto el estudiante con base en el nuevo saldo.

En [PagoService.java](file:///c:/Users/Estudiante/Documents/NetBeansProjects/AdminNexus/src/controller/PagoService.java):

```diff
             // 1. Obtener saldo actual del Plan de Pago (id_estudiante)
             double saldoActual = 0;
             int cuotasPagadasActuales = 0;
-            String sqlPlan = "SELECT saldo_pendiente, cuotas_pagadas FROM planes_pago WHERE id_estudiante = ?";
+            double montoFinal = 0;
+            int cuotasTotales = 0;
+            java.sql.Date fechaProximoPagoActual = null;
+            String sqlPlan = "SELECT saldo_pendiente, cuotas_pagadas, monto_final, cuotas_totales, fecha_proximo_pago FROM planes_pago WHERE id_estudiante = ?";
             try (PreparedStatement psPlan = conn.prepareStatement(sqlPlan)) {
                 psPlan.setInt(1, pago.getEstudianteId());
                 try (ResultSet rs = psPlan.executeQuery()) {
                     if (rs.next()) {
                         saldoActual = rs.getDouble("saldo_pendiente");
                         cuotasPagadasActuales = rs.getInt("cuotas_pagadas");
+                        montoFinal = rs.getDouble("monto_final");
+                        cuotasTotales = rs.getInt("cuotas_totales");
+                        fechaProximoPagoActual = rs.getDate("fecha_proximo_pago");
                     } else {
                         throw new SQLException("No se encontró el plan de pago para el estudiante especificado.");
                     }
                 }
             }

             // 2. Calcular nuevo saldo y estado
             double nuevoSaldo = saldoActual - pago.getMonto();
             if (nuevoSaldo < 0) nuevoSaldo = 0; 
             
             pago.setSaldoRestante(nuevoSaldo);
             
             // 3. Determinar cuotas pagadas según modalidad
             if (pago.getModalidad() == PagoRealizado.ModalidadPago.CARRERA_TOTAL) {
-                // Obtener totales para setear al máximo
-                String sqlTot = "SELECT cuotas_totales FROM planes_pago WHERE id_estudiante = ?";
-                try (PreparedStatement psTot = conn.prepareStatement(sqlTot)) {
-                    psTot.setInt(1, pago.getEstudianteId());
-                    try (ResultSet rs = psTot.executeQuery()) {
-                        if (rs.next()) cuotasPagadasActuales = rs.getInt("cuotas_totales");
-                    }
-                }
+                cuotasPagadasActuales = cuotasTotales;
             } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.MEDIA_CARRERA) {
-                String sqlTot = "SELECT cuotas_totales FROM planes_pago WHERE id_estudiante = ?";
-                try (PreparedStatement psTot = conn.prepareStatement(sqlTot)) {
-                    psTot.setInt(1, pago.getEstudianteId());
-                    try (ResultSet rs = psTot.executeQuery()) {
-                        if (rs.next()) {
-                            int total = rs.getInt("cuotas_totales");
-                            // Se registra al menos la mitad de las cuotas totales
-                            cuotasPagadasActuales = Math.max(cuotasPagadasActuales + 1, (int) Math.ceil(total / 2.0));
-                        }
-                    }
-                }
+                cuotasPagadasActuales = Math.max(cuotasPagadasActuales, (int) Math.ceil(cuotasTotales / 2.0));
             } else {
-                cuotasPagadasActuales++;
+                // Cálculo proporcional basado en dinero real aportado
+                double totalPagadoReal = montoFinal - nuevoSaldo;
+                double valorPorCuota = montoFinal / cuotasTotales;
+                if (valorPorCuota > 0) {
+                    cuotasPagadasActuales = (int) (totalPagadoReal / valorPorCuota);
+                } else {
+                    cuotasPagadasActuales = cuotasTotales;
+                }
             }
```

### Modificación 2: Conservar y Calcular la Fecha Próxima de Pago Dinámicamente
Modificamos la lógica de actualización del plan para conservar el vencimiento guardado si es un abono, y calcular el estado en base a la fecha límite real.

En [PagoService.java](file:///c:/Users/Estudiante/Documents/NetBeansProjects/AdminNexus/src/controller/PagoService.java):

```diff
-            String nuevoEstado = "AL_DIA";
             java.time.LocalDate proximaFecha = null;
             java.time.LocalDate hoy = java.time.LocalDate.now();
             
             if (pago.getModalidad() == PagoRealizado.ModalidadPago.CUOTA_MENSUAL) {
                 // Obtener la fecha de vencimiento actual para sumarle 30 días laborales
                 String sqlFechaActual = "SELECT fecha_proximo_pago FROM planes_pago WHERE id_estudiante = ?";
                 try (PreparedStatement psFecha = conn.prepareStatement(sqlFechaActual)) {
                     psFecha.setInt(1, pago.getEstudianteId());
                     try (ResultSet rs = psFecha.executeQuery()) {
                         if (rs.next() && rs.getDate("fecha_proximo_pago") != null) {
                             java.time.LocalDate vencimientoActual = rs.getDate("fecha_proximo_pago").toLocalDate();
                             // Si ya venció, empezamos desde hoy. Si no, desde el vencimiento.
                             java.time.LocalDate inicio = vencimientoActual.isBefore(hoy) ? hoy : vencimientoActual;
                             proximaFecha = PagosController.calcularFechaVencimiento(inicio, 30);
                         } else {
                             proximaFecha = PagosController.calcularFechaVencimiento(hoy, 30);
                         }
                     }
                 }
             } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.CARRERA_TOTAL) {
                 proximaFecha = null; // No hay más fechas de pago
             } else if (pago.getModalidad() == PagoRealizado.ModalidadPago.MEDIA_CARRERA) {
                 // Por defecto, le damos 60 días para el próximo pago si paga la mitad
                 proximaFecha = PagosController.calcularFechaVencimiento(hoy, 60);
             }

+            // Determinar la fecha de próximo pago final
+            java.time.LocalDate fechaVencimientoFinal = null;
+            if (nuevoSaldo <= 0.001) {
+                fechaVencimientoFinal = null; // Paz y salvo, no hay más pagos
+            } else if (proximaFecha != null) {
+                fechaVencimientoFinal = proximaFecha; // Nueva fecha calculada
+            } else if (fechaProximoPagoActual != null) {
+                fechaVencimientoFinal = fechaProximoPagoActual.toLocalDate(); // Conservar la fecha previa para abonos
+            } else {
+                fechaVencimientoFinal = PagosController.calcularFechaVencimiento(hoy, 30); // Asignar una por defecto si no tenía
+            }
+
+            // Determinar estado inmediato basado en la fecha límite final
+            String nuevoEstado = "AL_DIA";
+            if (nuevoSaldo > 0.001 && fechaVencimientoFinal != null) {
+                if (fechaVencimientoFinal.isBefore(hoy)) {
+                    nuevoEstado = "ATRASADO";
+                } else if (java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaVencimientoFinal) <= 2) {
+                    nuevoEstado = "POR_VENCER";
+                }
+            }
```

Y luego, en la query de actualización del plan:

```diff
             // 5. Actualizar el Plan de Pago (id_estudiante)
             String sqlUpdatePlan = "UPDATE planes_pago SET saldo_pendiente = ?, cuotas_pagadas = ?, estado = ?, " +
                                    "fecha_ultimo_pago = ?, fecha_proximo_pago = ? " +
                                    "WHERE id_estudiante = ?";
             try (PreparedStatement psUpdatePlan = conn.prepareStatement(sqlUpdatePlan)) {
                 psUpdatePlan.setDouble(1, nuevoSaldo);
                 psUpdatePlan.setInt(2, cuotasPagadasActuales);
                 psUpdatePlan.setString(3, nuevoEstado);
                 psUpdatePlan.setDate(4, java.sql.Date.valueOf(hoy));
                 
-                if (proximaFecha != null && nuevoSaldo > 0.001) {
-                    psUpdatePlan.setDate(5, java.sql.Date.valueOf(proximaFecha));
+                if (fechaVencimientoFinal != null) {
+                    psUpdatePlan.setDate(5, java.sql.Date.valueOf(fechaVencimientoFinal));
                 } else {
-                    // Si es Carrera Total o el saldo está totalmente pagado, eliminamos la fecha de próximo pago
                     psUpdatePlan.setNull(5, java.sql.Types.DATE);
                 }
                 psUpdatePlan.setInt(6, pago.getEstudianteId());
                 psUpdatePlan.executeUpdate();
             }
```

---

## 📈 Conclusiones del Análisis
1.  **Validado**: El bug existe tal cual se describió. El uso de la última cuota con abonos parciales rompe tanto el indicador visual `5/5` como el cálculo de las cuotas futuras y el vencimiento de recordatorios.
2.  **Seguridad**: La solución propuesta corrige todos los frentes de inconsistencia lógica en el controlador y la base de datos de manera limpia, sin requerir cambios invasivos en la interfaz visual.
