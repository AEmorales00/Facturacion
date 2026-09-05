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
/* Deja el cliente puesto en la factura (los tres campos de arriba
   y tambien seleccionado en la lista desplegable). */
function pintarCliente(cliente) {
    document.getElementById('cliente_id').value        = cliente.id;
    document.getElementById('nombre_cliente').value    = cliente.nombre;
    document.getElementById('direccion_cliente').value = cliente.direccion;
    document.getElementById('lista_clientes').value    = cliente.id;
}

/* Busca un cliente en la lista que PHP dejo en la variable CLIENTES
   (ver el <script> del final de index.php). No consulta al servidor. */
function buscarEnClientes(id) {
    for (var i = 0; i < CLIENTES.length; i++) {
        if (String(CLIENTES[i].id) === String(id)) {
            return CLIENTES[i];
        }
    }
    return null;
}

/* FORMA A: el cajero elige el cliente de la lista desplegable */
function seleccionarCliente() {
    var lista   = document.getElementById('lista_clientes');
    var cliente = buscarEnClientes(lista.value);

    if (!cliente) {
        limpiarCliente();
        return;
    }

    document.getElementById('nit').value = cliente.nit;
    pintarCliente(cliente);
}

/* Boton "Facturar" de la tabla de clientes (panel de abajo) */
function seleccionarDesdePanel(id) {
    var cliente = buscarEnClientes(id);

    if (!cliente) {
        alert('Cliente no encontrado');
        return;
    }

    document.getElementById('nit').value = cliente.nit;
    pintarCliente(cliente);
    window.scrollTo(0, 0);   // subimos para que se vea el cliente puesto
}

/* FORMA B: buscarlo por NIT (consulta al servidor) */
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
            pintarCliente(datos.cliente);
        });
}

function limpiarCliente() {
    document.getElementById('cliente_id').value        = '';
    document.getElementById('nombre_cliente').value    = '';
    document.getElementById('direccion_cliente').value = '';
    document.getElementById('lista_clientes').value    = '';
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
            /* Lo agregamos a la lista desplegable sin recargar la pagina
               y lo dejamos seleccionado en la factura */
            CLIENTES.push(resultado.cliente);

            var opcion  = document.createElement('option');
            opcion.value       = resultado.cliente.id;
            opcion.textContent = resultado.cliente.nit + '  -  ' + resultado.cliente.nombre;
            document.getElementById('lista_clientes').appendChild(opcion);

            document.getElementById('nit').value = resultado.cliente.nit;
            pintarCliente(resultado.cliente);
            cerrarModalCliente();
        });
}


/* ------------------------------------------------------------
   2. PRODUCTOS
   ------------------------------------------------------------ */
/* Mete un producto al carrito (o le suma cantidad si ya estaba).
   Lo usan las dos formas de agregar: la lista desplegable de la seccion 2
   y el boton "+ Agregar" del catalogo. */
function agregarAlCarrito(producto, cantidad) {
    cantidad = parseInt(cantidad, 10);

    if (isNaN(cantidad) || cantidad < 1) {
        cantidad = 1;
    }

    // Los ids de la BD llegan como texto, por eso los comparamos como texto
    var id       = String(producto.id);
    var repetido = null;

    for (var i = 0; i < carrito.length; i++) {
        if (String(carrito[i].id) === id) {
            repetido = carrito[i];
        }
    }

    if (repetido) {
        repetido.cantidad = repetido.cantidad + cantidad;
    } else {
        carrito.push({
            id:          id,
            codigo:      producto.codigo,
            descripcion: producto.descripcion,
            precio:      parseFloat(producto.precio_usd),
            cantidad:    cantidad
        });
    }

    dibujarDetalle();
}

/* Busca un producto en el catalogo que PHP dejo en la variable CATALOGO
   (ver el <script> del final de index.php). No consulta al servidor. */
function buscarEnCatalogo(id) {
    for (var i = 0; i < CATALOGO.length; i++) {
        if (String(CATALOGO[i].id) === String(id)) {
            return CATALOGO[i];
        }
    }
    return null;
}

/* El cajero elige el producto de la lista desplegable */
function agregarDesdeLista() {
    var lista    = document.getElementById('lista_productos');
    var cantidad = document.getElementById('cantidad_producto');
    var producto = buscarEnCatalogo(lista.value);

    if (!producto) {
        alert('Seleccione un producto de la lista');
        lista.focus();
        return;
    }

    agregarAlCarrito(producto, cantidad.value);

    // Dejamos la lista lista para el siguiente producto
    lista.value     = '';
    cantidad.value  = 1;
    lista.focus();
}

/* Boton "+ Agregar" de la tabla del catalogo (panel de abajo) */
function agregarDesdeCatalogo(id) {
    var producto = buscarEnCatalogo(id);

    if (!producto) {
        alert('Producto no encontrado en el catalogo');
        return;
    }

    agregarAlCarrito(producto, 1);
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

            /* Recargamos la pantalla: asi queda limpia para la siguiente
               venta y de paso se actualizan el stock del catalogo y la
               lista de ultimas facturas del panel de abajo. */
            location.reload();
        });
}


/* ------------------------------------------------------------
   6. PESTANAS DE LOS PANELES (catalogo / clientes / ultimas facturas)
   ------------------------------------------------------------ */
function mostrarPanel(nombre) {
    var paneles  = ['catalogo', 'clientes', 'facturas'];

    for (var i = 0; i < paneles.length; i++) {
        var panel   = document.getElementById('panel_'   + paneles[i]);
        var pestana = document.getElementById('pestana_' + paneles[i]);

        if (paneles[i] === nombre) {
            panel.classList.remove('oculto');
            pestana.classList.add('activa');
        } else {
            panel.classList.add('oculto');
            pestana.classList.remove('activa');
        }
    }
}


/* ------------------------------------------------------------
   7. ATAJOS DE TECLADO: Enter para buscar / agregar
   ------------------------------------------------------------ */
document.getElementById('nit').addEventListener('keypress', function (evento) {
    if (evento.key === 'Enter') { buscarCliente(); }
});

document.getElementById('cantidad_producto').addEventListener('keypress', function (evento) {
    if (evento.key === 'Enter') { agregarDesdeLista(); }
});
