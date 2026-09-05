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

/* Catalogo completo: se usa para la lista desplegable y para el
   panel de abajo. Asi el cajero no tiene que saberse el codigo. */
$catalogo = $conexion->query(
    "SELECT id, codigo, descripcion, precio_usd, stock
     FROM   productos
     ORDER  BY descripcion"
)->fetchAll();

/* Clientes registrados: se usan para la lista desplegable y para el
   panel de abajo. Asi no hay que escribir el NIT de memoria. */
$clientes = $conexion->query(
    "SELECT c.id, c.nit, c.nombre, c.direccion, c.telefono,
            COUNT(f.id) AS facturas
     FROM   clientes c
     LEFT JOIN facturas f ON f.cliente_id = c.id
     GROUP  BY c.id, c.nit, c.nombre, c.direccion, c.telefono
     ORDER  BY c.nombre"
)->fetchAll();

/* Ultimas facturas emitidas, para verlas sin salir de la pantalla */
$ultimas_facturas = $conexion->query(
    "SELECT f.id, f.fecha, f.metodo_pago, f.total_usd, f.total_gtq,
            c.nit, c.nombre
     FROM   facturas f
     INNER JOIN clientes c ON c.id = f.cliente_id
     ORDER  BY f.id DESC
     LIMIT  10"
)->fetchAll();
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
    <!-- Forma A: elegirlo de la lista. No hay que saberse el NIT. -->
    <div class="fila">
        <select id="lista_clientes" class="ancho-total" onchange="seleccionarCliente()">
            <option value="">-- Seleccione el cliente --</option>
            <?php foreach ($clientes as $cliente) { ?>
            <option value="<?php echo $cliente['id']; ?>"><?php
                echo htmlspecialchars($cliente['nit'] . '  -  ' . $cliente['nombre']);
            ?></option>
            <?php } ?>
        </select>
        <button class="boton boton-verde" onclick="abrirModalCliente()">+ Nuevo Cliente</button>
    </div>

    <!-- Forma B: buscarlo por NIT -->
    <div class="fila" style="margin-top:10px;">
        <input type="text" id="nit" class="ancho-total" placeholder="...o buscar por NIT (ej. 1234567-8)">
        <button class="boton boton-azul" onclick="buscarCliente()">Buscar</button>
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
        <select id="lista_productos" class="ancho-total">
            <option value="">-- Seleccione un producto del cat&aacute;logo --</option>
            <?php foreach ($catalogo as $producto) { ?>
            <option value="<?php echo $producto['id']; ?>"><?php
                echo htmlspecialchars($producto['codigo'] . '  -  ' . $producto['descripcion']
                     . '   ($ ' . number_format($producto['precio_usd'], 2)
                     . '  |  stock ' . $producto['stock'] . ')');
            ?></option>
            <?php } ?>
        </select>
        <input type="number" id="cantidad_producto" min="1" value="1" style="width:90px;" title="Cantidad">
        <button class="boton boton-azul" onclick="agregarDesdeLista()">Agregar Producto</button>
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

