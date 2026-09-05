<?php
/* ============================================================
   PANTALLA PRINCIPAL: FACTURACION
   ============================================================ */
require 'config/conexion.php';
require 'includes/banguat.php';

// Consultamos el tipo de cambio al abrir la pantalla
$tipo_cambio = obtener_tipo_cambio($conexion);

// Verificamos que la BD responda (para el indicador "En linea")
$bd_en_linea = (bool) $conexion->query("SELECT 1")->fetch();
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Sistema POS - Facturacion</title>
    <link rel="stylesheet" href="css/estilos.css">
</head>
<body>

<!-- ================= ENCABEZADO ================= -->
<div class="encabezado">
    <div>
        <h1>SISTEMA POS - FACTURACI&Oacute;N</h1>
        <div>
            Conexi&oacute;n BD:
            <span class="<?php echo $bd_en_linea ? 'en-linea' : 'sin-linea'; ?>">
                &bull; <?php echo $bd_en_linea ? 'En l&iacute;nea' : 'Sin conexi&oacute;n'; ?>
            </span>
        </div>
    </div>
    <div class="derecha">
        <strong>Fecha:</strong> <?php echo date('d/m/Y'); ?><br>
        <strong>Cajero:</strong> Usuario Demo
    </div>
</div>

<!-- ================= MENU ================= -->
<div class="menu">
    <a href="index.php"    class="activo">Facturaci&oacute;n</a>
    <a href="facturas.php">Facturas registradas</a>
    <a href="productos.php">Productos</a>
    <a href="demo_ws.php">Demo Web Service Banguat</a>
</div>

<!-- ================= 1. CLIENTE ================= -->
<div class="seccion">
    <h2>1. DATOS DEL CLIENTE</h2>
    <div class="fila">
        <input type="text" id="nit" class="ancho-total" placeholder="NIT del cliente (ej. 1234567-8)">
        <button class="boton boton-azul"  onclick="buscarCliente()">Buscar</button>
        <button class="boton boton-verde" onclick="abrirModalCliente()">+ Nuevo Cliente</button>
    </div>
    <div class="fila" style="margin-top:10px;">
        <input type="text" id="nombre_cliente"    class="ancho-total" readonly placeholder="Nombre del cliente">
        <input type="text" id="direccion_cliente" class="ancho-total" readonly placeholder="Direcci&oacute;n">
        <select id="metodo_pago">
            <option value="Efectivo">Pago: Efectivo</option>
            <option value="Tarjeta">Pago: Tarjeta</option>
            <option value="Transferencia">Pago: Transferencia</option>
        </select>
    </div>
    <input type="hidden" id="cliente_id" value="">
</div>

<!-- ================= 2. PRODUCTOS ================= -->
<div class="seccion">
    <h2>2. AGREGAR PRODUCTOS A LA VENTA</h2>
    <div class="fila">
        <input type="text" id="buscar_producto" class="ancho-total"
               placeholder="Escanear c&oacute;digo de barras o ingresar nombre del producto...">
        <button class="boton boton-azul" onclick="agregarProducto()">Agregar Producto</button>
    </div>
</div>

<!-- ================= 3. DETALLE ================= -->
<div class="seccion">
    <h2>3. DETALLE DE LA FACTURA</h2>
    <table>
        <thead>
            <tr>
                <th>C&oacute;digo</th>
                <th>Descripci&oacute;n</th>
                <th style="width:110px;">Cant.</th>
                <th style="width:130px;">Precio Unit. ($)</th>
                <th style="width:130px;">Subtotal ($)</th>
                <th style="width:80px;">Acci&oacute;n</th>
            </tr>
        </thead>
        <tbody id="detalle">
            <tr><td colspan="6" class="vacio">No hay productos agregados</td></tr>
        </tbody>
    </table>
</div>

<!-- ================= PIE: CAMBIO Y TOTALES ================= -->
<div class="pie">
    <div class="caja-cambio">
        <h3>Tipo de Cambio del D&iacute;a:</h3>
        <div class="valor">
            1 USD = <strong id="tipo_cambio_texto">Q <?php echo number_format($tipo_cambio['referencia'], 5); ?></strong> GTQ
        </div>
        <div class="fuente" id="origen_cambio">Fuente: <?php echo $tipo_cambio['origen']; ?></div>
        <button class="boton boton-gris" style="margin-top:10px;" onclick="refrescarTipoCambio()">
            Actualizar desde el Banguat
        </button>
    </div>

    <div class="caja-totales">
        <div class="linea"><span>Sub-Total:</span>            <span id="subtotal">$ 0.00</span></div>
        <div class="linea"><span>Impuesto (IVA 12%):</span>   <span id="iva">$ 0.00</span></div>
        <div class="separador"></div>
        <div class="linea"><span class="total-usd">Total USD:</span>        <span class="total-usd" id="total_usd">$ 0.00</span></div>
        <div class="linea"><span class="total-gtq">Total Local (GTQ):</span><span class="total-gtq" id="total_gtq">Q 0.00</span></div>
        <button class="boton boton-verde" style="width:100%; margin-top:15px; padding:14px;"
                onclick="guardarFactura()">GUARDAR FACTURA</button>
    </div>
</div>

<!-- ================= MODAL NUEVO CLIENTE ================= -->
<div class="modal" id="modal_cliente">
    <div class="modal-caja">
        <h2>Nuevo Cliente</h2>
        <input type="text" id="nuevo_nit"       placeholder="NIT">
        <input type="text" id="nuevo_nombre"    placeholder="Nombre completo">
        <input type="text" id="nuevo_direccion" placeholder="Direcci&oacute;n">
        <input type="text" id="nuevo_telefono"  placeholder="Tel&eacute;fono">
        <div style="text-align:right; margin-top:10px;">
            <button class="boton boton-gris"  onclick="cerrarModalCliente()">Cancelar</button>
            <button class="boton boton-verde" onclick="guardarCliente()">Guardar</button>
        </div>
    </div>
</div>

<script>
    // El tipo de cambio que trae PHP se lo pasamos a JavaScript
    var TIPO_CAMBIO = <?php echo $tipo_cambio['referencia']; ?>;
</script>
<script src="js/app.js"></script>

</body>
</html>
