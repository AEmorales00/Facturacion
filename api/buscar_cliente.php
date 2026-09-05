<?php
/* Busca un cliente por su NIT.  Se llama desde js/app.js
   Ejemplo: api/buscar_cliente.php?nit=1234567-8 */

require '../config/conexion.php';

$nit = isset($_GET['nit']) ? trim($_GET['nit']) : '';

if ($nit === '') {
    responder_json(array('ok' => false, 'mensaje' => 'Escriba un NIT'));
}

// CONSULTA A LA BD (con parametro, para evitar inyeccion SQL)
$sql = "SELECT id, nit, nombre, direccion FROM clientes WHERE nit = :nit";
$sentencia = $conexion->prepare($sql);
$sentencia->execute(array(':nit' => $nit));
$cliente = $sentencia->fetch();

if (!$cliente) {
    responder_json(array('ok' => false, 'mensaje' => 'Cliente no encontrado'));
}

responder_json(array('ok' => true, 'cliente' => $cliente));
