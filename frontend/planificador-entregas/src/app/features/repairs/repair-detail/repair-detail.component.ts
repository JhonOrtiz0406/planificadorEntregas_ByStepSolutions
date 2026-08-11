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
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatRadioModule } from '@angular/material/radio';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { RepairService } from '../../../core/services/repair.service';
import { AuthService } from '../../../core/services/auth.service';
import { Repair, RepairStatus, RepairPayment, AddRepairPaymentRequest } from '../../../core/models/repair.model';

@Component({
  selector: 'app-repair-detail',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatSelectModule, MatFormFieldModule, MatInputModule,
    MatSnackBarModule, MatProgressSpinnerModule, MatDividerModule,
    MatRadioModule, MatDatepickerModule, MatNativeDateModule
  ],
  templateUrl: './repair-detail.component.html',
  styleUrl: './repair-detail.component.css'
})
export class RepairDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private repairService = inject(RepairService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);
  authService = inject(AuthService);

  repair = signal<Repair | null>(null);
  loading = signal(true);
  updating = signal(false);
  deletingPhoto = signal(false);
  payments = signal<RepairPayment[]>([]);
  loadingPayments = signal(false);
  addingPayment = signal(false);
  selectedPhoto = signal<string | null>(null);

  paymentForm = this.fb.group({
    amount: [null as number | null],
    amountDisplay: [''],
    paymentDate: [new Date() as Date | null],
    paymentMethod: [''],
    notes: ['']
  });

  readonly paymentMethods = ['Efectivo', 'Transferencia', 'Tarjeta', 'Otro'];

  statusForm = this.fb.group({
    repairStatus: ['' as RepairStatus | '']
  });

  readonly statusOptions: { value: RepairStatus; label: string }[] = [
    { value: 'RECEIVED', label: 'Recibido' },
    { value: 'IN_PROGRESS', label: 'En proceso' },
    { value: 'READY_TO_DELIVER', label: 'Listo para entregar' },
    { value: 'DELIVERED', label: 'Entregado' }
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.repairService.getById(id).subscribe({
      next: (repair) => {
        this.repair.set(repair);
        this.statusForm.patchValue({ repairStatus: repair.repairStatus });
        this.loading.set(false);
        this.loadPayments(id);
      },
      error: () => {
        this.snackBar.open('Arreglo no encontrado', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/repairs']);
      }
    });
  }

  updateStatus(): void {
    const repair = this.repair();
    if (!repair || !this.authService.canUpdateStatus()) return;
    const status = this.statusForm.value.repairStatus;
    if (!status) return;
    this.updating.set(true);
    this.repairService.updateStatus(repair.id, { repairStatus: status }).subscribe({
      next: (updated) => {
        this.repair.set(updated);
        this.updating.set(false);
        this.snackBar.open('Estado actualizado correctamente', 'Cerrar', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: (err) => {
        this.updating.set(false);
        this.snackBar.open(err.error?.data?.title || err.error?.message || 'Error al actualizar', 'Cerrar', { duration: 3000 });
      }
    });
  }

  private loadPayments(repairId: string): void {
    this.loadingPayments.set(true);
    this.repairService.getPayments(repairId).subscribe({
      next: (records) => { this.payments.set(records); this.loadingPayments.set(false); },
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

  addPayment(): void {
    const val = this.paymentForm.value;
    const repair = this.repair();
    if (!val.amount || !val.paymentDate || !repair) return;

    const balance = repair.balanceDue ?? 0;
    if (val.amount > balance) {
      this.snackBar.open(
        `El abono no puede superar el saldo pendiente (${balance.toLocaleString('es-CO')})`,
        'Cerrar', { duration: 4000, panelClass: ['snack-error'] }
      );
      return;
    }

    this.addingPayment.set(true);
    const dateStr = (val.paymentDate as Date).toISOString().split('T')[0];
    const req: AddRepairPaymentRequest = {
      amount: val.amount,
      paymentDate: dateStr,
      paymentMethod: val.paymentMethod || undefined,
      notes: val.notes || undefined
    };
    this.repairService.addPayment(repair.id, req).subscribe({
      next: (payment) => {
        this.payments.update(p => [payment, ...p]);
        this.recomputeTotals();
        this.paymentForm.reset({ paymentDate: new Date() });
        this.addingPayment.set(false);
        this.snackBar.open('Abono registrado', 'Cerrar', { duration: 2000 });
      },
      error: (err) => {
        this.addingPayment.set(false);
        this.snackBar.open(err.error?.data?.title || err.error?.message || 'Error al registrar abono', 'Cerrar', { duration: 4000, panelClass: ['snack-error'] });
      }
    });
  }

  deletePayment(payment: RepairPayment): void {
    if (!this.repair()) return;
    this.repairService.deletePayment(this.repair()!.id, payment.id).subscribe({
      next: () => {
        this.payments.update(p => p.filter(x => x.id !== payment.id));
        this.recomputeTotals();
        this.snackBar.open('Abono eliminado', 'Cerrar', { duration: 2000 });
      },
      error: () => this.snackBar.open('Error al eliminar abono', 'Cerrar', { duration: 3000 })
    });
  }

  private recomputeTotals(): void {
    const total = this.totalPaid;
    this.repair.update(r => {
      if (!r) return r;
      const totalPrice = r.totalPrice ?? 0;
      let balance = totalPrice - total;
      if (balance < 0) balance = 0;
      let paymentStatus: Repair['paymentStatus'] = 'UNPAID';
      if (total > 0 && total >= totalPrice) paymentStatus = 'PAID';
      else if (total > 0) paymentStatus = 'PARTIAL';
      return { ...r, paymentAmount: total, balanceDue: balance, paymentStatus };
    });
  }

  deletePhoto(url: string): void {
    if (!this.repair()) return;
    this.deletingPhoto.set(true);
    this.repairService.deletePhoto(this.repair()!.id, url).subscribe({
      next: (updated) => {
        this.repair.set(updated);
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
      'Tarjeta': 'credit_card', 'Otro': 'swap_horiz'
    };
    return method ? (icons[method] ?? 'attach_money') : 'attach_money';
  }

  deleteRepair(): void {
    if (!confirm('¿Estás seguro de eliminar este arreglo?')) return;
    this.repairService.delete(this.repair()!.id).subscribe({
      next: () => {
        this.snackBar.open('Arreglo eliminado', 'Cerrar', { duration: 2000 });
        this.router.navigate(['/repairs']);
      }
    });
  }

  getStatusLabel(s: string): string {
    return this.statusOptions.find(o => o.value === s)?.label ?? s;
  }

  getPaymentLabel(s: string): string {
    return { 'UNPAID': 'Pendiente de pago', 'PARTIAL': 'Abono parcial', 'PAID': 'Pagado' }[s] || s;
  }

  get totalPaid(): number {
    return this.payments().reduce((s, r) => s + r.amount, 0);
  }
}
