-- Esencias & Estilo Solano. 17 de noviembre de 2026.
-- Migración de estructura01 con devoluciones y referencia única de cobro (28tablas,46FK).
-- No ejecutar sobre la estructura01 actualizada que ya incluye funcion_material.
-- OTRO conserva sin inventar el significado del artículo previo; clasificarlo expresamente antes de vincularlo a fórmulas o empaques.
ALTER TABLE articulo ADD COLUMN funcion_material VARCHAR(32) NOT NULL DEFAULT 'OTRO';
ALTER TABLE articulo ALTER COLUMN funcion_material DROP DEFAULT;
ALTER TABLE articulo DROP CHECK ck_articulo_1;
ALTER TABLE articulo DROP CHECK ck_articulo_2;
ALTER TABLE articulo DROP CHECK ck_articulo_3;
ALTER TABLE articulo DROP CHECK ck_articulo_4;
ALTER TABLE articulo DROP CHECK ck_articulo_5;
ALTER TABLE articulo DROP CHECK ck_articulo_6;
ALTER TABLE articulo DROP CHECK ck_articulo_7;
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_1 CHECK (unidad_base IN ('ML','UNIDAD','G'));
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_2 CHECK (minimo_base>=0);
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_3 CHECK (contenido_nominal_ml IS NULL OR contenido_nominal_ml>0);
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_4 CHECK (funcion_material NOT IN ('ESENCIA','ALCOHOL','FIJADOR','FEROMONAS') OR unidad_base='ML');
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_5 CHECK (funcion_material NOT IN ('FRASCO','VALVULA','TAPA','ETIQUETA','ATOMIZADOR') OR unidad_base='UNIDAD');
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_6 CHECK (funcion_material NOT IN ('FRASCO','ATOMIZADOR') OR (contenido_nominal_ml IS NOT NULL AND contenido_nominal_ml>0));
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_7 CHECK (`articulo_id` IS NULL OR REGEXP_LIKE(`articulo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'));
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_8 CHECK (`unidad_base` IN ('ML','UNIDAD','G'));
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_9 CHECK (`clase` IN ('LIQUIDO','ENVASE','ACCESORIO','PERFUME_SELLADO','PERFUME_ABIERTO','MUESTRA'));
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_10 CHECK (`activo` IN (0,1));
ALTER TABLE articulo ADD CONSTRAINT ck_articulo_11 CHECK (`funcion_material` IN ('ESENCIA','ALCOHOL','FIJADOR','FEROMONAS','FRASCO','VALVULA','TAPA','ETIQUETA','ATOMIZADOR','PERFUME','OTRO'));
