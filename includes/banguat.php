<?php
/* ============================================================
   WEB SERVICE DEL BANCO DE GUATEMALA
   ------------------------------------------------------------
   URL      : http://www.banguat.gob.gt/variables/ws/TipoCambio.asmx
   Metodo   : TipoCambioDia  -> devuelve el tipo de cambio del dia
   Protocolo: SOAP 1.1 (se envia un XML y devuelve otro XML)

   Como funciona el archivo:
     1) consultar_banguat()      -> habla con el WS y devuelve el numero
     2) obtener_tipo_cambio($cx) -> primero busca en la BD; si no hay
                                    dato de hoy, llama al WS y lo guarda
   ============================================================ */

// Ojo: tiene que ser https. Si se usa http, el servidor del Banguat responde
// un redireccionamiento (302) y no el XML.
define('BANGUAT_URL',    'https://www.banguat.gob.gt/variables/ws/TipoCambio.asmx');
// Esto NO es una direccion web: es el nombre del metodo que pide SOAP.
// Por eso se queda con http, no se cambia.
define('BANGUAT_ACCION', 'http://www.banguat.gob.gt/variables/ws/TipoCambioDia');

/**
 * Llama al Web Service del Banguat y devuelve el tipo de cambio del dia.
 * Devuelve un arreglo: ['ok'=>true, 'referencia'=>7.66, 'fecha'=>'05/09/2026']
 * o ['ok'=>false, 'error'=>'...'] si algo falla.
 */
function consultar_banguat() {

    // ---- 1. Armamos el sobre (envelope) SOAP que pide el Banguat ----
    $peticion_xml = '<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:xsd="http://www.w3.org/2001/XMLSchema"
               xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <TipoCambioDia xmlns="http://www.banguat.gob.gt/variables/ws/" />
  </soap:Body>
</soap:Envelope>';

    // ---- 2. Enviamos la peticion con cURL ----
    $curl = curl_init(BANGUAT_URL);
    curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);   // que devuelva la respuesta
    curl_setopt($curl, CURLOPT_POST,           true);   // metodo POST
    curl_setopt($curl, CURLOPT_POSTFIELDS,     $peticion_xml);
    curl_setopt($curl, CURLOPT_TIMEOUT,        10);     // maximo 10 segundos
    curl_setopt($curl, CURLOPT_HTTPHEADER, array(
        'Content-Type: text/xml; charset=utf-8',
        'SOAPAction: "' . BANGUAT_ACCION . '"',
        'Content-Length: ' . strlen($peticion_xml)
    ));

    $respuesta_xml = curl_exec($curl);
    $error_curl    = curl_error($curl);
    curl_close($curl);

    if ($respuesta_xml === false || $error_curl !== '') {
        return array('ok' => false, 'error' => 'No se pudo conectar al Banguat: ' . $error_curl);
    }

    // ---- 3. Leemos el XML de respuesta ----
    // Le quitamos los prefijos "soap:" para que SimpleXML lo lea sin complicaciones
    $limpio = str_replace(array('soap:', 'xmlns='), array('', 'ns='), $respuesta_xml);
    $xml    = @simplexml_load_string($limpio);

    if ($xml === false) {
        return array('ok' => false, 'error' => 'La respuesta del Banguat no es un XML valido');
    }

    // La respuesta trae:  ...<VarDolar><fecha>..</fecha><referencia>7.66</referencia></VarDolar>
    $referencias = $xml->xpath('//referencia');
    $fechas      = $xml->xpath('//fecha');

    if (empty($referencias)) {
        return array('ok' => false, 'error' => 'El Banguat no devolvio tipo de cambio');
    }

    return array(
        'ok'         => true,
        'referencia' => (float) $referencias[0],
        'fecha'      => empty($fechas) ? date('d/m/Y') : (string) $fechas[0],
        'xml'        => $respuesta_xml   // se usa en la demo para mostrar el XML crudo
    );
}

/**
 * Devuelve el tipo de cambio que va a usar el sistema.
 * Estrategia:
 *   1. Si ya lo consultamos hoy, lo lee de la tabla tipo_cambio (mas rapido).
 *   2. Si no, llama al Web Service y lo guarda en la BD.
 *   3. Si el WS falla, usa el ultimo valor guardado (para que la demo no se caiga).
 */
function obtener_tipo_cambio($conexion) {

    // 1. Buscamos en la base de datos el dato de hoy
    $sql = "SELECT referencia, origen FROM tipo_cambio WHERE fecha = CURDATE()";
    $sentencia = $conexion->query($sql);
    $fila = $sentencia->fetch();

    if ($fila && $fila['origen'] === 'Banguat WS') {
        return array(
            'referencia' => (float) $fila['referencia'],
            'origen'     => 'Base de datos (consultado hoy al Banguat)'
        );
    }

    // 2. No hay dato de hoy: le preguntamos al Web Service
    $ws = consultar_banguat();

    if ($ws['ok']) {
        // Lo guardamos para no volver a preguntar hoy
        $sql = "INSERT INTO tipo_cambio (fecha, referencia, origen)
                VALUES (CURDATE(), :referencia, 'Banguat WS')
                ON DUPLICATE KEY UPDATE referencia = :referencia2, origen = 'Banguat WS'";
        $sentencia = $conexion->prepare($sql);
        $sentencia->execute(array(
            ':referencia'  => $ws['referencia'],
            ':referencia2' => $ws['referencia']
        ));

        return array(
            'referencia' => $ws['referencia'],
            'origen'     => 'Web Service Banguat'
        );
    }

    // 3. El WS fallo: usamos el ultimo valor que tengamos guardado
    $sql = "SELECT referencia FROM tipo_cambio ORDER BY fecha DESC LIMIT 1";
    $sentencia = $conexion->query($sql);
    $fila = $sentencia->fetch();

    return array(
        'referencia' => $fila ? (float) $fila['referencia'] : 7.80,
        'origen'     => 'Respaldo (el WS no respondio)'
    );
}
