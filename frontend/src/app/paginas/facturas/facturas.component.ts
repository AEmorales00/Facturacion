import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../servicios/api.service';
import { Factura } from '../../modelos/modelos';

/**
 * Consulta de las facturas ya emitidas, con filtros por NIT y por rango de
 * fechas. El filtrado se hace en el backend con una sola consulta SQL, no
 * trayendo todo y recortando aqui.
 */
@Component({
  selector: 'app-facturas',
  imports: [FormsModule, DecimalPipe, DatePipe, RouterLink],
  templateUrl: './facturas.component.html',
  styleUrl: './facturas.component.css'
})
export class FacturasComponent implements OnInit {
  private api = inject(ApiService);

  filtros = { nit: '', desde: '', hasta: '' };

  readonly facturas = signal<Factura[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  /** Id de la factura cuyo detalle esta desplegado. */
  readonly abierta = signal<number | null>(null);

  /** Resumen de lo que se esta viendo con los filtros aplicados. */
  readonly totalGtq = computed(() =>
    this.facturas().reduce((suma, f) => suma + f.totalGtq, 0)
  );
  readonly totalUsd = computed(() =>
    this.facturas().reduce((suma, f) => suma + f.totalUsd, 0)
  );

  ngOnInit(): void {
    this.consultar();
  }

  consultar(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.api
      .facturas({
        nit: this.filtros.nit.trim() || undefined,
        desde: this.filtros.desde || undefined,
        hasta: this.filtros.hasta || undefined
      })
      .subscribe({
        next: (lista) => {
          this.facturas.set(lista);
          this.cargando.set(false);
        },
        error: (e: Error) => {
          this.error.set(e.message);
          this.cargando.set(false);
        }
      });
  }

  limpiar(): void {
    this.filtros = { nit: '', desde: '', hasta: '' };
    this.consultar();
  }

  alternarDetalle(id: number): void {
    this.abierta.update((actual) => (actual === id ? null : id));
  }

  hayFiltros(): boolean {
    return !!(this.filtros.nit || this.filtros.desde || this.filtros.hasta);
  }
}
