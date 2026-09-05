<?php
/* ============================================================
   CONSULTA DE FACTURAS REGISTRADAS
   Aqui se demuestra el REQUISITO 3: query en la BD sobre el
   registro de datos (con JOIN entre 3 tablas y filtros).
   ============================================================ */
require 'config/conexion.php';

// --- Filtros que llegan por la URL (metodo GET) ---
$desde  = isset($_GET['desde'])  ? $_GET['desde']  : date('Y-m-01');  // 1ro del mes
$hasta  = isset($_GET['hasta'])  ? $_GET['hasta']  : date('Y-m-d');   // hoy
$buscar = isset($_GET['buscar']) ? trim($_GET['buscar']) : '';

/* ---------- CONSULTA PRINCIPAL ----------
   Une facturas + clientes y cuenta los productos de cada factura */
$sql = "SELECT  f.id,
                f.fecha,
                f.metodo_pago,
                f.tipo_cambio,
                f.subtotal_usd,
                f.iva_usd,
                f.total_usd,
                f.total_gtq,
                c.nit,
                c.nombre,
                (SELECT SUM(d.cantidad)
                   FROM factura_detalle d
                  WHERE d.factura_id = f.id) AS articulos
        FROM    facturas f
        INNER JOIN clientes c ON c.id = f.cliente_id
        WHERE   DATE(f.fecha) BETWEEN :desde AND :hasta
          AND  (c.nombre LIKE :buscar OR c.nit LIKE :buscar2 OR :vacio = '')
        ORDER BY f.id DESC";

$sentencia = $conexion->prepare($sql);
$sentencia->execute(array(
    ':desde'  => $desde,
    ':hasta'  => $hasta,
    ':buscar' => '%' . $buscar . '%',
    ':buscar2'=> '%' . $buscar . '%',
    ':vacio'  => $buscar
));
$facturas = $sentencia->fetchAll();

/* ---------- CONSULTA DE RESUMEN ----------
   Usa los mismos filtros que el listado, para que los numeros
   de arriba siempre cuadren con las filas de abajo. */
$sql_resumen = "SELECT COUNT(*)                    AS cantidad,
                       IFNULL(SUM(f.total_usd), 0)   AS total_usd,
                       IFNULL(SUM(f.total_gtq), 0)   AS total_gtq,
                       IFNULL(AVG(f.tipo_cambio), 0) AS cambio_promedio
                FROM   facturas f
                INNER JOIN clientes c ON c.id = f.cliente_id
                WHERE  DATE(f.fecha) BETWEEN :desde AND :hasta
                  AND (c.nombre LIKE :buscar OR c.nit LIKE :buscar2 OR :vacio = '')";
$sentencia = $conexion->prepare($sql_resumen);
$sentencia->execute(array(
    ':desde'  => $desde,
    ':hasta'  => $hasta,
    ':buscar' => '%' . $buscar . '%',
    ':buscar2'=> '%' . $buscar . '%',
    ':vacio'  => $buscar
));
$resumen = $sentencia->fetch();

/* ---------- DETALLE DE UNA FACTURA (si se hizo clic en "Ver") ---------- */
$detalle = array();
$factura_vista = isset($_GET['ver']) ? (int) $_GET['ver'] : 0;

