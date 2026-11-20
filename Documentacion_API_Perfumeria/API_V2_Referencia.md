# Referencia de servicios REST V2

GA7-220501096-AA5-EV03 · Esencias & Estilo Solano  
Jhon Steven Alvarez Ruiz · Ficha 3388966 · 17 de noviembre de 2026

Esta referencia describe las 48 combinaciones de método y ruta de PS-API-V2-r3. Las fichas detallan los permisos, entradas, respuestas, errores y reintentos de la versión. CONTRATO_API_V2.txt ofrece el resumen del protocolo. Los archivos Java y SQL del paquete definen el comportamiento persistente.

## Protocolo y acceso

Base local: `http://127.0.0.1:8095/api/v2`. El cliente obtiene primero GET /auth/sesion y conserva cookie PSSESSION y token csrf. Las escrituras envían X-CSRF-Token, incluido login y logout. Un login correcto cambia el identificador de sesión y el token. Las cuentas y sus integrantes deben permanecer activos. El perfil y la propiedad del documento se comprueban en el servidor.

Los cuerpos son JSON y usan snake_case. Importes COP: hasta dos decimales; cantidades: hasta seis; costo unitario base: hasta ocho. Se recomienda enviar decimales como cadenas. El servidor utiliza BigDecimal y valida tipos, escala y rango. El formulario del navegador no sustituye esa validación. Una dimensión de masa no se transforma en volumen sin un dato aplicable.

Las respuestas controladas de error contienen codigo y mensaje. Sus condiciones se detallan por ruta; un error del servidor HTTP ajeno a estas validaciones puede tener otra representación. Un error de disponibilidad no confirma la operación. Ante un resultado incierto de una alta sin clave de operación, consulte los registros antes de volver a crearla.

| HTTP | Significado |
| --- | --- |
| 400 | JSON o datos inválidos |
| 401 | Sesión ausente o credenciales inválidas |
| 403 | Perfil, propiedad o CSRF no autorizado |
| 404 | Recurso solicitado no encontrado |
| 409 | Integridad, estado, existencias, valoración o contenido del reintento incompatible |
| 429 | Bloqueo temporal del acceso después de tres credenciales erróneas |
| 503 | Datos no disponibles |

### Errores comunes del protocolo

Estos errores se aplican cuando la solicitud atraviesa la validación indicada. Las fichas añaden condiciones de negocio propias de su operación. Una lista sin coincidencias devuelve un arreglo vacío; la ausencia de filas en una lista no es un error 404.

| HTTP | Código | Condición |
| --- | --- | --- |
| 400 | JSON_INVALIDO | Cuerpo JSON que el servidor no puede interpretar |
| 400 | DATOS_INVALIDOS | Un campo evaluado incumple tipo, formato, escala, longitud o rango |
| 401 | SESION_REQUERIDA | Ruta protegida sin usuario autenticado |
| 401 | SESION_INVALIDA | Cuenta o integrante ya no disponible al recuperar la identidad |
| 403 | CSRF_INVALIDO | Escritura sin cookie y token coincidentes |
| 403 | PERMISO_DENEGADO | Operación que exige un perfil o propiedad no autorizados |
| 404 | NO_ENCONTRADO | Búsqueda requerida de un recurso concreto sin resultado |
| 409 | REGLA_NEGOCIO | Una regla de estado, cantidad, origen o contenido rechaza la operación |
| 409 | CONFLICTO_DATOS | Restricción relacional o única incumplida |
| 503 | DATOS_NO_DISPONIBLES | Fallo de acceso a datos que no corresponde a integridad |

Los campos evaluados con Datos.texto usan V.texto: strip, máximo 240 unidades UTF-16 y ausencia de controles; las columnas del modelo pueden imponer un límite menor. V.decimal rechaza precisión superior a 14 dígitos después de ajustar a la escala de la operación (2, 6 u 8 decimales). Así, un valor puede tener una escala válida y aun superar el rango admitido.

### Reintentos con clave de operación

Recepción, apertura, emisión, cobro, incidencia y gasto admiten clave_operacion. La huella codifica tipos y longitudes, ordena las claves de mapas, conserva el orden de las listas y normaliza los números. Un número JSON y una cadena decimal son tipos diferentes: el reintento conserva el mismo contenido y sus tipos. El mismo identificador con otra operación, venta o cuerpo se rechaza según el ámbito de la ficha. Una referencia electrónica repetida del mismo medio y una factura completa repetida del mismo proveedor también se rechazan con claves nuevas.

### Fechas y respuestas

Los instantes se presentan como ISO UTC con Z. El período comercial de reportes utiliza America/Bogota. Una fecha de calendario como vencimiento no se cambia de día por la zona del navegador. PresentacionJson añade alias estables como fecha_creacion, fecha_emision, fecha_cobro, responsable_nombre, fondo_cop, importe_esperado_cop, documento_ref y costo_unitario, conservando los campos relacionales.

GET /inicio devuelve conteos y alertas del conjunto visible: ventas cuenta hasta las últimas 500 ventas de todos los estados. El resumen diario del cliente filtra ventas emitidas por fecha comercial; los informes por período se consultan en /reportes.

## Índice de rutas

