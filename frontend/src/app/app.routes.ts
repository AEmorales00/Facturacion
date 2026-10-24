import { Routes } from '@angular/router';

/* Las pantallas se cargan con loadComponent (lazy loading): el navegador
   descarga el codigo de cada una solo cuando se entra, no todo de golpe. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'facturacion' },
  {
    path: 'facturacion',
    title: 'Facturacion',
    loadComponent: () =>
      import('./paginas/facturacion/facturacion.component').then((m) => m.FacturacionComponent)
  },
  {
    path: 'catalogo',
    title: 'Catalogo',
    loadComponent: () =>
      import('./paginas/catalogo/catalogo.component').then((m) => m.CatalogoComponent)
  },
  {
    path: 'productos',
    title: 'Productos',
    loadComponent: () =>
      import('./paginas/productos/productos.component').then((m) => m.ProductosComponent)
  },
  {
    path: 'facturas',
    title: 'Facturas emitidas',
    loadComponent: () =>
      import('./paginas/facturas/facturas.component').then((m) => m.FacturasComponent)
  },
  {
    path: 'web-service',
    title: 'Web Service del Banguat',
    loadComponent: () => import('./paginas/demo-ws/demo-ws.component').then((m) => m.DemoWsComponent)
  },
  { path: '**', redirectTo: 'facturacion' }
];
