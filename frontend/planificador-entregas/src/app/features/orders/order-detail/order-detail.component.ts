import { Component, OnInit, inject, signal, ViewChild, ElementRef } from '@angular/core';
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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { OrderService } from '../../../core/services/order.service';
import { AuthService } from '../../../core/services/auth.service';
import { CategoryStatusService } from '../../../core/services/category-status.service';
import { Order, ProgressStatus, PaymentStatus, PaymentRecord, AddPaymentRecordRequest } from '../../../core/models/order.model';
import { ConfirmDeliveredDialogComponent } from './confirm-delivered-dialog.component';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatSelectModule, MatFormFieldModule, MatInputModule,
    MatSnackBarModule, MatProgressSpinnerModule, MatDividerModule, MatDialogModule,
    MatRadioModule, MatDatepickerModule, MatNativeDateModule
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
  deletingPhoto = signal(false);
  progressOptions = signal<{ value: string; label: string }[]>([]);
  paymentRecords = signal<PaymentRecord[]>([]);
  loadingPayments = signal(false);
  addingPayment = signal(false);
  selectedPhoto = signal<string | null>(null);
  highlightPayments = signal(false);

  @ViewChild('paymentHistory') paymentHistoryRef?: ElementRef<HTMLElement>;

  paymentForm = this.fb.group({
    amount: [null as number | null],
    amountDisplay: [''],
    paymentDate: [null as Date | null],
    paymentMethod: [''],
    notes: ['']
  });

  readonly paymentMethods = [
    'Efectivo', 'Transferencia', 'Tarjeta débito', 'Tarjeta crédito', 'Especie / Canje'
  ];

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
        this.loadPaymentRecords(id);
      },
      error: () => {
        this.snackBar.open('Pedido no encontrado', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/orders']);
      }
    });
  }

  private watchPaymentStatus(): void {
    this.statusForm.get('paymentStatus')?.valueChanges.subscribe(val => {
      if (val === 'PARTIAL') {
        this.goToPaymentHistory();
      } else if (val === 'PAID') {
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

  private goToPaymentHistory(): void {
    // Defer to next tick so the element is rendered/visible
    setTimeout(() => {
      this.paymentHistoryRef?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      this.highlightPayments.set(true);
      setTimeout(() => this.highlightPayments.set(false), 2400);
    }, 50);
    this.snackBar.open(
      'Registra el abono en "Historial de Abonos" para dejar la trazabilidad',
      'Entendido',
      { duration: 5000, panelClass: ['snack-success'] }
    );
  }

  updateStatus(): void {
    const order = this.order();
    if (!order || !this.authService.canUpdateStatus()) return;
    this.updating.set(true);
    const val = this.statusForm.value;
    this.orderService.updateStatus(order.id, {
      progressStatus: val.progressStatus as ProgressStatus,
      paymentStatus: val.paymentStatus as PaymentStatus
      // paymentAmount intentionally omitted — el monto abonado proviene del Historial de Abonos
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

  private loadPaymentRecords(orderId: string): void {
    this.loadingPayments.set(true);
    this.orderService.getPaymentRecords(orderId).subscribe({
      next: (records) => { this.paymentRecords.set(records); this.loadingPayments.set(false); },
      error: () => this.loadingPayments.set(false)
    });
  }

  onPaymentAmountFormInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let raw = input.value.replace(/[^0-9]/g, '');
    if (raw.length > 13) raw = raw.slice(0, 13);
    const formatted = raw ? Number(raw).toLocaleString('es-CO') : '';
    const numeric = raw ? Number(raw) : null;
    this.paymentForm.get('amountDisplay')?.setValue(formatted, { emitEvent: false });
    this.paymentForm.get('amount')?.setValue(numeric, { emitEvent: false });
    input.value = formatted;
  }

  addPaymentRecord(): void {
    const val = this.paymentForm.value;
    if (!val.amount || !val.paymentDate || !this.order()) return;
    this.addingPayment.set(true);
    const dateStr = (val.paymentDate as Date).toISOString().split('T')[0];
    const req: AddPaymentRecordRequest = {
      amount: val.amount,
      paymentDate: dateStr,
      paymentMethod: val.paymentMethod || undefined,
      notes: val.notes || undefined
    };
    this.orderService.addPaymentRecord(this.order()!.id, req).subscribe({
      next: (record) => {
        this.paymentRecords.update(r => [record, ...r]);
        const total = this.paymentRecords().reduce((s, r) => s + r.amount, 0);
        this.order.update(o => o ? { ...o, paymentAmount: total } : o);
        this.statusForm.patchValue({
          paymentAmountDisplay: total.toLocaleString('es-CO'),
          paymentAmount: total as any
        });
        this.paymentForm.reset();
        this.addingPayment.set(false);
        this.snackBar.open('Abono registrado', 'Cerrar', { duration: 2000 });
      },
      error: (err) => {
        this.addingPayment.set(false);
        this.snackBar.open(err.error?.message || 'Error al registrar abono', 'Cerrar', { duration: 3000 });
      }
    });
  }

  deletePaymentRecord(record: PaymentRecord): void {
    if (!this.order()) return;
    this.orderService.deletePaymentRecord(this.order()!.id, record.id).subscribe({
      next: () => {
        this.paymentRecords.update(r => r.filter(p => p.id !== record.id));
        const total = this.paymentRecords().reduce((s, r) => s + r.amount, 0);
        this.order.update(o => o ? { ...o, paymentAmount: total } : o);
        this.statusForm.patchValue({
          paymentAmountDisplay: total > 0 ? total.toLocaleString('es-CO') : '',
          paymentAmount: total > 0 ? total as any : null
        });
        this.snackBar.open('Abono eliminado', 'Cerrar', { duration: 2000 });
      },
      error: () => this.snackBar.open('Error al eliminar abono', 'Cerrar', { duration: 3000 })
    });
  }

  deletePhoto(url: string): void {
    if (!this.order()) return;
    this.deletingPhoto.set(true);
    this.orderService.deletePhoto(this.order()!.id, url).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.deletingPhoto.set(false);
        this.snackBar.open('Foto eliminada', 'Cerrar', { duration: 2000 });
      },
      error: () => {
        this.deletingPhoto.set(false);
        this.snackBar.open('Error al eliminar foto', 'Cerrar', { duration: 3000 });
      }
    });
  }

  openPhoto(url: string): void { this.selectedPhoto.set(url); }
  closePhoto(): void { this.selectedPhoto.set(null); }

  paymentMethodIcon(method?: string): string {
    const icons: Record<string, string> = {
      'Efectivo': 'payments', 'Transferencia': 'account_balance',
      'Tarjeta débito': 'credit_card', 'Tarjeta crédito': 'credit_card',
      'Especie / Canje': 'diamond'
    };
    return method ? (icons[method] ?? 'attach_money') : 'attach_money';
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

  get totalPaid(): number {
    return this.paymentRecords().reduce((s, r) => s + r.amount, 0);
  }

  orderPhotos(): string[] {
    const o = this.order();
    if (!o) return [];
    if (o.photoUrls && o.photoUrls.length > 0) return o.photoUrls;
    if (o.photoUrl) return [o.photoUrl];
    return [];
  }
}
