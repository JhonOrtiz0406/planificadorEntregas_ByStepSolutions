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
import { OrderService } from '../../../core/services/order.service';
import { AuthService } from '../../../core/services/auth.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink, FormsModule, MatCardModule, MatButtonModule,
    MatIconModule, MatTableModule, MatChipsModule, MatProgressSpinnerModule,
    MatFormFieldModule, MatInputModule, MatSelectModule
  ],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css'
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  authService = inject(AuthService);
  private router = inject(Router);

  orders = signal<Order[]>([]);
  filteredOrders = signal<Order[]>([]);
  loading = signal(true);
  searchText = '';
  filterStatus = '';

  displayedColumns = ['orderNumber', 'productName', 'clientName', 'deliveryDate', 'progressStatus', 'paymentStatus', 'actions'];

  ngOnInit(): void {
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
    this.filteredOrders.set(result);
  }

  getStatusLabel(s: string): string {
    return { 'NOT_STARTED': 'Sin iniciar', 'IN_PREPARATION': 'En preparación', 'DELIVERED': 'Entregado' }[s] || s;
  }

  getPaymentLabel(s: string): string {
    return { 'UNPAID': 'No pagado', 'PARTIAL': 'Abono', 'PAID': 'Pagado' }[s] || s;
  }
}
