import { Component, EventEmitter, Input, OnChanges, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OrganizationService } from '../../../core/services/organization.service';
import { Organization } from '../../../core/models/organization.model';
import { apiErrorMessage } from '../../../core/services/whatsapp.service';

const PHONE_PATTERN = /^\+?[0-9\s-]{10,18}$/;

/** Datos del administrador (responsable) y número principal de la organización. */
@Component({
  selector: 'app-org-contact-card',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatSnackBarModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title><mat-icon>badge</mat-icon> Administrador y contacto</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @if (!editing()) {
          <div class="contact-grid">
            <div><span class="lbl">Nombre</span>
              <span>{{ fullName() || '—' }}</span></div>
            <div><span class="lbl">Email</span><span>{{ org.adminEmail || '—' }}</span></div>
            <div><span class="lbl">Celular personal</span><span>{{ formatPhone(org.adminPhone) }}</span></div>
            <div><span class="lbl">Celular de la organización (WhatsApp)</span>
              <span>{{ formatPhone(org.organizationPhone) }}</span></div>
          </div>
          @if (canEdit) {
            <button mat-stroked-button (click)="startEdit()" style="margin-top:12px">
              <mat-icon>edit</mat-icon> Editar
            </button>
          }
        } @else {
          <form [formGroup]="form" (ngSubmit)="save()" style="display:flex;flex-direction:column;gap:4px;margin-top:8px">
            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>Nombres *</mat-label>
                <input matInput formControlName="adminFirstName">
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Apellidos *</mat-label>
                <input matInput formControlName="adminLastName">
              </mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>Celular personal *</mat-label>
                <input matInput formControlName="adminPhone" type="tel" placeholder="300 123 4567">
                @if (form.get('adminPhone')?.hasError('pattern')) { <mat-error>Celular inválido</mat-error> }
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Celular de la organización (WhatsApp)</mat-label>
                <input matInput formControlName="organizationPhone" type="tel" placeholder="300 765 4321">
                @if (form.get('organizationPhone')?.hasError('pattern')) { <mat-error>Celular inválido</mat-error> }
              </mat-form-field>
            </div>
            <div style="display:flex;gap:8px;justify-content:flex-end">
              <button mat-button type="button" (click)="editing.set(false)">Cancelar</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || saving()">
                @if (saving()) { <mat-spinner diameter="18" style="display:inline-block;margin-right:6px"></mat-spinner> }
                Guardar
              </button>
            </div>
          </form>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .contact-grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:12px; }
    .contact-grid > div { display:flex; flex-direction:column; gap:2px; }
    .lbl { font-size:.75rem; color:var(--text-secondary); text-transform:uppercase; letter-spacing:.03em; }
    .row { display:flex; gap:12px; flex-wrap:wrap; }
    .row mat-form-field { flex:1; min-width:200px; }
  `]
})
export class OrgContactCardComponent implements OnChanges {
  @Input({ required: true }) org!: Organization;
  @Input() canEdit = false;
  @Output() orgUpdated = new EventEmitter<Organization>();

  private fb = inject(FormBuilder);
  private orgService = inject(OrganizationService);
  private snackBar = inject(MatSnackBar);

  editing = signal(false);
  saving = signal(false);

  form = this.fb.group({
    adminFirstName: ['', [Validators.required, Validators.maxLength(100)]],
    adminLastName: ['', [Validators.required, Validators.maxLength(100)]],
    adminPhone: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
    organizationPhone: ['', [Validators.pattern(PHONE_PATTERN)]]
  });

  ngOnChanges(): void {
    this.editing.set(false);
  }

  fullName(): string {
    return [this.org.adminFirstName, this.org.adminLastName].filter(Boolean).join(' ');
  }

  formatPhone(phone?: string): string {
    if (!phone) return '—';
    const d = phone.replace(/\D/g, '');
    if (d.length === 12 && d.startsWith('57')) {
      return `+57 ${d.slice(2, 5)} ${d.slice(5, 8)} ${d.slice(8)}`;
    }
    return phone;
  }

  startEdit(): void {
    this.form.reset({
      adminFirstName: this.org.adminFirstName ?? '',
      adminLastName: this.org.adminLastName ?? '',
      adminPhone: this.org.adminPhone ?? '',
      organizationPhone: this.org.organizationPhone ?? ''
    });
    this.editing.set(true);
  }

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.saving.set(true);
    this.orgService.update(this.org.id, {
      adminFirstName: v.adminFirstName?.trim() ?? '',
      adminLastName: v.adminLastName?.trim() ?? '',
      adminPhone: v.adminPhone?.trim() ?? '',
      organizationPhone: v.organizationPhone?.trim() ?? ''
    }).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.editing.set(false);
        this.orgUpdated.emit(updated);
        this.snackBar.open('Datos actualizados', 'Cerrar', { duration: 2500 });
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open(apiErrorMessage(err, 'Error al guardar'), 'Cerrar', { duration: 4000 });
      }
    });
  }
}
