import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RepairService } from '../../../core/services/repair.service';
import { AuthService } from '../../../core/services/auth.service';
import { Repair, RepairStatus } from '../../../core/models/repair.model';

@Component({
  selector: 'app-repair-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink, FormsModule, MatCardModule, MatButtonModule,
    MatIconModule, MatTableModule, MatChipsModule, MatProgressSpinnerModule,
    MatFormFieldModule, MatInputModule, MatSelectModule
  ],
  templateUrl: './repair-list.component.html',
  styleUrl: './repair-list.component.css'
})
export class RepairListComponent implements OnInit {
  private repairService = inject(RepairService);
  authService = inject(AuthService);

  repairs = signal<Repair[]>([]);
  filteredRepairs = signal<Repair[]>([]);
  loading = signal(true);
  searchText = '';
  filterStatus = '';

  displayedColumns = ['client', 'clientPhone', 'item', 'entryDate', 'deliveryDate', 'totalPrice', 'paymentAmount', 'balanceDue', 'repairStatus', 'paymentStatus', 'actions'];

  readonly statusOptions: { value: RepairStatus; label: string }[] = [
    { value: 'RECEIVED', label: 'Recibido' },
    { value: 'IN_PROGRESS', label: 'En proceso' },
    { value: 'READY_TO_DELIVER', label: 'Listo para entregar' },
    { value: 'DELIVERED', label: 'Entregado' }
  ];

  ngOnInit(): void {
    this.loadRepairs();
  }

  loadRepairs(): void {
    this.loading.set(true);
    this.repairService.getAll().subscribe({
      next: (repairs) => {
        this.repairs.set(repairs);
        this.applyFilter();
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  applyFilter(): void {
    let result = this.repairs();
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      result = result.filter(r =>
        r.clientFirstName.toLowerCase().includes(q) ||
        r.clientLastName.toLowerCase().includes(q) ||
        (r.clientPhone ?? '').toLowerCase().includes(q) ||
        r.itemDescription.toLowerCase().includes(q)
      );
    }
    if (this.filterStatus) {
      result = result.filter(r => r.repairStatus === this.filterStatus);
    }
    this.filteredRepairs.set(result);
  }

  getStatusLabel(s: string): string {
    return this.statusOptions.find(o => o.value === s)?.label ?? s;
  }

  getPaymentLabel(s: string): string {
    return { 'UNPAID': 'Pendiente de pago', 'PARTIAL': 'Abono parcial', 'PAID': 'Pagado' }[s] || s;
  }
}