<!-- ================= 4. PANELES DE CONSULTA ================= -->
<div class="seccion" style="margin-top:20px;">
    <div class="pestanas">
        <button class="pestana activa" id="pestana_catalogo" onclick="mostrarPanel('catalogo')">
            Cat&aacute;logo de productos (<?php echo count($catalogo); ?>)
        </button>
        <button class="pestana" id="pestana_clientes" onclick="mostrarPanel('clientes')">
            Clientes (<?php echo count($clientes); ?>)
        </button>
        <button class="pestana" id="pestana_facturas" onclick="mostrarPanel('facturas')">
            &Uacute;ltimas facturas (<?php echo count($ultimas_facturas); ?>)
        </button>
    </div>

    <!-- ----- Panel: catalogo ----- -->
    <div class="panel" id="panel_catalogo">
        <table>
            <thead>
                <tr><th>C&oacute;digo</th><th>Descripci&oacute;n</th>
                    <th>Precio ($)</th><th>Precio (Q)</th>
                    <th class="centro">Stock</th><th class="centro">Acci&oacute;n</th></tr>
            </thead>
            <tbody>
            <?php if (empty($catalogo)) { ?>
                <tr><td colspan="6" class="vacio">No hay productos registrados</td></tr>
            <?php } else { ?>
                <?php foreach ($catalogo as $producto) { ?>
                <tr>
                    <td><?php echo htmlspecialchars($producto['codigo']); ?></td>
                    <td><?php echo htmlspecialchars($producto['descripcion']); ?></td>
                    <td class="derecha">$ <?php echo number_format($producto['precio_usd'], 2); ?></td>
                    <td class="derecha">Q <?php echo number_format($producto['precio_usd'] * $tipo_cambio['referencia'], 2); ?></td>
                    <td class="centro"><?php echo $producto['stock']; ?></td>
                    <td class="centro">
                        <button class="boton boton-verde" style="padding:6px 12px;"
                                onclick="agregarDesdeCatalogo(<?php echo $producto['id']; ?>)">+ Agregar</button>
                    </td>
                </tr>
                <?php } ?>
            <?php } ?>
            </tbody>
        </table>
        <div class="fuente" style="margin-top:10px;">
            Para registrar productos nuevos entre a <a href="productos.php">Productos</a>.
        </div>
    </div>

    <!-- ----- Panel: clientes ----- -->
    <div class="panel oculto" id="panel_clientes">
        <table>
            <thead>
                <tr><th>NIT</th><th>Nombre</th><th>Direcci&oacute;n</th><th>Tel&eacute;fono</th>
                    <th class="centro">Facturas</th><th class="centro">Acci&oacute;n</th></tr>
            </thead>
            <tbody>
            <?php if (empty($clientes)) { ?>
                <tr><td colspan="6" class="vacio">No hay clientes registrados</td></tr>
            <?php } else { ?>
                <?php foreach ($clientes as $cliente) { ?>
                <tr>
                    <td><?php echo htmlspecialchars($cliente['nit']); ?></td>
                    <td><?php echo htmlspecialchars($cliente['nombre']); ?></td>
                    <td><?php echo htmlspecialchars($cliente['direccion']); ?></td>
                    <td><?php echo htmlspecialchars($cliente['telefono'] === null ? '-' : $cliente['telefono']); ?></td>
                    <td class="centro"><?php echo $cliente['facturas']; ?></td>
                    <td class="centro">
                        <button class="boton boton-verde" style="padding:6px 12px;"
                                onclick="seleccionarDesdePanel(<?php echo $cliente['id']; ?>)">Facturar</button>
                    </td>
                </tr>
                <?php } ?>
            <?php } ?>
            </tbody>
        </table>
        <div class="fuente" style="margin-top:10px;">
            Para registrar un cliente nuevo use el bot&oacute;n <strong>+ Nuevo Cliente</strong> de la secci&oacute;n 1.
        </div>
    </div>

    <!-- ----- Panel: ultimas facturas ----- -->
    <div class="panel oculto" id="panel_facturas">
        <table>
            <thead>
                <tr><th>No.</th><th>Fecha</th><th>NIT</th><th>Cliente</th>
                    <th>Pago</th><th>Total $</th><th>Total Q</th><th class="centro">Ver</th></tr>
            </thead>
            <tbody>
            <?php if (empty($ultimas_facturas)) { ?>
                <tr><td colspan="8" class="vacio">Todav&iacute;a no hay facturas emitidas</td></tr>
            <?php } else { ?>
                <?php foreach ($ultimas_facturas as $factura) { ?>
                <tr>
                    <td><?php echo $factura['id']; ?></td>
                    <td><?php echo date('d/m/Y H:i', strtotime($factura['fecha'])); ?></td>
                    <td><?php echo htmlspecialchars($factura['nit']); ?></td>
                    <td><?php echo htmlspecialchars($factura['nombre']); ?></td>
                    <td><?php echo htmlspecialchars($factura['metodo_pago']); ?></td>
                    <td class="derecha">$ <?php echo number_format($factura['total_usd'], 2); ?></td>
                    <td class="derecha">Q <?php echo number_format($factura['total_gtq'], 2); ?></td>
                    <td class="centro"><a href="facturas.php?ver=<?php echo $factura['id']; ?>">Ver</a></td>
                </tr>
                <?php } ?>
            <?php } ?>
            </tbody>
        </table>
        <div class="fuente" style="margin-top:10px;">
            Se muestran las 10 m&aacute;s recientes. El listado completo con filtros est&aacute; en
            <a href="facturas.php">Facturas registradas</a>.
        </div>
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

    // El catalogo tambien viaja a JavaScript: asi la lista desplegable
    // y los botones "+ Agregar" no necesitan consultar al servidor.
    var CATALOGO = <?php echo json_encode($catalogo); ?>;

    // Lo mismo con los clientes, para la lista desplegable de la seccion 1
    var CLIENTES = <?php echo json_encode($clientes); ?>;
</script>
<script src="js/app.js"></script>

</body>
</html>
