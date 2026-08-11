import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Repair, CreateRepairRequest, UpdateRepairStatusRequest, RepairPayment, AddRepairPaymentRequest } from '../models/repair.model';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class RepairService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/repairs`;

  getAll(): Observable<Repair[]> {
    return this.http.get<any>(this.baseUrl).pipe(map(r => r.data));
  }

  getById(id: string): Observable<Repair> {
    return this.http.get<any>(`${this.baseUrl}/${id}`).pipe(map(r => r.data));
  }

  create(request: CreateRepairRequest): Observable<Repair> {
    return this.http.post<any>(this.baseUrl, request).pipe(map(r => r.data));
  }

  update(id: string, request: Partial<CreateRepairRequest>): Observable<Repair> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, request).pipe(map(r => r.data));
  }

  updateStatus(id: string, request: UpdateRepairStatusRequest): Observable<Repair> {
    return this.http.patch<any>(`${this.baseUrl}/${id}/status`, request).pipe(map(r => r.data));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<any>(`${this.baseUrl}/${id}`);
  }

  uploadPhoto(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', 'arreglos');
    return this.http.post<any>(`${environment.apiUrl}/files/upload`, formData).pipe(map(r => r.data));
  }

  deletePhoto(repairId: string, url: string): Observable<Repair> {
    return this.http.delete<any>(`${this.baseUrl}/${repairId}/photos`, { body: { url } }).pipe(map(r => r.data));
  }

  getPayments(repairId: string): Observable<RepairPayment[]> {
    return this.http.get<any>(`${this.baseUrl}/${repairId}/payments`).pipe(map(r => r.data));
  }

  addPayment(repairId: string, request: AddRepairPaymentRequest): Observable<RepairPayment> {
    return this.http.post<any>(`${this.baseUrl}/${repairId}/payments`, request).pipe(map(r => r.data));
  }

  deletePayment(repairId: string, paymentId: string): Observable<void> {
    return this.http.delete<any>(`${this.baseUrl}/${repairId}/payments/${paymentId}`);
  }
}
