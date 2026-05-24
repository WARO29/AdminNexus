-- Script para eliminar la columna 'facultad' de la tabla programas
-- Ejecutar este script en la base de datos adminnexus

USE adminnexus;

-- Eliminar la columna facultad de la tabla programas
ALTER TABLE programas DROP COLUMN facultad;

-- Verificar que la columna fue eliminada
DESCRIBE programas;
