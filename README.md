# Sistema de Facturación Web — AWS S3 + Angular + Spring Boot

Proyecto 2 del curso de Desarrollo Web. Evoluciona el sistema de facturación del
Proyecto 1 (PHP + MySQL) hacia una aplicación en la nube:

| Capa | Tecnología | Carpeta |
|---|---|---|
| Frontend | Angular 19 (SPA) | `frontend/` |
| Backend | Java 21 + Spring Boot 3.5 (API REST) | `backend/` |
| Imágenes | AWS S3 | `backend/.../servicio/AlmacenamientoS3.java` |
| Base de datos | MySQL / PostgreSQL (H2 en local) | `bd/facturacion.sql` |
| Tipo de cambio | Web Service SOAP del Banco de Guatemala | `backend/.../servicio/BanguatServicio.java` |

El código PHP del Proyecto 1 (`index.php`, `api/`, `config/`, `includes/`) se
conserva en la raíz como referencia de la primera fase.

---

## 1. Qué hace el sistema

| Pantalla | Ruta | Qué hace |
|---|---|---|
| Facturar | `/facturacion` | Busca cliente, busca producto, **muestra su fotografía y datos al seleccionarlo**, calcula totales y emite la factura |
| Catálogo | `/catalogo` | Galería de productos con las imágenes servidas desde S3 |
| Productos | `/productos` | Alta, edición y baja; **aquí se sube la fotografía al bucket** |
| Facturas emitidas | `/facturas` | Consulta con filtros por NIT y rango de fechas, con detalle desplegable |
| Web Service | `/web-service` | Muestra el XML SOAP enviado y recibido del Banguat (para la demo) |

---

## 2. Correr el proyecto en la computadora

Se necesitan **Java 21** y **Node 20 o superior**.

### Backend

```bash
cd backend && ./mvnw spring-boot:run
```

Queda en `http://localhost:8080`. Sin configurar nada usa:

- **H2** como base de datos, en el archivo `backend/datos/facturacion.mv.db`
  (no hay que instalar MySQL). Se puede inspeccionar en `http://localhost:8080/h2`.
- **Disco local** para las imágenes, en `backend/uploads/`.

Para comprobar que arrancó bien:

```bash
curl http://localhost:8080/api/salud
```

### Frontend

```bash
cd frontend && npm install && npx ng serve
```

Queda en `http://localhost:4200`.

---

## 3. Configurar AWS S3

### 3.1 Crear el bucket

1. En la consola de AWS entrar a **S3 → Create bucket**.
2. Nombre único, por ejemplo `facturacion-productos-<su-carnet>`. Región `us-east-1`.
3. Dejar **Block all public access activado** (el sistema usa URLs firmadas,
   que es más seguro y no requiere abrir el bucket).

### 3.2 Crear las credenciales

