import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { CategoryStatus } from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryStatusService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/category-statuses`;

  getByCategory(category: string): Observable<CategoryStatus[]> {
    return this.http.get<any>(`${this.baseUrl}?category=${category}`).pipe(map(r => r.data));
  }

  getAllCategories(): Observable<Record<string, string>> {
    return this.http.get<any>(`${this.baseUrl}/categories`).pipe(map(r => r.data));
  }
}
