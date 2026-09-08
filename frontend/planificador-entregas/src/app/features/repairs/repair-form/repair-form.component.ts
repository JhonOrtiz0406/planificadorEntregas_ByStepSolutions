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
import { RepairService } from '../../../core/services/repair.service';
import { compressImage } from '../../../core/utils/image-compression.util';

@Component({
  selector: 'app-repair-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatDatepickerModule,
    MatNativeDateModule, MatProgressSpinnerModule, MatSnackBarModule
  ],
  templateUrl: './repair-form.component.html',
  styleUrl: './repair-form.component.css'
})
export class RepairFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private repairService = inject(RepairService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);

  form!: FormGroup;
  loading = signal(false);
  uploadingPhoto = signal(false);
  editMode = signal(false);
  repairId: string | null = null;
  today = new Date();
  photoUrls = signal<string[]>([]);
  readonly MAX_PHOTOS = 3;

  ngOnInit(): void {
    this.initForm();
    this.repairId = this.route.snapshot.paramMap.get('id');
    if (this.repairId) {
      this.editMode.set(true);
      this.loadRepair();
    }
  }

  private initForm(): void {
    this.form = this.fb.group({
      clientFirstName: ['', [Validators.required, Validators.maxLength(255)]],
      clientLastName: ['', [Validators.required, Validators.maxLength(255)]],
      clientPhone: ['', [Validators.required, Validators.pattern('^[0-9+\\-\\s()]{7,15}$')]],
      itemDescription: ['', [Validators.required, Validators.maxLength(255)]],
      repairDescription: ['', [Validators.required]],
      entryDate: [this.today, Validators.required],
      deliveryDate: [null],
      totalPriceDisplay: ['', Validators.required],
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

  private loadRepair(): void {
    this.loading.set(true);
    this.repairService.getById(this.repairId!).subscribe({
      next: (repair) => {
        const priceDisplay = repair.totalPrice != null
          ? Number(repair.totalPrice).toLocaleString('es-CO')
          : '';
        this.form.patchValue({
          clientFirstName: repair.clientFirstName,
          clientLastName: repair.clientLastName,
          clientPhone: repair.clientPhone,
          itemDescription: repair.itemDescription,
          repairDescription: repair.repairDescription,
          entryDate: new Date(repair.entryDate + 'T00:00:00'),
          deliveryDate: repair.deliveryDate ? new Date(repair.deliveryDate + 'T00:00:00') : null,
          totalPriceDisplay: priceDisplay
        });
        this.photoUrls.set(repair.photoUrls ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Error al cargar el arreglo', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/repairs']);
      }
    });
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.processFile(file);
    (event.target as HTMLInputElement).value = '';
  }

  onCameraCapture(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.processFile(file);
    (event.target as HTMLInputElement).value = '';
  }

  canAddPhoto(): boolean {
    return this.photoUrls().length < this.MAX_PHOTOS;
  }

  removeLocalPhoto(url: string): void {
    this.photoUrls.update(photos => photos.filter(p => p !== url));
  }

  private async processFile(file: File): Promise<void> {
    if (!this.canAddPhoto()) {
      this.snackBar.open(`Máximo ${this.MAX_PHOTOS} fotos por arreglo`, 'Cerrar', { duration: 3000 });
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.snackBar.open('Solo se permiten archivos de imagen', 'Cerrar', { duration: 3000 });
      return;
    }
    this.uploadingPhoto.set(true);
    const uploadFile = await compressImage(file);
    this.repairService.uploadPhoto(uploadFile).subscribe({
      next: ({ url }) => {
        this.photoUrls.update(photos => [...photos, url]);
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
    const toDateStr = (d: Date | string | null) =>
      d instanceof Date ? d.toISOString().split('T')[0] : d;

    const request = {
      clientFirstName: value.clientFirstName,
      clientLastName: value.clientLastName,
      clientPhone: value.clientPhone,
      itemDescription: value.itemDescription,
      repairDescription: value.repairDescription,
      entryDate: toDateStr(value.entryDate) ?? undefined,
      deliveryDate: toDateStr(value.deliveryDate) ?? undefined,
      totalPrice: this.parsePriceValue() ?? undefined,
      photoUrls: this.photoUrls()
    };

    const operation = this.editMode()
      ? this.repairService.update(this.repairId!, request)
      : this.repairService.create(request as any);

    operation.subscribe({
      next: (repair) => {
        const msg = this.editMode() ? 'Arreglo actualizado' : 'Arreglo creado correctamente';
        this.snackBar.open(msg, 'Cerrar', { duration: 3000 });
        this.router.navigate(['/repairs', repair.id]);
      },
      error: (err) => {
        this.loading.set(false);
        const errData = err.error?.data;
        if (errData && typeof errData === 'object') {
          const fieldLabels: Record<string, string> = {
            clientFirstName: 'Nombres',
            clientLastName: 'Apellidos',
            clientPhone: 'Celular',
            itemDescription: 'Artículo',
            repairDescription: 'Descripción del arreglo',
            totalPrice: 'Valor total',
          };
          const msgs = Object.entries(errData)
            .map(([f, m]) => `${fieldLabels[f] ?? f}: ${m}`)
            .join('\n');
          this.snackBar.open(msgs, 'Cerrar', {
            duration: 7000,
            panelClass: ['snack-error'],
          });
        } else {
          const msg = err.error?.data?.title || err.error?.message || 'Error al guardar el arreglo';
          this.snackBar.open(msg, 'Cerrar', { duration: 4000, panelClass: ['snack-error'] });
        }
      }
    });
  }

  cancel(): void {
    this.router.navigate([this.repairId ? ['/repairs', this.repairId] : ['/repairs']]);
  }
}
