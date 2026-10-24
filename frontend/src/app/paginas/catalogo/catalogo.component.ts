import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../servicios/api.service';
import { Producto } from '../../modelos/modelos';

/**
 * Catalogo visual: muestra cada producto con la fotografia que esta
 * guardada en el bucket de AWS S3, junto con su precio en dolares y su
 * equivalente en quetzales del dia.
 */
@Component({
  selector: 'app-catalogo',
  imports: [FormsModule, DecimalPipe, RouterLink],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.css'
})
export class CatalogoComponent implements OnInit {
  private api = inject(ApiService);

  texto = '';
  readonly productos = signal<Producto[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly tipoCambio = this.api.tipoCambio;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.api.productos(this.texto.trim() || undefined).subscribe({
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

  limpiar(): void {
    this.texto = '';
    this.cargar();
  }

  /** Cuantos productos del catalogo ya tienen su fotografia subida a S3. */
  conFoto(): number {
    return this.productos().filter((p) => p.imagenUrl).length;
  }
}
