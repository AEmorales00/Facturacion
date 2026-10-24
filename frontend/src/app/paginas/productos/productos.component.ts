import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../servicios/api.service';
import { Producto } from '../../modelos/modelos';

/**
 * Administracion del catalogo: alta, edicion y baja de productos.
 *
 * Es la pantalla donde la imagen sale del navegador y llega a AWS S3:
 * el archivo se manda al backend dentro de un FormData, el backend lo sube
 * al bucket y guarda la clave del objeto en la base de datos.
 */
@Component({
  selector: 'app-productos',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './productos.component.html',
  styleUrl: './productos.component.css'
})
export class ProductosComponent implements OnInit {
  private api = inject(ApiService);

  readonly productos = signal<Producto[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);

  /** null = estamos dando de alta; con id = estamos editando ese producto. */
  readonly editando = signal<Producto | null>(null);

  formulario = { codigo: '', descripcion: '', precioUsd: 0, stock: 0 };
  archivo: File | null = null;
  /** Previsualizacion local, antes de que la imagen llegue a S3. */
  readonly vistaPrevia = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.productos().subscribe({
      next: (lista) => {
        this.productos.set(lista);
        this.cargando.set(false);
      },
      error: (e: Error) => {
        this.error.set(e.message);
        this.cargando.set(false);
      }
    });
  }

  /**
   * Se dispara al elegir un archivo. Genera la previsualizacion con un
   * object URL para que el usuario vea la foto antes de subirla.
   */
  alElegirArchivo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0] ?? null;

    this.liberarVistaPrevia();
    this.archivo = archivo;
    this.vistaPrevia.set(archivo ? URL.createObjectURL(archivo) : null);
  }

  editar(producto: Producto): void {
    this.editando.set(producto);
    this.formulario = {
      codigo: producto.codigo,
      descripcion: producto.descripcion,
      precioUsd: producto.precioUsd,
      stock: producto.stock
    };
    this.liberarVistaPrevia();
    this.archivo = null;
    this.vistaPrevia.set(producto.imagenUrl);
    this.mensaje.set(null);
    this.error.set(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelar(): void {
    this.editando.set(null);
    this.formulario = { codigo: '', descripcion: '', precioUsd: 0, stock: 0 };
    this.liberarVistaPrevia();
    this.archivo = null;
    this.vistaPrevia.set(null);
    this.error.set(null);
  }

  guardar(): void {
    this.guardando.set(true);
    this.error.set(null);
    this.mensaje.set(null);

    const enEdicion = this.editando();
    const datos = { ...this.formulario, imagen: this.archivo };

    const peticion = enEdicion
      ? this.api.actualizarProducto(enEdicion.id, datos)
      : this.api.crearProducto(datos);

    peticion.subscribe({
      next: (producto) => {
        this.guardando.set(false);
        this.mensaje.set(
          enEdicion
            ? `Se actualizo ${producto.descripcion}.`
            : `Se registro ${producto.descripcion}.`
        );
        this.cancelar();
        this.cargar();
      },
      error: (e: Error) => {
        this.guardando.set(false);
        this.error.set(e.message);
      }
    });
  }

  eliminar(producto: Producto): void {
    if (!confirm(`Se va a borrar ${producto.descripcion} del catalogo. Continuar?`)) return;

    this.api.eliminarProducto(producto.id).subscribe({
      next: () => {
        this.mensaje.set(`Se borro ${producto.descripcion}.`);
        this.cargar();
      },
      error: (e: Error) => this.error.set(e.message)
    });
  }

  /** Los object URL ocupan memoria hasta que se liberan a mano. */
  private liberarVistaPrevia(): void {
    const actual = this.vistaPrevia();
    if (actual?.startsWith('blob:')) {
      URL.revokeObjectURL(actual);
    }
  }
}
