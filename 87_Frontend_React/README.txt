POS INTEGRAL — COMPONENTE FRONT-END REACT
GA7-220501096-AA4-EV03
Jhon Steven Alvarez Ruiz · Ficha 3388966

Cobertura
19 vistas: acceso, inicio, usuarios, roles, nueva venta, pago, comprobante, productos, existencias, kardex, mesas, caja, recetas, empleados, novedades, liquidación, cuentas, asientos y reportes. Identidad, ventas, inventario, gastronomía/caja, nómina y contabilidad son seis dominios de escritura; Reportes constituye la séptima área.

Implementación
React 19.3.1 y ReactDOM 19.3.1, componentes de función con JSX, useReducer para el estado compartido y useState para formularios, menús y filtros. Vite 8.3.5 construye los recursos del cliente. El backend del proyecto continúa siendo Java/Spring con MySQL 8.4 y su modelo de 26 tablas. Este componente funciona con estado sintético en memoria y permite verificar el frente de interfaz antes de la integración con servicios.

Ejecución
Requiere Node 22.12 o superior compatible con Vite y npm.
1. En la carpeta del proyecto ejecutar npm ci.
2. Ejecutar npm test para las comprobaciones de reglas e integración del estado.
3. Ejecutar npm run build para compilar.
4. Ejecutar npm run preview y abrir http://127.0.0.1:8087.
Para desarrollo se puede usar npm run dev.
El archivo package-lock.json fija las dependencias. node_modules y dist se crean al instalar y compilar; no se incluyen como dependencias instaladas en la entrega.

Acceso de demostración
Negocio: Café Demo A.
Correo: operador@example.invalid.
Contraseña: demo1234.
Las credenciales anteriores son ficticias. El acceso es local y no representa autorización del servidor. La contraseña se limpia después de cada envío y no se conserva en el estado compartido. Recargar reinicia los datos de la demostración.

Reglas conservadas del antecedente 68
- Venta por unidades: cantidad entera positiva y segura; precio histórico en el detalle.
- Confirmación de pago por el importe exacto y con caja abierta. Un segundo envío no vuelve a descontar una venta vacía.
- Descuento directo o por receta; las necesidades de todas las recetas se agregan por insumo antes de cambiar saldos.
- Disponibilidad calculada por el insumo limitante, sin descuento adicional de un producto terminado.
- Caja separa efectivo de confirmaciones externas y distingue sus sesiones.
- Novedades por empleado/período y previsualización didáctica de nómina.
- Asiento de dos líneas equilibradas con cuentas distintas y activas.
- Fechas de presentación y filtros en America/Bogota. Se rechazan períodos de consulta invertidos.
- Reporte y CSV comparten las mismas ventas filtradas. El CSV conserva origen, negocio, rol de consulta, rango y fecha de corte.

Precisión y límites
Los importes se calculan en centavos enteros, con hasta dos decimales en COP, coherentes con el catálogo Java. Las cantidades de insumos se representan en milésimas para conservar exactitud con tres decimales. Se validan límites de enteros seguros. El negocio y el rol de consulta pertenecen al contexto fijo de demostración; su autorización real corresponde al servidor.
Los pagos, datos de identidad, salarios, novedades y cuentas son ejemplos sintéticos. La nómina es un borrador didáctico, sin cálculo legal de aportes o retenciones. Las propuestas contables no constituyen registros productivos. No hay conexión bancaria ni escritura en MySQL desde este componente.

Organización
src/App.jsx: estructura, navegación y coordinación del estado.
src/vistas.jsx: 19 componentes de vista y estado de sus filtros.
src/componentes.jsx: Card, DataTable, Field, DemoForm, RouteLink y RoleMatrix.
src/modelo.js: reglas puras, estado inicial, reducer y utilidades de fecha/moneda/CSV.
src/styles.css: tokens y composición derivados del antecedente 68, con adaptación por tamaño de pantalla.
pruebas/modelo.test.js: comprobaciones funcionales del estado.
documentacion/qa: resultados, navegación y capturas reales del navegador.
TRAZABILIDAD.json: correspondencia de vistas y requisitos.

Validación realizada
Compilación real con Vite. Once casos funcionales aprobados con node:test: cantidades, atomicidad local del cobro, demanda compartida de insumos, precisión, precio histórico, caja, fecha Bogotá/CSV, nómina, asientos e identidad. Navegación de las 19 vistas verificada en navegador. Flujo de interfaz real: CAF001x2 y REC001x1, venta DEMO-0001 por 17000 COP, saldos CAF001=10, INS001=990 g e INS002=400 ml. Se verificaron texto literal con caracteres especiales, rechazo de rango invertido y limpieza de contraseña. La descarga CSV del navegador no se ejecutó; la función de serialización y su correspondencia con el filtro se comprobaron en las pruebas de estado.

Fuentes
SENA, guía 7 AA4 EV03 e instrumento IE-GA7-220501096-AA4-EV03.
SENA, CF20 Desarrollo de aplicaciones web con Java, sección 3 React.
Antecedentes del proyecto: arquitectura GA4, prototipo GA5 de 19 vistas, modelo físico GA6 de 26 tablas, interfaz 68 corregida, plan 76, estándar 77 y definición de componentes 86.
React: https://react.dev/learn/creating-a-react-app
React: https://react.dev/learn/your-first-component
React: https://react.dev/learn/managing-state
Vite: https://vite.dev/guide/
