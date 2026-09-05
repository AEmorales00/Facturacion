# Sistema POS - Facturación con Web Service del Banguat

Utilitario de facturación (venta) hecho con **HTML, CSS, JavaScript, PHP y MySQL**.
El tipo de cambio del dólar se obtiene del **Web Service del Banco de Guatemala**.

---

## 1. Qué hace el sistema

| Pantalla | Archivo | Qué hace |
|---|---|---|
| Facturación | `index.php` | Busca el cliente, agrega productos, calcula totales y guarda la factura |
| Facturas registradas | `facturas.php` | Consulta en la BD las facturas emitidas, con filtros y resumen |
| Productos | `productos.php` | Catálogo, registro de productos y precios convertidos a quetzales |
| Demo del Web Service | `demo_ws.php` | Explica paso a paso la conexión con el Banguat (para la presentación) |

---

## 2. Estructura de archivos

```
Facturacion/
├── index.php            Pantalla de facturación (formulario principal)
├── facturas.php         Consulta de facturas registradas (query a la BD)
├── productos.php        Catálogo de productos
├── demo_ws.php          Demostración del Web Service del Banguat
│
├── config/
│   └── conexion.php     Conexión a MySQL (usuario y contraseña van aquí)
│
├── includes/
│   └── banguat.php      Llamada SOAP al Web Service del Banguat
│
├── api/                 Archivos que responden en JSON al JavaScript
│   ├── buscar_cliente.php
│   ├── nuevo_cliente.php
│   ├── buscar_producto.php
│   ├── tipo_cambio.php
│   └── guardar_factura.php
│
├── css/estilos.css      Todos los estilos
├── js/app.js            Toda la lógica del navegador
└── bd/facturacion.sql   Script de la base de datos
```

Regla que se siguió: **el HTML no lleva estilos ni scripts adentro**. El CSS está
solo en `css/estilos.css` y el JavaScript solo en `js/app.js`.

---

## 3. Instalación

### Paso 1 — Crear la base de datos

Con XAMPP: abrir **phpMyAdmin → Importar** y seleccionar `bd/facturacion.sql`.

Desde la terminal:

```bash
mysql -u root -p < bd/facturacion.sql
```

### Paso 2 — Configurar la conexión

Abrir `config/conexion.php` y poner el usuario y la contraseña de MySQL:

```php
$USUARIO = 'root';
$CLAVE   = '';        // en XAMPP normalmente va vacía
```

### Paso 3 — Levantar el servidor

Con XAMPP: copiar la carpeta a `htdocs/` y abrir `http://localhost/Facturacion/`.

O con el servidor que trae PHP:

```bash
php -S localhost:8000
```

Luego abrir `http://localhost:8000`.

---

## 4. Cómo se conecta con el Banguat

- **URL:** `https://www.banguat.gob.gt/variables/ws/TipoCambio.asmx` (con **https**: si se usa `http` el Banguat responde un redireccionamiento 302 y no el XML)
- **Método:** `TipoCambioDia`
- **Protocolo:** SOAP 1.1 (se envía un XML y devuelve otro XML)

El flujo está en `includes/banguat.php`:

1. `consultar_banguat()` arma el XML, lo envía con cURL y lee la etiqueta `<referencia>`.
2. `obtener_tipo_cambio()` decide de dónde sacar el dato:
   - si ya se consultó hoy → lo lee de la tabla `tipo_cambio` (más rápido);
   - si no → llama al Web Service y guarda el resultado;
   - si el Web Service no responde → usa el último valor guardado, para que la demo no se caiga.

La conversión final es:

```
Total GTQ = Total USD × tipo de cambio
```

Se calcula en dos lugares: en `js/app.js` (`calcularTotales()`) para mostrarlo en
pantalla, y en `api/guardar_factura.php` para guardarlo en la BD. El valor que
queda grabado en la factura es siempre el del servidor.

---

## 5. Dónde está cada requisito de la evaluación

| Requisito | Dónde verlo |
|---|---|
| 1. Control del código | Código comentado en español, funciones cortas y con nombre descriptivo |
| 2. Código limpio y ordenado | CSS, JS y PHP en carpetas separadas; nada de estilos ni scripts dentro del HTML |
| 3. Query en la BD sobre el registro de datos | `facturas.php` (JOIN de facturas + clientes + detalle, filtros y totales) y `productos.php` |
| 4. Demo del tipo de cambio con el WS | `demo_ws.php` — muestra el XML enviado, el XML recibido, el dato extraído y cómo se aplica a la factura |

---

## 6. Datos de prueba que ya vienen cargados

**Clientes:** `1234567-8` (Juan Pérez), `9876543-2` (María López), `CF` (Consumidor Final)

**Productos:** `P001-AZ`, `P023-LC`, `P105-PH`, `P210-AC`, `P330-FR`

---

## 7. Preguntas que pueden hacer en la presentación

**¿Por qué los precios se leen otra vez en el servidor al guardar?**
Porque cualquiera puede modificar los valores desde el navegador. `api/guardar_factura.php`
solo recibe el id del producto y la cantidad; el precio lo vuelve a consultar en la BD.

**¿Qué es una transacción y por qué se usa?**
En `api/guardar_factura.php` se usa `beginTransaction()` / `commit()`. Si falla el guardado
del detalle, se hace `rollBack()` y no queda una factura a medias en la base de datos.

**¿Por qué se guarda el tipo de cambio dentro de la factura?**
Porque el tipo de cambio varía cada día. Si mañana cambia, la factura de hoy debe conservar
el valor con el que se cobró.

**¿Qué pasa si el Banguat no responde?**
El sistema usa el último valor guardado en la tabla `tipo_cambio` y lo indica en pantalla
("Respaldo (el WS no respondió)").
