-- Esencias & Estilo Solano. 17 de noviembre de 2026.
-- Aplicar sólo a la estructura previa sin esta restricción; no ejecutar tras crear el01 actual.
-- Una recepción representa la factura completa de un proveedor.
ALTER TABLE documento_inventario ADD CONSTRAINT uq_documento_inventario_4 UNIQUE (proveedor_id,referencia_factura);
