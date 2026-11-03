-- ----------------------------------------------------------------------------
-- MySQL Workbench Migration
-- Migrated Schemata: sena_pos_ga7_auth
-- Source Schemata: sena_pos_ga7_auth
-- Created: Tue Nov  3 08:17:54 2026
-- Workbench Version: 8.0.47
-- ----------------------------------------------------------------------------

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_as_cs;
SET SESSION time_zone = '+00:00';
SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- ----------------------------------------------------------------------------
-- Schema sena_pos_ga7_auth
-- ----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `sena_pos_ga7_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs ;

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.tenant
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`tenant` (
  `tenant_id` CHAR(36) NOT NULL,
  `codigo` VARCHAR(64) NOT NULL,
  `nombre` VARCHAR(128) NOT NULL,
  `zona_horaria` VARCHAR(128) NOT NULL,
  `moneda` CHAR(3) NOT NULL,
  `activo` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`tenant_id`),
  UNIQUE INDEX `uq_tenant_01` (`codigo` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Identidad. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.usuario
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`usuario` (
  `tenant_id` CHAR(36) NOT NULL,
  `usuario_id` CHAR(36) NOT NULL,
  `correo` VARCHAR(254) NOT NULL,
  `hash_password` VARCHAR(255) NOT NULL,
  `intentos_fallidos` INT NOT NULL DEFAULT 0,
  `bloqueado_hasta` DATETIME(6) NULL DEFAULT NULL,
  `activo` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`tenant_id`, `usuario_id`),
  UNIQUE INDEX `uq_usuario_01` (`tenant_id` ASC, `correo` ASC) VISIBLE,
  CONSTRAINT `fk_usuario_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Identidad. La normalización de correo se fija como minúsculas y recorte para el prototipo académico; no autentica por sí sola.';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.rol
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`rol` (
  `tenant_id` CHAR(36) NOT NULL,
  `rol_id` CHAR(36) NOT NULL,
  `nombre` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`tenant_id`, `rol_id`),
  UNIQUE INDEX `uq_rol_01` (`tenant_id` ASC, `nombre` ASC) VISIBLE,
  CONSTRAINT `fk_rol_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Identidad. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.usuario_rol
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`usuario_rol` (
  `tenant_id` CHAR(36) NOT NULL,
  `usuario_id` CHAR(36) NOT NULL,
  `rol_id` CHAR(36) NOT NULL,
  PRIMARY KEY (`tenant_id`, `usuario_id`, `rol_id`),
  INDEX `fk_usuario_rol_02` (`tenant_id` ASC, `rol_id` ASC) VISIBLE,
  CONSTRAINT `fk_usuario_rol_01`
    FOREIGN KEY (`tenant_id` , `usuario_id`)
    REFERENCES `sena_pos_ga7_auth`.`usuario` (`tenant_id` , `usuario_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_usuario_rol_02`
    FOREIGN KEY (`tenant_id` , `rol_id`)
    REFERENCES `sena_pos_ga7_auth`.`rol` (`tenant_id` , `rol_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Identidad. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.producto
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`producto` (
  `tenant_id` CHAR(36) NOT NULL,
  `producto_id` CHAR(36) NOT NULL,
  `codigo` VARCHAR(64) NOT NULL,
  `nombre` VARCHAR(128) NOT NULL,
  `precio_vigente` DECIMAL(14,2) NOT NULL,
  `tasa_iva` DECIMAL(5,4) NOT NULL,
  `tipo` VARCHAR(32) NOT NULL,
  `unidad` VARCHAR(32) NOT NULL,
  `modo_descuento` VARCHAR(32) NOT NULL,
  `activo` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`tenant_id`, `producto_id`),
  UNIQUE INDEX `uq_producto_01` (`tenant_id` ASC, `codigo` ASC) VISIBLE,
  CONSTRAINT `fk_producto_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Catálogo e inventario. unidad se conserva aquí; detalle_receta la consulta en lugar de duplicarla.';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.bodega
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`bodega` (
  `tenant_id` CHAR(36) NOT NULL,
  `bodega_id` CHAR(36) NOT NULL,
  `nombre` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`tenant_id`, `bodega_id`),
  UNIQUE INDEX `uq_bodega_01` (`tenant_id` ASC, `nombre` ASC) VISIBLE,
  CONSTRAINT `fk_bodega_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Catálogo e inventario. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.existencia
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`existencia` (
  `tenant_id` CHAR(36) NOT NULL,
  `bodega_id` CHAR(36) NOT NULL,
  `producto_id` CHAR(36) NOT NULL,
  `stock` DECIMAL(14,3) NOT NULL DEFAULT 0,
  PRIMARY KEY (`tenant_id`, `bodega_id`, `producto_id`),
  INDEX `fk_existencia_02` (`tenant_id` ASC, `producto_id` ASC) VISIBLE,
  CONSTRAINT `fk_existencia_01`
    FOREIGN KEY (`tenant_id` , `bodega_id`)
    REFERENCES `sena_pos_ga7_auth`.`bodega` (`tenant_id` , `bodega_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_existencia_02`
    FOREIGN KEY (`tenant_id` , `producto_id`)
    REFERENCES `sena_pos_ga7_auth`.`producto` (`tenant_id` , `producto_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Catálogo e inventario. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.mesa
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`mesa` (
  `tenant_id` CHAR(36) NOT NULL,
  `mesa_id` CHAR(36) NOT NULL,
  `codigo` VARCHAR(64) NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'DISPONIBLE',
  PRIMARY KEY (`tenant_id`, `mesa_id`),
  UNIQUE INDEX `uq_mesa_01` (`tenant_id` ASC, `codigo` ASC) VISIBLE,
  CONSTRAINT `fk_mesa_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Gastronomía y caja. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.caja
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`caja` (
  `tenant_id` CHAR(36) NOT NULL,
  `caja_id` CHAR(36) NOT NULL,
  `nombre` VARCHAR(128) NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'ACTIVA',
  PRIMARY KEY (`tenant_id`, `caja_id`),
  UNIQUE INDEX `uq_caja_01` (`tenant_id` ASC, `nombre` ASC) VISIBLE,
  CONSTRAINT `fk_caja_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Gastronomía y caja. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.sesion_caja
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`sesion_caja` (
  `tenant_id` CHAR(36) NOT NULL,
  `sesion_id` CHAR(36) NOT NULL,
  `caja_id` CHAR(36) NOT NULL,
  `usuario_id` CHAR(36) NOT NULL,
  `abierta_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `cerrada_en` DATETIME(6) NULL DEFAULT NULL,
  `fondo_inicial` DECIMAL(14,2) NOT NULL,
  `total_esperado` DECIMAL(14,2) NULL DEFAULT NULL,
  `total_contado` DECIMAL(14,2) NULL DEFAULT NULL,
  `caja_abierta_id` CHAR(36) GENERATED ALWAYS AS (CASE WHEN cerrada_en IS NULL THEN caja_id ELSE NULL END) VIRTUAL,
  PRIMARY KEY (`tenant_id`, `sesion_id`),
  UNIQUE INDEX `uq_sesion_abierta` (`tenant_id` ASC, `caja_abierta_id` ASC) VISIBLE,
  INDEX `fk_sesion_caja_01` (`tenant_id` ASC, `caja_id` ASC) VISIBLE,
  INDEX `fk_sesion_caja_02` (`tenant_id` ASC, `usuario_id` ASC) VISIBLE,
  CONSTRAINT `fk_sesion_caja_01`
    FOREIGN KEY (`tenant_id` , `caja_id`)
    REFERENCES `sena_pos_ga7_auth`.`caja` (`tenant_id` , `caja_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_sesion_caja_02`
    FOREIGN KEY (`tenant_id` , `usuario_id`)
    REFERENCES `sena_pos_ga7_auth`.`usuario` (`tenant_id` , `usuario_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Gastronomía y caja. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.receta
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`receta` (
  `tenant_id` CHAR(36) NOT NULL,
  `receta_id` CHAR(36) NOT NULL,
  `producto_plato_id` CHAR(36) NOT NULL,
  `version` INT NOT NULL,
  `rendimiento` DECIMAL(14,3) NOT NULL,
  `activa` TINYINT NOT NULL DEFAULT 0,
  `plato_activo_id` CHAR(36) GENERATED ALWAYS AS (CASE WHEN activa=1 THEN producto_plato_id ELSE NULL END) VIRTUAL,
  PRIMARY KEY (`tenant_id`, `receta_id`),
  UNIQUE INDEX `uq_receta_01` (`tenant_id` ASC, `producto_plato_id` ASC, `version` ASC) VISIBLE,
  UNIQUE INDEX `uq_receta_activa` (`tenant_id` ASC, `plato_activo_id` ASC) VISIBLE,
  CONSTRAINT `fk_receta_01`
    FOREIGN KEY (`tenant_id` , `producto_plato_id`)
    REFERENCES `sena_pos_ga7_auth`.`producto` (`tenant_id` , `producto_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Gastronomía y caja. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.detalle_receta
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`detalle_receta` (
  `tenant_id` CHAR(36) NOT NULL,
  `receta_id` CHAR(36) NOT NULL,
  `linea_no` INT NOT NULL,
  `producto_insumo_id` CHAR(36) NOT NULL,
  `cantidad` DECIMAL(14,3) NOT NULL,
  PRIMARY KEY (`tenant_id`, `receta_id`, `linea_no`),
  INDEX `fk_detalle_receta_02` (`tenant_id` ASC, `producto_insumo_id` ASC) VISIBLE,
  CONSTRAINT `fk_detalle_receta_01`
    FOREIGN KEY (`tenant_id` , `receta_id`)
    REFERENCES `sena_pos_ga7_auth`.`receta` (`tenant_id` , `receta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_detalle_receta_02`
    FOREIGN KEY (`tenant_id` , `producto_insumo_id`)
    REFERENCES `sena_pos_ga7_auth`.`producto` (`tenant_id` , `producto_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Gastronomía y caja. La unidad del insumo se obtiene con JOIN a producto. Una receta habilitada requiere al menos un detalle, control transaccional adicional.';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.venta
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`venta` (
  `tenant_id` CHAR(36) NOT NULL,
  `venta_id` CHAR(36) NOT NULL,
  `usuario_id` CHAR(36) NOT NULL,
  `bodega_id` CHAR(36) NOT NULL,
  `mesa_id` CHAR(36) NULL DEFAULT NULL,
  `sesion_caja_id` CHAR(36) NULL DEFAULT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'BORRADOR',
  `numero_factura` VARCHAR(64) NULL DEFAULT NULL,
  `creada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `pagada_en` DATETIME(6) NULL DEFAULT NULL,
  PRIMARY KEY (`tenant_id`, `venta_id`),
  UNIQUE INDEX `uq_venta_01` (`tenant_id` ASC, `numero_factura` ASC) VISIBLE,
  INDEX `fk_venta_01` (`tenant_id` ASC, `usuario_id` ASC) VISIBLE,
  INDEX `fk_venta_02` (`tenant_id` ASC, `bodega_id` ASC) VISIBLE,
  INDEX `fk_venta_03` (`tenant_id` ASC, `mesa_id` ASC) VISIBLE,
  INDEX `fk_venta_04` (`tenant_id` ASC, `sesion_caja_id` ASC) VISIBLE,
  CONSTRAINT `fk_venta_01`
    FOREIGN KEY (`tenant_id` , `usuario_id`)
    REFERENCES `sena_pos_ga7_auth`.`usuario` (`tenant_id` , `usuario_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_venta_02`
    FOREIGN KEY (`tenant_id` , `bodega_id`)
    REFERENCES `sena_pos_ga7_auth`.`bodega` (`tenant_id` , `bodega_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_venta_03`
    FOREIGN KEY (`tenant_id` , `mesa_id`)
    REFERENCES `sena_pos_ga7_auth`.`mesa` (`tenant_id` , `mesa_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_venta_04`
    FOREIGN KEY (`tenant_id` , `sesion_caja_id`)
    REFERENCES `sena_pos_ga7_auth`.`sesion_caja` (`tenant_id` , `sesion_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Ventas. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.detalle_venta
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`detalle_venta` (
  `tenant_id` CHAR(36) NOT NULL,
  `venta_id` CHAR(36) NOT NULL,
  `linea_no` INT NOT NULL,
  `producto_id` CHAR(36) NOT NULL,
  `receta_id` CHAR(36) NULL DEFAULT NULL,
  `cantidad` DECIMAL(14,3) NOT NULL,
  `precio_unitario` DECIMAL(14,2) NOT NULL,
  `tasa_iva` DECIMAL(5,4) NOT NULL,
  PRIMARY KEY (`tenant_id`, `venta_id`, `linea_no`),
  INDEX `fk_detalle_venta_02` (`tenant_id` ASC, `producto_id` ASC) VISIBLE,
  INDEX `fk_detalle_venta_03` (`tenant_id` ASC, `receta_id` ASC) VISIBLE,
  CONSTRAINT `fk_detalle_venta_01`
    FOREIGN KEY (`tenant_id` , `venta_id`)
    REFERENCES `sena_pos_ga7_auth`.`venta` (`tenant_id` , `venta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_detalle_venta_02`
    FOREIGN KEY (`tenant_id` , `producto_id`)
    REFERENCES `sena_pos_ga7_auth`.`producto` (`tenant_id` , `producto_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_detalle_venta_03`
    FOREIGN KEY (`tenant_id` , `receta_id`)
    REFERENCES `sena_pos_ga7_auth`.`receta` (`tenant_id` , `receta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Ventas. Precio y tasa son hechos históricos de la línea, no copias de su valor vigente.';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.pago
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`pago` (
  `tenant_id` CHAR(36) NOT NULL,
  `pago_id` CHAR(36) NOT NULL,
  `venta_id` CHAR(36) NOT NULL,
  `referencia` VARCHAR(64) NOT NULL,
  `importe` DECIMAL(14,2) NOT NULL,
  `estado` VARCHAR(32) NOT NULL,
  `confirmado_en` DATETIME(6) NULL DEFAULT NULL,
  `venta_confirmada_id` CHAR(36) GENERATED ALWAYS AS (CASE WHEN estado='CONFIRMADO' THEN venta_id ELSE NULL END) VIRTUAL,
  PRIMARY KEY (`tenant_id`, `pago_id`),
  UNIQUE INDEX `uq_pago_01` (`tenant_id` ASC, `referencia` ASC) VISIBLE,
  UNIQUE INDEX `uq_pago_confirmado` (`tenant_id` ASC, `venta_confirmada_id` ASC) VISIBLE,
  INDEX `fk_pago_01` (`tenant_id` ASC, `venta_id` ASC) VISIBLE,
  CONSTRAINT `fk_pago_01`
    FOREIGN KEY (`tenant_id` , `venta_id`)
    REFERENCES `sena_pos_ga7_auth`.`venta` (`tenant_id` , `venta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Ventas. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.movimiento_inventario
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`movimiento_inventario` (
  `tenant_id` CHAR(36) NOT NULL,
  `movimiento_id` CHAR(36) NOT NULL,
  `bodega_id` CHAR(36) NOT NULL,
  `producto_id` CHAR(36) NOT NULL,
  `venta_id` CHAR(36) NULL DEFAULT NULL,
  `linea_no` INT NULL DEFAULT NULL,
  `receta_id` CHAR(36) NULL DEFAULT NULL,
  `componente_no` INT NOT NULL DEFAULT 0,
  `cantidad_delta` DECIMAL(14,3) NOT NULL,
  `tipo_origen` VARCHAR(32) NOT NULL,
  `documento_origen` VARCHAR(128) NOT NULL,
  `registrado_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`tenant_id`, `movimiento_id`),
  UNIQUE INDEX `uq_movimiento_inventario_01` (`tenant_id` ASC, `tipo_origen` ASC, `documento_origen` ASC, `producto_id` ASC, `bodega_id` ASC, `componente_no` ASC) VISIBLE,
  UNIQUE INDEX `uq_movimiento_venta_componente` (`tenant_id` ASC, `venta_id` ASC, `linea_no` ASC, `componente_no` ASC) VISIBLE,
  INDEX `fk_movimiento_inventario_01` (`tenant_id` ASC, `bodega_id` ASC) VISIBLE,
  INDEX `fk_movimiento_inventario_02` (`tenant_id` ASC, `producto_id` ASC) VISIBLE,
  INDEX `fk_movimiento_inventario_04` (`tenant_id` ASC, `receta_id` ASC) VISIBLE,
  CONSTRAINT `fk_movimiento_inventario_01`
    FOREIGN KEY (`tenant_id` , `bodega_id`)
    REFERENCES `sena_pos_ga7_auth`.`bodega` (`tenant_id` , `bodega_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_movimiento_inventario_02`
    FOREIGN KEY (`tenant_id` , `producto_id`)
    REFERENCES `sena_pos_ga7_auth`.`producto` (`tenant_id` , `producto_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_movimiento_inventario_03`
    FOREIGN KEY (`tenant_id` , `venta_id` , `linea_no`)
    REFERENCES `sena_pos_ga7_auth`.`detalle_venta` (`tenant_id` , `venta_id` , `linea_no`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_movimiento_inventario_04`
    FOREIGN KEY (`tenant_id` , `receta_id`)
    REFERENCES `sena_pos_ga7_auth`.`receta` (`tenant_id` , `receta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Catálogo e inventario. FK compuesta de origen a detalle; la coherencia física de receta/directo y el saldo requieren servicio transaccional.';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.operacion_confirmacion
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`operacion_confirmacion` (
  `tenant_id` CHAR(36) NOT NULL,
  `clave_idempotencia` VARCHAR(128) NOT NULL,
  `venta_id` CHAR(36) NOT NULL,
  `huella_solicitud` CHAR(64) NOT NULL,
  `registrada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`tenant_id`, `clave_idempotencia`),
  INDEX `fk_operacion_confirmacion_01` (`tenant_id` ASC, `venta_id` ASC) VISIBLE,
  CONSTRAINT `fk_operacion_confirmacion_01`
    FOREIGN KEY (`tenant_id` , `venta_id`)
    REFERENCES `sena_pos_ga7_auth`.`venta` (`tenant_id` , `venta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Ventas. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.empleado
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`empleado` (
  `tenant_id` CHAR(36) NOT NULL,
  `empleado_id` CHAR(36) NOT NULL,
  `documento` VARCHAR(128) NOT NULL,
  `nombre` VARCHAR(128) NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'ACTIVO',
  PRIMARY KEY (`tenant_id`, `empleado_id`),
  UNIQUE INDEX `uq_empleado_01` (`tenant_id` ASC, `documento` ASC) VISIBLE,
  CONSTRAINT `fk_empleado_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Nómina. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.novedad_nomina
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`novedad_nomina` (
  `tenant_id` CHAR(36) NOT NULL,
  `novedad_id` CHAR(36) NOT NULL,
  `empleado_id` CHAR(36) NOT NULL,
  `periodo` CHAR(7) NOT NULL,
  `concepto` VARCHAR(128) NOT NULL,
  `tipo` VARCHAR(32) NOT NULL,
  `importe` DECIMAL(14,2) NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'BORRADOR',
  PRIMARY KEY (`tenant_id`, `novedad_id`),
  UNIQUE INDEX `uq_novedad_nomina_01` (`tenant_id` ASC, `novedad_id` ASC, `empleado_id` ASC) VISIBLE,
  INDEX `fk_novedad_nomina_01` (`tenant_id` ASC, `empleado_id` ASC) VISIBLE,
  CONSTRAINT `fk_novedad_nomina_01`
    FOREIGN KEY (`tenant_id` , `empleado_id`)
    REFERENCES `sena_pos_ga7_auth`.`empleado` (`tenant_id` , `empleado_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Nómina. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.liquidacion_nomina
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`liquidacion_nomina` (
  `tenant_id` CHAR(36) NOT NULL,
  `liquidacion_id` CHAR(36) NOT NULL,
  `periodo` CHAR(7) NOT NULL,
  `version` INT NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'BORRADOR',
  `creada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`tenant_id`, `liquidacion_id`),
  UNIQUE INDEX `uq_liquidacion_nomina_01` (`tenant_id` ASC, `periodo` ASC, `version` ASC) VISIBLE,
  CONSTRAINT `fk_liquidacion_nomina_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Nómina. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.detalle_nomina
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`detalle_nomina` (
  `tenant_id` CHAR(36) NOT NULL,
  `liquidacion_id` CHAR(36) NOT NULL,
  `empleado_id` CHAR(36) NOT NULL,
  `devengos` DECIMAL(14,2) NOT NULL,
  `deducciones` DECIMAL(14,2) NOT NULL,
  PRIMARY KEY (`tenant_id`, `liquidacion_id`, `empleado_id`),
  INDEX `fk_detalle_nomina_02` (`tenant_id` ASC, `empleado_id` ASC) VISIBLE,
  CONSTRAINT `fk_detalle_nomina_01`
    FOREIGN KEY (`tenant_id` , `liquidacion_id`)
    REFERENCES `sena_pos_ga7_auth`.`liquidacion_nomina` (`tenant_id` , `liquidacion_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_detalle_nomina_02`
    FOREIGN KEY (`tenant_id` , `empleado_id`)
    REFERENCES `sena_pos_ga7_auth`.`empleado` (`tenant_id` , `empleado_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Nómina. neto se calcula en vista; no es una columna almacenada en el modelo normalizado.';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.detalle_nomina_novedad
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`detalle_nomina_novedad` (
  `tenant_id` CHAR(36) NOT NULL,
  `novedad_id` CHAR(36) NOT NULL,
  `liquidacion_id` CHAR(36) NOT NULL,
  `empleado_id` CHAR(36) NOT NULL,
  PRIMARY KEY (`tenant_id`, `novedad_id`),
  INDEX `fk_detalle_nomina_novedad_01` (`tenant_id` ASC, `liquidacion_id` ASC, `empleado_id` ASC) VISIBLE,
  INDEX `fk_detalle_nomina_novedad_02` (`tenant_id` ASC, `novedad_id` ASC, `empleado_id` ASC) VISIBLE,
  CONSTRAINT `fk_detalle_nomina_novedad_01`
    FOREIGN KEY (`tenant_id` , `liquidacion_id` , `empleado_id`)
    REFERENCES `sena_pos_ga7_auth`.`detalle_nomina` (`tenant_id` , `liquidacion_id` , `empleado_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_detalle_nomina_novedad_02`
    FOREIGN KEY (`tenant_id` , `novedad_id` , `empleado_id`)
    REFERENCES `sena_pos_ga7_auth`.`novedad_nomina` (`tenant_id` , `novedad_id` , `empleado_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Nómina. Una novedad se aplica una sola vez; la PK mínima sustituye la superclave de cuatro columnas del diseño23. El puente se inserta durante aprobación, no para borradores de simulación.';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.cuenta
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`cuenta` (
  `tenant_id` CHAR(36) NOT NULL,
  `cuenta_id` CHAR(36) NOT NULL,
  `codigo` VARCHAR(64) NOT NULL,
  `nombre` VARCHAR(128) NOT NULL,
  `naturaleza` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`tenant_id`, `cuenta_id`),
  UNIQUE INDEX `uq_cuenta_01` (`tenant_id` ASC, `codigo` ASC) VISIBLE,
  CONSTRAINT `fk_cuenta_01`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `sena_pos_ga7_auth`.`tenant` (`tenant_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Contabilidad. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.asiento_contable
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`asiento_contable` (
  `tenant_id` CHAR(36) NOT NULL,
  `asiento_id` CHAR(36) NOT NULL,
  `fecha` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `tipo_origen` VARCHAR(32) NOT NULL,
  `venta_id` CHAR(36) NULL DEFAULT NULL,
  `liquidacion_id` CHAR(36) NULL DEFAULT NULL,
  `documento_origen` VARCHAR(128) NOT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'BORRADOR',
  PRIMARY KEY (`tenant_id`, `asiento_id`),
  UNIQUE INDEX `uq_asiento_contable_01` (`tenant_id` ASC, `venta_id` ASC) VISIBLE,
  UNIQUE INDEX `uq_asiento_contable_02` (`tenant_id` ASC, `liquidacion_id` ASC) VISIBLE,
  CONSTRAINT `fk_asiento_contable_01`
    FOREIGN KEY (`tenant_id` , `venta_id`)
    REFERENCES `sena_pos_ga7_auth`.`venta` (`tenant_id` , `venta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_asiento_contable_02`
    FOREIGN KEY (`tenant_id` , `liquidacion_id`)
    REFERENCES `sena_pos_ga7_auth`.`liquidacion_nomina` (`tenant_id` , `liquidacion_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Contabilidad. Sustituye origen_id polimórfico sin FK por dos referencias tipadas y exclusivas. Reversiones se rastrean como documento compensatorio; no permiten reciclar el origen.';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.linea_asiento
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`linea_asiento` (
  `tenant_id` CHAR(36) NOT NULL,
  `asiento_id` CHAR(36) NOT NULL,
  `linea_no` INT NOT NULL,
  `cuenta_id` CHAR(36) NOT NULL,
  `debito` DECIMAL(14,2) NOT NULL DEFAULT 0,
  `credito` DECIMAL(14,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (`tenant_id`, `asiento_id`, `linea_no`),
  INDEX `fk_linea_asiento_02` (`tenant_id` ASC, `cuenta_id` ASC) VISIBLE,
  CONSTRAINT `fk_linea_asiento_01`
    FOREIGN KEY (`tenant_id` , `asiento_id`)
    REFERENCES `sena_pos_ga7_auth`.`asiento_contable` (`tenant_id` , `asiento_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_linea_asiento_02`
    FOREIGN KEY (`tenant_id` , `cuenta_id`)
    REFERENCES `sena_pos_ga7_auth`.`cuenta` (`tenant_id` , `cuenta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Contabilidad. ';

-- ----------------------------------------------------------------------------
-- Table sena_pos_ga7_auth.evento_integracion
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sena_pos_ga7_auth`.`evento_integracion` (
  `tenant_id` CHAR(36) NOT NULL,
  `evento_id` CHAR(36) NOT NULL,
  `tipo` VARCHAR(32) NOT NULL,
  `venta_id` CHAR(36) NULL DEFAULT NULL,
  `liquidacion_id` CHAR(36) NULL DEFAULT NULL,
  `estado` VARCHAR(32) NOT NULL DEFAULT 'PENDIENTE',
  `creada_en` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `procesada_en` DATETIME(6) NULL DEFAULT NULL,
  `ultimo_error` TEXT NULL DEFAULT NULL,
  PRIMARY KEY (`tenant_id`, `evento_id`),
  UNIQUE INDEX `uq_evento_integracion_01` (`tenant_id` ASC, `tipo` ASC, `venta_id` ASC) VISIBLE,
  UNIQUE INDEX `uq_evento_integracion_02` (`tenant_id` ASC, `tipo` ASC, `liquidacion_id` ASC) VISIBLE,
  INDEX `fk_evento_integracion_01` (`tenant_id` ASC, `venta_id` ASC) VISIBLE,
  INDEX `fk_evento_integracion_02` (`tenant_id` ASC, `liquidacion_id` ASC) VISIBLE,
  CONSTRAINT `fk_evento_integracion_01`
    FOREIGN KEY (`tenant_id` , `venta_id`)
    REFERENCES `sena_pos_ga7_auth`.`venta` (`tenant_id` , `venta_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_evento_integracion_02`
    FOREIGN KEY (`tenant_id` , `liquidacion_id`)
    REFERENCES `sena_pos_ga7_auth`.`liquidacion_nomina` (`tenant_id` , `liquidacion_id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_as_cs
COMMENT = 'Contabilidad. ';

-- ----------------------------------------------------------------------------
-- View sena_pos_ga7_auth.v_detalle_nomina
-- ----------------------------------------------------------------------------
USE `sena_pos_ga7_auth`;
CREATE  OR REPLACE SQL SECURITY INVOKER VIEW sena_pos_ga7_auth.v_detalle_nomina AS SELECT tenant_id, liquidacion_id, empleado_id, devengos, deducciones, devengos-deducciones AS neto FROM sena_pos_ga7_auth.detalle_nomina;

-- ----------------------------------------------------------------------------
-- View sena_pos_ga7_auth.v_detalle_receta
-- ----------------------------------------------------------------------------
USE `sena_pos_ga7_auth`;
CREATE  OR REPLACE SQL SECURITY INVOKER VIEW sena_pos_ga7_auth.v_detalle_receta AS SELECT d.tenant_id,d.receta_id,d.linea_no,d.producto_insumo_id,d.cantidad,p.unidad FROM sena_pos_ga7_auth.detalle_receta d JOIN sena_pos_ga7_auth.producto p ON p.tenant_id=d.tenant_id AND p.producto_id=d.producto_insumo_id;

-- ----------------------------------------------------------------------------
-- View sena_pos_ga7_auth.v_total_venta
-- ----------------------------------------------------------------------------
USE `sena_pos_ga7_auth`;
CREATE  OR REPLACE SQL SECURITY INVOKER VIEW sena_pos_ga7_auth.v_total_venta AS SELECT tenant_id,venta_id,SUM(ROUND(cantidad*precio_unitario,2)) AS base,SUM(ROUND(ROUND(cantidad*precio_unitario,2)*tasa_iva,2)) AS iva,SUM(ROUND(cantidad*precio_unitario,2)+ROUND(ROUND(cantidad*precio_unitario,2)*tasa_iva,2)) AS total FROM sena_pos_ga7_auth.detalle_venta GROUP BY tenant_id,venta_id;
SET FOREIGN_KEY_CHECKS = 1;
