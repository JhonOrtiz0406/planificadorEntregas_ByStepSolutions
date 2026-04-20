import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventClickArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import listPlugin from '@fullcalendar/list';
import interactionPlugin from '@fullcalendar/interaction';
import esLocale from '@fullcalendar/core/locales/es';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { CategoryStatusService } from '../../core/services/category-status.service';
import { Order } from '../../core/models/order.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, FullCalendarModule,
    MatCardModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatProgressSpinnerModule, MatBadgeModule, MatDividerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private orderService = inject(OrderService);
  private categoryStatusService = inject(CategoryStatusService);
  authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(true);
  pendingOrders = signal<Order[]>([]);
  calendarEvents = signal<any[]>([]);
  statusLabels = signal<Record<string, string>>({});

  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, listPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    locale: esLocale,
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,listWeek'
    },
    events: [],
    eventClick: this.handleEventClick.bind(this),
    eventTimeFormat: { hour: '2-digit', minute: '2-digit', meridiem: false },
    height: 'auto',
    eventDisplay: 'block',
    dayMaxEvents: 3
  };

  ngOnInit(): void {
    const category = this.authService.currentUser()?.organizationCategory ?? 'GENERAL';
    this.categoryStatusService.getByCategory(category).subscribe(statuses => {
      const map: Record<string, string> = {};
      statuses.forEach(s => map[s.statusKey] = s.label);
      this.statusLabels.set(map);
    });
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.orderService.getPending().subscribe({
      next: (orders) => {
        this.pendingOrders.set(orders);
        this.loadCalendarEvents();
      },
      error: () => this.loading.set(false)
    });
  }

  private loadCalendarEvents(): void {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth() - 1, 1).toISOString().split('T')[0];
    const end = new Date(now.getFullYear(), now.getMonth() + 3, 0).toISOString().split('T')[0];

    this.orderService.getCalendarEvents(start, end).subscribe({
      next: (orders) => {
        const events = this.orderService.toCalendarEvents(orders);
        this.calendarOptions = { ...this.calendarOptions, events };
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  handleEventClick(info: EventClickArg): void {
    const orderId = info.event.id;
    this.router.navigate(['/orders', orderId]);
  }

  navigateToOrder(order: Order): void {
    this.router.navigate(['/orders', order.id]);
  }

  navigateToNewOrder(): void {
    this.router.navigate(['/orders/new']);
  }

  getStatusLabel(status: string): string {
    return this.statusLabels()[status] ?? status;
  }

  getPaymentLabel(status: string): string {
    const labels: Record<string, string> = {
      'UNPAID': 'No pagado',
      'PARTIAL': 'Abono',
      'PAID': 'Pagado'
    };
    return labels[status] || status;
  }

  getDaysLabel(days: number, overdue: boolean): string {
    if (overdue) return 'Vencido';
    if (days === 0) return '¡Hoy!';
    if (days === 1) return 'Mañana';
    return `${days} días`;
  }

  getDaysClass(order: Order): string {
    if (order.overdue) return 'days-overdue';
    if (order.daysUntilDelivery <= 1) return 'days-critical';
    if (order.daysUntilDelivery <= 3) return 'days-warning';
    if (order.daysUntilDelivery <= 5) return 'days-soon';
    return 'days-ok';
  }

  get overdueCount(): number {
    return this.pendingOrders().filter(o => o.overdue).length;
  }

  get todayCount(): number {
    return this.pendingOrders().filter(o => o.daysUntilDelivery === 0).length;
  }
}
