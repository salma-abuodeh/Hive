import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse } from '../../features/auth/models/auth.models';

export interface CompanyApplicationRequest {
  name: string;
  type: 'COMPANY' | 'SCHOOL';
  domain?: string;
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);

  apply(payload: CompanyApplicationRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/company-applications`, payload);
  }

  create(payload: CompanyApplicationRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/companies`, payload);
  }
}