if ($factura_vista > 0) {
    $sql_detalle = "SELECT p.codigo, p.descripcion,
                           d.cantidad, d.precio_unitario_usd, d.subtotal_usd
                    FROM   factura_detalle d
                    INNER JOIN productos p ON p.id = d.producto_id
                    WHERE  d.factura_id = :id";
    $sentencia = $conexion->prepare($sql_detalle);
    $sentencia->execute(array(':id' => $factura_vista));
    $detalle = $sentencia->fetchAll();
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Facturas registradas</title>
    <link rel="stylesheet" href="css/estilos.css">
</head>
<body>

<div class="encabezado">
    <div><h1>FACTURAS REGISTRADAS</h1><div>Consulta sobre la base de datos</div></div>
    <div class="derecha"><strong>Fecha:</strong> <?php echo date('d/m/Y'); ?></div>
</div>

<div class="menu">
    <a href="index.php">Facturaci&oacute;n</a>
    <a href="facturas.php" class="activo">Facturas registradas</a>
    <a href="productos.php">Productos</a>
    <a href="demo_ws.php">Demo Web Service Banguat</a>
</div>

<!-- ---------- FILTROS ---------- -->
<div class="seccion">
    <h2>FILTROS DE B&Uacute;SQUEDA</h2>
    <form method="get" class="fila">
        <input type="date" name="desde" value="<?php echo htmlspecialchars($desde); ?>">
        <input type="date" name="hasta" value="<?php echo htmlspecialchars($hasta); ?>">
        <input type="text" name="buscar" class="ancho-total" placeholder="Nombre o NIT del cliente"
               value="<?php echo htmlspecialchars($buscar); ?>">
        <button type="submit" class="boton boton-azul">Buscar</button>
        <a href="facturas.php" class="boton boton-gris" style="text-decoration:none;">Limpiar</a>
    </form>
</div>

<!-- ---------- RESUMEN ---------- -->
<div class="seccion">
    <h2>RESUMEN DEL PERIODO</h2>
    <div class="fila">
        <div class="caja-cambio"><h3><?php echo $resumen['cantidad']; ?></h3><div class="fuente">Facturas emitidas</div></div>
        <div class="caja-cambio"><h3>$ <?php echo number_format($resumen['total_usd'], 2); ?></h3><div class="fuente">Total en d&oacute;lares</div></div>
        <div class="caja-cambio"><h3>Q <?php echo number_format($resumen['total_gtq'], 2); ?></h3><div class="fuente">Total en quetzales</div></div>
        <div class="caja-cambio"><h3><?php echo number_format($resumen['cambio_promedio'], 5); ?></h3><div class="fuente">Tipo de cambio promedio</div></div>
    </div>
</div>

<!-- ---------- LISTADO ---------- -->
<div class="seccion">
    <h2>LISTADO DE FACTURAS</h2>
    <table>
        <thead>
            <tr>
                <th>No.</th><th>Fecha</th><th>NIT</th><th>Cliente</th><th>Pago</th>
                <th class="centro">Art&iacute;culos</th><th>T. Cambio</th>
                <th>Total $</th><th>Total Q</th><th>Ver</th>
            </tr>
        </thead>
        <tbody>
        <?php if (empty($facturas)) { ?>
            <tr><td colspan="10" class="vacio">No hay facturas en este periodo</td></tr>
        <?php } else { ?>
            <?php foreach ($facturas as $factura) { ?>
            <tr>
                <td><?php echo $factura['id']; ?></td>
                <td><?php echo date('d/m/Y H:i', strtotime($factura['fecha'])); ?></td>
                <td><?php echo htmlspecialchars($factura['nit']); ?></td>
                <td><?php echo htmlspecialchars($factura['nombre']); ?></td>
                <td><?php echo htmlspecialchars($factura['metodo_pago']); ?></td>
                <td class="centro"><?php echo $factura['articulos']; ?></td>
                <td class="derecha"><?php echo number_format($factura['tipo_cambio'], 5); ?></td>
                <td class="derecha">$ <?php echo number_format($factura['total_usd'], 2); ?></td>
                <td class="derecha">Q <?php echo number_format($factura['total_gtq'], 2); ?></td>
                <td class="centro"><a href="facturas.php?ver=<?php echo $factura['id']; ?>">Ver</a></td>
            </tr>
            <?php } ?>
        <?php } ?>
        </tbody>
    </table>
</div>

<!-- ---------- DETALLE DE UNA FACTURA ---------- -->
<?php if ($factura_vista > 0) { ?>
<div class="seccion">
    <h2>DETALLE DE LA FACTURA No. <?php echo $factura_vista; ?></h2>
    <table>
        <thead>
            <tr><th>C&oacute;digo</th><th>Descripci&oacute;n</th><th class="centro">Cant.</th>
                <th>Precio Unit. ($)</th><th>Subtotal ($)</th></tr>
        </thead>
        <tbody>
        <?php foreach ($detalle as $linea) { ?>
            <tr>
                <td><?php echo htmlspecialchars($linea['codigo']); ?></td>
                <td><?php echo htmlspecialchars($linea['descripcion']); ?></td>
                <td class="centro"><?php echo $linea['cantidad']; ?></td>
                <td class="derecha">$ <?php echo number_format($linea['precio_unitario_usd'], 2); ?></td>
                <td class="derecha">$ <?php echo number_format($linea['subtotal_usd'], 2); ?></td>
            </tr>
        <?php } ?>
        </tbody>
    </table>
</div>
<?php } ?>

</body>
</html>
