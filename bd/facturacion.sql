-- ============================================================
--  BASE DE DATOS: Sistema de Facturacion (Proyecto 2)
--  Curso: Desarrollo Web
-- ------------------------------------------------------------
--  Este script es de REFERENCIA y para cargar la base en la nube.
--  El backend de Spring Boot crea y actualiza las tablas solo
--  (spring.jpa.hibernate.ddl-auto=update) a partir de las clases
--  @Entity, asi que no es obligatorio ejecutarlo.
--
--  Ejecutar en local:
--    mysql -u root -p < bd/facturacion.sql
--
--  Ejecutar en la nube (Railway, Aiven, RDS...):
--    mysql -h HOST -u USUARIO -p NOMBRE_BD < bd/facturacion.sql
--    (en ese caso borre las dos primeras instrucciones, porque el
--     proveedor ya le creo la base de datos)
-- ============================================================

DROP DATABASE IF EXISTS facturacion;
CREATE DATABASE facturacion CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE facturacion;

-- ------------------------------------------------------------
-- 1. CLIENTES
-- ------------------------------------------------------------
CREATE TABLE clientes (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nit       VARCHAR(20)  NOT NULL UNIQUE,
    nombre    VARCHAR(100) NOT NULL,
    direccion VARCHAR(150) DEFAULT 'Ciudad, Guatemala',
    telefono  VARCHAR(20)  DEFAULT NULL
);

-- ------------------------------------------------------------
-- 2. PRODUCTOS  (los precios se guardan en dolares)
-- ------------------------------------------------------------
--  imagen_clave es la novedad del Proyecto 2: guarda la "key" del
--  objeto dentro del bucket de AWS S3, por ejemplo
--  productos/8f3c1b2a-....jpg
--
--  Se guarda la clave y no la URL completa porque, si el bucket es
--  privado, la URL va firmada y vence a las pocas horas: guardarla
--  dejaria enlaces rotos. La clave no cambia nunca, y el backend
--  arma la URL al momento de leer el producto.
-- ------------------------------------------------------------
CREATE TABLE productos (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    codigo       VARCHAR(20)  NOT NULL UNIQUE,
    descripcion  VARCHAR(100) NOT NULL,
    precio_usd   DECIMAL(10,2) NOT NULL,
    stock        INT NOT NULL DEFAULT 0,
    imagen_clave VARCHAR(255) DEFAULT NULL
);

-- ------------------------------------------------------------
-- 3. FACTURAS (encabezado)
-- ------------------------------------------------------------
CREATE TABLE facturas (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id   INT NOT NULL,
    fecha        DATETIME NOT NULL,
    metodo_pago  VARCHAR(20)  NOT NULL DEFAULT 'Efectivo',
    tipo_cambio  DECIMAL(10,5) NOT NULL,   -- 1 USD = X GTQ (viene del Banguat)
    subtotal_usd DECIMAL(10,2) NOT NULL,
    iva_usd      DECIMAL(10,2) NOT NULL,
    total_usd    DECIMAL(10,2) NOT NULL,
    total_gtq    DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_factura_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

-- ------------------------------------------------------------
-- 4. DETALLE DE FACTURA (una fila por producto vendido)
-- ------------------------------------------------------------
CREATE TABLE factura_detalle (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    factura_id          INT NOT NULL,
    producto_id         INT NOT NULL,
    cantidad            INT NOT NULL,
    precio_unitario_usd DECIMAL(10,2) NOT NULL,
    subtotal_usd        DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_factura  FOREIGN KEY (factura_id)  REFERENCES facturas(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- ------------------------------------------------------------
-- 5. TIPO DE CAMBIO
--    Guarda lo que responde el Web Service del Banguat, para no
--    consultarlo a cada rato y para poder seguir facturando
--    aunque el servicio este caido.
-- ------------------------------------------------------------
CREATE TABLE tipo_cambio (
    fecha         DATE PRIMARY KEY,
    referencia    DECIMAL(10,5) NOT NULL,
    origen        VARCHAR(20) NOT NULL DEFAULT 'Banguat WS',
    consultado_en DATETIME NOT NULL
);

-- ============================================================
--  DATOS DE PRUEBA
--  El backend tambien los carga solo si las tablas estan vacias
--  (ver la clase DatosIniciales).
-- ============================================================

INSERT INTO clientes (nit, nombre, direccion, telefono) VALUES
('1234567-8', 'Juan Perez',       'Ciudad, Guatemala', '5555-1111'),
('9876543-2', 'Maria Lopez',      'Mixco, Guatemala',  '5555-2222'),
('CF',        'Consumidor Final', 'Ciudad, Guatemala', NULL);

-- Los productos se registran sin fotografia: la imagen se sube desde
-- la pantalla "Productos" del Angular y es el backend quien la manda
-- al bucket de S3 y llena imagen_clave.
INSERT INTO productos (codigo, descripcion, precio_usd, stock) VALUES
('P001-AZ', 'Arroz Gold 2lb',      2.50, 100),
('P023-LC', 'Leche Entera 1L',     3.10,  80),
('P105-PH', 'Papel Higienico 4pk', 4.25,  50),
('P210-AC', 'Aceite Vegetal 1L',   5.75,  40),
('P330-FR', 'Frijol Negro 1lb',    1.90, 120);

-- Valor de respaldo por si el WS del Banguat no responde el dia de la demo
INSERT INTO tipo_cambio (fecha, referencia, origen, consultado_en)
VALUES (CURDATE(), 7.80000, 'Respaldo', NOW());

-- ============================================================
--  CONSULTAS DE EJEMPLO PARA LA PRESENTACION
-- ============================================================

-- Facturas emitidas con su cliente y cuantos productos lleva cada una
-- SELECT f.id, f.fecha, c.nit, c.nombre,
--        f.tipo_cambio, f.total_usd, f.total_gtq,
--        COUNT(d.id) AS lineas
-- FROM facturas f
-- JOIN clientes c         ON c.id = f.cliente_id
-- LEFT JOIN factura_detalle d ON d.factura_id = f.id
-- GROUP BY f.id, f.fecha, c.nit, c.nombre, f.tipo_cambio, f.total_usd, f.total_gtq
-- ORDER BY f.fecha DESC;

-- Productos mas vendidos
-- SELECT p.codigo, p.descripcion, SUM(d.cantidad) AS vendidos
-- FROM factura_detalle d
-- JOIN productos p ON p.id = d.producto_id
-- GROUP BY p.codigo, p.descripcion
-- ORDER BY vendidos DESC;

-- Cuantos productos del catalogo ya tienen su fotografia en S3
-- SELECT
--   COUNT(*)                                            AS total,
--   SUM(CASE WHEN imagen_clave IS NOT NULL THEN 1 ELSE 0 END) AS con_foto
-- FROM productos;
