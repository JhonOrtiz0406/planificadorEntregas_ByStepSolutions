import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Si el token está expirado, cerrar sesión inmediatamente
  if (authService.getToken() && authService.isTokenExpired()) {
    authService.logout();
    return false;
  }

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/auth/login']);
  return false;
};

export const roleGuard = (...roles: string[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.getToken() && authService.isTokenExpired()) {
      authService.logout();
      return false;
    }

    if (!authService.isAuthenticated()) {
      router.navigate(['/auth/login']);
      return false;
    }

    if (authService.hasRole(...roles)) {
      return true;
    }

    // Redirigir según el rol del usuario
    const user = authService.currentUser();
    if (user?.role === 'PLATFORM_ADMIN') {
      router.navigate(['/organizations']);
    } else {
      router.navigate(['/dashboard']);
    }
    return false;
  };
};
