import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface WhatsAppConfigView {
  enabled: boolean;
  status: 'PENDING' | 'CONNECTED' | 'ERROR' | string;
  wabaId?: string;
  phoneNumberId?: string;
  displayPhoneNumber?: string;
  verifiedName?: string;
  hasAccessToken: boolean;
  supportContactText?: string;
  qualityRating?: string;
  lastError?: string;
  lastVerifiedAt?: string;
}

export interface WhatsAppEventView {
  key: string;
  label: string;
  templateName: string;
  enabled: boolean;
  templateStatus: string;
  rejectionReason?: string;
}

export interface WhatsAppOverview {
  organizationPhone?: string;
  config: WhatsAppConfigView | null;
  events: WhatsAppEventView[];
  monthCounts: Record<string, number>;
}

export interface WhatsAppMessageView {
  id: string;
  eventKey: string;
  entityType?: string;
  entityId?: string;
  toPhone: string;
  templateName?: string;
  status: string;
  skipReason?: string;
  errorCode?: string;
  errorMessage?: string;
  attempts: number;
  createdAt: string;
  sentAt?: string;
}

export interface WhatsAppCredentialsRequest {
  wabaId: string;
  phoneNumberId: string;
  accessToken?: string;
  supportContactText?: string;
}

export interface WhatsAppTestResult {
  success: boolean;
  errorCode?: string;
  errorMessage?: string;
}

/** Módulo WhatsApp de UNA organización (/api/organizations/{id}/whatsapp). */
@Injectable({ providedIn: 'root' })
export class WhatsAppService {
  private http = inject(HttpClient);

  private url(orgId: string): string {
    return `${environment.apiUrl}/organizations/${orgId}/whatsapp`;
  }

  getOverview(orgId: string): Observable<WhatsAppOverview> {
    return this.http.get<any>(this.url(orgId)).pipe(map(r => r.data));
  }

  getMessages(orgId: string, limit = 20): Observable<WhatsAppMessageView[]> {
    return this.http.get<any>(`${this.url(orgId)}/messages`, { params: { limit } }).pipe(map(r => r.data));
  }

  saveCredentials(orgId: string, request: WhatsAppCredentialsRequest): Observable<WhatsAppConfigView> {
    return this.http.put<any>(`${this.url(orgId)}/credentials`, request).pipe(map(r => r.data));
  }

  verify(orgId: string): Observable<WhatsAppConfigView> {
    return this.http.post<any>(`${this.url(orgId)}/verify`, {}).pipe(map(r => r.data));
  }

  setEnabled(orgId: string, enabled: boolean): Observable<WhatsAppConfigView> {
    return this.http.patch<any>(`${this.url(orgId)}/enabled`, { enabled }).pipe(map(r => r.data));
  }

  syncTemplates(orgId: string): Observable<unknown> {
    return this.http.post<any>(`${this.url(orgId)}/templates/sync`, {}).pipe(map(r => r.data));
  }

  refreshTemplates(orgId: string): Observable<unknown> {
    return this.http.post<any>(`${this.url(orgId)}/templates/refresh`, {}).pipe(map(r => r.data));
  }

  updateEvents(orgId: string, events: Record<string, boolean>): Observable<WhatsAppOverview> {
    return this.http.put<any>(`${this.url(orgId)}/events`, { events }).pipe(map(r => r.data));
  }

  sendTest(orgId: string, phone: string): Observable<WhatsAppTestResult> {
    return this.http.post<any>(`${this.url(orgId)}/test`, { phone }).pipe(map(r => r.data));
  }
}

/** Extrae el mensaje de error de las respuestas del backend (ApiResponse o ControlledErrorResponse). */
export function apiErrorMessage(err: any, fallback: string): string {
  return err?.error?.data?.errors?.[0]?.description
    || err?.error?.message
    || fallback;
}
