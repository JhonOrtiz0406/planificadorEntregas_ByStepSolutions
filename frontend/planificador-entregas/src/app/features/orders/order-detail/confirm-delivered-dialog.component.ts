import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-confirm-delivered-dialog',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <mat-dialog-content style="padding:32px 24px 16px;text-align:center">
      <mat-icon style="font-size:56px;width:56px;height:56px;color:#4f46e5;margin-bottom:12px">
        local_shipping
      </mat-icon>
      <h3 style="margin:0 0 8px;color:#1e1b4b">¿El pedido ya fue entregado?</h3>
      <p style="color:#6b7280;margin:0;font-size:.95rem">
        Has marcado el pago como <strong>Pagado</strong>.<br>
        ¿El pedido también fue entregado al cliente?
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="center" style="padding:16px 24px 24px;gap:12px">
      <button mat-stroked-button (click)="close(false)" style="min-width:140px">
        <mat-icon>inventory_2</mat-icon>
        No, aún por entregar
      </button>
      <button mat-raised-button color="primary" (click)="close(true)" style="min-width:140px">
        <mat-icon>check_circle</mat-icon>
        Sí, marcar entregado
      </button>
    </mat-dialog-actions>
  `
})
export class ConfirmDeliveredDialogComponent {
  private ref = inject(MatDialogRef<ConfirmDeliveredDialogComponent>);
  close(delivered: boolean): void { this.ref.close(delivered); }
}