| N.º | Método | Ruta | Perfil |
| --- | --- | --- | --- |
| 1 | GET | /api/v2/auth/sesion | Público; crea/recupera sesión anónima |
| 2 | POST | /api/v2/auth/login | Público con cookie y CSRF de sesión anónima |
| 3 | POST | /api/v2/auth/logout | ADMINISTRADOR y VENDEDORA |
| 4 | GET | /api/v2/inicio | ADMINISTRADOR y VENDEDORA |
| 5 | GET | /api/v2/configuracion | ADMINISTRADOR y VENDEDORA |
| 6 | GET | /api/v2/usuarios | ADMINISTRADOR |
| 7 | POST | /api/v2/usuarios | ADMINISTRADOR |
| 8 | GET | /api/v2/clientes | ADMINISTRADOR y VENDEDORA |
| 9 | GET | /api/v2/proveedores | ADMINISTRADOR |
| 10 | GET | /api/v2/fragancias | ADMINISTRADOR y VENDEDORA |
| 11 | POST | /api/v2/clientes | ADMINISTRADOR y VENDEDORA |
| 12 | POST | /api/v2/proveedores | ADMINISTRADOR |
| 13 | POST | /api/v2/fragancias | ADMINISTRADOR |
| 14 | GET | /api/v2/articulos | ADMINISTRADOR y VENDEDORA |
| 15 | POST | /api/v2/articulos | ADMINISTRADOR |
| 16 | PUT | /api/v2/articulos/{id}/minimo | ADMINISTRADOR |
| 17 | GET | /api/v2/movimientos | ADMINISTRADOR |
| 18 | GET | /api/v2/formulas | ADMINISTRADOR y VENDEDORA |
| 19 | POST | /api/v2/formulas | ADMINISTRADOR |
| 20 | GET | /api/v2/ofertas | ADMINISTRADOR y VENDEDORA |
| 21 | POST | /api/v2/ofertas | ADMINISTRADOR |
| 22 | PUT | /api/v2/ofertas/{id}/precio | ADMINISTRADOR |
| 23 | PUT | /api/v2/ofertas/{id}/suministro | ADMINISTRADOR |
| 24 | GET | /api/v2/recepciones | ADMINISTRADOR |
| 25 | GET | /api/v2/recepciones/{id} | ADMINISTRADOR |
| 26 | POST | /api/v2/recepciones | ADMINISTRADOR |
| 27 | POST | /api/v2/aperturas | ADMINISTRADOR |
| 28 | GET | /api/v2/ventas | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 29 | POST | /api/v2/ventas | ADMINISTRADOR y VENDEDORA |
| 30 | GET | /api/v2/ventas/{id} | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 31 | GET | /api/v2/ventas/{id}/comprobante | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 32 | POST | /api/v2/ventas/{id}/descuento | ADMINISTRADOR |
| 33 | POST | /api/v2/ventas/{id}/emitir | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 34 | POST | /api/v2/ventas/{id}/anular | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 35 | POST | /api/v2/ventas/{id}/cobros | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 36 | GET | /api/v2/cartera | ADMINISTRADOR |
| 37 | GET | /api/v2/turnos | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 38 | POST | /api/v2/turnos | ADMINISTRADOR y VENDEDORA |
| 39 | GET | /api/v2/turnos/{id} | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 40 | POST | /api/v2/turnos/{id}/cerrar | ADMINISTRADOR; VENDEDORA solo documentos propios |
| 41 | GET | /api/v2/incidencias | ADMINISTRADOR |
| 42 | POST | /api/v2/incidencias | ADMINISTRADOR |
| 43 | GET | /api/v2/gastos | ADMINISTRADOR |
| 44 | POST | /api/v2/gastos | ADMINISTRADOR |
| 45 | GET | /api/v2/reportes | ADMINISTRADOR todos tipos; VENDEDORA solo tipo=ventas propias |
| 46 | GET | /api/v2/exportacion.xlsx | ADMINISTRADOR |
| 47 | GET | /api/v2/conteos | ADMINISTRADOR |
| 48 | POST | /api/v2/conteos | ADMINISTRADOR |

## Fichas de operación

### 01. GET /api/v2/auth/sesion

**Acceso:** Público; crea/recupera sesión anónima.

**Respuesta correcta:** HTTP 200; estructura `sesion` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/AuthController.java`, método `sesion`; servicio `AuthController.estado`.

### 02. POST /api/v2/auth/login

**Acceso:** Público con cookie y CSRF de sesión anónima.

**Respuesta correcta:** HTTP 200; estructura `sesion` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| nombre_acceso | Sí | texto |
| clave | Sí | Contraseña sin recortar espacios; 1 a 72 bytes UTF-8 |

**Reglas:** Tres credenciales erróneas activan un bloqueo de 30 segundos; una petición durante el bloqueo recibe 429. El login correcto rota identificador y CSRF.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 401 | Nombre de acceso, clave o cuenta no válidos |
| 429 | Acceso durante el bloqueo de 30 segundos tras tres rechazos |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/AuthController.java`, método `login`; servicio `AuthService.login`.

