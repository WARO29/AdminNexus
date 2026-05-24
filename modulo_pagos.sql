SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS logs_auditoria_financiera;
DROP TABLE IF EXISTS pagos_realizados;
DROP TABLE IF EXISTS planes_pago;

USE adminnexus;

CREATE TABLE planes_pago (
    id_plan_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    monto_base DECIMAL(10, 2) NOT NULL DEFAULT 500000.00,
    descuento_porcentaje DECIMAL(5, 2) DEFAULT 0.00,
    descuento_aplicado BOOLEAN DEFAULT FALSE,
    monto_final DECIMAL(10, 2) NOT NULL,
    saldo_pendiente DECIMAL(10, 2) NOT NULL,
    cuotas_totales INT NOT NULL DEFAULT 5,
    cuotas_pagadas INT DEFAULT 0,
    estado ENUM('AL_DIA', 'POR_VENCER', 'ATRASADO') DEFAULT 'AL_DIA',
    fecha_proximo_pago DATE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pagos_realizados (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    monto DECIMAL(10, 2) NOT NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modalidad ENUM('ABONO_SABADO','ABONO_SEMANA','CUOTA_MENSUAL','CARRERA_TOTAL','MEDIA_CARRERA') NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    comprobante VARCHAR(255),
    saldo_restante DECIMAL(10, 2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE logs_auditoria_financiera (
    id_log INT AUTO_INCREMENT PRIMARY KEY,
    idusuario INT, -- Lo dejamos como INT normal sin FK física para máxima compatibilidad
    accion VARCHAR(100) NOT NULL,
    detalle TEXT, 
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_dispositivo VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE planes_pago 
ADD CONSTRAINT fk_plan_est_pagos FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE;

ALTER TABLE pagos_realizados 
ADD CONSTRAINT fk_pago_est_pagos FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;
