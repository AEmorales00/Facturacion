/* ============================================================
   LOGICA DE LA PANTALLA DE FACTURACION
   ------------------------------------------------------------
   Aqui esta todo lo que pasa en el navegador:
     - buscar cliente
     - agregar / quitar productos
     - calcular totales
     - guardar la factura
   Las consultas a la base de datos las hacen los archivos
   de la carpeta /api (en PHP); aqui solo se los pedimos.
   ============================================================ */

var IVA = 0.12;        // 12% de impuesto
var carrito = [];      // aqui se van guardando los productos de la venta


/* ------------------------------------------------------------
   1. CLIENTE
   ------------------------------------------------------------ */
function buscarCliente() {
    var nit = document.getElementById('nit').value.trim();

    if (nit === '') {
        alert('Escriba el NIT del cliente');
        return;
    }

    fetch('api/buscar_cliente.php?nit=' + encodeURIComponent(nit))
        .then(function (respuesta) { return respuesta.json(); })
        .then(function (datos) {
            if (!datos.ok) {
                alert(datos.mensaje);
                limpiarCliente();
                return;
            }
            document.getElementById('cliente_id').value        = datos.cliente.id;
            document.getElementById('nombre_cliente').value    = datos.cliente.nombre;
            document.getElementById('direccion_cliente').value = datos.cliente.direccion;
        });
}

function limpiarCliente() {
    document.getElementById('cliente_id').value        = '';
    document.getElementById('nombre_cliente').value    = '';
    document.getElementById('direccion_cliente').value = '';
}

function abrirModalCliente()  { document.getElementById('modal_cliente').classList.add('visible'); }
function cerrarModalCliente() { document.getElementById('modal_cliente').classList.remove('visible'); }

function guardarCliente() {
    var datos = new FormData();
    datos.append('nit',       document.getElementById('nuevo_nit').value.trim());
    datos.append('nombre',    document.getElementById('nuevo_nombre').value.trim());
    datos.append('direccion', document.getElementById('nuevo_direccion').value.trim());
    datos.append('telefono',  document.getElementById('nuevo_telefono').value.trim());

    fetch('api/nuevo_cliente.php', { method: 'POST', body: datos })
        .then(function (respuesta) { return respuesta.json(); })
        .then(function (resultado) {
            if (!resultado.ok) {
                alert(resultado.mensaje);
                return;
            }
            // Dejamos el cliente nuevo seleccionado en la factura
            document.getElementById('cliente_id').value        = resultado.cliente.id;
            document.getElementById('nit').value               = resultado.cliente.nit;
            document.getElementById('nombre_cliente').value    = resultado.cliente.nombre;
            document.getElementById('direccion_cliente').value = resultado.cliente.direccion;
            cerrarModalCliente();
        });
}


/* ------------------------------------------------------------
   2. PRODUCTOS
   ------------------------------------------------------------ */
function agregarProducto() {
    var campo = document.getElementById('buscar_producto');
    var texto = campo.value.trim();

    if (texto === '') {
        alert('Escriba el codigo o el nombre del producto');
        return;
    }

    fetch('api/buscar_producto.php?texto=' + encodeURIComponent(texto))
        .then(function (respuesta) { return respuesta.json(); })
        .then(function (datos) {
            if (!datos.ok) {
                alert(datos.mensaje);
                return;
            }

            var producto = datos.producto;

            // Si el producto ya esta en la lista, solo le sumamos 1
            var repetido = null;
            for (var i = 0; i < carrito.length; i++) {
                if (carrito[i].id === producto.id) {
                    repetido = carrito[i];
                }
            }

            if (repetido) {
                repetido.cantidad = repetido.cantidad + 1;
            } else {
                carrito.push({
                    id:          producto.id,
                    codigo:      producto.codigo,
                    descripcion: producto.descripcion,
                    precio:      parseFloat(producto.precio_usd),
                    cantidad:    1
                });
            }

            campo.value = '';
            campo.focus();
            dibujarDetalle();
        });
}

function cambiarCantidad(indice, nuevaCantidad) {
    nuevaCantidad = parseInt(nuevaCantidad, 10);

    if (isNaN(nuevaCantidad) || nuevaCantidad < 1) {
        nuevaCantidad = 1;
    }

    carrito[indice].cantidad = nuevaCantidad;
    dibujarDetalle();
}

function quitarProducto(indice) {
    carrito.splice(indice, 1);   // saca 1 elemento de la posicion indicada
    dibujarDetalle();
}


/* ------------------------------------------------------------
   3. DIBUJAR LA TABLA Y CALCULAR TOTALES
   ------------------------------------------------------------ */