1. **IAM → Users → Create user**, por ejemplo `facturacion-backend`.
2. Adjuntar una política con permiso sobre ese bucket:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::NOMBRE-DEL-BUCKET/*"
    }
  ]
}
```

3. **Security credentials → Create access key** y guardar las dos llaves.

> Las llaves nunca se escriben en el código ni se suben a git. Van en variables
> de entorno (`.gitignore` ya bloquea `.env`). Ver `.env.ejemplo`.

### 3.3 Activar S3 en el backend

```bash
export ALMACENAMIENTO=s3
export S3_BUCKET=nombre-del-bucket
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
cd backend && ./mvnw spring-boot:run
```

`GET /api/salud` debe responder `"almacenamiento": "AWS S3 (bucket ...)"`.
Después se sube una foto desde la pantalla **Productos** y debe aparecer en el bucket.

### 3.4 CORS del bucket

Solo hace falta si se usa **bucket público** en vez de URLs firmadas. En
**S3 → Permissions → CORS**:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET"],
    "AllowedOrigins": ["https://tu-frontend.vercel.app"],
    "ExposeHeaders": []
  }
]
```

Y en el backend `S3_URLS_FIRMADAS=false`.

---

## 4. Desplegar en la web

### 4.1 Base de datos

Crear una base MySQL o PostgreSQL en Railway, Aiven, Supabase o AWS RDS. Las
tablas se crean solas al arrancar el backend (`ddl-auto=update`); si se prefiere
cargarlas a mano está `bd/facturacion.sql`.

### 4.2 Backend (Render, Railway o Fly.io)

- Comando de build: `cd backend && ./mvnw clean package -DskipTests`
- Comando de arranque: `java -jar backend/target/backend-1.0.0.jar`
- Variables de entorno: las de `.env.ejemplo`.

`PORT` lo inyecta la plataforma y el backend ya lo respeta.

### 4.3 Frontend (Vercel o Netlify)

1. Poner la URL real de la API en `frontend/src/environments/environment.prod.ts`.
2. Build: `cd frontend && npm run build`
3. Carpeta publicada: `frontend/dist/frontend/browser`
4. Como es una SPA, redirigir todas las rutas a `index.html`
   (en Vercel es automático; en Netlify se agrega `/* /index.html 200`).

### 4.4 Cerrar el CORS

Con el frontend ya desplegado, poner su dominio en el backend:

```
CORS_ORIGENES=https://tu-frontend.vercel.app
URL_PUBLICA=https://tu-api.onrender.com
```

---

## 5. La API

| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/salud` | Comprobar el despliegue y qué almacenamiento está activo |
| GET | `/api/clientes?texto=` | Listar o buscar clientes |
| GET | `/api/clientes/nit/{nit}` | Buscar por NIT exacto |
| POST | `/api/clientes` | Registrar cliente (JSON) |
| GET | `/api/productos?texto=` | Catálogo, con la URL de la imagen ya resuelta |
| POST | `/api/productos` | Alta **con imagen** (`multipart/form-data`) |
| PUT | `/api/productos/{id}` | Edición, imagen opcional |
| DELETE | `/api/productos/{id}` | Baja (falla si el producto ya se facturó) |
| POST | `/api/facturas` | Emitir factura |
| GET | `/api/facturas?nit=&desde=&hasta=` | Consulta con filtros |
| GET | `/api/tipo-cambio?refrescar=` | Tipo de cambio del día |
| GET | `/api/tipo-cambio/demo` | XML SOAP enviado y recibido |

Ejemplo de emisión:

```bash
curl -X POST http://localhost:8080/api/facturas \
  -H 'Content-Type: application/json' \
  -d '{"clienteId":1,"metodoPago":"Tarjeta","items":[{"productoId":1,"cantidad":2}]}'
```

---

## 6. Decisiones de diseño que vale la pena poder explicar

**¿Por qué el backend no recibe los precios?**
`POST /api/facturas` solo acepta `productoId` y `cantidad`. El precio se vuelve a
leer de la base de datos en `FacturaServicio`. Si se recibiera del navegador,
cualquiera podría cambiarlo desde las herramientas de desarrollador.

**¿Por qué se guarda la clave del objeto y no la URL?**
Con bucket privado la URL va firmada y vence en una hora. Si se guardara en la
base de datos, los enlaces quedarían rotos. La clave (`productos/abc.jpg`) no
cambia nunca y la URL se arma al momento de leer.

**¿Por qué el tipo de cambio se copia dentro de la factura?**
Porque el Banguat da un valor distinto cada día. La factura debe conservar para
siempre el valor con el que realmente se cobró.

**¿Qué pasa si el Banguat no responde?**
`BanguatServicio` usa tres niveles: la cache del día en base de datos, luego el
Web Service, y si falla, el último valor guardado. El sistema nunca deja de
facturar y la pantalla dice de dónde salió el dato.

**¿Qué es `@Transactional` y por qué importa aquí?**
Emitir una factura escribe el encabezado, el detalle y descuenta el stock. Si algo
falla a medias, Spring deshace todo: no quedan facturas incompletas ni inventario
descontado de más. Está probado con el caso de stock insuficiente en la segunda línea.

**¿Cómo se cambia de disco local a S3?**
`AlmacenamientoImagenes` es una interfaz con dos implementaciones. Se elige con la
propiedad `app.almacenamiento`. Ninguna otra clase del sistema sabe dónde está
guardada la imagen.

---

## 7. Datos de prueba

**Clientes:** `1234567-8` (Juan Pérez), `9876543-2` (María López), `CF` (Consumidor Final)

**Productos:** `P001-AZ`, `P023-LC`, `P105-PH`, `P210-AC`, `P330-FR`
