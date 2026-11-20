-- Esencias & Estilo Solano. 17 de noviembre de 2026.
-- Migración exclusiva de la estructura inicial (SHA256 a6d5beed96333456ef6dd9bfce73a5db4d2643e4ffaa79236afa946be9607fd9).
-- No ejecutar después de crear directamente la estructura 01 actualizada.
-- Aplicar sin escrituras concurrentes; conservar respaldo antes de migrar una base existente.
ALTER TABLE movimiento_caja ADD COLUMN incidencia_id CHAR(36) NULL;
ALTER TABLE movimiento_caja DROP CHECK ck_movimiento_caja_1;
ALTER TABLE movimiento_caja DROP CHECK ck_movimiento_caja_2;
ALTER TABLE movimiento_caja DROP CHECK ck_movimiento_caja_3;
ALTER TABLE movimiento_caja DROP CHECK ck_movimiento_caja_4;
ALTER TABLE movimiento_caja DROP CHECK ck_movimiento_caja_5;
ALTER TABLE movimiento_caja DROP CHECK ck_movimiento_caja_6;
ALTER TABLE movimiento_caja DROP CHECK ck_movimiento_caja_7;
ALTER TABLE movimiento_caja DROP CHECK ck_movimiento_caja_8;
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_1 CHECK (importe_cop>0);
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_2 CHECK (TRIM(motivo)<>'');
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_3 CHECK (gasto_id IS NULL OR sentido='SALIDA');
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_4 CHECK (incidencia_id IS NULL OR sentido='SALIDA');
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_5 CHECK (gasto_id IS NULL OR incidencia_id IS NULL);
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_6 CHECK (`movimiento_caja_id` IS NULL OR REGEXP_LIKE(`movimiento_caja_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'));
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_7 CHECK (`turno_id` IS NULL OR REGEXP_LIKE(`turno_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'));
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_8 CHECK (`sentido` IN ('ENTRADA','SALIDA'));
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_9 CHECK (`gasto_id` IS NULL OR REGEXP_LIKE(`gasto_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'));
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_10 CHECK (`registrado_por_usuario_id` IS NULL OR REGEXP_LIKE(`registrado_por_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'));
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_11 CHECK (`incidencia_id` IS NULL OR REGEXP_LIKE(`incidencia_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'));
ALTER TABLE movimiento_caja ADD CONSTRAINT fk_46_movimiento_caja FOREIGN KEY (incidencia_id) REFERENCES incidencia(incidencia_id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE cobro ADD CONSTRAINT uq_cobro_2 UNIQUE (medio,referencia_externa);
