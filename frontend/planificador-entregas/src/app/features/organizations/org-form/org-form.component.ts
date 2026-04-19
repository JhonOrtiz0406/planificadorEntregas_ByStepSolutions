import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrganizationService } from '../../../core/services/organization.service';
import { CategoryStatusService } from '../../../core/services/category-status.service';

@Component({
  selector: 'app-org-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatSelectModule, MatSnackBarModule, MatProgressSpinnerModule],
  template: `
    <div style="max-width:600px;margin:0 auto">
      <mat-card>
        <mat-card-header>
          <mat-card-title><mat-icon>business</mat-icon> Nueva Organización</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" style="display:flex;flex-direction:column;gap:16px;margin-top:16px">
            <mat-form-field appearance="outline">
              <mat-label>Nombre de la organización *</mat-label>
              <input matInput formControlName="name" placeholder="Ej: Joyería El Diamante">
              <mat-icon matSuffix>business</mat-icon>
              @if (form.get('name')?.hasError('required')) {
                <mat-error>El nombre es requerido</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Categoría *</mat-label>
              <mat-select formControlName="category">
                @for (cat of categoryEntries(); track cat.key) {
                  <mat-option [value]="cat.key">{{ cat.label }}</mat-option>
                }
              </mat-select>
              <mat-icon matSuffix>category</mat-icon>
              @if (form.get('category')?.hasError('required')) {
                <mat-error>La categoría es requerida</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Logo URL (opcional)</mat-label>
              <input matInput formControlName="logoUrl" placeholder="https://...">
              <mat-icon matSuffix>image</mat-icon>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Email del administrador *</mat-label>
              <input matInput formControlName="adminEmail" type="email" placeholder="admin@empresa.com">
              <mat-icon matSuffix>email</mat-icon>
              @if (form.get('adminEmail')?.hasError('required')) {
                <mat-error>El email es requerido</mat-error>
              }
              @if (form.get('adminEmail')?.hasError('email')) {
                <mat-error>Email inválido</mat-error>
              }
            </mat-form-field>
            <div style="display:flex;gap:12px;justify-content:flex-end">
              <button mat-button type="button" routerLink="/organizations">Cancelar</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading()">
                @if (loading()) { <mat-spinner diameter="20" style="display:inline-block;margin-right:8px"></mat-spinner> }
                Crear Organización
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class OrgFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private orgService = inject(OrganizationService);
  private categoryService = inject(CategoryStatusService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  loading = signal(false);
  categoryEntries = signal<{ key: string; label: string }[]>([]);

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    category: ['GENERAL', [Validators.required]],
    logoUrl: [''],
    adminEmail: ['', [Validators.required, Validators.email]]
  });

  ngOnInit(): void {
    this.categoryService.getAllCategories().subscribe(cats => {
      this.categoryEntries.set(Object.entries(cats).map(([key, label]) => ({ key, label })));
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.orgService.create(this.form.value as any).subscribe({
      next: (org) => {
        this.snackBar.open('Organización creada. Invitación enviada al administrador.', 'Cerrar', { duration: 4000 });
        this.router.navigate(['/organizations', org.id]);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al crear la organización', 'Cerrar', { duration: 4000 });
        this.loading.set(false);
      }
    });
  }
}