### 03. POST /api/v2/auth/logout

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `sesion_anonima` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Invalida la sesión y devuelve una nueva sesión anónima con CSRF. Una segunda salida sin usuario autenticado recibe 401.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/AuthController.java`, método `logout`; servicio `AuthController.logout`.

### 04. GET /api/v2/inicio

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `inicio` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `inicio`; servicio `ReportesService.inicio`.

### 05. GET /api/v2/configuracion

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `configuracion` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `config`; servicio `ApiController.config`.

### 06. GET /api/v2/usuarios

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `usuarios[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `users`; servicio `AuthService.usuarios`.

### 07. POST /api/v2/usuarios

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| nombre_visible | Sí | texto |
| nombre_acceso | Sí | Texto; se guarda en minúscula |
| clave | Sí | Contraseña sin recortar espacios; mínimo 10 caracteres y máximo 72 bytes UTF-8 |
| perfil | Sí | ADMINISTRADOR o VENDEDORA |

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 409 | Nombre de acceso ya registrado; restricción única |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `user`; servicio `AuthService.crear`.

### 08. GET /api/v2/clientes

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `cliente[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `simples`; servicio `CatalogoService.listar`.

### 09. GET /api/v2/proveedores

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `proveedor[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `simples`; servicio `CatalogoService.listar`.

### 10. GET /api/v2/fragancias

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `fragancia[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `simples`; servicio `CatalogoService.listar`.

### 11. POST /api/v2/clientes

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| nombre | Sí | texto |
| contacto_cobranza | No | texto |

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `simple`; servicio `CatalogoService.crear`.

### 12. POST /api/v2/proveedores

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| nombre | Sí | texto |
| contacto | No | texto |

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `simple`; servicio `CatalogoService.crear`.

### 13. POST /api/v2/fragancias

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| codigo_interno | Sí | texto |
| nombre_comercial | Sí | texto |

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `simple`; servicio `CatalogoService.crear`.

### 14. GET /api/v2/articulos

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `articulo_con_saldo[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Solo Administrador recibe costo_promedio_cop y valoracion_pendiente. Vendedora no recibe esos campos. alerta es verdadera cuando minimo_base > 0 y saldo_base <= minimo_base.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `arts`; servicio `InventarioService.articulos`.

### 15. POST /api/v2/articulos

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| codigo | Sí | texto |
| nombre | Sí | texto |
| unidad_base | Sí | ML, UNIDAD o G |
| clase | Sí | LIQUIDO, ENVASE, ACCESORIO, PERFUME_SELLADO, PERFUME_ABIERTO o MUESTRA |
| funcion_material | Sí | ESENCIA, ALCOHOL, FIJADOR, FEROMONAS, FRASCO, VALVULA, TAPA, ETIQUETA, ATOMIZADOR, PERFUME u OTRO |
| contenido_nominal_ml | No | Decimal positivo, hasta 6 decimales; obligatorio para FRASCO y ATOMIZADOR |
| minimo_base | No | Decimal no negativo, hasta 6 decimales; predeterminado: 0 |

**Reglas:** Unidad, clase y función deben ser compatibles. PERFUME requiere clase sellado o abierto; frasco y atomizador requieren capacidad nominal positiva. Crear el artículo no crea stock ni precio.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 409 | Combinación de clase, función, unidad o capacidad incompatible; código de artículo repetido |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `art`; servicio `CatalogoService.articulo`.

### 16. PUT /api/v2/articulos/{id}/minimo

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| minimo_base | Sí | Decimal no negativo, hasta 6 decimales |

**Reglas:** Requiere artículo activo. Cambia el mínimo, sin movimiento de inventario.

**Reintento:** Asignación repetible del mínimo; no crea movimiento ni admite clave de operación.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Artículo activo no encontrado |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `min`; servicio `CatalogoService.minimo`.

### 17. GET /api/v2/movimientos

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `movimiento_base[]` de la sección Respuestas.

Parámetros de consulta:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| articulo_id | No | UUID |

No requiere cuerpo JSON.

**Reglas:** articulo_id es opcional. Devuelve hasta 500 movimientos en orden descendente, con o sin filtro.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `mov`; servicio `InventarioService.movimientos`.

### 18. GET /api/v2/formulas

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `formula_version_con_componentes[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `forms`; servicio `CatalogoService.formulas`.

### 19. POST /api/v2/formulas

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| fragancia_id | Sí | UUID |
| variante | Sí | NORMAL, EXTRA_FIJADOR o EXTRA_FEROMONAS |
| volumen_preparado_ml | Sí | Decimal positivo, hasta 6 decimales |
| componentes | Sí | Lista de 1 a 100 elementos: {articulo_id:UUID, cantidad_ml_por_preparacion:decimal positivo de hasta 6 decimales} |

**Reglas:** Requiere fragancia activa y líquidos tipados ESENCIA, ALCOHOL, FIJADOR o FEROMONAS en ML. La suma debe coincidir exactamente con 30, 50 o 100 ml. Inserta una nueva versión APROBADA por el Administrador.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Fragancia activa o artículo de un componente no encontrado |
| 409 | Componente que no es un líquido tipado permitido |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `form`; servicio `CatalogoService.crearFormula`.

### 20. GET /api/v2/ofertas

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `oferta_actual[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve la última especificación APROBADA y precio vigente o null. La disponibilidad de unidades completas queda limitada por todos los materiales directos y de la fórmula.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `offers`; servicio `CatalogoService.ofertas`.

### 21. POST /api/v2/ofertas

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| codigo_venta | Sí | texto |
| nombre_venta | Sí | texto |
| modalidad | Sí | TERMINADO, PREPARACION, RECARGA, DECANT o ADICIONAL |
| fragancia_id | No | UUID |
| volumen_ml | No | Decimal positivo, hasta 6 decimales |
| formula_id | No | UUID |
| precio_cop | No | Decimal positivo, hasta 2 decimales |
| componentes | No | Lista de {articulo_id:UUID, cantidad_base:decimal positivo de hasta 6 decimales}; según modalidad |

**Reglas:** Crea oferta y suministro en una transacción. PREPARACION y RECARGA requieren fórmula de igual fragancia y volumen. Se aplican las reglas de materiales de la sección siguiente.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Artículo, fórmula aprobada o recurso referenciado no encontrado |
| 409 | Componentes, materiales, fragancia o volumen incompatibles con la modalidad; código de oferta repetido |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `offer`; servicio `CatalogoService.oferta`.

### 22. PUT /api/v2/ofertas/{id}/precio

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| importe_unitario_cop | Sí | Decimal positivo, hasta 2 decimales |

**Reglas:** Cierra la vigencia del precio anterior e inserta uno nuevo. La respuesta id identifica el PRECIO, no la oferta.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Oferta no encontrada |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `price`; servicio `CatalogoService.precio`.

### 23. PUT /api/v2/ofertas/{id}/suministro

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| formula_id | No | UUID |
| componentes | No | Lista de {articulo_id:UUID, cantidad_base:decimal positivo de hasta 6 decimales}; según modalidad |

**Reglas:** Inserta una especificación APROBADA y conserva las anteriores. La respuesta id identifica la ESPECIFICACION, no la oferta.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Oferta, fórmula aprobada o artículo no encontrado |
| 409 | Fórmula, volumen, fragancia o materiales incompatibles con la modalidad |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `supply`; servicio `CatalogoService.suministro`.

### 24. GET /api/v2/recepciones

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `recepcion_resumen[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve hasta las últimas 500 recepciones; no incluye otros tipos de documento.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `receipts`; servicio `InventarioService.recepciones`.

### 25. GET /api/v2/recepciones/{id}

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `recepcion_detalle` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Documento de recepción no encontrado |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `receipt`; servicio `InventarioService.detalleRecepcion`.

### 26. POST /api/v2/recepciones

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| proveedor_id | Sí | UUID |
| documento_proveedor | Sí | texto |
| clave_operacion | Sí | texto |
| lineas | Sí | Lista de 1 a 100 elementos: {articulo_id:UUID, cantidad:decimal positivo de hasta 6 decimales, unidad_codigo:ML/L/UNIDAD/G, costo_total_cop:decimal no negativo de hasta 2 decimales} |

**Reglas:** Bloquea los artículos durante la recepción. Convierte 1 L a 1000 ML exactamente, exige cantidades enteras en UNIDAD y rechaza masa frente a volumen. La factura completa del proveedor es única. El costo unitario tiene ocho decimales.

**Reintento:** La clave es única en documento_inventario y la huella incluye operación RECEPCION y datos. Repetición idéntica devuelve el mismo id; contenido u operación distintos: 409.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Artículo activo no encontrado |
| 409 | Clave usada con otro contenido/operación; proveedor más factura completa repetidos; relación de proveedor no válida por integridad |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `receive`; servicio `InventarioService.recepcion`.

### 27. POST /api/v2/aperturas

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| articulo_sellado_id | Sí | UUID |
| articulo_abierto_id | Sí | UUID |
| unidades | Sí | Entero de 1 a 10000 |
| volumen_util_ml | Sí | Decimal positivo, hasta 6 decimales |
| motivo | Sí | texto |
| clave_operacion | Sí | texto |

**Reglas:** Origen y destino son distintos: PERFUME_SELLADO en UNIDAD pasa a PERFUME_ABIERTO en ML. El volumen no supera capacidad nominal por unidades, con stock y costo conocidos. Transfiere costo y material; no registra una compra. id identifica el documento TRANSFORMACION.

**Reintento:** La clave comparte ámbito documento_inventario y la huella incluye operación APERTURA y datos. Repetición idéntica devuelve el mismo id; otra operación o cuerpo: 409.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Artículo de origen o destino no encontrado |
| 409 | Origen/destino incompatibles, volumen superior al nominal, existencias o valoración insuficientes, clave incompatible |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `open`; servicio `InventarioService.apertura`.

### 28. GET /api/v2/ventas

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `venta_resumen[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve hasta las últimas 500 ventas, ordenadas por creación y de todos los estados. Vendedora consulta solo ventas de su autoría.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `sales`; servicio `VentasService.listar`.

### 29. POST /api/v2/ventas

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| cliente_id | No | UUID de cliente activo |
| lineas | Sí | Lista de 1 a 100 elementos: {especificacion_id:UUID, unidades:entero de 1 a 10000} |

**Reglas:** Crea BORRADOR con descripción y precio vigente fijados. Exige suministro aprobado y precio. No consume ni cobra; conserva al actor autenticado como autor.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Oferta o especificación activa, o cliente referenciado, no encontrado |
| 404 | No hay precio vigente para la especificación solicitada |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `sale`; servicio `VentasService.crear`.

### 30. GET /api/v2/ventas/{id}

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `venta_detalle` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve el detalle y naturaleza del recibo de operación. costo_materiales solo se agrega para Administrador. No constituye facturación electrónica DIAN.

**Reintento:** Consulta sin clave de operación.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Venta no encontrada |
| 403 | Vendedora intenta consultar una venta ajena |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `detail`; servicio `VentasService.detalle`.

### 31. GET /api/v2/ventas/{id}/comprobante

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `venta_detalle` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve el mismo detalle autorizado de la venta; no genera una factura electrónica DIAN.

Comparte la respuesta del detalle de venta; también devuelve BORRADOR y ANULADA_BORRADOR. La interfaz presenta la naturaleza y el estado del documento.

**Reintento:** Consulta sin clave de operación.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Venta no encontrada |
| 403 | Venta ajena a la Vendedora |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `detail`; servicio `VentasService.detalle`.

### 32. POST /api/v2/ventas/{id}/descuento

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `venta_detalle` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| importe_cop | Sí | Decimal positivo, hasta 2 decimales |
| motivo | Sí | texto |

**Reglas:** Solo BORRADOR. El importe es menor que el bruto. Conserva una autorización explícita con actor, motivo y fecha; reemplaza su valor, sin acumular otro descuento ni inferir porcentaje.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Venta no encontrada |
| 409 | Venta fuera de BORRADOR o descuento incompatible con el total |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `discount`; servicio `VentasService.descuento`.

### 33. POST /api/v2/ventas/{id}/emitir

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `venta_detalle` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| clave_operacion | Sí | texto |
| credito | Sí | booleano JSON obligatorio |
| fecha_vencimiento | No | YYYY-MM-DD; obligatorio en crédito |
| condicion_pactada | No | Texto; obligatorio en crédito |
| cobro | No | Objeto {turno_id:UUID, medio:EFECTIVO/NEQUI/DAVIPLATA/TARJETA, importe_cop:decimal positivo de hasta 2 decimales, referencia_externa?:texto, soporte_privado_ref?:texto, confirmado:true}; obligatorio en contado |

**Reglas:** Contado exige total exacto y cobro confirmado anidado: emisión, consumo y cobro son atómicos. Crédito exige Administrador, cliente, condición y vencimiento no anterior al día comercial actual; rechaza un cobro anidado. Valida stock, costo y materiales y pasa a EMITIDA. Consume una sola vez.

**Reintento:** La clave de emisión queda ligada a la venta y su huella. Repetición de la misma clave y cuerpo devuelve detalle; otros datos: 409. En contado se deriva una clave de cobro del identificador de venta y clave de emisión.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Venta, cliente, turno o artículo referenciado no encontrado |
| 403 | Venta ajena; Vendedora intenta autorizar crédito |
| 409 | Estado o clave incompatible, falta de stock/valoración, cobro de contado incompatible con el total o turno cerrado |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `issue`; servicio `OperacionVentaService.emitir`.

### 34. POST /api/v2/ventas/{id}/anular

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `venta_detalle` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Solo BORRADOR pasa a ANULADA_BORRADOR. No requiere cuerpo. La repetición o una venta emitida producen 409. No es devolución ni reversión de una venta emitida.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Venta no encontrada |
| 403 | Venta ajena a la Vendedora |
| 409 | La venta no está en BORRADOR |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `cancel`; servicio `VentasService.anular`.

### 35. POST /api/v2/ventas/{id}/cobros

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| turno_id | Sí | UUID de turno abierto propio o permitido a Administrador |
| medio | Sí | EFECTIVO, NEQUI, DAVIPLATA o TARJETA |
| importe_cop | Sí | Decimal positivo, hasta 2 decimales |
| referencia_externa | No | Texto; obligatorio para medio electrónico |
| soporte_privado_ref | No | texto |
| confirmado | Sí | true |
| clave_operacion | Sí | texto |

**Reglas:** Requiere venta EMITIDA y saldo suficiente; impide sobrepago y valida venta y turno permitidos. No consume stock. Un reembolso no borra el cobro original. Una referencia electrónica se utiliza una sola vez por medio.

**Reintento:** La clave es única en cobro; la huella incluye venta_id y cuerpo. Repetición idéntica devuelve id; misma clave en otra venta o con otros datos: 409.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Venta o turno no encontrado |
| 403 | Venta o turno ajeno a la Vendedora |
| 409 | Venta no emitida, turno cerrado, abono superior al saldo, clave incompatible o referencia electrónica repetida |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `collect`; servicio `CajaService.cobrar`.

### 36. GET /api/v2/cartera

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `cartera[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Solo ventas EMITIDAS con crédito y saldo positivo, en orden de vencimiento. Muestra cartera vigente.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `ledger`; servicio `VentasService.cartera`.

### 37. GET /api/v2/turnos

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `turno_resumen[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve hasta los últimos 500 turnos. Vendedora consulta sus propios turnos.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `shifts`; servicio `CajaService.turnos`.

### 38. POST /api/v2/turnos

**Acceso:** ADMINISTRADOR y VENDEDORA.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| caja_id | No | UUID de caja activa; predeterminado: primera caja activa |
| fondo_cop | Sí | Decimal no negativo, hasta 2 decimales |

**Reglas:** Abre para el actor actual. No permite otro turno abierto de la caja o del mismo responsable.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Caja activa no encontrada |
| 409 | Ya existe turno abierto para la caja o el responsable |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `shift`; servicio `CajaService.abrir`.

### 39. GET /api/v2/turnos/{id}

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `turno_detalle` de la sección Respuestas.

No requiere cuerpo JSON.

**Reintento:** Consulta sin clave de operación.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Turno no encontrado |
| 403 | Turno ajeno a la Vendedora |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `shiftd`; servicio `CajaService.detalle`.

### 40. POST /api/v2/turnos/{id}/cerrar

**Acceso:** ADMINISTRADOR; VENDEDORA solo documentos propios.

**Respuesta correcta:** HTTP 200; estructura `turno_detalle` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| conciliaciones | Sí | Exactamente 4 medios distintos: {medio, importe_verificado_cop:decimal no negativo de hasta 2 decimales, soporte_privado_ref?:texto} |
| observacion | No | texto |

**Reglas:** Requiere turno abierto y los cuatro medios. Conserva diferencia = verificado - esperado y observación. Repetir el cierre produce 409.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Turno no encontrado |
| 403 | Turno ajeno a la Vendedora |
| 409 | El turno ya está cerrado |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `close`; servicio `CajaService.cerrar`.

### 41. GET /api/v2/incidencias

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `incidencia[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve hasta las últimas 500 incidencias. La lista devuelve campos de la incidencia, sin añadir sus renglones ni el reembolso.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `incidents`; servicio `CajaService.incidencias`.

### 42. POST /api/v2/incidencias

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| tipo | Sí | MERMA, MUESTRA, REPOSICION, DEVOLUCION, ERROR_MEZCLA, CAMBIO_ENVASE o REPOSICION_LIQUIDO |
| venta_id | No | UUID; se acompaña de linea_no |
| linea_no | No | Entero de 1 a 10000; se acompaña de venta_id |
| motivo | Sí | texto |
| clave_operacion | Sí | texto |
| lineas | No | Lista de {articulo_id:UUID, cantidad_base:decimal positivo de hasta 6 decimales, direccion:SALIDA/ENTRADA, costo_unitario_base_cop?:decimal no negativo de hasta 8 decimales} |
| turno_id | No | UUID; obligatorio cuando hay devolucion_cop |
| devolucion_cop | No | Decimal positivo, hasta 2 decimales |

**Reglas:** Admite los siete tipos enumerados. Sin renglones no mueve material. No permite artículos repetidos; MERMA y MUESTRA solo admiten SALIDA. Una salida requiere stock y costo; una entrada requiere costo conocido. Con origen conserva el costo histórico y limita retorno al suministro menos retornos previos. Dinero solo para DEVOLUCION de venta EMITIDA con línea y turno abiertos, hasta cobrado menos devuelto. La salida de efectivo queda ligada por FK a la incidencia.

**Reintento:** La clave es única en incidencia; el cuerpo determina la huella. Repetición idéntica devuelve id; cuerpo distinto: 409.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Venta/línea, artículo o turno de origen no encontrado |
| 409 | Origen, dirección o valoración incompatibles; stock insuficiente; reembolso superior al cobrado neto; clave incompatible |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `incident`; servicio `CajaService.incidencia`.

### 43. GET /api/v2/gastos

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `gasto[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve hasta los últimos 500 gastos ordenados por fecha.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `expenses`; servicio `CajaService.gastos`.

### 44. POST /api/v2/gastos

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| fecha | Sí | YYYY-MM-DD |
| concepto | Sí | texto |
| importe_cop | Sí | Decimal positivo, hasta 2 decimales |
| documento_ref | Sí | texto |
| proveedor_id | No | UUID |
| turno_id | No | UUID de turno abierto |
| medio | Sí | EFECTIVO, NEQUI, DAVIPLATA o TARJETA |
| clave_operacion | Sí | texto |

**Reglas:** Solo EFECTIVO con turno genera una SALIDA en movimiento_caja. Los gastos electrónicos permanecen reportables sin duplicar un movimiento de efectivo.

**Reintento:** La clave es única en gasto; el cuerpo determina la huella. Repetición idéntica devuelve id; cuerpo distinto: 409.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Turno no encontrado |
| 409 | Turno cerrado, proveedor no válido por integridad o clave incompatible |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `expense`; servicio `CajaService.gasto`.

### 45. GET /api/v2/reportes

**Acceso:** ADMINISTRADOR todos tipos; VENDEDORA solo tipo=ventas propias.

**Respuesta correcta:** HTTP 200; estructura `reporte` de la sección Respuestas.

Parámetros de consulta:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| tipo | No | ventas, medios, rotacion, costos, vendedoras, gastos, devoluciones |
| desde | No | YYYY-MM-DD |
| hasta | No | YYYY-MM-DD |

No requiere cuerpo JSON.

**Reglas:** tipo predeterminado: ventas. desde predeterminado: día comercial actual; hasta predeterminado: desde. Valida desde <= hasta, período inclusivo de 1 a 366 días y máximo 10000 filas. Convierte límites de Bogotá a UTC.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `reports`; servicio `ReportesService.reporte`.

### 46. GET /api/v2/exportacion.xlsx

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `xlsx` de la sección Respuestas.

Parámetros de consulta:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| mes | Sí | YYYY-MM |

No requiere cuerpo JSON.

**Reglas:** mes usa YYYY-MM. Genera cinco hojas: Ventas, Cobros, Gastos, Cartera y Devoluciones. Cartera es vigente al generar con instante UTC; no representa un cierre histórico mensual.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `excel`; servicio `ReportesService.excel`.

### 47. GET /api/v2/conteos

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `conteo_con_renglones[]` de la sección Respuestas.

No requiere cuerpo JSON.

**Reglas:** Devuelve hasta los últimos 500 conteos con sus renglones.

**Reintento:** Consulta sin clave de operación.

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `counts`; servicio `InventarioService.conteos`.

### 48. POST /api/v2/conteos

**Acceso:** ADMINISTRADOR.

**Respuesta correcta:** HTTP 200; estructura `id` de la sección Respuestas.

Cuerpo JSON:

| Campo | Obligatorio | Tipo o regla |
| --- | --- | --- |
| observacion | No | texto |
| renglones | Sí | Lista de 1 a 100 elementos: {articulo_id:UUID, cantidad_medida:decimal no negativo de hasta 6 decimales, unidad_medicion:ML/L/UNIDAD/G, observacion?:texto} |

**Reglas:** Un renglón por artículo, medición compatible y cantidades enteras para UNIDAD. Conserva saldo esperado del instante y termina CERRADO. El conteo no ajusta automáticamente el stock.

**Reintento:** Esta operación no admite clave de operación. Consulte el resultado antes de repetirla si hubo una interrupción.

| HTTP | Condición de esta operación |
| --- | --- |
| 404 | Artículo activo no encontrado |

**Implementación:** `backend/src/main/java/co/edu/sena/perfumeria/web/ApiController.java`, método `count`; servicio `InventarioService.conteo`.

## Materiales de la oferta

PREPARACION: fórmula y un FRASCO de la misma capacidad, una VALVULA, una TAPA y una ETIQUETA. RECARGA: misma fórmula, sin frasco, válvula ni tapa; admite cero o una etiqueta configurada. DECANT: 5 o 10 ml de un PERFUME_ABIERTO tipado como PERFUME, un ATOMIZADOR completo de igual capacidad y una ETIQUETA. No se añade otra válvula al atomizador completo. PREPARACION y RECARGA no reciben otros líquidos directos fuera de su fórmula. TERMINADO y ADICIONAL usan componentes directos, sin fórmula.

## Semántica de informes

| Informe | Selección y significado |
| --- | --- |
| ventas | Ventas EMITIDAS por fecha comercial de emisión. Cobrado, saldo y devoluciones acumuladas reflejan el estado vigente de esas ventas, aunque el cobro o devolución haya ocurrido fuera del período. |
| medios | Cobros por su fecha de confirmación, agrupados por medio. |
| rotacion | Unidades y bruto por oferta según emisión. No mezcla ml con unidades comerciales. |
| costos | Costo y margen originales y ajustados por incidencias acumuladas. Un costo desconocido conserva margen desconocido. |
| vendedoras | Autor de la venta, incluidos administradores cuando venden; cobros acumulados de esas ventas. No equivale a cobrador ni calcula nómina o porcentaje de comisión. |
| devoluciones | Fecha efectiva del movimiento de reembolso, incluso si la venta pertenece a otro período. |
| gastos | Fecha de calendario registrada para el gasto. |
| Excel | Ventas, Cobros y Gastos por sus fechas respectivas; Cartera vigente con instante de generación; Devoluciones por fecha del reembolso. |

## Respuestas

Las estructuras siguientes describen los campos y su semántica; no son capturas de una operación del negocio. Un sufijo ? indica un campo condicional u opcional. Los alias de presentación coexisten con campos relacionales cuando así los devuelve la implementación.

### id

```json
{
  "type": "object",
  "fields": [
    "id:UUID"
  ],
  "note": "ID de entidad creada:precio/suministro/devuelve su propia entidad,no oferta."
}
```

### sesion

```json
{
  "type": "object",
  "fields": [
    "csrf:string",
    "autenticado:boolean",
    "actor?:{usuario_id,nombre,perfil}"
  ]
}
```

### sesion_anonima

```json
{
  "type": "object",
  "fields": [
    "csrf:string nuevo",
    "autenticado:false"
  ]
}
```

### inicio

```json
{
  "type": "object",
  "fields": [
    "articulos:count",
    "alertas:articulo_con_saldo[]",
    "ventas:count de últimas500ventas visibles,incluye estados",
    "cartera?:decimal soloAdmin"
  ],
  "note": "No es contador de ventas de hoy ni suma global de volumen; el frontend calcula su resumen comercial con listas/fechas."
}
```

### configuracion

```json
{
  "type": "object",
  "fields": [
    "unidades:tablaunidad[]",
    "medios:EFECTIVO/NEQUI/DAVIPLATA/TARJETA",
    "cajas:cajasactivas[]"
  ]
}
```

### usuarios[]

```json
{
  "type": "array",
  "fields": [
    "usuario_id",
    "nombre_acceso",
    "perfil",
    "activo",
    "nombre_visible"
  ],
  "note": "No devuelvehash_clave."
}
```

### cliente[]

```json
{
  "type": "array",
  "base_table": "cliente",
  "limit": 500
}
```

### proveedor[]

```json
{
  "type": "array",
  "base_table": "proveedor",
  "limit": 500
}
```

### fragancia[]

```json
{
  "type": "array",
  "base_table": "fragancia"
}
```

### articulo_con_saldo[]

```json
{
  "type": "array",
  "base_table": "articulo",
  "additional_fields": [
    "saldo_base",
    "alerta",
    "valoracion_pendiente:soloAdmin",
    "costo_promedio_cop:soloAdmin/nullsi pendiente"
  ]
}
```

### movimiento_base[]

```json
{
  "type": "array",
  "base_view": "v_movimiento_base",
  "additional_fields": [
    "codigo/nombre soloenconsultasinarticulo_id",
    "alias fecha/costo_unitario/direccion"
  ],
  "limit": 500
}
```

### formula_version_con_componentes[]

```json
{
  "type": "array",
  "base_table": "formula_version",
  "additional_fields": [
    "nombre_comercial",
    "componentes:formula_componente[]concodigo/nombre"
  ]
}
```

### oferta_actual[]

```json
{
  "type": "array",
  "base_table": "oferta",
  "additional_fields": [
    "especificacion_id",
    "formula_id",
    "version",
    "precio_cop:nullosinprecio",
    "componentes:especificacion_componente[]connombre/unidad_base",
    "disponible_unidades"
  ]
}
```

### recepcion_resumen[]

```json
{
  "type": "array",
  "base_table": "documento_inventario;tipoRECEPCION",
  "additional_fields": [
    "proveedor_nombre",
    "total_cop",
    "fecha(aliasregistrado_en)"
  ],
  "limit": 500
}
```

### recepcion_detalle

```json
{
  "type": "object",
  "base_table": "documento_inventario;tipoRECEPCION",
  "additional_fields": [
    "lineas:renglon_inventario[]",
    "total_cop",
    "proveedor_nombre"
  ]
}
```

### venta_resumen[]

```json
{
  "type": "array",
  "base_table": "venta",
  "additional_fields": [
    "total_cop",
    "cobrado_cop",
    "saldo_cop",
    "cliente",
    "vendedora",
    "cliente_nombre",
    "fecha_creacion",
    "fecha_emision"
  ],
  "limit": 500
}
```

### venta_detalle

```json
{
  "type": "object",
  "base_table": "venta",
  "additional_fields": [
    "total_cop",
    "bruto_cop",
    "descuento_cop",
    "cobrado_cop",
    "saldo_cop",
    "estado_pago:SIN_PAGO/PARCIAL/PAGADA",
    "lineas:linea_venta[]+modalidad/volumen_ml/fragancia_id/aliases",
    "cobros:cobro[]",
    "credito:credito_venta|null",
    "naturaleza:recibo noDIAN",
    "costo_materiales_cop:soloAdmin"
  ]
}
```

### cartera[]

```json
{
  "type": "array",
  "fields": [
    "venta_id",
    "numero_recibo",
    "cliente",
    "contacto_cobranza",
    "fecha_vencimiento",
    "condicion_pactada",
    "total_cop",
    "cobrado_cop",
    "saldo_cop",
    "cliente_nombre"
  ]
}
```

### turno_resumen[]

```json
{
  "type": "array",
  "base_table": "turno_caja",
  "additional_fields": [
    "responsable",
    "caja",
    "estado:ABIERTO/CERRADO",
    "responsable_nombre",
    "fecha_apertura",
    "fondo_cop"
  ],
  "limit": 500
}
```

### turno_detalle

```json
{
  "type": "object",
  "base_table": "turno_caja",
  "additional_fields": [
    "responsable",
    "totales_por_medio",
    "esperado_efectivo_cop",
    "movimientos:movimiento_caja[]",
    "cobros:cobro[]",
    "conciliaciones:conciliacion_turno[]+esperado_cop/importe_esperado_cop/diferencia_cop",
    "estado/fecha_apertura/fondo_cop/responsable_nombre"
  ]
}
```

### incidencia[]

```json
{
  "type": "array",
  "base_table": "incidencia",
  "additional_fields": [
    "fecha(aliasregistrada_en)"
  ],
  "limit": 500,
  "note": "El listado devuelve la incidencia y el alias de fecha. No incorpora renglones de inventario ni el importe de reembolso."
}
```

### gasto[]

```json
{
  "type": "array",
  "base_table": "gasto",
  "additional_fields": [
    "documento_ref(aliassoporte_ref)"
  ],
  "limit": 500
}
```

### reporte

```json
{
  "type": "object",
  "fields": [
    "tipo",
    "desde",
    "hasta",
    "zona:America/Bogota",
    "filas:arraydependientedeltipo"
  ],
  "limit": 10000
}
```

### xlsx

```json
{
  "type": "binary",
  "mime": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "content_disposition": "attachment; filename=\"ventas-gastos-YYYY-MM.xlsx\"",
  "sheets": [
    "Ventas",
    "Cobros",
    "Gastos",
    "Cartera",
    "Devoluciones"
  ]
}
```

### conteo_con_renglones[]

```json
{
  "type": "array",
  "base_table": "conteo_inventario",
  "additional_fields": [
    "renglones:conteo_renglon[]+codigo/nombre/unidad_base"
  ],
  "limit": 500
}
```

## Código y ejecución

El paquete incluye las fuentes integrales y el DDL. Instalación y configuración: software/README.md y software/documentacion/OPERACION_TECNICA.md. Las 44 unitarias Java y las 428 comprobaciones HTTP/MySQL se contabilizan por separado; no equivalen a aceptación de usuario. El código fuente está disponible en:

https://github.com/SirHegel/sena-pos-integral/tree/main/Perfumeria_Esencias_Estilo_Solano