function dibujarDetalle() {
    var cuerpo = document.getElementById('detalle');
    var html   = '';

    if (carrito.length === 0) {
        cuerpo.innerHTML = '<tr><td colspan="6" class="vacio">No hay productos agregados</td></tr>';
        calcularTotales();
        return;
    }

    for (var i = 0; i < carrito.length; i++) {
        var linea    = carrito[i];
        var subtotal = linea.precio * linea.cantidad;

        html += '<tr>' +
                    '<td>' + linea.codigo + '</td>' +
                    '<td>' + linea.descripcion + '</td>' +
                    '<td class="centro">' +
                        '<input type="number" min="1" value="' + linea.cantidad + '" style="width:70px;" ' +
                               'onchange="cambiarCantidad(' + i + ', this.value)">' +
                    '</td>' +
                    '<td class="derecha">$ ' + linea.precio.toFixed(2) + '</td>' +
                    '<td class="derecha">$ ' + subtotal.toFixed(2) + '</td>' +
                    '<td class="centro">' +
                        '<button class="boton boton-rojo" onclick="quitarProducto(' + i + ')">X</button>' +
                    '</td>' +
                '</tr>';
    }

    cuerpo.innerHTML = html;
    calcularTotales();
}

/* Redondea a 2 decimales, igual que lo hace PHP con round($n, 2).
   Es importante que los dos redondeen igual, si no la pantalla muestra
   un total y la base de datos guarda otro (diferencia de centavos). */
function redondear(numero) {
    return Math.round(numero * 100) / 100;
}

function calcularTotales() {
    var subtotal = 0;

    for (var i = 0; i < carrito.length; i++) {
        subtotal = subtotal + (carrito[i].precio * carrito[i].cantidad);
    }

    var iva      = redondear(subtotal * IVA);
    var totalUsd = redondear(subtotal + iva);
    var totalGtq = redondear(totalUsd * TIPO_CAMBIO);   // <-- aqui se usa el dato del Banguat

    document.getElementById('subtotal').textContent  = '$ ' + subtotal.toFixed(2);
    document.getElementById('iva').textContent       = '$ ' + iva.toFixed(2);
    document.getElementById('total_usd').textContent = '$ ' + totalUsd.toFixed(2);
    document.getElementById('total_gtq').textContent = 'Q ' + totalGtq.toFixed(2);
}


/* ------------------------------------------------------------
   4. TIPO DE CAMBIO (Web Service del Banguat)
   ------------------------------------------------------------ */
function refrescarTipoCambio() {
    fetch('api/tipo_cambio.php?refrescar=1')
        .then(function (respuesta) { return respuesta.json(); })
        .then(function (datos) {
            TIPO_CAMBIO = parseFloat(datos.referencia);
            document.getElementById('tipo_cambio_texto').textContent = 'Q ' + TIPO_CAMBIO.toFixed(5);
            document.getElementById('origen_cambio').textContent     = 'Fuente: ' + datos.origen;
            calcularTotales();   // recalculamos con el cambio nuevo
        });
}


/* ------------------------------------------------------------
   5. GUARDAR LA FACTURA
   ------------------------------------------------------------ */
function guardarFactura() {
    var clienteId = document.getElementById('cliente_id').value;

    if (clienteId === '') {
        alert('Primero busque o registre el cliente');
        return;
    }
    if (carrito.length === 0) {
        alert('Agregue al menos un producto');
        return;
    }

    // Solo mandamos el id y la cantidad; el precio lo pone el servidor
    var items = [];
    for (var i = 0; i < carrito.length; i++) {
        items.push({ producto_id: carrito[i].id, cantidad: carrito[i].cantidad });
    }

    var datos = new FormData();
    datos.append('cliente_id',  clienteId);
    datos.append('metodo_pago', document.getElementById('metodo_pago').value);
    datos.append('items',       JSON.stringify(items));

    fetch('api/guardar_factura.php', { method: 'POST', body: datos })
        .then(function (respuesta) { return respuesta.json(); })
        .then(function (resultado) {
            if (!resultado.ok) {
                alert(resultado.mensaje);
                return;
            }

            alert('Factura No. ' + resultado.factura_id + ' guardada.\n' +
                  'Total: $ ' + resultado.total_usd + '  =  Q ' + resultado.total_gtq + '\n' +
                  'Tipo de cambio usado: ' + resultado.tipo_cambio);

            // Limpiamos la pantalla para la siguiente venta
            carrito = [];
            limpiarCliente();
            document.getElementById('nit').value = '';
            dibujarDetalle();
        });
}


/* ------------------------------------------------------------
   6. ATAJOS DE TECLADO: Enter para buscar / agregar
   ------------------------------------------------------------ */
document.getElementById('nit').addEventListener('keypress', function (evento) {
    if (evento.key === 'Enter') { buscarCliente(); }
});

document.getElementById('buscar_producto').addEventListener('keypress', function (evento) {
    if (evento.key === 'Enter') { agregarProducto(); }
});
