import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Cliente,
  DemostracionWs,
  Factura,
  PeticionFactura,
  Producto,
  TipoCambio
} from '../modelos/modelos';

/* ============================================================
   SERVICIO DE API
   ------------------------------------------------------------
   Unico punto del frontend que habla con el backend de Spring
   Boot. Los componentes nunca usan HttpClient directamente:
   si manana cambia una ruta, se corrige aqui y nada mas.
   ============================================================ */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  /**
   * El tipo de cambio se guarda en una signal porque lo necesitan varias
   * pantallas a la vez (la barra lateral, facturacion y el catalogo).
   * Asi se consulta una sola vez y todas se enteran del valor.
   */
  readonly tipoCambio = signal<TipoCambio | null>(null);

  // ---------------------------------------------------------------- clientes

  clientes(texto?: string): Observable<Cliente[]> {
    const params = texto ? new HttpParams().set('texto', texto) : undefined;
    return this.http
      .get<Cliente[]>(`${this.base}/api/clientes`, { params })
      .pipe(catchError(this.alError));
  }

  clientePorNit(nit: string): Observable<Cliente> {
    return this.http
      .get<Cliente>(`${this.base}/api/clientes/nit/${encodeURIComponent(nit)}`)
      .pipe(catchError(this.alError));
  }

  crearCliente(cliente: Partial<Cliente>): Observable<Cliente> {
    return this.http
      .post<Cliente>(`${this.base}/api/clientes`, cliente)
      .pipe(catchError(this.alError));
  }

  // --------------------------------------------------------------- productos

  productos(texto?: string): Observable<Producto[]> {
    const params = texto ? new HttpParams().set('texto', texto) : undefined;
    return this.http
      .get<Producto[]>(`${this.base}/api/productos`, { params })
      .pipe(catchError(this.alError));
  }

  /**
   * El alta va como FormData, no como JSON, porque viaja la fotografia.
   * El backend la recibe, la sube al bucket de AWS S3 y guarda la referencia.
   */
  crearProducto(datos: {
    codigo: string;
    descripcion: string;
    precioUsd: number;
    stock: number;
    imagen?: File | null;
  }): Observable<Producto> {
    return this.http
      .post<Producto>(`${this.base}/api/productos`, this.aFormData(datos))
      .pipe(catchError(this.alError));
  }

  actualizarProducto(
    id: number,
    datos: {
      codigo?: string;
      descripcion?: string;
      precioUsd?: number;
      stock?: number;
      imagen?: File | null;
    }
  ): Observable<Producto> {
    return this.http
      .put<Producto>(`${this.base}/api/productos/${id}`, this.aFormData(datos))
      .pipe(catchError(this.alError));
  }

  eliminarProducto(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.base}/api/productos/${id}`)
      .pipe(catchError(this.alError));
  }

  // ---------------------------------------------------------------- facturas

  emitirFactura(peticion: PeticionFactura): Observable<Factura> {
    return this.http
      .post<Factura>(`${this.base}/api/facturas`, peticion)
      .pipe(catchError(this.alError));
  }

  facturas(filtros: { nit?: string; desde?: string; hasta?: string } = {}): Observable<Factura[]> {
    let params = new HttpParams();
    if (filtros.nit) params = params.set('nit', filtros.nit);
    if (filtros.desde) params = params.set('desde', filtros.desde);
    if (filtros.hasta) params = params.set('hasta', filtros.hasta);

    return this.http
      .get<Factura[]>(`${this.base}/api/facturas`, { params })
      .pipe(catchError(this.alError));
  }

  // ------------------------------------------------------------ tipo cambio

  /** Consulta el tipo de cambio y lo deja disponible para toda la aplicacion. */
  cargarTipoCambio(refrescar = false): Observable<TipoCambio> {
    const params = new HttpParams().set('refrescar', refrescar);
    return this.http
      .get<TipoCambio>(`${this.base}/api/tipo-cambio`, { params })
      .pipe(
        tap((valor) => this.tipoCambio.set(valor)),
        catchError(this.alError)
      );
  }

  demostracionWs(): Observable<DemostracionWs> {
    return this.http
      .get<DemostracionWs>(`${this.base}/api/tipo-cambio/demo`)
      .pipe(catchError(this.alError));
  }

  // ----------------------------------------------------------------- apoyo

  /** Arma el FormData saltandose los campos que no se enviaron. */
  private aFormData(datos: Record<string, unknown>): FormData {
    const cuerpo = new FormData();
    for (const [clave, valor] of Object.entries(datos)) {
      if (valor === null || valor === undefined || valor === '') continue;
      cuerpo.append(clave, valor instanceof File ? valor : String(valor));
    }
    return cuerpo;
  }

  /**
   * Traduce el error HTTP a un mensaje que se le pueda ensenar al usuario.
   * El backend siempre responde {estado, mensaje, errores}, asi que primero
   * se busca ahi; si no hay nada, se explica la causa mas probable.
   */
  private alError(error: HttpErrorResponse) {
    let mensaje = error.error?.mensaje;

    if (!mensaje && error.status === 0) {
      mensaje =
        'No se pudo contactar al servidor. Revise que el backend este corriendo ' +
        'y que su direccion este permitida en CORS.';
    }

    return throwError(() => new Error(mensaje ?? `Error ${error.status} al llamar a la API`));
  }
}
