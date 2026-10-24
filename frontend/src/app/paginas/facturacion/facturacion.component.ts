import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../servicios/api.service';
import { Cliente, Factura, LineaCarrito, Producto } from '../../modelos/modelos';

/* ============================================================
   PANTALLA DE FACTURACION
   ------------------------------------------------------------
   Es el flujo principal del sistema:
     1. Se busca al cliente por su NIT.
     2. Se busca un producto; al seleccionarlo se muestra su
        fotografia (que vive en AWS S3) y sus datos.
     3. Se agrega al detalle con la cantidad deseada.
     4. Se emite la factura.

   Los totales que se ven en pantalla son solo una vista previa.
   El valor que queda guardado lo calcula el backend leyendo los
   precios de la base de datos, para que nadie pueda alterarlos
   desde el navegador.
   ============================================================ */
@Component({
  selector: 'app-facturacion',
  imports: [FormsModule, DecimalPipe, RouterLink],
  templateUrl: './facturacion.component.html',
  styleUrl: './facturacion.component.css'
})
export class FacturacionComponent {
  private api = inject(ApiService);

  // ------------------------------------------------------------- cliente
  textoCliente = '';
  readonly cliente = signal<Cliente | null>(null);
  readonly errorCliente = signal<string | null>(null);
  readonly formularioClienteVisible = signal(false);
  nuevoCliente = { nit: '', nombre: '', direccion: '', telefono: '' };

  // ------------------------------------------------------------ producto
  textoProducto = '';
  readonly resultados = signal<Producto[]>([]);
  readonly buscando = signal(false);
  readonly seleccionado = signal<Producto | null>(null);
  cantidad = 1;

  // -------------------------------------------------------------- factura
  metodoPago = 'Efectivo';
  readonly carrito = signal<LineaCarrito[]>([]);
  readonly emitiendo = signal(false);
  readonly error = signal<string | null>(null);
  readonly emitida = signal<Factura | null>(null);

  readonly tipoCambio = this.api.tipoCambio;

  /* Los totales se recalculan solos cada vez que cambia el carrito.
     computed() es la forma de Angular de declarar un valor derivado. */
  readonly subtotal = computed(() =>
    this.carrito().reduce((suma, l) => suma + l.producto.precioUsd * l.cantidad, 0)
  );
  readonly iva = computed(() => this.subtotal() * 0.12);
  readonly totalUsd = computed(() => this.subtotal() + this.iva());
  readonly totalGtq = computed(() => this.totalUsd() * (this.tipoCambio()?.referencia ?? 0));

  /** Lo que ya esta en el carrito tambien ocupa stock. */
  readonly disponible = computed(() => {
    const producto = this.seleccionado();
    if (!producto) return 0;
    const enCarrito = this.carrito().find((l) => l.producto.id === producto.id)?.cantidad ?? 0;
    return producto.stock - enCarrito;
  });

  // --------------------------------------------------------------- cliente

  buscarCliente(): void {
    const nit = this.textoCliente.trim();
    if (!nit) return;

    this.errorCliente.set(null);
    this.api.clientePorNit(nit).subscribe({
      next: (c) => {
        this.cliente.set(c);
        this.formularioClienteVisible.set(false);
      },
      error: () => {
        this.cliente.set(null);
        this.errorCliente.set(`No hay ningun cliente con el NIT ${nit}.`);
        this.nuevoCliente = { nit, nombre: '', direccion: '', telefono: '' };
      }
    });
  }

  mostrarFormularioCliente(): void {
    this.formularioClienteVisible.set(true);
  }

  guardarCliente(): void {
    this.api.crearCliente(this.nuevoCliente).subscribe({
      next: (c) => {
        this.cliente.set(c);
        this.textoCliente = c.nit;
        this.formularioClienteVisible.set(false);
        this.errorCliente.set(null);
      },
      error: (e: Error) => this.errorCliente.set(e.message)
    });
  }

  cambiarCliente(): void {
    this.cliente.set(null);
    this.textoCliente = '';
    this.errorCliente.set(null);
  }

  // -------------------------------------------------------------- producto

  buscarProducto(): void {
    const texto = this.textoProducto.trim();
    if (!texto) {
      this.resultados.set([]);
      return;
    }

    this.buscando.set(true);
    this.api.productos(texto).subscribe({
      next: (lista) => {
        this.resultados.set(lista);
        this.buscando.set(false);
        // Si solo hay una coincidencia se selecciona sola: es lo que el
        // cajero espera al escribir el codigo exacto del producto.
        if (lista.length === 1) this.seleccionar(lista[0]);
      },
      error: (e: Error) => {
        this.buscando.set(false);
        this.error.set(e.message);
      }
    });
  }

  /** Al seleccionar se muestra la fotografia y el detalle del producto. */
  seleccionar(producto: Producto): void {
    this.seleccionado.set(producto);
    this.cantidad = 1;
  }

  agregarAlDetalle(): void {
    const producto = this.seleccionado();
    if (!producto) return;

    const cantidad = Math.max(1, Math.floor(this.cantidad));
    if (cantidad > this.disponible()) {
      this.error.set(
        `Solo quedan ${this.disponible()} unidades de ${producto.descripcion} sin facturar.`
      );
      return;
    }

    this.error.set(null);
    this.carrito.update((lineas) => {
      const existente = lineas.find((l) => l.producto.id === producto.id);
      if (existente) {
        return lineas.map((l) =>
          l.producto.id === producto.id ? { ...l, cantidad: l.cantidad + cantidad } : l
        );
      }
      return [...lineas, { producto, cantidad }];
    });

    this.cantidad = 1;
  }

  quitar(productoId: number): void {
    this.carrito.update((lineas) => lineas.filter((l) => l.producto.id !== productoId));
  }

  cambiarCantidad(productoId: number, valor: string): void {
    const cantidad = Math.max(1, Math.floor(Number(valor) || 1));
    this.carrito.update((lineas) =>
      lineas.map((l) =>
        l.producto.id === productoId
          ? { ...l, cantidad: Math.min(cantidad, l.producto.stock) }
          : l
      )
    );
  }

  // --------------------------------------------------------------- emitir

  emitir(): void {
    const cliente = this.cliente();
    if (!cliente || this.carrito().length === 0) return;

    this.emitiendo.set(true);
    this.error.set(null);

    this.api
      .emitirFactura({
        clienteId: cliente.id,
        metodoPago: this.metodoPago,
        items: this.carrito().map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad }))
      })
      .subscribe({
        next: (factura) => {
          this.emitida.set(factura);
          this.emitiendo.set(false);
        },
        error: (e: Error) => {
          this.error.set(e.message);
          this.emitiendo.set(false);
        }
      });
  }

  /** Deja la pantalla lista para la siguiente venta. */
  nuevaVenta(): void {
    this.emitida.set(null);
    this.carrito.set([]);
    this.seleccionado.set(null);
    this.resultados.set([]);
    this.textoProducto = '';
    this.error.set(null);
  }

  imprimir(): void {
    window.print();
  }
}
