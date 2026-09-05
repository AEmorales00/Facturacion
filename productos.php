<?php
/* ============================================================
   CATALOGO DE PRODUCTOS
   Muestra el precio en dolares y su equivalente en quetzales
   usando el tipo de cambio del Banguat.
   ============================================================ */
require 'config/conexion.php';
require 'includes/banguat.php';

$tipo_cambio = obtener_tipo_cambio($conexion);
$mensaje = '';

// --- Registrar un producto nuevo ---
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $codigo      = trim($_POST['codigo']);
    $descripcion = trim($_POST['descripcion']);
    $precio      = (float) $_POST['precio_usd'];
    $stock       = (int) $_POST['stock'];

    if ($codigo === '' || $descripcion === '' || $precio <= 0) {
        $mensaje = '<div class="mensaje mensaje-error">Complete el c&oacute;digo, la descripci&oacute;n y un precio mayor a cero.</div>';
    } else {
        $sentencia = $conexion->prepare("SELECT id FROM productos WHERE codigo = :codigo");
        $sentencia->execute(array(':codigo' => $codigo));

        if ($sentencia->fetch()) {
            $mensaje = '<div class="mensaje mensaje-error">Ese c&oacute;digo ya existe.</div>';
        } else {
            $sql = "INSERT INTO productos (codigo, descripcion, precio_usd, stock)
                    VALUES (:codigo, :descripcion, :precio, :stock)";
            $sentencia = $conexion->prepare($sql);
            $sentencia->execute(array(
                ':codigo'      => $codigo,
                ':descripcion' => $descripcion,
                ':precio'      => $precio,
                ':stock'       => $stock
            ));
            $mensaje = '<div class="mensaje mensaje-ok">Producto registrado correctamente.</div>';
        }
    }
}

// --- CONSULTA: catalogo con las ventas de cada producto ---
$sql = "SELECT  p.id, p.codigo, p.descripcion, p.precio_usd, p.stock,
                IFNULL(SUM(d.cantidad), 0) AS vendidos
        FROM    productos p
        LEFT JOIN factura_detalle d ON d.producto_id = p.id
        GROUP BY p.id, p.codigo, p.descripcion, p.precio_usd, p.stock
        ORDER BY p.codigo";
$productos = $conexion->query($sql)->fetchAll();
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Productos</title>
    <link rel="stylesheet" href="css/estilos.css">
</head>
<body>

<div class="encabezado">
    <div><h1>CAT&Aacute;LOGO DE PRODUCTOS</h1>
         <div>Precios convertidos con el tipo de cambio del Banguat</div></div>
    <div class="derecha">
        <strong>1 USD = Q <?php echo number_format($tipo_cambio['referencia'], 5); ?></strong><br>
        <?php echo $tipo_cambio['origen']; ?>
    </div>
</div>

<div class="menu">
    <a href="index.php">Facturaci&oacute;n</a>
    <a href="facturas.php">Facturas registradas</a>
    <a href="productos.php" class="activo">Productos</a>
    <a href="demo_ws.php">Demo Web Service Banguat</a>
</div>

<?php echo $mensaje; ?>

<div class="seccion">
    <h2>REGISTRAR PRODUCTO</h2>
    <form method="post" class="fila">
        <input type="text"   name="codigo"      placeholder="C&oacute;digo (ej. P400-SL)" required>
        <input type="text"   name="descripcion" class="ancho-total" placeholder="Descripci&oacute;n" required>
        <input type="number" name="precio_usd"  step="0.01" min="0.01" placeholder="Precio $" required>
        <input type="number" name="stock"       min="0" value="0" placeholder="Stock">
        <button type="submit" class="boton boton-verde">Guardar</button>
    </form>
</div>

<div class="seccion">
    <h2>PRODUCTOS EN EL SISTEMA</h2>
    <table>
        <thead>
            <tr><th>C&oacute;digo</th><th>Descripci&oacute;n</th>
                <th>Precio ($)</th><th>Precio (Q)</th>
                <th class="centro">Stock</th><th class="centro">Vendidos</th></tr>
        </thead>
        <tbody>
        <?php foreach ($productos as $producto) { ?>
            <tr>
                <td><?php echo htmlspecialchars($producto['codigo']); ?></td>
                <td><?php echo htmlspecialchars($producto['descripcion']); ?></td>
                <td class="derecha">$ <?php echo number_format($producto['precio_usd'], 2); ?></td>
                <td class="derecha">Q <?php echo number_format($producto['precio_usd'] * $tipo_cambio['referencia'], 2); ?></td>
                <td class="centro"><?php echo $producto['stock']; ?></td>
                <td class="centro"><?php echo $producto['vendidos']; ?></td>
            </tr>
        <?php } ?>
        </tbody>
    </table>
</div>

</body>
</html>
