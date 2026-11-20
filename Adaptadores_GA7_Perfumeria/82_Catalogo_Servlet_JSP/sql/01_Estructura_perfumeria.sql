-- Esencias & Estilo Solano. 17 de noviembre de 2026.
-- MySQL 8.4 / InnoDB / UTF-8. Crear en esquema aislado asignado por el operador.
-- El archivo no elimina bases ni carga personas, usuarios o movimientos reales.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_as_cs;
SET SESSION time_zone='+00:00';

CREATE TABLE `integrante_equipo` (
  `integrante_id` CHAR(36) NOT NULL,
  `nombre_visible` VARCHAR(160) NOT NULL,
  `vinculo` VARCHAR(32) NOT NULL,
  `activo` TINYINT NOT NULL,
  PRIMARY KEY (`integrante_id`),
  CONSTRAINT `ck_integrante_equipo_1` CHECK (`integrante_id` IS NULL OR REGEXP_LIKE(`integrante_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_integrante_equipo_2` CHECK (`vinculo` IN ('PROPIETARIO','COLABORADORA')),
  CONSTRAINT `ck_integrante_equipo_3` CHECK (`activo` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `usuario` (
  `usuario_id` CHAR(36) NOT NULL,
  `integrante_id` CHAR(36) NOT NULL,
  `nombre_acceso` VARCHAR(160) NOT NULL,
  `hash_clave` VARCHAR(255) NOT NULL,
  `perfil` VARCHAR(32) NOT NULL,
  `activo` TINYINT NOT NULL,
  PRIMARY KEY (`usuario_id`),
  CONSTRAINT `uq_usuario_1` UNIQUE (`integrante_id`),
  CONSTRAINT `uq_usuario_2` UNIQUE (`nombre_acceso`),
  CONSTRAINT `ck_usuario_1` CHECK (TRIM(nombre_acceso) <> ''),
  CONSTRAINT `ck_usuario_2` CHECK (nombre_acceso=LOWER(TRIM(nombre_acceso))),
  CONSTRAINT `ck_usuario_3` CHECK (`usuario_id` IS NULL OR REGEXP_LIKE(`usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_usuario_4` CHECK (`integrante_id` IS NULL OR REGEXP_LIKE(`integrante_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_usuario_5` CHECK (`perfil` IN ('ADMINISTRADOR','VENDEDORA')),
  CONSTRAINT `ck_usuario_6` CHECK (`activo` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `cliente` (
  `cliente_id` CHAR(36) NOT NULL,
  `nombre` VARCHAR(160) NOT NULL,
  `contacto_cobranza` VARCHAR(160) NULL,
  `activo` TINYINT NOT NULL,
  PRIMARY KEY (`cliente_id`),
  CONSTRAINT `ck_cliente_1` CHECK (`cliente_id` IS NULL OR REGEXP_LIKE(`cliente_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_cliente_2` CHECK (`activo` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `proveedor` (
  `proveedor_id` CHAR(36) NOT NULL,
  `nombre` VARCHAR(160) NOT NULL,
  `contacto` VARCHAR(160) NULL,
  PRIMARY KEY (`proveedor_id`),
  CONSTRAINT `ck_proveedor_1` CHECK (`proveedor_id` IS NULL OR REGEXP_LIKE(`proveedor_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `unidad` (
  `unidad_codigo` VARCHAR(12) NOT NULL,
  `dimension` VARCHAR(32) NOT NULL,
  `factor_base` DECIMAL(18,6) NOT NULL,
  PRIMARY KEY (`unidad_codigo`),
  CONSTRAINT `ck_unidad_1` CHECK ((unidad_codigo='ML' AND dimension='VOLUMEN' AND factor_base=1) OR (unidad_codigo='L' AND dimension='VOLUMEN' AND factor_base=1000) OR (unidad_codigo='UNIDAD' AND dimension='CONTEO' AND factor_base=1) OR (unidad_codigo='G' AND dimension='MASA' AND factor_base=1)),
  CONSTRAINT `ck_unidad_2` CHECK (`unidad_codigo` IN ('ML','L','UNIDAD','G')),
  CONSTRAINT `ck_unidad_3` CHECK (`dimension` IN ('VOLUMEN','CONTEO','MASA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `articulo` (
  `articulo_id` CHAR(36) NOT NULL,
  `codigo` VARCHAR(160) NOT NULL,
  `nombre` VARCHAR(160) NOT NULL,
  `unidad_base` VARCHAR(12) NOT NULL,
  `clase` VARCHAR(32) NOT NULL,
  `contenido_nominal_ml` DECIMAL(18,6) NULL,
  `minimo_base` DECIMAL(18,6) NOT NULL,
  `activo` TINYINT NOT NULL,
  `funcion_material` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`articulo_id`),
  CONSTRAINT `uq_articulo_1` UNIQUE (`codigo`),
  CONSTRAINT `ck_articulo_1` CHECK (unidad_base IN ('ML','UNIDAD','G')),
  CONSTRAINT `ck_articulo_2` CHECK (minimo_base>=0),
  CONSTRAINT `ck_articulo_3` CHECK (contenido_nominal_ml IS NULL OR contenido_nominal_ml>0),
  CONSTRAINT `ck_articulo_4` CHECK (funcion_material NOT IN ('ESENCIA','ALCOHOL','FIJADOR','FEROMONAS') OR unidad_base='ML'),
  CONSTRAINT `ck_articulo_5` CHECK (funcion_material NOT IN ('FRASCO','VALVULA','TAPA','ETIQUETA','ATOMIZADOR') OR unidad_base='UNIDAD'),
  CONSTRAINT `ck_articulo_6` CHECK (funcion_material NOT IN ('FRASCO','ATOMIZADOR') OR (contenido_nominal_ml IS NOT NULL AND contenido_nominal_ml>0)),
  CONSTRAINT `ck_articulo_7` CHECK (`articulo_id` IS NULL OR REGEXP_LIKE(`articulo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_articulo_8` CHECK (`unidad_base` IN ('ML','UNIDAD','G')),
  CONSTRAINT `ck_articulo_9` CHECK (`clase` IN ('LIQUIDO','ENVASE','ACCESORIO','PERFUME_SELLADO','PERFUME_ABIERTO','MUESTRA')),
  CONSTRAINT `ck_articulo_10` CHECK (`activo` IN (0,1)),
  CONSTRAINT `ck_articulo_11` CHECK (`funcion_material` IN ('ESENCIA','ALCOHOL','FIJADOR','FEROMONAS','FRASCO','VALVULA','TAPA','ETIQUETA','ATOMIZADOR','PERFUME','OTRO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `fragancia` (
  `fragancia_id` CHAR(36) NOT NULL,
  `codigo_interno` VARCHAR(160) NOT NULL,
  `nombre_comercial` VARCHAR(160) NOT NULL,
  `activo` TINYINT NOT NULL,
  PRIMARY KEY (`fragancia_id`),
  CONSTRAINT `uq_fragancia_1` UNIQUE (`codigo_interno`),
  CONSTRAINT `ck_fragancia_1` CHECK (`fragancia_id` IS NULL OR REGEXP_LIKE(`fragancia_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_fragancia_2` CHECK (`activo` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `formula_version` (
  `formula_id` CHAR(36) NOT NULL,
  `fragancia_id` CHAR(36) NOT NULL,
  `variante` VARCHAR(32) NOT NULL,
  `version` INT NOT NULL,
  `volumen_preparado_ml` DECIMAL(18,6) NOT NULL,
  `estado` VARCHAR(32) NOT NULL,
  `aprobada_por_usuario_id` CHAR(36) NULL,
  `aprobada_en` DATETIME(6) NULL,
  PRIMARY KEY (`formula_id`),
  CONSTRAINT `uq_formula_version_1` UNIQUE (`fragancia_id`,`variante`,`volumen_preparado_ml`,`version`),
  CONSTRAINT `ck_formula_version_1` CHECK (version>0),
  CONSTRAINT `ck_formula_version_2` CHECK (volumen_preparado_ml IN (30,50,100)),
  CONSTRAINT `ck_formula_version_3` CHECK ((estado='BORRADOR' AND aprobada_por_usuario_id IS NULL AND aprobada_en IS NULL) OR (estado IN ('APROBADA','RETIRADA') AND aprobada_por_usuario_id IS NOT NULL AND aprobada_en IS NOT NULL)),
  CONSTRAINT `ck_formula_version_4` CHECK (`formula_id` IS NULL OR REGEXP_LIKE(`formula_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_formula_version_5` CHECK (`fragancia_id` IS NULL OR REGEXP_LIKE(`fragancia_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_formula_version_6` CHECK (`variante` IN ('NORMAL','EXTRA_FIJADOR','EXTRA_FEROMONAS')),
  CONSTRAINT `ck_formula_version_7` CHECK (`estado` IN ('BORRADOR','APROBADA','RETIRADA')),
  CONSTRAINT `ck_formula_version_8` CHECK (`aprobada_por_usuario_id` IS NULL OR REGEXP_LIKE(`aprobada_por_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `formula_componente` (
  `formula_id` CHAR(36) NOT NULL,
  `linea_no` INT NOT NULL,
  `articulo_id` CHAR(36) NOT NULL,
  `cantidad_ml_por_preparacion` DECIMAL(18,6) NOT NULL,
  PRIMARY KEY (`formula_id`,`linea_no`),
  CONSTRAINT `uq_formula_componente_1` UNIQUE (`formula_id`,`articulo_id`),
  CONSTRAINT `ck_formula_componente_1` CHECK (linea_no>0),
  CONSTRAINT `ck_formula_componente_2` CHECK (cantidad_ml_por_preparacion>0),
  CONSTRAINT `ck_formula_componente_3` CHECK (`formula_id` IS NULL OR REGEXP_LIKE(`formula_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_formula_componente_4` CHECK (`articulo_id` IS NULL OR REGEXP_LIKE(`articulo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `oferta` (
  `oferta_id` CHAR(36) NOT NULL,
  `codigo_venta` VARCHAR(160) NOT NULL,
  `nombre_venta` VARCHAR(160) NOT NULL,
  `modalidad` VARCHAR(32) NOT NULL,
  `fragancia_id` CHAR(36) NULL,
  `volumen_ml` DECIMAL(18,6) NULL,
  `activo` TINYINT NOT NULL,
  PRIMARY KEY (`oferta_id`),
  CONSTRAINT `uq_oferta_1` UNIQUE (`codigo_venta`),
  CONSTRAINT `ck_oferta_1` CHECK (volumen_ml IS NULL OR volumen_ml>0),
  CONSTRAINT `ck_oferta_2` CHECK ((modalidad IN ('PREPARACION','RECARGA') AND fragancia_id IS NOT NULL AND volumen_ml IN (30,50,100)) OR (modalidad='DECANT' AND fragancia_id IS NOT NULL AND volumen_ml IN (5,10)) OR modalidad IN ('TERMINADO','ADICIONAL')),
  CONSTRAINT `ck_oferta_3` CHECK (`oferta_id` IS NULL OR REGEXP_LIKE(`oferta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_oferta_4` CHECK (`modalidad` IN ('TERMINADO','PREPARACION','RECARGA','DECANT','ADICIONAL')),
  CONSTRAINT `ck_oferta_5` CHECK (`fragancia_id` IS NULL OR REGEXP_LIKE(`fragancia_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_oferta_6` CHECK (`activo` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `especificacion_suministro` (
  `especificacion_id` CHAR(36) NOT NULL,
  `oferta_id` CHAR(36) NOT NULL,
  `version` INT NOT NULL,
  `formula_id` CHAR(36) NULL,
  `estado` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`especificacion_id`),
  CONSTRAINT `uq_especificacion_suministro_1` UNIQUE (`oferta_id`,`version`),
  CONSTRAINT `ck_especificacion_suministro_1` CHECK (version>0),
  CONSTRAINT `ck_especificacion_suministro_2` CHECK (`especificacion_id` IS NULL OR REGEXP_LIKE(`especificacion_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_especificacion_suministro_3` CHECK (`oferta_id` IS NULL OR REGEXP_LIKE(`oferta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_especificacion_suministro_4` CHECK (`formula_id` IS NULL OR REGEXP_LIKE(`formula_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_especificacion_suministro_5` CHECK (`estado` IN ('BORRADOR','APROBADA','RETIRADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `especificacion_componente` (
  `especificacion_id` CHAR(36) NOT NULL,
  `linea_no` INT NOT NULL,
  `articulo_id` CHAR(36) NOT NULL,
  `cantidad_base` DECIMAL(18,6) NOT NULL,
  PRIMARY KEY (`especificacion_id`,`linea_no`),
  CONSTRAINT `uq_especificacion_componente_1` UNIQUE (`especificacion_id`,`articulo_id`),
  CONSTRAINT `ck_especificacion_componente_1` CHECK (linea_no>0),
  CONSTRAINT `ck_especificacion_componente_2` CHECK (cantidad_base>0),
  CONSTRAINT `ck_especificacion_componente_3` CHECK (`especificacion_id` IS NULL OR REGEXP_LIKE(`especificacion_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_especificacion_componente_4` CHECK (`articulo_id` IS NULL OR REGEXP_LIKE(`articulo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `precio_oferta` (
  `precio_id` CHAR(36) NOT NULL,
  `oferta_id` CHAR(36) NOT NULL,
  `vigente_desde` DATETIME(6) NOT NULL,
  `vigente_hasta` DATETIME(6) NULL,
  `importe_unitario_cop` DECIMAL(18,2) NOT NULL,
  PRIMARY KEY (`precio_id`),
  CONSTRAINT `uq_precio_oferta_1` UNIQUE (`oferta_id`,`vigente_desde`),
  CONSTRAINT `ck_precio_oferta_1` CHECK (importe_unitario_cop>0),
  CONSTRAINT `ck_precio_oferta_2` CHECK (vigente_hasta IS NULL OR vigente_hasta>vigente_desde),
  CONSTRAINT `ck_precio_oferta_3` CHECK (`precio_id` IS NULL OR REGEXP_LIKE(`precio_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_precio_oferta_4` CHECK (`oferta_id` IS NULL OR REGEXP_LIKE(`oferta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `caja` (
  `caja_id` CHAR(36) NOT NULL,
  `nombre` VARCHAR(160) NOT NULL,
  `activa` TINYINT NOT NULL,
  PRIMARY KEY (`caja_id`),
  CONSTRAINT `uq_caja_1` UNIQUE (`nombre`),
  CONSTRAINT `ck_caja_1` CHECK (`caja_id` IS NULL OR REGEXP_LIKE(`caja_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_caja_2` CHECK (`activa` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `turno_caja` (
  `turno_id` CHAR(36) NOT NULL,
  `caja_id` CHAR(36) NOT NULL,
  `responsable_usuario_id` CHAR(36) NOT NULL,
  `abierta_en` DATETIME(6) NOT NULL,
  `cerrada_en` DATETIME(6) NULL,
  `fondo_inicial_cop` DECIMAL(18,2) NOT NULL,
  `observacion_cierre` TEXT NULL,
  PRIMARY KEY (`turno_id`),
  CONSTRAINT `ck_turno_caja_1` CHECK (fondo_inicial_cop>=0),
  CONSTRAINT `ck_turno_caja_2` CHECK (cerrada_en IS NULL OR cerrada_en>=abierta_en),
  CONSTRAINT `ck_turno_caja_3` CHECK (`turno_id` IS NULL OR REGEXP_LIKE(`turno_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_turno_caja_4` CHECK (`caja_id` IS NULL OR REGEXP_LIKE(`caja_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_turno_caja_5` CHECK (`responsable_usuario_id` IS NULL OR REGEXP_LIKE(`responsable_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `venta` (
  `venta_id` CHAR(36) NOT NULL,
  `numero_recibo` VARCHAR(160) NULL,
  `cliente_id` CHAR(36) NULL,
  `vendedora_usuario_id` CHAR(36) NOT NULL,
  `creada_en` DATETIME(6) NOT NULL,
  `emitida_en` DATETIME(6) NULL,
  `estado` VARCHAR(32) NOT NULL,
  `clave_emision` VARCHAR(160) NULL,
  `huella_emision` CHAR(64) NULL,
  PRIMARY KEY (`venta_id`),
  CONSTRAINT `uq_venta_1` UNIQUE (`numero_recibo`),
  CONSTRAINT `uq_venta_2` UNIQUE (`clave_emision`),
  CONSTRAINT `ck_venta_1` CHECK ((estado='EMITIDA' AND emitida_en IS NOT NULL AND numero_recibo IS NOT NULL AND clave_emision IS NOT NULL) OR (estado IN ('BORRADOR','ANULADA_BORRADOR') AND emitida_en IS NULL AND numero_recibo IS NULL AND clave_emision IS NULL)),
  CONSTRAINT `ck_venta_2` CHECK (`venta_id` IS NULL OR REGEXP_LIKE(`venta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_venta_3` CHECK (`cliente_id` IS NULL OR REGEXP_LIKE(`cliente_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_venta_4` CHECK (`vendedora_usuario_id` IS NULL OR REGEXP_LIKE(`vendedora_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_venta_5` CHECK (`estado` IN ('BORRADOR','EMITIDA','ANULADA_BORRADOR')),
  CONSTRAINT `ck_venta_6` CHECK (`huella_emision` IS NULL OR REGEXP_LIKE(`huella_emision`, '^[0-9a-f]{64}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `linea_venta` (
  `venta_id` CHAR(36) NOT NULL,
  `linea_no` INT NOT NULL,
  `especificacion_id` CHAR(36) NOT NULL,
  `cantidad_unidades` INT NOT NULL,
  `precio_unitario_cop` DECIMAL(18,2) NOT NULL,
  `descripcion_entregada` VARCHAR(500) NOT NULL,
  PRIMARY KEY (`venta_id`,`linea_no`),
  CONSTRAINT `ck_linea_venta_1` CHECK (linea_no>0),
  CONSTRAINT `ck_linea_venta_2` CHECK (cantidad_unidades>0),
  CONSTRAINT `ck_linea_venta_3` CHECK (precio_unitario_cop>0),
  CONSTRAINT `ck_linea_venta_4` CHECK (TRIM(descripcion_entregada)<>''),
  CONSTRAINT `ck_linea_venta_5` CHECK (`venta_id` IS NULL OR REGEXP_LIKE(`venta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_linea_venta_6` CHECK (`especificacion_id` IS NULL OR REGEXP_LIKE(`especificacion_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `descuento_autorizado` (
  `venta_id` CHAR(36) NOT NULL,
  `importe_cop` DECIMAL(18,2) NOT NULL,
  `autorizado_por_usuario_id` CHAR(36) NOT NULL,
  `autorizado_en` DATETIME(6) NOT NULL,
  `motivo` VARCHAR(500) NOT NULL,
  PRIMARY KEY (`venta_id`),
  CONSTRAINT `ck_descuento_autorizado_1` CHECK (importe_cop>0),
  CONSTRAINT `ck_descuento_autorizado_2` CHECK (TRIM(motivo)<>''),
  CONSTRAINT `ck_descuento_autorizado_3` CHECK (`venta_id` IS NULL OR REGEXP_LIKE(`venta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_descuento_autorizado_4` CHECK (`autorizado_por_usuario_id` IS NULL OR REGEXP_LIKE(`autorizado_por_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `credito_venta` (
  `venta_id` CHAR(36) NOT NULL,
  `fecha_vencimiento` DATE NOT NULL,
  `condicion_pactada` VARCHAR(500) NOT NULL,
  PRIMARY KEY (`venta_id`),
  CONSTRAINT `ck_credito_venta_1` CHECK (TRIM(condicion_pactada)<>''),
  CONSTRAINT `ck_credito_venta_2` CHECK (`venta_id` IS NULL OR REGEXP_LIKE(`venta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `cobro` (
  `cobro_id` CHAR(36) NOT NULL,
  `venta_id` CHAR(36) NOT NULL,
  `turno_id` CHAR(36) NOT NULL,
  `cobrador_usuario_id` CHAR(36) NOT NULL,
  `medio` VARCHAR(32) NOT NULL,
  `importe_cop` DECIMAL(18,2) NOT NULL,
  `confirmado_en` DATETIME(6) NOT NULL,
  `referencia_externa` VARCHAR(160) NULL,
  `soporte_privado_ref` VARCHAR(500) NULL,
  `clave_operacion` VARCHAR(160) NOT NULL,
  `huella_solicitud` CHAR(64) NOT NULL,
  PRIMARY KEY (`cobro_id`),
  CONSTRAINT `uq_cobro_1` UNIQUE (`clave_operacion`),
  CONSTRAINT `uq_cobro_2` UNIQUE (`medio`,`referencia_externa`),
  CONSTRAINT `ck_cobro_1` CHECK (importe_cop>0),
  CONSTRAINT `ck_cobro_2` CHECK (TRIM(clave_operacion)<>''),
  CONSTRAINT `ck_cobro_3` CHECK (medio='EFECTIVO' OR (referencia_externa IS NOT NULL AND TRIM(referencia_externa)<>'')),
  CONSTRAINT `ck_cobro_4` CHECK (`cobro_id` IS NULL OR REGEXP_LIKE(`cobro_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_cobro_5` CHECK (`venta_id` IS NULL OR REGEXP_LIKE(`venta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_cobro_6` CHECK (`turno_id` IS NULL OR REGEXP_LIKE(`turno_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_cobro_7` CHECK (`cobrador_usuario_id` IS NULL OR REGEXP_LIKE(`cobrador_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_cobro_8` CHECK (`medio` IN ('EFECTIVO','NEQUI','DAVIPLATA','TARJETA')),
  CONSTRAINT `ck_cobro_9` CHECK (`huella_solicitud` IS NULL OR REGEXP_LIKE(`huella_solicitud`, '^[0-9a-f]{64}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `conciliacion_turno` (
  `turno_id` CHAR(36) NOT NULL,
  `medio` VARCHAR(32) NOT NULL,
  `importe_verificado_cop` DECIMAL(18,2) NOT NULL,
  `soporte_privado_ref` VARCHAR(500) NULL,
  PRIMARY KEY (`turno_id`,`medio`),
  CONSTRAINT `ck_conciliacion_turno_1` CHECK (importe_verificado_cop>=0),
  CONSTRAINT `ck_conciliacion_turno_2` CHECK (`turno_id` IS NULL OR REGEXP_LIKE(`turno_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_conciliacion_turno_3` CHECK (`medio` IN ('EFECTIVO','NEQUI','DAVIPLATA','TARJETA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `gasto` (
  `gasto_id` CHAR(36) NOT NULL,
  `fecha` DATE NOT NULL,
  `concepto` VARCHAR(160) NOT NULL,
  `importe_cop` DECIMAL(18,2) NOT NULL,
  `proveedor_id` CHAR(36) NULL,
  `soporte_ref` VARCHAR(500) NULL,
  `registrado_por_usuario_id` CHAR(36) NOT NULL,
  `clave_operacion` VARCHAR(160) NOT NULL,
  `huella_solicitud` CHAR(64) NOT NULL,
  `medio` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`gasto_id`),
  CONSTRAINT `uq_gasto_1` UNIQUE (`clave_operacion`),
  CONSTRAINT `ck_gasto_1` CHECK (importe_cop>0),
  CONSTRAINT `ck_gasto_2` CHECK (`gasto_id` IS NULL OR REGEXP_LIKE(`gasto_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_gasto_3` CHECK (`proveedor_id` IS NULL OR REGEXP_LIKE(`proveedor_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_gasto_4` CHECK (`registrado_por_usuario_id` IS NULL OR REGEXP_LIKE(`registrado_por_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_gasto_5` CHECK (`huella_solicitud` IS NULL OR REGEXP_LIKE(`huella_solicitud`, '^[0-9a-f]{64}$', 'c')),
  CONSTRAINT `ck_gasto_6` CHECK (`medio` IN ('EFECTIVO','NEQUI','DAVIPLATA','TARJETA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `movimiento_caja` (
  `movimiento_caja_id` CHAR(36) NOT NULL,
  `turno_id` CHAR(36) NOT NULL,
  `sentido` VARCHAR(32) NOT NULL,
  `importe_cop` DECIMAL(18,2) NOT NULL,
  `motivo` VARCHAR(500) NOT NULL,
  `gasto_id` CHAR(36) NULL,
  `registrado_por_usuario_id` CHAR(36) NOT NULL,
  `registrado_en` DATETIME(6) NOT NULL,
  `incidencia_id` CHAR(36) NULL,
  PRIMARY KEY (`movimiento_caja_id`),
  CONSTRAINT `ck_movimiento_caja_1` CHECK (importe_cop>0),
  CONSTRAINT `ck_movimiento_caja_2` CHECK (TRIM(motivo)<>''),
  CONSTRAINT `ck_movimiento_caja_3` CHECK (gasto_id IS NULL OR sentido='SALIDA'),
  CONSTRAINT `ck_movimiento_caja_4` CHECK (incidencia_id IS NULL OR sentido='SALIDA'),
  CONSTRAINT `ck_movimiento_caja_5` CHECK (gasto_id IS NULL OR incidencia_id IS NULL),
  CONSTRAINT `ck_movimiento_caja_6` CHECK (`movimiento_caja_id` IS NULL OR REGEXP_LIKE(`movimiento_caja_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_movimiento_caja_7` CHECK (`turno_id` IS NULL OR REGEXP_LIKE(`turno_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_movimiento_caja_8` CHECK (`sentido` IN ('ENTRADA','SALIDA')),
  CONSTRAINT `ck_movimiento_caja_9` CHECK (`gasto_id` IS NULL OR REGEXP_LIKE(`gasto_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_movimiento_caja_10` CHECK (`registrado_por_usuario_id` IS NULL OR REGEXP_LIKE(`registrado_por_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_movimiento_caja_11` CHECK (`incidencia_id` IS NULL OR REGEXP_LIKE(`incidencia_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `incidencia` (
  `incidencia_id` CHAR(36) NOT NULL,
  `tipo` VARCHAR(32) NOT NULL,
  `venta_id` CHAR(36) NULL,
  `linea_no` INT NULL,
  `descripcion` VARCHAR(500) NOT NULL,
  `registrada_en` DATETIME(6) NOT NULL,
  `registrada_por_usuario_id` CHAR(36) NOT NULL,
  `clave_operacion` VARCHAR(160) NOT NULL,
  `huella_solicitud` CHAR(64) NOT NULL,
  PRIMARY KEY (`incidencia_id`),
  CONSTRAINT `uq_incidencia_1` UNIQUE (`clave_operacion`),
  CONSTRAINT `ck_incidencia_1` CHECK ((venta_id IS NULL)=(linea_no IS NULL)),
  CONSTRAINT `ck_incidencia_2` CHECK (TRIM(descripcion)<>''),
  CONSTRAINT `ck_incidencia_3` CHECK (`incidencia_id` IS NULL OR REGEXP_LIKE(`incidencia_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_incidencia_4` CHECK (`tipo` IN ('MUESTRA','MERMA','ERROR_MEZCLA','CAMBIO_ENVASE','REPOSICION_LIQUIDO','REPOSICION','DEVOLUCION')),
  CONSTRAINT `ck_incidencia_5` CHECK (`venta_id` IS NULL OR REGEXP_LIKE(`venta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_incidencia_6` CHECK (`registrada_por_usuario_id` IS NULL OR REGEXP_LIKE(`registrada_por_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_incidencia_7` CHECK (`huella_solicitud` IS NULL OR REGEXP_LIKE(`huella_solicitud`, '^[0-9a-f]{64}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `conteo_inventario` (
  `conteo_id` CHAR(36) NOT NULL,
  `corte_en` DATETIME(6) NOT NULL,
  `realizado_por_usuario_id` CHAR(36) NOT NULL,
  `estado` VARCHAR(32) NOT NULL,
  `observacion` VARCHAR(500) NULL,
  PRIMARY KEY (`conteo_id`),
  CONSTRAINT `ck_conteo_inventario_1` CHECK (`conteo_id` IS NULL OR REGEXP_LIKE(`conteo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_conteo_inventario_2` CHECK (`realizado_por_usuario_id` IS NULL OR REGEXP_LIKE(`realizado_por_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_conteo_inventario_3` CHECK (`estado` IN ('ABIERTO','CERRADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `conteo_renglon` (
  `conteo_id` CHAR(36) NOT NULL,
  `articulo_id` CHAR(36) NOT NULL,
  `cantidad_medida` DECIMAL(18,6) NOT NULL,
  `unidad_medicion` VARCHAR(12) NOT NULL,
  `esperado_base_snapshot` DECIMAL(18,6) NOT NULL,
  `observacion` VARCHAR(500) NULL,
  PRIMARY KEY (`conteo_id`,`articulo_id`),
  CONSTRAINT `ck_conteo_renglon_1` CHECK (cantidad_medida>=0),
  CONSTRAINT `ck_conteo_renglon_2` CHECK (esperado_base_snapshot>=0),
  CONSTRAINT `ck_conteo_renglon_3` CHECK (`conteo_id` IS NULL OR REGEXP_LIKE(`conteo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_conteo_renglon_4` CHECK (`articulo_id` IS NULL OR REGEXP_LIKE(`articulo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `documento_inventario` (
  `documento_id` CHAR(36) NOT NULL,
  `tipo` VARCHAR(32) NOT NULL,
  `registrado_en` DATETIME(6) NOT NULL,
  `estado` VARCHAR(32) NOT NULL,
  `registrado_por_usuario_id` CHAR(36) NOT NULL,
  `venta_id` CHAR(36) NULL,
  `linea_no` INT NULL,
  `proveedor_id` CHAR(36) NULL,
  `referencia_factura` VARCHAR(160) NULL,
  `fecha_factura` DATE NULL,
  `total_documento_cop` DECIMAL(18,2) NULL,
  `incidencia_id` CHAR(36) NULL,
  `conteo_id` CHAR(36) NULL,
  `documento_origen_id` CHAR(36) NULL,
  `motivo` VARCHAR(500) NOT NULL,
  `clave_operacion` VARCHAR(160) NOT NULL,
  `huella_solicitud` CHAR(64) NOT NULL,
  PRIMARY KEY (`documento_id`),
  CONSTRAINT `uq_documento_inventario_1` UNIQUE (`venta_id`,`linea_no`),
  CONSTRAINT `uq_documento_inventario_2` UNIQUE (`conteo_id`),
  CONSTRAINT `uq_documento_inventario_3` UNIQUE (`clave_operacion`),
  CONSTRAINT `uq_documento_inventario_4` UNIQUE (`proveedor_id`,`referencia_factura`),
  CONSTRAINT `ck_documento_inventario_1` CHECK ((venta_id IS NULL)=(linea_no IS NULL)),
  CONSTRAINT `ck_documento_inventario_2` CHECK (total_documento_cop IS NULL OR total_documento_cop>=0),
  CONSTRAINT `ck_documento_inventario_3` CHECK (documento_origen_id IS NULL OR documento_origen_id<>documento_id),
  CONSTRAINT `ck_documento_inventario_4` CHECK (TRIM(motivo)<>''),
  CONSTRAINT `ck_documento_inventario_5` CHECK (TRIM(clave_operacion)<>''),
  CONSTRAINT `ck_documento_inventario_6` CHECK ((tipo='RECEPCION' AND proveedor_id IS NOT NULL AND referencia_factura IS NOT NULL AND TRIM(referencia_factura)<>'' AND venta_id IS NULL AND incidencia_id IS NULL AND conteo_id IS NULL AND documento_origen_id IS NULL) OR (tipo='SUMINISTRO' AND venta_id IS NOT NULL AND proveedor_id IS NULL AND referencia_factura IS NULL AND fecha_factura IS NULL AND total_documento_cop IS NULL AND incidencia_id IS NULL AND conteo_id IS NULL AND documento_origen_id IS NULL) OR (tipo='CONSUMO_NO_VENTA' AND incidencia_id IS NOT NULL AND venta_id IS NULL AND proveedor_id IS NULL AND referencia_factura IS NULL AND fecha_factura IS NULL AND total_documento_cop IS NULL AND conteo_id IS NULL) OR (tipo='TRANSFORMACION' AND venta_id IS NULL AND proveedor_id IS NULL AND referencia_factura IS NULL AND fecha_factura IS NULL AND total_documento_cop IS NULL AND conteo_id IS NULL) OR (tipo='AJUSTE_CONTEO' AND conteo_id IS NOT NULL AND venta_id IS NULL AND proveedor_id IS NULL AND referencia_factura IS NULL AND fecha_factura IS NULL AND total_documento_cop IS NULL AND incidencia_id IS NULL AND documento_origen_id IS NULL)),
  CONSTRAINT `ck_documento_inventario_7` CHECK (`documento_id` IS NULL OR REGEXP_LIKE(`documento_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_documento_inventario_8` CHECK (`tipo` IN ('RECEPCION','SUMINISTRO','CONSUMO_NO_VENTA','TRANSFORMACION','AJUSTE_CONTEO')),
  CONSTRAINT `ck_documento_inventario_9` CHECK (`estado` IN ('BORRADOR','CONFIRMADO')),
  CONSTRAINT `ck_documento_inventario_10` CHECK (`registrado_por_usuario_id` IS NULL OR REGEXP_LIKE(`registrado_por_usuario_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_documento_inventario_11` CHECK (`venta_id` IS NULL OR REGEXP_LIKE(`venta_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_documento_inventario_12` CHECK (`proveedor_id` IS NULL OR REGEXP_LIKE(`proveedor_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_documento_inventario_13` CHECK (`incidencia_id` IS NULL OR REGEXP_LIKE(`incidencia_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_documento_inventario_14` CHECK (`conteo_id` IS NULL OR REGEXP_LIKE(`conteo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_documento_inventario_15` CHECK (`documento_origen_id` IS NULL OR REGEXP_LIKE(`documento_origen_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_documento_inventario_16` CHECK (`huella_solicitud` IS NULL OR REGEXP_LIKE(`huella_solicitud`, '^[0-9a-f]{64}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `renglon_inventario` (
  `documento_id` CHAR(36) NOT NULL,
  `linea_no` INT NOT NULL,
  `articulo_id` CHAR(36) NOT NULL,
  `cantidad_ingresada_signed` DECIMAL(18,6) NOT NULL,
  `unidad_ingreso` VARCHAR(12) NOT NULL,
  `costo_unitario_base_cop` DECIMAL(20,8) NULL,
  PRIMARY KEY (`documento_id`,`linea_no`),
  CONSTRAINT `ck_renglon_inventario_1` CHECK (linea_no>0),
  CONSTRAINT `ck_renglon_inventario_2` CHECK (cantidad_ingresada_signed<>0),
  CONSTRAINT `ck_renglon_inventario_3` CHECK (costo_unitario_base_cop IS NULL OR costo_unitario_base_cop>=0),
  CONSTRAINT `ck_renglon_inventario_4` CHECK (`documento_id` IS NULL OR REGEXP_LIKE(`documento_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c')),
  CONSTRAINT `ck_renglon_inventario_5` CHECK (`articulo_id` IS NULL OR REGEXP_LIKE(`articulo_id`, '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', 'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

ALTER TABLE `usuario` ADD CONSTRAINT `fk_01_usuario` FOREIGN KEY (`integrante_id`) REFERENCES `integrante_equipo` (`integrante_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `articulo` ADD CONSTRAINT `fk_02_articulo` FOREIGN KEY (`unidad_base`) REFERENCES `unidad` (`unidad_codigo`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `formula_version` ADD CONSTRAINT `fk_03_formula_version` FOREIGN KEY (`fragancia_id`) REFERENCES `fragancia` (`fragancia_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `formula_version` ADD CONSTRAINT `fk_04_formula_version` FOREIGN KEY (`aprobada_por_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `formula_componente` ADD CONSTRAINT `fk_05_formula_componente` FOREIGN KEY (`formula_id`) REFERENCES `formula_version` (`formula_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `formula_componente` ADD CONSTRAINT `fk_06_formula_componente` FOREIGN KEY (`articulo_id`) REFERENCES `articulo` (`articulo_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `oferta` ADD CONSTRAINT `fk_07_oferta` FOREIGN KEY (`fragancia_id`) REFERENCES `fragancia` (`fragancia_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `especificacion_suministro` ADD CONSTRAINT `fk_08_especificacion_suministro` FOREIGN KEY (`oferta_id`) REFERENCES `oferta` (`oferta_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `especificacion_suministro` ADD CONSTRAINT `fk_09_especificacion_suministro` FOREIGN KEY (`formula_id`) REFERENCES `formula_version` (`formula_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `especificacion_componente` ADD CONSTRAINT `fk_10_especificacion_componente` FOREIGN KEY (`especificacion_id`) REFERENCES `especificacion_suministro` (`especificacion_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `especificacion_componente` ADD CONSTRAINT `fk_11_especificacion_componente` FOREIGN KEY (`articulo_id`) REFERENCES `articulo` (`articulo_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `precio_oferta` ADD CONSTRAINT `fk_12_precio_oferta` FOREIGN KEY (`oferta_id`) REFERENCES `oferta` (`oferta_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `turno_caja` ADD CONSTRAINT `fk_13_turno_caja` FOREIGN KEY (`caja_id`) REFERENCES `caja` (`caja_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `turno_caja` ADD CONSTRAINT `fk_14_turno_caja` FOREIGN KEY (`responsable_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `venta` ADD CONSTRAINT `fk_15_venta` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`cliente_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `venta` ADD CONSTRAINT `fk_16_venta` FOREIGN KEY (`vendedora_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `linea_venta` ADD CONSTRAINT `fk_17_linea_venta` FOREIGN KEY (`venta_id`) REFERENCES `venta` (`venta_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `linea_venta` ADD CONSTRAINT `fk_18_linea_venta` FOREIGN KEY (`especificacion_id`) REFERENCES `especificacion_suministro` (`especificacion_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `descuento_autorizado` ADD CONSTRAINT `fk_19_descuento_autorizado` FOREIGN KEY (`venta_id`) REFERENCES `venta` (`venta_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `descuento_autorizado` ADD CONSTRAINT `fk_20_descuento_autorizado` FOREIGN KEY (`autorizado_por_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `credito_venta` ADD CONSTRAINT `fk_21_credito_venta` FOREIGN KEY (`venta_id`) REFERENCES `venta` (`venta_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `cobro` ADD CONSTRAINT `fk_22_cobro` FOREIGN KEY (`venta_id`) REFERENCES `venta` (`venta_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `cobro` ADD CONSTRAINT `fk_23_cobro` FOREIGN KEY (`turno_id`) REFERENCES `turno_caja` (`turno_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `cobro` ADD CONSTRAINT `fk_24_cobro` FOREIGN KEY (`cobrador_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `conciliacion_turno` ADD CONSTRAINT `fk_25_conciliacion_turno` FOREIGN KEY (`turno_id`) REFERENCES `turno_caja` (`turno_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `gasto` ADD CONSTRAINT `fk_26_gasto` FOREIGN KEY (`proveedor_id`) REFERENCES `proveedor` (`proveedor_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `gasto` ADD CONSTRAINT `fk_27_gasto` FOREIGN KEY (`registrado_por_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `movimiento_caja` ADD CONSTRAINT `fk_28_movimiento_caja` FOREIGN KEY (`turno_id`) REFERENCES `turno_caja` (`turno_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `movimiento_caja` ADD CONSTRAINT `fk_29_movimiento_caja` FOREIGN KEY (`gasto_id`) REFERENCES `gasto` (`gasto_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `movimiento_caja` ADD CONSTRAINT `fk_30_movimiento_caja` FOREIGN KEY (`registrado_por_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `incidencia` ADD CONSTRAINT `fk_31_incidencia` FOREIGN KEY (`venta_id`,`linea_no`) REFERENCES `linea_venta` (`venta_id`,`linea_no`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `incidencia` ADD CONSTRAINT `fk_32_incidencia` FOREIGN KEY (`registrada_por_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `conteo_inventario` ADD CONSTRAINT `fk_33_conteo_inventario` FOREIGN KEY (`realizado_por_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `conteo_renglon` ADD CONSTRAINT `fk_34_conteo_renglon` FOREIGN KEY (`conteo_id`) REFERENCES `conteo_inventario` (`conteo_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `conteo_renglon` ADD CONSTRAINT `fk_35_conteo_renglon` FOREIGN KEY (`articulo_id`) REFERENCES `articulo` (`articulo_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `conteo_renglon` ADD CONSTRAINT `fk_36_conteo_renglon` FOREIGN KEY (`unidad_medicion`) REFERENCES `unidad` (`unidad_codigo`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `documento_inventario` ADD CONSTRAINT `fk_37_documento_inventario` FOREIGN KEY (`registrado_por_usuario_id`) REFERENCES `usuario` (`usuario_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `documento_inventario` ADD CONSTRAINT `fk_38_documento_inventario` FOREIGN KEY (`venta_id`,`linea_no`) REFERENCES `linea_venta` (`venta_id`,`linea_no`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `documento_inventario` ADD CONSTRAINT `fk_39_documento_inventario` FOREIGN KEY (`proveedor_id`) REFERENCES `proveedor` (`proveedor_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `documento_inventario` ADD CONSTRAINT `fk_40_documento_inventario` FOREIGN KEY (`incidencia_id`) REFERENCES `incidencia` (`incidencia_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `documento_inventario` ADD CONSTRAINT `fk_41_documento_inventario` FOREIGN KEY (`conteo_id`) REFERENCES `conteo_inventario` (`conteo_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `documento_inventario` ADD CONSTRAINT `fk_42_documento_inventario` FOREIGN KEY (`documento_origen_id`) REFERENCES `documento_inventario` (`documento_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `renglon_inventario` ADD CONSTRAINT `fk_43_renglon_inventario` FOREIGN KEY (`documento_id`) REFERENCES `documento_inventario` (`documento_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `renglon_inventario` ADD CONSTRAINT `fk_44_renglon_inventario` FOREIGN KEY (`articulo_id`) REFERENCES `articulo` (`articulo_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `renglon_inventario` ADD CONSTRAINT `fk_45_renglon_inventario` FOREIGN KEY (`unidad_ingreso`) REFERENCES `unidad` (`unidad_codigo`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `movimiento_caja` ADD CONSTRAINT `fk_46_movimiento_caja` FOREIGN KEY (`incidencia_id`) REFERENCES `incidencia` (`incidencia_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

INSERT INTO unidad(unidad_codigo,dimension,factor_base) VALUES ('ML','VOLUMEN',1),('L','VOLUMEN',1000),('UNIDAD','CONTEO',1),('G','MASA',1);

CREATE OR REPLACE VIEW v_movimiento_base AS
SELECT r.documento_id,r.linea_no,r.articulo_id,d.tipo,d.registrado_en,d.venta_id,d.linea_no AS venta_linea_no,
 r.cantidad_ingresada_signed * ui.factor_base / ub.factor_base AS cantidad_base,r.costo_unitario_base_cop
FROM renglon_inventario r JOIN documento_inventario d ON d.documento_id=r.documento_id
JOIN articulo a ON a.articulo_id=r.articulo_id JOIN unidad ui ON ui.unidad_codigo=r.unidad_ingreso
JOIN unidad ub ON ub.unidad_codigo=a.unidad_base
WHERE d.estado='CONFIRMADO' AND ui.dimension=ub.dimension;
CREATE OR REPLACE VIEW v_stock_base AS
SELECT a.articulo_id,COALESCE(SUM(m.cantidad_base),0) AS stock_base,a.unidad_base,a.minimo_base
FROM articulo a LEFT JOIN v_movimiento_base m ON m.articulo_id=a.articulo_id
GROUP BY a.articulo_id,a.unidad_base,a.minimo_base;
CREATE OR REPLACE VIEW v_total_venta AS
SELECT v.venta_id,COALESCE(l.bruto,0) AS bruto,COALESCE(d.importe_cop,0) AS descuento,
 COALESCE(l.bruto,0)-COALESCE(d.importe_cop,0) AS total,
 COALESCE(c.cobrado,0) AS cobrado,COALESCE(l.bruto,0)-COALESCE(d.importe_cop,0)-COALESCE(c.cobrado,0) AS saldo
FROM venta v LEFT JOIN (SELECT venta_id,SUM(cantidad_unidades*precio_unitario_cop) bruto FROM linea_venta GROUP BY venta_id)l ON l.venta_id=v.venta_id
LEFT JOIN descuento_autorizado d ON d.venta_id=v.venta_id
LEFT JOIN (SELECT venta_id,SUM(importe_cop)cobrado FROM cobro GROUP BY venta_id)c ON c.venta_id=v.venta_id;
CREATE OR REPLACE VIEW v_costo_venta AS
SELECT d.venta_id,SUM(CASE WHEN r.costo_unitario_base_cop IS NULL THEN 1 ELSE 0 END) AS costos_pendientes,
 SUM(-r.cantidad_base*r.costo_unitario_base_cop) AS costo_materiales
FROM documento_inventario d JOIN v_movimiento_base r ON r.documento_id=d.documento_id
WHERE d.tipo='SUMINISTRO' AND r.cantidad_base<0 GROUP BY d.venta_id;
CREATE OR REPLACE VIEW v_ventas_asesora AS
SELECT v.vendedora_usuario_id,DATE(v.emitida_en) AS fecha_utc,COUNT(*) AS ventas,SUM(t.total) AS vendido
FROM venta v JOIN v_total_venta t ON t.venta_id=v.venta_id WHERE v.estado='EMITIDA'
GROUP BY v.vendedora_usuario_id,DATE(v.emitida_en);
