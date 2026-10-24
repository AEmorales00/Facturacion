/* ============================================================
   MODELOS
   Describen la forma exacta del JSON que devuelve la API.
   Tenerlos en un solo archivo hace que, si cambia el backend,
   TypeScript marque el error en todos los lugares afectados.
   ============================================================ */

export interface Cliente {
  id: number;
  nit: string;
  nombre: string;
  direccion: string;
  telefono: string | null;
}

export interface Producto {
  id: number;
  codigo: string;
  descripcion: string;
  precioUsd: number;
  stock: number;
  /** Clave del objeto dentro del bucket de S3. */
  imagenClave: string | null;
  /** URL lista para usar en un <img>. La arma el backend. */
  imagenUrl: string | null;
}

export interface TipoCambio {
  referencia: number;
  /** De donde salio el dato: Web Service, cache o respaldo. */
  origen: string;
  fecha: string;
}

/** Una linea del carrito, antes de enviarla al backend. */
export interface LineaCarrito {
  producto: Producto;
  cantidad: number;
}

/** Lo que se envia al emitir. Solo id y cantidad: el precio lo pone el servidor. */
export interface PeticionFactura {
  clienteId: number;
  metodoPago: string;
  items: { productoId: number; cantidad: number }[];
}

export interface LineaFactura {
  productoId: number;
  codigo: string;
  descripcion: string;
  imagenUrl: string | null;
  cantidad: number;
  precioUnitarioUsd: number;
  subtotalUsd: number;
}

export interface Factura {
  id: number;
  fecha: string;
  metodoPago: string;
  clienteId: number;
  clienteNit: string;
  clienteNombre: string;
  clienteDireccion: string;
  tipoCambio: number;
  subtotalUsd: number;
  ivaUsd: number;
  totalUsd: number;
  totalGtq: number;
  detalle: LineaFactura[];
}

/** Intercambio SOAP completo, para la pantalla de demostracion. */
export interface DemostracionWs {
  url: string;
  accionSoap: string;
  xmlEnviado: string;
  xmlRecibido: string;
  referencia: number | null;
  error: string | null;
}
