import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ApiService } from '../../servicios/api.service';
import { DemostracionWs } from '../../modelos/modelos';

/**
 * Pantalla de demostracion del Web Service del Banco de Guatemala.
 *
 * Sirve para la presentacion: ensena el XML exacto que se le manda al
 * Banguat, el XML que responde, el dato que se extrae de ahi y como ese
 * numero termina convirtiendo los totales de la factura.
 */
@Component({
  selector: 'app-demo-ws',
  imports: [DecimalPipe],
  templateUrl: './demo-ws.component.html',
  styleUrl: './demo-ws.component.css'
})
export class DemoWsComponent implements OnInit {
  private api = inject(ApiService);

  readonly demo = signal<DemostracionWs | null>(null);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);

  /** Ejemplo con el que se ilustra la conversion. */
  readonly ejemploUsd = 100;

  ngOnInit(): void {
    this.consultar();
  }

  consultar(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.api.demostracionWs().subscribe({
      next: (d) => {
        this.demo.set(d);
        this.cargando.set(false);
        // Si el WS respondio, se refresca tambien el indicador de la barra.
        if (d.referencia) this.api.cargarTipoCambio(false).subscribe();
      },
      error: (e: Error) => {
        this.error.set(e.message);
        this.cargando.set(false);
      }
    });
  }
}
