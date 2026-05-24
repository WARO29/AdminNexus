-- ============================================================
-- Script para LIMPIAR la tabla de programas
-- AdminNexus - Sistema de Gestión
-- ============================================================

-- IMPORTANTE: Este script eliminará TODOS los programas existentes
-- Ejecuta esto solo si quieres empezar con la tabla vacía

USE adminnexus;

-- Eliminar todos los programas
DELETE FROM programas;

-- Reiniciar el contador de ID (opcional)
ALTER TABLE programas AUTO_INCREMENT = 1;

-- Verificar que la tabla esté vacía
SELECT COUNT(*) as total_programas FROM programas;

-- Debería mostrar: total_programas = 0

-- ============================================================
-- NOTA: Después de ejecutar este script, la tabla estará vacía
-- y podrás agregar programas desde el sistema usando el botón
-- "+ Nuevo Programa"
-- ============================================================
