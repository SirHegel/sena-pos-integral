-- Evolución académica GA8 del caso CU06. Aplicar después de 01, 02 y 03.
-- Ejecutar sobre la misma base de datos seleccionada para la integración 92.
-- Una sola aplicación: la columna y el CHECK no existen en el esquema anterior.
ALTER TABLE existencia
  ADD COLUMN minimo DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  ADD CONSTRAINT existencia_minimo_no_negativo CHECK (minimo >= 0);
