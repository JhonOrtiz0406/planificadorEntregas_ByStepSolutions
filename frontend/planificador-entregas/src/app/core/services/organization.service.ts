import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Organization, CreateOrganizationRequest, InviteMemberRequest } from '../models/organization.model';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/organizations`;

  getAll(): Observable<Organization[]> {
    return this.http.get<any>(this.baseUrl).pipe(map(r => r.data));
  }

  getById(id: string): Observable<Organization> {
    return this.http.get<any>(`${this.baseUrl}/${id}`).pipe(map(r => r.data));
  }

  create(request: CreateOrganizationRequest): Observable<Organization> {
    return this.http.post<any>(this.baseUrl, request).pipe(map(r => r.data));
  }

  update(id: string, request: Partial<CreateOrganizationRequest>): Observable<Organization> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, request).pipe(map(r => r.data));
  }

  getMembers(id: string): Observable<User[]> {
    return this.http.get<any>(`${this.baseUrl}/${id}/members`).pipe(map(r => r.data));
  }

  inviteMember(id: string, request: InviteMemberRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${id}/invite`, request).pipe(map(r => r.data));
  }

  removeMember(orgId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${orgId}/members/${userId}`);
  }

  deleteMemberPermanently(orgId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${orgId}/members/${userId}/permanent`);
  }

  disable(id: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/disable`, {});
  }

  enable(id: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/enable`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateIcon(id: string, iconName: string): Observable<Organization> {
    return this.http.patch<any>(`${this.baseUrl}/${id}/icon`, { iconName }).pipe(map(r => r.data));
  }

  uploadLogo(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${environment.apiUrl}/files/upload/logo`, formData).pipe(
      map(r => r.data['url'])
    );
  }
}
