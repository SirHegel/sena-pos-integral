USE pos_integral_ga6;

-- Evolución de aplicación de la base académica aislada, posterior al DDL61 y CHECK61.
-- La carga inicial de esta API se hace sin pagos; no se atribuye medio a registros históricos.
-- Esta migración debe ejecutarse antes de cualquier semilla o prueba de pago de la API.
ALTER TABLE pago ADD COLUMN medio VARCHAR(16) NOT NULL;
ALTER TABLE pago ADD CONSTRAINT ck_pago_medio CHECK (medio IN ('EFECTIVO','EXTERNO'));
