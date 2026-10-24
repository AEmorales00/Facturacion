import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { ApiService } from './servicios/api.service';

/**
 * Armazon de la aplicacion: la navegacion lateral y el indicador del tipo
 * de cambio, que acompana al usuario en todas las pantallas porque es el
 * dato con el que se convierte cada total a quetzales.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DecimalPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private api = inject(ApiService);

  readonly tipoCambio = this.api.tipoCambio;
  readonly consultando = signal(false);
  readonly menuAbierto = signal(false);

  ngOnInit(): void {
    this.actualizar(false);
  }

  /** @param forzar true vuelve a preguntarle al Web Service en vez de usar la cache. */
  actualizar(forzar: boolean): void {
    this.consultando.set(true);
    this.api.cargarTipoCambio(forzar).subscribe({
      next: () => this.consultando.set(false),
      error: () => this.consultando.set(false)
    });
  }

  alternarMenu(): void {
    this.menuAbierto.update((abierto) => !abierto);
  }

  cerrarMenu(): void {
    this.menuAbierto.set(false);
  }
}
