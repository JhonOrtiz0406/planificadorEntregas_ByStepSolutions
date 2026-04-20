import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatRadioModule } from '@angular/material/radio';
import { OrderService } from '../../../core/services/order.service';
import { AuthService } from '../../../core/services/auth.service';
import { CategoryStatusService } from '../../../core/services/category-status.service';
import { Order, ProgressStatus, PaymentStatus } from '../../../core/models/order.model';
import { ConfirmDeliveredDialogComponent } from './confirm-delivered-dialog.component';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatSelectModule, MatFormFieldModule, MatInputModule,
    MatSnackBarModule, MatProgressSpinnerModule, MatDividerModule, MatDialogModule,
    MatRadioModule
  ],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.css'
})
export class OrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private orderService = inject(OrderService);
  private categoryStatusService = inject(CategoryStatusService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private fb = inject(FormBuilder);
  authService = inject(AuthService);

  order = signal<Order | null>(null);
  loading = signal(true);
  updating = signal(false);
  progressOptions = signal<{ value: string; label: string }[]>([]);

  statusForm = this.fb.group({
    progressStatus: [''],
    paymentStatus: [''],
    paymentAmount: [null as number | null],
    paymentAmountDisplay: ['']
  });

  readonly paymentOptions: { value: PaymentStatus; label: string }[] = [
    { value: 'UNPAID', label: 'No pagado' },
    { value: 'PARTIAL', label: 'Abono' },
    { value: 'PAID', label: 'Pagado' }
  ];

  ngOnInit(): void {
    const category = this.authService.currentUser()?.organizationCategory ?? 'GENERAL';
    this.categoryStatusService.getByCategory(category).subscribe(statuses => {
      this.progressOptions.set(statuses.map(s => ({ value: s.statusKey, label: s.label })));
    });

    const id = this.route.snapshot.paramMap.get('id')!;
    this.orderService.getById(id).subscribe({
      next: (order) => {
        this.order.set(order);
        const amountDisplay = order.paymentAmount != null
          ? Number(order.paymentAmount).toLocaleString('es-CO')
          : '';
        this.statusForm.patchValue({
          progressStatus: order.progressStatus,
          paymentStatus: order.paymentStatus,
          paymentAmount: order.paymentAmount as any,
          paymentAmountDisplay: amountDisplay
        });
        this.loading.set(false);
        this.watchPaymentStatus();
      },
      error: () => {
        this.snackBar.open('Pedido no encontrado', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/orders']);
      }
    });
  }

  private watchPaymentStatus(): void {
    this.statusForm.get('paymentStatus')?.valueChanges.subscribe(val => {
      if (val === 'PAID') {
        const previousProgress = this.statusForm.get('progressStatus')?.value ?? null;
        const ref = this.dialog.open(ConfirmDeliveredDialogComponent, { width: '400px' });
        ref.afterClosed().subscribe(delivered => {
          if (delivered === true) {
            this.statusForm.get('progressStatus')?.setValue('DELIVERED', { emitEvent: false });
          } else {
            // Restore the previous progress status — don't force IN_PREPARATION
            this.statusForm.get('progressStatus')?.setValue(previousProgress, { emitEvent: false });
          }
        });
      }
    });
  }

  onPaymentAmountInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let raw = input.value.replace(/[^0-9]/g, '');
    if (raw.length > 13) raw = raw.slice(0, 13);
    const formatted = raw ? Number(raw).toLocaleString('es-CO') : '';
    const numeric = raw ? Number(raw) : null;
    this.statusForm.get('paymentAmountDisplay')?.setValue(formatted, { emitEvent: false });
    this.statusForm.get('paymentAmount')?.setValue(numeric, { emitEvent: false });
    input.value = formatted;
  }

  updateStatus(): void {
    const order = this.order();
    if (!order || !this.authService.canUpdateStatus()) return;
    this.updating.set(true);
    const val = this.statusForm.value;
    this.orderService.updateStatus(order.id, {
      progressStatus: val.progressStatus as ProgressStatus,
      paymentStatus: val.paymentStatus as PaymentStatus,
      paymentAmount: val.paymentAmount || undefined
    }).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.updating.set(false);
        const ref = this.snackBar.open(
          '¡Estado actualizado correctamente!',
          'Ir al Panel',
          { duration: 3000, panelClass: ['snack-success'] }
        );
        ref.onAction().subscribe(() => this.router.navigate(['/dashboard']));
        setTimeout(() => this.router.navigate(['/dashboard']), 3000);
      },
      error: (err) => {
        this.updating.set(false);
        const errData = err.error?.data;
        if (errData && typeof errData === 'object') {
          const msgs = Object.entries(errData)
            .map(([field, msg]) => `${this.fieldLabel(field)}: ${msg}`)
            .join(' | ');
          this.snackBar.open(msgs, 'Cerrar', { duration: 6000, panelClass: ['snack-error'] });
        } else {
          this.snackBar.open(err.error?.message || 'Error al actualizar', 'Cerrar', { duration: 3000 });
        }
      }
    });
  }

  private fieldLabel(field: string): string {
    const labels: Record<string, string> = {
      deliveryDate: 'Fecha de entrega',
      progressStatus: 'Estado del pedido',
      paymentStatus: 'Estado de pago',
      paymentAmount: 'Monto de abono',
    };
    return labels[field] ?? field;
  }

  deleteOrder(): void {
    if (!confirm('¿Estás seguro de eliminar este pedido?')) return;
    this.orderService.delete(this.order()!.id).subscribe({
      next: () => {
        this.snackBar.open('Pedido eliminado', 'Cerrar', { duration: 2000 });
        this.router.navigate(['/orders']);
      }
    });
  }

  getProgressLabel(s: string): string {
    return this.progressOptions().find(o => o.value === s)?.label ?? s;
  }

  getPaymentLabel(s: string): string {
    return { 'UNPAID': 'No pagado', 'PARTIAL': 'Abono', 'PAID': 'Pagado' }[s] || s;
  }

  get isPartialPayment(): boolean {
    return this.statusForm.get('paymentStatus')?.value === 'PARTIAL';
  }
}
