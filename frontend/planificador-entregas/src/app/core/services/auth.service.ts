import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, User } from '../models/user.model';

declare const google: any;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly TOKEN_KEY = 'dp_auth_token';
  private readonly USER_KEY = 'dp_user';

  currentUser = signal<User | null>(this.getStoredUser());
  isAuthenticated = signal<boolean>(this.checkAuth());

  constructor() {
    // Auto-logout if token is already expired on startup
    if (this.getToken() && this.isTokenExpired()) {
      this.clearSession();
    }
  }

  /** Verifica si el JWT almacenado es válido y no ha expirado */
  private checkAuth(): boolean {
    const token = this.getToken();
    if (!token) return false;
    return !this.isTokenExpiredFor(token);
  }

  /** Devuelve true si el token almacenado ha expirado */
  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;
    return this.isTokenExpiredFor(token);
  }

  private isTokenExpiredFor(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expMs = payload.exp * 1000;
      return Date.now() >= expMs;
    } catch {
      return true;
    }
  }

  initGoogleAuth(callback: (response: any) => void): void {
    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback,
      auto_select: false,
      cancel_on_tap_outside: true
    });
  }

  renderGoogleButton(element: HTMLElement): void {
    google.accounts.id.renderButton(element, {
      theme: 'outline',
      size: 'large',
      width: 300,
      text: 'signin_with',
      shape: 'rectangular',
      logo_alignment: 'left'
    });
  }

  loginWithGoogle(idToken: string, fcmToken?: string, invitationToken?: string): Observable<any> {
    return this.http.post<any>(`${environment.apiUrl}/auth/google`, {
      idToken,
      fcmToken,
      invitationToken
    }).pipe(
      tap(response => {
        if (response.success) {
          this.setSession(response.data);
        }
      })
    );
  }

  private setSession(authResponse: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, authResponse.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(authResponse.user));
    this.currentUser.set(authResponse.user);
    this.isAuthenticated.set(true);
  }

  private clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }

  logout(): void {
    this.clearSession();
    try {
      google.accounts.id.disableAutoSelect();
    } catch { /* google may not be loaded */ }
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private getStoredUser(): User | null {
    const stored = localStorage.getItem(this.USER_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  hasRole(...roles: string[]): boolean {
    const user = this.currentUser();
    return user ? roles.includes(user.role) : false;
  }

  isOrgAdmin(): boolean {
    return this.hasRole('ORG_ADMIN');
  }

  isPlatformAdmin(): boolean {
    return this.hasRole('PLATFORM_ADMIN');
  }

  isEmployee(): boolean {
    return this.hasRole('ORG_EMPLOYEE');
  }

  isDelivery(): boolean {
    return this.hasRole('ORG_DELIVERY');
  }

  canManageOrders(): boolean {
    return this.hasRole('ORG_ADMIN');
  }

  canCreateOrders(): boolean {
    return this.hasRole('ORG_ADMIN', 'ORG_EMPLOYEE');
  }

  canUpdateStatus(): boolean {
    return this.hasRole('ORG_ADMIN');
  }

  canAccessOrders(): boolean {
    return this.hasRole('ORG_ADMIN', 'ORG_EMPLOYEE', 'ORG_DELIVERY');
  }
}
