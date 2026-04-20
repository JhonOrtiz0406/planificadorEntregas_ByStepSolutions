import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { OrganizationService } from '../../../core/services/organization.service';
import { AuthService } from '../../../core/services/auth.service';
import { Organization } from '../../../core/models/organization.model';

@Component({
  selector: 'app-org-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatChipsModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h2><mat-icon>business</mat-icon> Organizaciones</h2>
        @if (authService.isPlatformAdmin()) {
          <button mat-raised-button color="primary" routerLink="/organizations/new">
            <mat-icon>add</mat-icon> Nueva Organización
          </button>
        }
      </div>
      @if (loading()) {
        <div style="display:flex;justify-content:center;padding:64px"><mat-spinner diameter="48"></mat-spinner></div>
      } @else {
        <div class="orgs-grid">
          @for (org of orgs(); track org.id) {
            <mat-card class="org-card" [routerLink]="['/organizations', org.id]">
              <mat-card-header>
                @if (org.logoUrl) {
                  <img mat-card-avatar [src]="org.logoUrl" [alt]="org.name"
                       style="object-fit:cover;border-radius:50%">
                } @else if (org.iconName) {
                  <mat-icon mat-card-avatar style="font-size:40px;color:#6366f1">{{ org.iconName }}</mat-icon>
                } @else {
                  <div mat-card-avatar [style.background]="getOrgColor(org.name)"
                       style="display:flex;align-items:center;justify-content:center;
                              border-radius:50%;font-size:18px;font-weight:700;color:#fff">
                    {{ org.name.charAt(0).toUpperCase() }}
                  </div>
                }
                <mat-card-title>{{ org.name }}</mat-card-title>
                <mat-card-subtitle>{{ org.adminEmail }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <mat-chip [class]="org.active ? 'chip-active' : 'chip-inactive'">
                  {{ org.active ? 'Activa' : 'Inactiva' }}
                </mat-chip>
              </mat-card-content>
            </mat-card>
          }
          @if (orgs().length === 0) {
            <div style="grid-column:1/-1;text-align:center;padding:48px;color:#999">
              <mat-icon style="font-size:48px;width:48px;height:48px">business_center</mat-icon>
              <p>No hay organizaciones registradas</p>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { display: flex; flex-direction: column; gap: 24px; }
    .page-header { display: flex; align-items: center; justify-content: space-between; }
    .page-header h2 { display: flex; align-items: center; gap: 8px; margin: 0; }
    .orgs-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
    .org-card { cursor: pointer; transition: box-shadow 0.2s; }
    .org-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.15) !important; }
    .chip-active { background: #d1fae5 !important; color: #065f46 !important; }
    .chip-inactive { background: #fee2e2 !important; color: #991b1b !important; }
  `]
})
export class OrgListComponent implements OnInit {
  private orgService = inject(OrganizationService);
  authService = inject(AuthService);

  orgs = signal<Organization[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.orgService.getAll().subscribe({
      next: (orgs) => { this.orgs.set(orgs); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  getOrgColor(name: string): string {
    const colors = ['#6366f1','#0ea5e9','#10b981','#f59e0b','#ef4444','#8b5cf6','#ec4899','#14b8a6'];
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  }
}
