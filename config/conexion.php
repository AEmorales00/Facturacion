<?php
/* ============================================================
   CONEXION A LA BASE DE DATOS
   Aqui se cambian el usuario y la contrasena de MySQL.
   ============================================================ */

$HOST   = 'localhost';
$BD     = 'facturacion';
$USUARIO = 'root';
$CLAVE   = '';          // en XAMPP normalmente va vacia

// Para que los decimales salgan limpios en JSON (7.62635 y no 7.6263500000001)
ini_set('serialize_precision', -1);

// El IVA en Guatemala es del 12%
define('IVA', 0.12);

try {
    // PDO es la forma recomendada de conectarse a MySQL desde PHP
    $conexion = new PDO(
        "mysql:host=$HOST;dbname=$BD;charset=utf8mb4",
        $USUARIO,
        $CLAVE
    );
    // Que los errores se muestren como excepciones (mas facil de depurar)
    $conexion->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    // Que los resultados vengan como arreglos asociativos: $fila['nombre']
    $conexion->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
} catch (PDOException $e) {
    die('Error de conexion a la base de datos: ' . $e->getMessage());
}

/* Funcion de ayuda: responde en formato JSON y termina el script.
   La usan los archivos de la carpeta /api */
function responder_json($datos) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($datos);
    exit;
}
