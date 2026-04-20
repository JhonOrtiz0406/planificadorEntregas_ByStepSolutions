import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';
import { OrgChoice } from '../../../core/models/user.model';

@Component({
  selector: 'app-org-selection',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatSnackBarModule],
  template: `
    <div class="selection-container">
      <mat-card class="selection-card">
        <mat-card-header>
          <mat-icon mat-card-avatar>business</mat-icon>
          <mat-card-title>Selecciona una Organización</mat-card-title>
          <mat-card-subtitle>Tienes acceso a múltiples organizaciones</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="org-list">
            @for (org of orgs(); track org.id) {
              <button class="org-btn" (click)="selectOrg(org)" [disabled]="loading()">
                <div class="org-icon">
                  <mat-icon>{{ org.iconName || 'business' }}</mat-icon>
                </div>
                <div class="org-info">
                  <span class="org-name">{{ org.name }}</span>
                  <span class="org-role">{{ roleLabel(org.userRole) }}</span>
                </div>
                @if (loading() && selectedId() === org.id) {
                  <mat-spinner diameter="20"></mat-spinner>
                } @else {
                  <mat-icon class="arrow">chevron_right</mat-icon>
                }
              </button>
            }
          </div>
          <div class="logout-row">
            <button mat-button color="warn" (click)="logout()">
              <mat-icon>logout</mat-icon> Cerrar Sesión
            </button>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .selection-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: #f5f5f5;
      padding: 16px;
    }
    .selection-card {
      width: 100%;
      max-width: 440px;
    }
    .org-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: 16px;
    }
    .org-btn {
      display: flex;
      align-items: center;
      gap: 16px;
      width: 100%;
      padding: 16px;
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      text-align: left;
    }
    .org-btn:hover:not(:disabled) {
      border-color: #1976d2;
      background: #f3f6ff;
      box-shadow: 0 2px 8px rgba(25,118,210,0.1);
    }
    .org-btn:disabled { opacity: 0.6; cursor: not-allowed; }
    .org-icon { color: #1976d2; }
    .org-info { flex: 1; display: flex; flex-direction: column; }
    .org-name { font-weight: 500; font-size: 15px; }
    .org-role { font-size: 12px; color: #666; }
    .arrow { color: #999; }
    .logout-row { display: flex; justify-content: center; margin-top: 16px; }
  `]
})
export class OrgSelectionComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  orgs = signal<OrgChoice[]>([]);
  loading = signal(false);
  selectedId = signal<string | null>(null);

  ngOnInit(): void {
    const choices = this.authService.getStoredOrgChoices();
    const token = this.authService.getSelectionToken();
    if (!choices.length || !token) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.orgs.set(choices);
  }

  selectOrg(org: OrgChoice): void {
    const token = this.authService.getSelectionToken();
    if (!token) return;
    this.loading.set(true);
    this.selectedId.set(org.id);
    this.authService.selectOrganization(token, org.id).subscribe({
      next: (res) => {
        if (res.success) {
          this.authService.clearSelectionState();
          this.authService.setSession(res.data);
          this.router.navigate(['/dashboard']);
        } else {
          this.snackBar.open(res.message || 'Error al seleccionar organización', 'Cerrar', { duration: 4000 });
          this.loading.set(false);
          this.selectedId.set(null);
        }
      },
      error: () => {
        this.snackBar.open('Error al seleccionar organización', 'Cerrar', { duration: 4000 });
        this.loading.set(false);
        this.selectedId.set(null);
      }
    });
  }

  roleLabel(role: string): string {
    const map: Record<string, string> = {
      ORG_ADMIN: 'Administrador',
      ORG_EMPLOYEE: 'Empleado',
      ORG_DELIVERY: 'Repartidor',
      PLATFORM_ADMIN: 'Admin de Plataforma'
    };
    return map[role] ?? role;
  }

  logout(): void {
    this.authService.logout();
  }
}
