import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
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
import { MatRadioModule } from '@angular/material/radio';
import { OrderService } from '../../../core/services/order.service';
import { AuthService } from '../../../core/services/auth.service';
import { CategoryStatusService } from '../../../core/services/category-status.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink, FormsModule, MatCardModule, MatButtonModule,
    MatIconModule, MatTableModule, MatChipsModule, MatProgressSpinnerModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatRadioModule
  ],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css'
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  private categoryStatusService = inject(CategoryStatusService);
  authService = inject(AuthService);
  private router = inject(Router);

  orders = signal<Order[]>([]);
  filteredOrders = signal<Order[]>([]);
  statusOptions = signal<{ value: string; label: string }[]>([]);
  loading = signal(true);
  searchText = '';
  filterStatus = '';

  displayedColumns = ['orderNumber', 'productName', 'clientName', 'deliveryDate', 'totalPrice', 'progressStatus', 'paymentStatus', 'actions'];
  dateSort: 'asc' | 'desc' | '' = '';
  priceSort: 'asc' | 'desc' | '' = '';

  ngOnInit(): void {
    const category = this.authService.currentUser()?.organizationCategory ?? 'GENERAL';
    this.categoryStatusService.getByCategory(category).subscribe(statuses => {
      this.statusOptions.set(statuses.map(s => ({ value: s.statusKey, label: s.label })));
    });
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.orderService.getAll().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.applyFilter();
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  applyFilter(): void {
    let result = this.orders();
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      result = result.filter(o =>
        o.productName.toLowerCase().includes(q) ||
        o.clientName.toLowerCase().includes(q) ||
        o.orderNumber.toLowerCase().includes(q)
      );
    }
    if (this.filterStatus) {
      result = result.filter(o => o.progressStatus === this.filterStatus);
    }
    if (this.dateSort) {
      result = [...result].sort((a, b) => {
        const diff = new Date(a.deliveryDate).getTime() - new Date(b.deliveryDate).getTime();
        return this.dateSort === 'asc' ? diff : -diff;
      });
    } else if (this.priceSort) {
      result = [...result].sort((a, b) => {
        const diff = (a.totalPrice ?? 0) - (b.totalPrice ?? 0);
        return this.priceSort === 'asc' ? diff : -diff;
      });
    }
    this.filteredOrders.set(result);
  }

  onDateSortChange(): void {
    this.priceSort = '';
    this.applyFilter();
  }

  onPriceSortChange(): void {
    this.dateSort = '';
    this.applyFilter();
  }

  getStatusLabel(s: string): string {
    return this.statusOptions().find(o => o.value === s)?.label ?? s;
  }

  getPaymentLabel(s: string): string {
    return { 'UNPAID': 'No pagado', 'PARTIAL': 'Abono', 'PAID': 'Pagado' }[s] || s;
  }
}
