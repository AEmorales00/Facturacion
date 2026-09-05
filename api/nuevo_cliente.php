<?php
/* Registra un cliente nuevo.  Recibe los datos por POST. */

require '../config/conexion.php';

$nit       = isset($_POST['nit'])       ? trim($_POST['nit'])       : '';
$nombre    = isset($_POST['nombre'])    ? trim($_POST['nombre'])    : '';
$direccion = isset($_POST['direccion']) ? trim($_POST['direccion']) : 'Ciudad, Guatemala';
$telefono  = isset($_POST['telefono'])  ? trim($_POST['telefono'])  : '';

if ($nit === '' || $nombre === '') {
    responder_json(array('ok' => false, 'mensaje' => 'El NIT y el nombre son obligatorios'));
}

// Verificamos que el NIT no exista ya
$sentencia = $conexion->prepare("SELECT id FROM clientes WHERE nit = :nit");
$sentencia->execute(array(':nit' => $nit));

if ($sentencia->fetch()) {
    responder_json(array('ok' => false, 'mensaje' => 'Ese NIT ya esta registrado'));
}

// INSERT en la base de datos
$sql = "INSERT INTO clientes (nit, nombre, direccion, telefono)
        VALUES (:nit, :nombre, :direccion, :telefono)";
$sentencia = $conexion->prepare($sql);
$sentencia->execute(array(
    ':nit'       => $nit,
    ':nombre'    => $nombre,
    ':direccion' => $direccion,
    ':telefono'  => $telefono
));

responder_json(array(
    'ok'      => true,
    'cliente' => array(
        'id'        => $conexion->lastInsertId(),
        'nit'       => $nit,
        'nombre'    => $nombre,
        'direccion' => $direccion
    )
));
