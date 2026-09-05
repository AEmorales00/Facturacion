<?php
/* ============================================================
   DEMO DEL WEB SERVICE DEL BANCO DE GUATEMALA
   ------------------------------------------------------------
   Esta pantalla es para el REQUISITO 4: explicar en clase, paso
   a paso, como se conecta el formulario con el WS del Banguat.
   ============================================================ */
require 'config/conexion.php';
require 'includes/banguat.php';

// Llamamos al Web Service en vivo (sin usar la copia de la BD)
$respuesta = consultar_banguat();

// Y vemos que tenemos guardado en la base de datos
$historial = $conexion->query(
    "SELECT fecha, referencia, origen, consultado_en
     FROM tipo_cambio ORDER BY fecha DESC LIMIT 10"
)->fetchAll();

$peticion_ejemplo = '<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <TipoCambioDia xmlns="http://www.banguat.gob.gt/variables/ws/" />
  </soap:Body>
</soap:Envelope>';
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Demo Web Service Banguat</title>
    <link rel="stylesheet" href="css/estilos.css">
</head>
<body>

<div class="encabezado">
    <div><h1>DEMO: WEB SERVICE DEL BANGUAT</h1>
         <div>C&oacute;mo obtiene el sistema el tipo de cambio</div></div>
    <div class="derecha">
        <strong>Estado:</strong>
        <span class="<?php echo $respuesta['ok'] ? 'en-linea' : 'sin-linea'; ?>">
            &bull; <?php echo $respuesta['ok'] ? 'WS respondiendo' : 'WS sin respuesta'; ?>
        </span>
    </div>
</div>

<div class="menu">
    <a href="index.php">Facturaci&oacute;n</a>
    <a href="facturas.php">Facturas registradas</a>
    <a href="productos.php">Productos</a>
    <a href="demo_ws.php" class="activo">Demo Web Service Banguat</a>
</div>

<div class="seccion">

    <!-- PASO 1 -->
    <div class="paso">
        <h3>Paso 1 &mdash; A qui&eacute;n le preguntamos</h3>
        <p>
            El Banco de Guatemala publica un servicio web (SOAP) con las variables
            econ&oacute;micas. Nosotros usamos el m&eacute;todo <strong>TipoCambioDia</strong>.
        </p>
        <p>
            URL: <code>https://www.banguat.gob.gt/variables/ws/TipoCambio.asmx</code><br>
            Archivo del proyecto: <code>includes/banguat.php</code>
        </p>
        <p>
            <strong>Detalle importante:</strong> la direcci&oacute;n tiene que ir con
            <code>https</code>. Si se usa <code>http</code>, el servidor del Banguat
            contesta un redireccionamiento (c&oacute;digo 302) en lugar del XML y el
            sistema no encuentra el tipo de cambio.
        </p>
    </div>

    <!-- PASO 2 -->
    <div class="paso">
        <h3>Paso 2 &mdash; Lo que le enviamos (XML de petici&oacute;n)</h3>
        <p>PHP arma este XML y lo env&iacute;a por POST con cURL:</p>
        <div class="bloque-xml"><?php echo htmlspecialchars($peticion_ejemplo); ?></div>
    </div>

    <!-- PASO 3 -->
    <div class="paso">
        <h3>Paso 3 &mdash; Lo que nos responde el Banguat (XML crudo)</h3>
        <?php if ($respuesta['ok']) { ?>
            <div class="bloque-xml"><?php echo htmlspecialchars($respuesta['xml']); ?></div>
        <?php } else { ?>
            <div class="mensaje mensaje-error">
                No se pudo contactar al Web Service: <?php echo htmlspecialchars($respuesta['error']); ?><br>
                El sistema sigue funcionando con el &uacute;ltimo valor guardado en la base de datos.
            </div>
        <?php } ?>
    </div>

    <!-- PASO 4 -->
    <div class="paso">
        <h3>Paso 4 &mdash; El dato que nos interesa</h3>
        <?php if ($respuesta['ok']) { ?>
            <div class="caja-cambio" style="max-width:420px;">
                <h3>Tipo de cambio de referencia</h3>
                <div class="valor">1 USD = <strong>Q <?php echo number_format($respuesta['referencia'], 5); ?></strong></div>
                <div class="fuente">Fecha reportada por el Banguat: <?php echo htmlspecialchars($respuesta['fecha']); ?></div>
            </div>
            <p>
                De todo el XML solo tomamos la etiqueta <code>&lt;referencia&gt;</code>,
                con la instrucci&oacute;n <code>$xml-&gt;xpath('//referencia')</code>.
            </p>
        <?php } else { ?>
            <p>Sin respuesta del servicio.</p>
        <?php } ?>
    </div>

    <!-- PASO 5 -->
    <div class="paso">
        <h3>Paso 5 &mdash; C&oacute;mo se usa en la factura</h3>
        <?php
            $referencia = $respuesta['ok'] ? $respuesta['referencia'] : 7.80;
            $ejemplo_sub = 12.35;
            $ejemplo_iva = round($ejemplo_sub * IVA, 2);
            $ejemplo_usd = $ejemplo_sub + $ejemplo_iva;
            $ejemplo_gtq = round($ejemplo_usd * $referencia, 2);
        ?>
        <table style="max-width:520px;">
            <tr><td>Sub-Total</td>            <td class="derecha">$ <?php echo number_format($ejemplo_sub, 2); ?></td></tr>
            <tr><td>IVA 12%</td>              <td class="derecha">$ <?php echo number_format($ejemplo_iva, 2); ?></td></tr>
            <tr><td><strong>Total USD</strong></td><td class="derecha"><strong>$ <?php echo number_format($ejemplo_usd, 2); ?></strong></td></tr>
            <tr><td><strong>Total GTQ</strong></td>
                <td class="derecha"><strong>Q <?php echo number_format($ejemplo_gtq, 2); ?></strong></td></tr>
        </table>
        <p>
            La f&oacute;rmula es: <code>Total GTQ = Total USD &times; <?php echo number_format($referencia, 5); ?></code><br>
            En el c&oacute;digo est&aacute; en <code>api/guardar_factura.php</code> (servidor) y en
            <code>js/app.js</code>, funci&oacute;n <code>calcularTotales()</code> (pantalla).
        </p>
    </div>

    <!-- PASO 6 -->
    <div class="paso">
        <h3>Paso 6 &mdash; Lo guardamos en la base de datos</h3>
        <p>
            Para no llamar al Web Service en cada venta, el valor del d&iacute;a se guarda
            en la tabla <code>tipo_cambio</code>. Si el WS se cae, el sistema usa el
            &uacute;ltimo valor guardado.
        </p>
        <table style="max-width:640px;">
            <thead><tr><th>Fecha</th><th>Referencia</th><th>Origen</th><th>Consultado</th></tr></thead>
            <tbody>
            <?php foreach ($historial as $fila) { ?>
                <tr>
                    <td><?php echo date('d/m/Y', strtotime($fila['fecha'])); ?></td>
                    <td class="derecha">Q <?php echo number_format($fila['referencia'], 5); ?></td>
                    <td><?php echo htmlspecialchars($fila['origen']); ?></td>
                    <td><?php echo date('d/m/Y H:i', strtotime($fila['consultado_en'])); ?></td>
                </tr>
            <?php } ?>
            </tbody>
        </table>
    </div>

</div>

<div style="text-align:center; margin-bottom:30px;">
    <a href="demo_ws.php" class="boton boton-azul" style="text-decoration:none;">Volver a llamar al Web Service</a>
</div>

</body>
</html>
