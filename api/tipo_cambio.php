<?php
/* Devuelve el tipo de cambio del dia (USD -> GTQ).
   Si se llama con ?refrescar=1 obliga a consultar de nuevo al Banguat. */

require '../config/conexion.php';
require '../includes/banguat.php';

if (isset($_GET['refrescar'])) {
    // Borramos el dato de hoy para forzar una nueva consulta al Web Service
    $conexion->exec("DELETE FROM tipo_cambio WHERE fecha = CURDATE() AND origen <> 'Respaldo'");
}

$tipo_cambio = obtener_tipo_cambio($conexion);

responder_json(array(
    'ok'         => true,
    'referencia' => round($tipo_cambio['referencia'], 5),
    'origen'     => $tipo_cambio['origen']
));
