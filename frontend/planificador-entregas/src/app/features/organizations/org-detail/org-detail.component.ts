import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialogModule, MatDialog, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
import { OrganizationService } from '../../../core/services/organization.service';
import { AuthService } from '../../../core/services/auth.service';
import { Organization } from '../../../core/models/organization.model';
import { User } from '../../../core/models/user.model';
import { InactiveMemberDialogComponent } from './inactive-member-dialog.component';

// --- Delete Org Dialog ---
@Component({
  selector: 'app-delete-org-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatInputModule, FormsModule],
  template: `
    <mat-dialog-content style="padding:24px;min-width:320px">
      <div style="text-align:center;margin-bottom:16px">
        <mat-icon style="font-size:48px;width:48px;height:48px;color:#ef4444">delete_forever</mat-icon>
      </div>
      <h3 style="margin:0 0 8px;color:#1e1b4b;text-align:center">Eliminar organización</h3>
      <p style="color:#6b7280;font-size:.9rem;margin:0 0 16px">
        Esta acción es <strong>irreversible</strong>. Se eliminarán todos los datos asociados.<br>
        Escribe <strong>{{ data.orgName }}</strong> para confirmar:
      </p>
      <input [(ngModel)]="confirmation" style="width:100%;padding:8px;border:1px solid #d1d5db;border-radius:6px;font-size:.95rem"
             placeholder="Nombre de la organización">
    </mat-dialog-content>
    <mat-dialog-actions align="end" style="padding:16px 24px;gap:8px">
      <button mat-stroked-button (click)="close(false)">Cancelar</button>
      <button mat-raised-button color="warn" [disabled]="confirmation !== data.orgName" (click)="close(true)">
        <mat-icon>delete_forever</mat-icon> Eliminar definitivamente
      </button>
    </mat-dialog-actions>
  `
})
export class DeleteOrgDialogComponent {
  data: { orgName: string } = inject(MAT_DIALOG_DATA);
  private ref = inject(MatDialogRef<DeleteOrgDialogComponent>);
  confirmation = '';
  close(confirmed: boolean): void { this.ref.close(confirmed); }
}

// --- Icon Picker Dialog ---
const ORG_ICONS = [
  'business', 'diamond', 'local_florist', 'local_pharmacy', 'local_shipping',
  'inventory_2', 'house', 'store', 'storefront', 'restaurant',
  'cake', 'pets', 'sports_soccer', 'fitness_center', 'spa',
  'medical_services', 'school', 'account_balance', 'work', 'factory'
];

