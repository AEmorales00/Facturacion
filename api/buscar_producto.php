<?php
/* Busca un producto por codigo o por parte de la descripcion.
   Ejemplo: api/buscar_producto.php?texto=P001-AZ
            api/buscar_producto.php?texto=arroz */

require '../config/conexion.php';

$texto = isset($_GET['texto']) ? trim($_GET['texto']) : '';

if ($texto === '') {
    responder_json(array('ok' => false, 'mensaje' => 'Escriba un codigo o nombre'));
}

// CONSULTA A LA BD: primero intenta por codigo exacto, si no por descripcion
$sql = "SELECT id, codigo, descripcion, precio_usd, stock
        FROM productos
        WHERE codigo = :exacto OR descripcion LIKE :parcial
        ORDER BY (codigo = :exacto2) DESC
        LIMIT 1";
$sentencia = $conexion->prepare($sql);
$sentencia->execute(array(
    ':exacto'  => $texto,
    ':exacto2' => $texto,
    ':parcial' => '%' . $texto . '%'
));
$producto = $sentencia->fetch();

if (!$producto) {
    responder_json(array('ok' => false, 'mensaje' => 'Producto no encontrado'));
}

responder_json(array('ok' => true, 'producto' => $producto));
