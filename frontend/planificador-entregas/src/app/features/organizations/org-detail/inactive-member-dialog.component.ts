import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-inactive-member-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <mat-dialog-content style="padding:24px;text-align:center">
      <mat-icon style="font-size:56px;width:56px;height:56px;color:#f59e0b;margin-bottom:12px">
        person_off
      </mat-icon>
      <h3 style="margin:0 0 8px;color:#1e1b4b">Miembro inhabilitado</h3>
      <p style="color:#6b7280;margin:0 0 4px">
        <strong>{{ data.member.name || data.member.email }}</strong>
      </p>
      <p style="color:#9ca3af;font-size:.85rem;margin:0">
        Esta cuenta está inhabilitada y no puede iniciar sesión.
        ¿Qué deseas hacer?
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end" style="padding:16px 24px;gap:8px">
      <button mat-stroked-button (click)="close('keep')">
        <mat-icon>lock</mat-icon>
        Mantener inhabilitado
      </button>
      <button mat-raised-button color="warn" (click)="close('delete')">
        <mat-icon>delete_forever</mat-icon>
        Eliminar permanentemente
      </button>
    </mat-dialog-actions>
  `
})
export class InactiveMemberDialogComponent {
  data: { member: User } = inject(MAT_DIALOG_DATA);
  private ref = inject(MatDialogRef<InactiveMemberDialogComponent>);

  close(action: 'keep' | 'delete'): void {
    this.ref.close(action);
  }
}
