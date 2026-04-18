import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // App SaaS con autenticación — renderizado del lado del cliente para todas las rutas
  { path: '**', renderMode: RenderMode.Client }
];
