import { Component, OnInit, AfterViewInit, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { environment } from '../../../../environments/environment';

declare const google: any;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit, AfterViewInit {
  // static: false — el elemento está dentro de un bloque @else,
  // Angular solo lo resuelve después del primer ciclo de detección de cambios
  @ViewChild('googleBtn', { static: false }) googleBtn!: ElementRef;

  authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);

  loading = false;
  appName = environment.appName;
  companyName = environment.companyName;
  invitationToken: string | null = null;

  private mapErrorMessage(msg: string): string {
    const map: Record<string, string> = {
      'Your account has been deactivated':
        'Tu cuenta ha sido deshabilitada. Contacta al administrador de tu organización.',
      'Access denied. You need an invitation to join the platform.':
        'No tienes acceso a la plataforma. Solicita una invitación a tu organización.',
      'Invalid Google token':
        'Token de Google inválido. Intenta iniciar sesión nuevamente.',
      'Email does not match invitation':
        'El correo no coincide con la invitación recibida.',
      'Invalid invitation':
        'La invitación es inválida o ha expirado. Solicita una nueva.',
      'Invitation already accepted':
        'Esta invitación ya fue utilizada anteriormente.',
    };
    return map[msg] || msg || 'Error al iniciar sesión. Verifica tu acceso.';
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }
    this.invitationToken = this.route.snapshot.queryParamMap.get('token');
  }

  ngAfterViewInit(): void {
    // Cargar Google Identity Services después de que la vista esté lista
    // (el div #googleBtn ya existe en el DOM en este punto)
    this.loadGoogleScript();
  }

  private loadGoogleScript(): void {
    if (typeof google !== 'undefined') {
      this.initGoogle();
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => this.initGoogle();
    script.onerror = () => {
      this.snackBar.open('Error cargando Google Sign-In. Verifica tu conexión.', 'Cerrar', { duration: 5000 });
    };
    document.head.appendChild(script);
  }

  private initGoogle(): void {
    if (!this.googleBtn?.nativeElement) {
      console.error('Google button container not found');
      return;
    }
    this.authService.initGoogleAuth(this.handleGoogleResponse.bind(this));
    this.authService.renderGoogleButton(this.googleBtn.nativeElement);
  }

  private async handleGoogleResponse(response: any): Promise<void> {
    this.loading = true;
    try {
      const fcmToken = await this.notificationService.requestPermissionAndGetToken();
      this.authService.loginWithGoogle(
        response.credential,
        fcmToken || undefined,
        this.invitationToken || undefined
      ).subscribe({
        next: (res) => {
          if (res.success) {
            const user = res.data.user;
            const redirectTo = user.organizationId ? '/dashboard' : '/organizations';
            this.router.navigate([redirectTo]);
          } else {
            this.snackBar.open(res.message || 'Error al iniciar sesión', 'Cerrar', { duration: 5000 });
          }
          this.loading = false;
        },
        error: (err) => {
          const raw = err.error?.message || '';
          const msg = this.mapErrorMessage(raw);
          this.snackBar.open(msg, 'Cerrar', { duration: 7000, panelClass: ['snack-error'] });
          this.loading = false;
        }
      });
    } catch {
      this.loading = false;
    }
  }
}
