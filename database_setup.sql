-- ============================================================
-- Script de Referencia - AdminNexus
-- Sistema de Gestión con Roles de Usuario
-- ============================================================

-- NOTA: Este script es solo de REFERENCIA
-- La base de datos 'adminnexus' y la tabla 'usuarios' ya existen

-- ============================================================
-- ESTRUCTURA DE LA TABLA (Ya existente)
-- ============================================================
/*
CREATE TABLE usuarios (
    idusuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre_admin VARCHAR(100) NOT NULL,
    user VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    rol ENUM('administrador', 'usuario') NOT NULL
);
*/

-- ============================================================
-- CREAR USUARIOS MANUALMENTE
-- ============================================================

-- Ejemplo 1: Crear un ADMINISTRADOR
INSERT INTO usuarios (nombre_admin, user, password, rol) 
VALUES ('Juan Pérez', 'admin', 'admin123', 'administrador');

-- Ejemplo 2: Crear un USUARIO regular
INSERT INTO usuarios (nombre_admin, user, password, rol) 
VALUES ('María García', 'maria', 'maria123', 'usuario');

-- Ejemplo 3: Crear otro ADMINISTRADOR
INSERT INTO usuarios (nombre_admin, user, password, rol) 
VALUES ('Carlos López', 'carlos', 'carlos123', 'administrador');

-- ============================================================
-- CONSULTAS ÚTILES
-- ============================================================

-- Ver todos los usuarios
SELECT * FROM usuarios;

-- Ver solo administradores
SELECT * FROM usuarios WHERE rol = 'administrador';

-- Ver solo usuarios regulares
SELECT * FROM usuarios WHERE rol = 'usuario';

-- Contar usuarios por rol
SELECT rol, COUNT(*) as total FROM usuarios GROUP BY rol;

-- ============================================================
-- MODIFICAR USUARIOS
-- ============================================================

-- Cambiar contraseña de un usuario
-- UPDATE usuarios SET password = 'nueva_contraseña' WHERE user = 'admin';

-- Cambiar rol de un usuario
-- UPDATE usuarios SET rol = 'administrador' WHERE user = 'maria';

-- Cambiar nombre de un usuario
-- UPDATE usuarios SET nombre_admin = 'Nuevo Nombre' WHERE user = 'admin';

-- ============================================================
-- ELIMINAR USUARIOS
-- ============================================================

-- Eliminar un usuario específico
-- DELETE FROM usuarios WHERE user = 'maria';

-- Eliminar todos los usuarios (CUIDADO - solo para pruebas)
-- DELETE FROM usuarios;

-- ============================================================
-- NOTAS IMPORTANTES
-- ============================================================
-- 1. El campo 'user' debe ser ÚNICO (no puede haber duplicados)
-- 2. El campo 'rol' solo acepta: 'administrador' o 'usuario'
-- 3. Las contraseñas se guardan en texto plano (mejorar en producción)
-- 4. Los usuarios se crean MANUALMENTE en la base de datos
-- 5. El sistema solo permite LOGIN, no registro desde la aplicación
-- 6. Más adelante se habilitará el módulo de gestión de usuarios

-- ============================================================
-- PLANTILLA PARA CREAR NUEVOS USUARIOS
-- ============================================================
/*
INSERT INTO usuarios (nombre_admin, user, password, rol) 
VALUES ('NOMBRE_COMPLETO', 'USUARIO', 'CONTRASEÑA', 'ROL');

Donde:
- NOMBRE_COMPLETO: Nombre completo del usuario (ej. 'Juan Pérez')
- USUARIO: Nombre de usuario para login (ej. 'jperez')
- CONTRASEÑA: Contraseña del usuario (ej. 'pass123')
- ROL: 'administrador' o 'usuario'
*/

