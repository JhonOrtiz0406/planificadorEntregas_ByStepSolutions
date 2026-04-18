import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-invite-accept',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="invite-container">
      <mat-card class="invite-card">
        @if (loading()) {
          <mat-card-content style="text-align:center;padding:48px">
            <mat-spinner diameter="48" style="margin:0 auto 16px"></mat-spinner>
            <p>Verificando invitación...</p>
          </mat-card-content>
        } @else if (valid()) {
          <mat-card-header>
            <mat-card-title><mat-icon>mail</mat-icon> Invitación válida</mat-card-title>
          </mat-card-header>
          <mat-card-content style="padding:24px">
            <p>Has sido invitado a unirte como <strong>{{ getRoleLabel(invitation()?.role) }}</strong>.</p>
            <p>Inicia sesión con tu cuenta de Google para continuar.</p>
            <button mat-raised-button color="primary" (click)="acceptAndLogin()">
              <mat-icon>login</mat-icon> Iniciar sesión con Google
            </button>
          </mat-card-content>
        } @else {
          <mat-card-header>
            <mat-card-title><mat-icon color="warn">error</mat-icon> Invitación inválida</mat-card-title>
          </mat-card-header>
          <mat-card-content style="padding:24px">
            <p>Esta invitación no es válida, ha expirado o ya fue utilizada.</p>
            <button mat-button routerLink="/auth/login">Volver al inicio</button>
          </mat-card-content>
        }
      </mat-card>
    </div>
  `,
  styles: [`.invite-container { min-height:100vh; display:flex; align-items:center; justify-content:center; background:linear-gradient(135deg,#3f51b5,#1a237e); padding:16px; }
    .invite-card { max-width:460px; width:100%; border-radius:16px !important; }`]
})
export class InviteAcceptComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);

  loading = signal(true);
  valid = signal(false);
  invitation = signal<any>(null);
  token = '';

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token')!;
    this.http.get<any>(`${environment.apiUrl}/invitations/accept/${this.token}`).subscribe({
      next: (res) => {
        if (res.success) { this.invitation.set(res.data); this.valid.set(true); }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  acceptAndLogin(): void {
    this.router.navigate(['/auth/login'], { queryParams: { token: this.token } });
  }

  getRoleLabel(role: string): string {
    return { 'ORG_ADMIN': 'Administrador', 'ORG_EMPLOYEE': 'Empleado', 'ORG_DELIVERY': 'Repartidor' }[role] || role;
  }
}