@Component({
  selector: 'app-icon-picker-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <mat-dialog-content style="padding:24px;min-width:360px">
      <h3 style="margin:0 0 16px;color:#1e1b4b">Seleccionar icono</h3>
      <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:8px">
        @for (icon of icons; track icon) {
          <button mat-icon-button
            [style.background]="selected === icon ? '#e0e7ff' : 'transparent'"
            [style.border]="selected === icon ? '2px solid #6366f1' : '2px solid transparent'"
            [style.border-radius]="'8px'"
            (click)="selected = icon" [title]="icon">
            <mat-icon>{{ icon }}</mat-icon>
          </button>
        }
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end" style="padding:16px 24px;gap:8px">
      <button mat-stroked-button (click)="close(null)">Cancelar</button>
      <button mat-raised-button color="primary" [disabled]="!selected" (click)="close(selected)">
        Confirmar
      </button>
    </mat-dialog-actions>
  `
})
export class IconPickerDialogComponent {
  private ref = inject(MatDialogRef<IconPickerDialogComponent>);
  icons = ORG_ICONS;
  selected = '';
  close(icon: string | null): void { this.ref.close(icon); }
}

@Component({
  selector: 'app-org-detail',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBarModule,
    MatProgressSpinnerModule, MatChipsModule, MatDividerModule, MatDialogModule
  ],
  templateUrl: './org-detail.component.html',
  styleUrl: './org-detail.component.css'
})
export class OrgDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private orgService = inject(OrganizationService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private fb = inject(FormBuilder);
  authService = inject(AuthService);

  org = signal<Organization | null>(null);
  members = signal<User[]>([]);
  loading = signal(true);
  loadError = signal(false);
  inviting = signal(false);

  inviteForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['ORG_EMPLOYEE', Validators.required]
  });

  memberColumns = ['status', 'name', 'email', 'role', 'actions'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.orgService.getById(id).subscribe({
      next: (org) => { this.org.set(org); this.loading.set(false); this.loadMembers(id); },
      error: () => { this.loading.set(false); this.loadError.set(true); }
    });
  }

  private loadMembers(orgId: string): void {
    this.orgService.getMembers(orgId).subscribe({
      next: (members) => this.members.set(members)
    });
  }

  inviteMember(): void {
    if (this.inviteForm.invalid || !this.org()) return;
    this.inviting.set(true);
    this.orgService.inviteMember(this.org()!.id, this.inviteForm.value as any).subscribe({
      next: () => {
        this.snackBar.open(`Invitación enviada a ${this.inviteForm.value.email}`, 'Cerrar', { duration: 3000 });
        this.inviteForm.reset({ role: 'ORG_EMPLOYEE' });
        this.inviting.set(false);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al enviar invitación', 'Cerrar', { duration: 4000 });
        this.inviting.set(false);
      }
    });
  }

  deactivateMember(userId: string): void {
    if (!confirm('¿Inhabilitar a este miembro?')) return;
    this.orgService.removeMember(this.org()!.id, userId).subscribe({
      next: () => {
        this.members.update(m => m.map(u => u.id === userId ? { ...u, active: false } : u));
        this.snackBar.open('Miembro inhabilitado', 'Cerrar', { duration: 2000 });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al inhabilitar miembro', 'Cerrar', { duration: 3000 });
      }
    });
  }

  openInactiveMemberDialog(member: User): void {
    const ref = this.dialog.open(InactiveMemberDialogComponent, {
      width: '420px',
      data: { member }
    });
    ref.afterClosed().subscribe(action => {
      if (action === 'delete') {
        this.orgService.deleteMemberPermanently(this.org()!.id, member.id).subscribe({
          next: () => {
            this.members.update(m => m.filter(u => u.id !== member.id));
            this.snackBar.open('Miembro eliminado permanentemente', 'Cerrar', { duration: 2000 });
          },
          error: (err) => {
            this.snackBar.open(err.error?.message || 'Error al eliminar miembro', 'Cerrar', { duration: 3000 });
          }
        });
      }
    });
  }

  openIconPicker(): void {
    const ref = this.dialog.open(IconPickerDialogComponent, { width: '380px' });
    ref.afterClosed().subscribe(iconName => {
      if (iconName) {
        this.orgService.updateIcon(this.org()!.id, iconName).subscribe({
          next: (updated) => {
            this.org.set(updated);
            this.snackBar.open('Icono actualizado', 'Cerrar', { duration: 2000 });
          },
          error: () => this.snackBar.open('Error al actualizar icono', 'Cerrar', { duration: 3000 })
        });
      }
    });
  }

  toggleOrgStatus(): void {
    const o = this.org();
    if (!o) return;
    const action = o.active ? this.orgService.disable(o.id) : this.orgService.enable(o.id);
    action.subscribe({
      next: () => {
        this.org.update(prev => prev ? { ...prev, active: !prev.active } : prev);
        this.snackBar.open(o.active ? 'Organización deshabilitada' : 'Organización habilitada', 'Cerrar', { duration: 2000 });
      },
      error: (err) => this.snackBar.open(err.error?.message || 'Error', 'Cerrar', { duration: 3000 })
    });
  }

  openDeleteOrgDialog(): void {
    const o = this.org();
    if (!o) return;
    const ref = this.dialog.open(DeleteOrgDialogComponent, {
      width: '440px',
      data: { orgName: o.name }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.orgService.delete(o.id).subscribe({
          next: () => {
            this.snackBar.open('Organización eliminada', 'Cerrar', { duration: 2000 });
            this.router.navigate(['/organizations']);
          },
          error: (err) => this.snackBar.open(err.error?.message || 'Error al eliminar', 'Cerrar', { duration: 3000 })
        });
      }
    });
  }

  getRoleLabel(role: string): string {
    return {
      'ORG_ADMIN': 'Administrador',
      'ORG_EMPLOYEE': 'Empleado',
      'ORG_DELIVERY': 'Repartidor',
      'PLATFORM_ADMIN': 'Admin Plataforma'
    }[role] || role;
  }

  canManageMember(member: User): boolean {
    return (this.authService.isOrgAdmin() || this.authService.isPlatformAdmin())
      && member.role !== 'PLATFORM_ADMIN';
  }
}
