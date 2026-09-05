<?php
/* ============================================================
   GUARDA LA FACTURA EN LA BASE DE DATOS
   ------------------------------------------------------------
   Recibe por POST:
     cliente_id  -> id del cliente
     metodo_pago -> Efectivo / Tarjeta / Transferencia
     items       -> JSON: [{"producto_id":1,"cantidad":2}, ...]

   Importante: los precios NO se toman de lo que manda el navegador,
   se vuelven a leer de la BD. Asi nadie puede cambiar el precio
   desde el formulario.
   ============================================================ */

require '../config/conexion.php';
require '../includes/banguat.php';

$cliente_id  = isset($_POST['cliente_id'])  ? (int) $_POST['cliente_id'] : 0;
$metodo_pago = isset($_POST['metodo_pago']) ? $_POST['metodo_pago']      : 'Efectivo';
$items       = isset($_POST['items'])       ? json_decode($_POST['items'], true) : array();

if ($cliente_id <= 0) {
    responder_json(array('ok' => false, 'mensaje' => 'Debe seleccionar un cliente'));
}
if (empty($items)) {
    responder_json(array('ok' => false, 'mensaje' => 'La factura no tiene productos'));
}

// Tipo de cambio del dia (viene del Web Service del Banguat)
$tipo_cambio = obtener_tipo_cambio($conexion);
$referencia  = $tipo_cambio['referencia'];

try {
    // Una transaccion: o se guarda todo, o no se guarda nada
    $conexion->beginTransaction();

    // ---- 1. Recalculamos los totales leyendo los precios de la BD ----
    $subtotal_usd = 0;
    $lineas = array();

    $buscar_producto = $conexion->prepare(
        "SELECT id, precio_usd FROM productos WHERE id = :id"
    );

    foreach ($items as $item) {
        $producto_id = (int) $item['producto_id'];
        $cantidad    = (int) $item['cantidad'];

        if ($cantidad <= 0) {
            continue;
        }

        $buscar_producto->execute(array(':id' => $producto_id));
        $producto = $buscar_producto->fetch();

        if (!$producto) {
            throw new Exception('Producto invalido en el detalle');
        }

        $linea_subtotal = $producto['precio_usd'] * $cantidad;
        $subtotal_usd  += $linea_subtotal;

        $lineas[] = array(
            'producto_id' => $producto_id,
            'cantidad'    => $cantidad,
            'precio'      => $producto['precio_usd'],
            'subtotal'    => $linea_subtotal
        );
    }

    if (empty($lineas)) {
        throw new Exception('La factura no tiene productos validos');
    }

    $iva_usd   = round($subtotal_usd * IVA, 2);
    $total_usd = round($subtotal_usd + $iva_usd, 2);
    $total_gtq = round($total_usd * $referencia, 2);   // <-- aqui se usa el Banguat

    // ---- 2. Guardamos el encabezado de la factura ----
    $sql = "INSERT INTO facturas
              (cliente_id, fecha, metodo_pago, tipo_cambio,
               subtotal_usd, iva_usd, total_usd, total_gtq)
            VALUES
              (:cliente_id, NOW(), :metodo_pago, :tipo_cambio,
               :subtotal, :iva, :total_usd, :total_gtq)";
    $sentencia = $conexion->prepare($sql);
    $sentencia->execute(array(
        ':cliente_id'  => $cliente_id,
        ':metodo_pago' => $metodo_pago,
        ':tipo_cambio' => $referencia,
        ':subtotal'    => round($subtotal_usd, 2),
        ':iva'         => $iva_usd,
        ':total_usd'   => $total_usd,
        ':total_gtq'   => $total_gtq
    ));

    $factura_id = $conexion->lastInsertId();

    // ---- 3. Guardamos el detalle y descontamos el stock ----
    $insertar_detalle = $conexion->prepare(
        "INSERT INTO factura_detalle
           (factura_id, producto_id, cantidad, precio_unitario_usd, subtotal_usd)
         VALUES (:factura_id, :producto_id, :cantidad, :precio, :subtotal)"
    );
    $descontar_stock = $conexion->prepare(
        "UPDATE productos SET stock = stock - :cantidad WHERE id = :producto_id"
    );

    foreach ($lineas as $linea) {
        $insertar_detalle->execute(array(
            ':factura_id'  => $factura_id,
            ':producto_id' => $linea['producto_id'],
            ':cantidad'    => $linea['cantidad'],
            ':precio'      => $linea['precio'],
            ':subtotal'    => $linea['subtotal']
        ));
        $descontar_stock->execute(array(
            ':cantidad'    => $linea['cantidad'],
            ':producto_id' => $linea['producto_id']
        ));
    }

    $conexion->commit();

    responder_json(array(
        'ok'          => true,
        'factura_id'  => $factura_id,
        'tipo_cambio' => $referencia,
        'total_usd'   => $total_usd,
        'total_gtq'   => $total_gtq
    ));

} catch (Exception $e) {
    $conexion->rollBack();   // si algo salio mal, deshacemos todo
    responder_json(array('ok' => false, 'mensaje' => 'Error al guardar: ' . $e->getMessage()));
}
