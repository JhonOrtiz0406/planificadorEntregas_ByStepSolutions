import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Order, CreateOrderRequest, UpdateOrderStatusRequest, CalendarEvent } from '../models/order.model';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/orders`;

  getAll(): Observable<Order[]> {
    return this.http.get<any>(this.baseUrl).pipe(map(r => r.data));
  }

  getPending(): Observable<Order[]> {
    return this.http.get<any>(`${this.baseUrl}/pending`).pipe(map(r => r.data));
  }

  getCalendarEvents(start: string, end: string): Observable<Order[]> {
    const params = new HttpParams().set('start', start).set('end', end);
    return this.http.get<any>(`${this.baseUrl}/calendar`, { params }).pipe(map(r => r.data));
  }

  getById(id: string): Observable<Order> {
    return this.http.get<any>(`${this.baseUrl}/${id}`).pipe(map(r => r.data));
  }

  create(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<any>(this.baseUrl, request).pipe(map(r => r.data));
  }

  update(id: string, request: Partial<CreateOrderRequest>): Observable<Order> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, request).pipe(map(r => r.data));
  }

  updateStatus(id: string, request: UpdateOrderStatusRequest): Observable<Order> {
    return this.http.patch<any>(`${this.baseUrl}/${id}/status`, request).pipe(map(r => r.data));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<any>(`${this.baseUrl}/${id}`);
  }

  toCalendarEvents(orders: Order[]): CalendarEvent[] {
    return orders.map(order => ({
      id: order.id,
      title: `${order.orderNumber} - ${order.clientName}`,
      date: order.deliveryDate,
      backgroundColor: this.getStatusColor(order),
      borderColor: this.getStatusColor(order),
      textColor: '#ffffff',
      extendedProps: order
    }));
  }

  private getStatusColor(order: Order): string {
    if (order.overdue) return '#ef4444';
    if (order.progressStatus === 'DELIVERED') return '#10b981';
    if (order.daysUntilDelivery <= 1) return '#ef4444';
    if (order.daysUntilDelivery <= 3) return '#f97316';
    if (order.daysUntilDelivery <= 5) return '#f59e0b';
    return '#3b82f6';
  }

  uploadPhoto(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${environment.apiUrl}/files/upload`, formData).pipe(map(r => r.data));
  }
}
