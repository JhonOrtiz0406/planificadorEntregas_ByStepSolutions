import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OrderService } from '../../../core/services/order.service';

@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatDatepickerModule,
    MatNativeDateModule, MatProgressSpinnerModule, MatSnackBarModule
  ],
  templateUrl: './order-form.component.html',
  styleUrl: './order-form.component.css'
})
export class OrderFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private orderService = inject(OrderService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);

  form!: FormGroup;
  loading = signal(false);
  uploadingPhoto = signal(false);
  editMode = signal(false);
  orderId: string | null = null;
  minDate = new Date();
  previewUrl = signal<string | null>(null);

  ngOnInit(): void {
    this.initForm();
    this.orderId = this.route.snapshot.paramMap.get('id');
    if (this.orderId) {
      this.editMode.set(true);
      this.loadOrder();
    }
  }

  private initForm(): void {
    this.form = this.fb.group({
      productName: ['', [Validators.required, Validators.maxLength(255)]],
      clientName: ['', [Validators.required, Validators.maxLength(255)]],
      clientPhone: ['', [Validators.pattern('^[0-9+\\-\\s()]{7,15}$')]],
      clientAddress: [''],
      description: [''],
      photoUrl: [''],
      deliveryDate: [null, Validators.required],
      totalPriceDisplay: [''],
    });
  }

  onPriceInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let raw = input.value.replace(/[^0-9]/g, '');
    if (raw.length > 13) raw = raw.slice(0, 13);
    const formatted = raw ? Number(raw).toLocaleString('es-CO') : '';
    this.form.get('totalPriceDisplay')?.setValue(formatted, { emitEvent: false });
    input.value = formatted;
  }

  private parsePriceValue(): number | null {
    const display = this.form.get('totalPriceDisplay')?.value as string;
    if (!display) return null;
    const raw = display.replace(/[^0-9]/g, '');
    return raw ? Number(raw) : null;
  }

  private loadOrder(): void {
    this.loading.set(true);
    this.orderService.getById(this.orderId!).subscribe({
      next: (order) => {
        const priceDisplay = order.totalPrice != null
          ? Number(order.totalPrice).toLocaleString('es-CO')
          : '';
        this.form.patchValue({
          ...order,
          deliveryDate: new Date(order.deliveryDate + 'T00:00:00'),
          totalPriceDisplay: priceDisplay
        });
        if (order.photoUrl) this.previewUrl.set(order.photoUrl);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Error al cargar el pedido', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/orders']);
      }
    });
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.processFile(file);
    }
  }

  onCameraCapture(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.processFile(file);
    }
  }

  private processFile(file: File): void {
    const reader = new FileReader();
    reader.onload = (e) => this.previewUrl.set(e.target?.result as string);
    reader.readAsDataURL(file);
    this.uploadPhoto(file);
  }

  private uploadPhoto(file: File): void {
    this.uploadingPhoto.set(true);
    this.orderService.uploadPhoto(file).subscribe({
      next: ({ url }) => {
        this.form.patchValue({ photoUrl: url });
        this.uploadingPhoto.set(false);
        this.snackBar.open('Foto subida correctamente', 'Cerrar', { duration: 2000 });
      },
      error: () => {
        this.uploadingPhoto.set(false);
        this.snackBar.open('Error al subir la foto', 'Cerrar', { duration: 3000 });
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);

    const value = this.form.value;
    const deliveryDate = value.deliveryDate instanceof Date
      ? value.deliveryDate.toISOString().split('T')[0]
      : value.deliveryDate;

    const { totalPriceDisplay, ...rest } = value;
    const request = { ...rest, deliveryDate, totalPrice: this.parsePriceValue() };

    const operation = this.editMode()
      ? this.orderService.update(this.orderId!, request)
      : this.orderService.create(request);

    operation.subscribe({
      next: (order) => {
        const msg = this.editMode() ? 'Pedido actualizado' : 'Pedido creado correctamente';
        this.snackBar.open(msg, 'Cerrar', { duration: 3000 });
        this.router.navigate(['/orders', order.id]);
      },
      error: (err) => {
        this.loading.set(false);
        const errData = err.error?.data;
        if (errData && typeof errData === 'object') {
          const fieldLabels: Record<string, string> = {
            deliveryDate: 'Fecha de entrega',
            productName: 'Nombre del producto',
            clientName: 'Nombre del cliente',
            totalPrice: 'Precio total',
          };
          const msgs = Object.entries(errData)
            .map(([f, m]) => `${fieldLabels[f] ?? f}: ${m}`)
            .join('\n');
          this.snackBar.open(msgs, 'Cerrar', {
            duration: 7000,
            panelClass: ['snack-error'],
          });
        } else {
          const msg = err.error?.message || 'Error al guardar el pedido';
          this.snackBar.open(msg, 'Cerrar', { duration: 4000, panelClass: ['snack-error'] });
        }
      }
    });
  }

  cancel(): void {
    this.router.navigate([this.orderId ? ['/orders', this.orderId] : ['/orders']]);
  }
}
