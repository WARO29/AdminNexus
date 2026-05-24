-- Script para eliminar la columna costo_matricula de la tabla programas
-- Ejecutar este script una sola vez en su base de datos adminnexus

USE adminnexus;

ALTER TABLE programas DROP COLUMN costo_matricula;
