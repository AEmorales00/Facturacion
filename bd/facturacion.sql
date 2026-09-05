-- ============================================================
--  BASE DE DATOS: Sistema POS - Facturacion
--  Curso: Desarrollo Web
--  Ejecutar:  mysql -u root -p < bd/facturacion.sql
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
    telefono  VARCHAR(20)  DEFAULT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 2. PRODUCTOS  (los precios se guardan en dolares)
-- ------------------------------------------------------------
CREATE TABLE productos (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    codigo      VARCHAR(20)  NOT NULL UNIQUE,
    descripcion VARCHAR(100) NOT NULL,
    precio_usd  DECIMAL(10,2) NOT NULL,
    stock       INT NOT NULL DEFAULT 0
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
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    factura_id         INT NOT NULL,
    producto_id        INT NOT NULL,
    cantidad           INT NOT NULL,
    precio_unitario_usd DECIMAL(10,2) NOT NULL,
    subtotal_usd       DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_factura  FOREIGN KEY (factura_id)  REFERENCES facturas(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- ------------------------------------------------------------
-- 5. TIPO DE CAMBIO (guarda lo que responde el Web Service del
--    Banco de Guatemala, para no consultarlo a cada rato y para
--    poder trabajar aunque el WS este caido)
-- ------------------------------------------------------------
CREATE TABLE tipo_cambio (
    fecha        DATE PRIMARY KEY,
    referencia   DECIMAL(10,5) NOT NULL,
    origen       VARCHAR(20) NOT NULL DEFAULT 'Banguat WS',
    consultado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
--  DATOS DE PRUEBA
-- ============================================================
INSERT INTO clientes (nit, nombre, direccion, telefono) VALUES
('1234567-8', 'Juan Perez',        'Ciudad, Guatemala',        '5555-1111'),
('9876543-2', 'Maria Lopez',       'Mixco, Guatemala',         '5555-2222'),
('CF',        'Consumidor Final',  'Ciudad, Guatemala',        NULL);

INSERT INTO productos (codigo, descripcion, precio_usd, stock) VALUES
('P001-AZ', 'Arroz Gold 2lb',       2.50, 100),
('P023-LC', 'Leche Entera 1L',      3.10,  80),
('P105-PH', 'Papel Higienico 4pk',  4.25,  50),
('P210-AC', 'Aceite Vegetal 1L',    5.75,  40),
('P330-FR', 'Frijol Negro 1lb',     1.90, 120);

-- Valor de respaldo por si el WS del Banguat no responde el dia de la demo
INSERT INTO tipo_cambio (fecha, referencia, origen) VALUES
(CURDATE(), 7.80000, 'Respaldo');
