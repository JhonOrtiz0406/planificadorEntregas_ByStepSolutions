import { Component, OnInit, AfterViewInit, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-no-org-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <div style="padding:8px">
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
        <mat-icon style="color:#e53935;font-size:32px;height:32px;width:32px">block</mat-icon>
        <h2 mat-dialog-title style="margin:0;color:#1e1b4b;font-size:1.1rem;font-weight:700">
          Sin acceso a la plataforma
        </h2>
      </div>
      <mat-dialog-content>
        <p style="color:#4b5563;line-height:1.6;margin:0 0 12px">
          Tu cuenta de Google no pertenece a ninguna organización activa en DeliveryPlanner.
        </p>
        <p style="color:#4b5563;line-height:1.6;margin:0">
          Si crees que es un error, contacta al administrador de tu organización o al soporte de
          <strong>ByStep Solutions</strong>.<br>
          <span style="color:#9ca3af;font-size:.85rem">
            Tras {{ attemptsLeft }} intento{{ attemptsLeft === 1 ? '' : 's' }} más se te
            mostrará el formulario de contacto.
          </span>
        </p>
      </mat-dialog-content>
      <mat-dialog-actions align="end" style="padding-top:16px">
        <button mat-raised-button color="primary" (click)="close()">Entendido</button>
      </mat-dialog-actions>
    </div>
  `
})
export class NoOrgDialogComponent {
  dialogRef = inject(MatDialogRef<NoOrgDialogComponent>);
  attemptsLeft = 1;
  close() { this.dialogRef.close(); }
}

declare const google: any;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule],
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
  private dialog = inject(MatDialog);

  private readonly NO_ACCESS_MAX_ATTEMPTS = 3;

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
            const data = res.data;
            if (data.requiresOrgSelection) {
              this.authService.storeSelectionState(data.selectionToken, data.availableOrgs);
              this.router.navigate(['/select-org']);
            } else if (data.noOrgAccess) {
              this.authService.storeNoAccessUser(data.user);
              this.handleNoOrgAccess(data.user?.email);
            } else {
              const user = data.user;
              const redirectTo = user.organizationId ? '/dashboard' : '/organizations';
              this.router.navigate([redirectTo]);
            }
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

  private handleNoOrgAccess(email: string | undefined): void {
    const key = email ? 'dp_noa_' + btoa(email) : 'dp_noa_unknown';
    const count = parseInt(localStorage.getItem(key) || '0', 10) + 1;

    if (count >= this.NO_ACCESS_MAX_ATTEMPTS) {
      localStorage.removeItem(key);
      this.router.navigate(['/auth/no-access']);
      return;
    }

    localStorage.setItem(key, String(count));
    const attemptsLeft = this.NO_ACCESS_MAX_ATTEMPTS - count;
    const ref = this.dialog.open(NoOrgDialogComponent, {
      width: '440px',
      disableClose: true
    });
    ref.componentInstance.attemptsLeft = attemptsLeft;
  }
}
