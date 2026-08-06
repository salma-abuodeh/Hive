import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CompanyApplicationRequest {
  name: string;
  type: 'COMPANY' | 'SCHOOL';
  domain?: string;
}

export interface CompanyResponse {
  id: number;
  name: string;
  type: 'COMPANY' | 'SCHOOL';
  domain?: string;
  logoUrl?: string;
  status: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);

  apply(payload: CompanyApplicationRequest): Observable<{ id: number; status: string }> {
    return this.http.post<{ id: number; status: string }>(`${environment.apiUrl}/company-applications`, payload);
  }

  create(payload: CompanyApplicationRequest): Observable<CompanyResponse> {
    return this.http.post<CompanyResponse>(`${environment.apiUrl}/companies`, payload);
  }
}
