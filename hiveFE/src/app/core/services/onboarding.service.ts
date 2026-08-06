import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CompanyResponse } from './company.service';

export interface RequestItem {
  id: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  requesterName: string;
  requesterEmail: string;
  companyId?: number;
  companyName: string;
  rejectionReason?: string;
  createdAt?: string;
  reviewedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  myCompanyApplications(): Observable<RequestItem[]> { return this.http.get<RequestItem[]>(`${this.api}/company-applications/me`); }
  pendingCompanyApplications(): Observable<RequestItem[]> { return this.http.get<RequestItem[]>(`${this.api}/company-applications`); }
  approveCompany(id: number): Observable<RequestItem> { return this.http.post<RequestItem>(`${this.api}/company-applications/${id}/approve`, {}); }
  rejectCompany(id: number, reason = ''): Observable<RequestItem> { return this.http.post<RequestItem>(`${this.api}/company-applications/${id}/reject`, { reason }); }

  joinableCompanies(): Observable<CompanyResponse[]> { return this.http.get<CompanyResponse[]>(`${this.api}/membership-requests/companies`); }
  requestMembership(companyId: number): Observable<RequestItem> { return this.http.post<RequestItem>(`${this.api}/membership-requests`, { companyId }); }
  myMembershipRequests(): Observable<RequestItem[]> { return this.http.get<RequestItem[]>(`${this.api}/membership-requests/me`); }
  pendingMembershipRequests(): Observable<RequestItem[]> { return this.http.get<RequestItem[]>(`${this.api}/membership-requests`); }
  approveMembership(id: number, roleName: 'Employee' | 'Manager'): Observable<RequestItem> { return this.http.post<RequestItem>(`${this.api}/membership-requests/${id}/approve`, { roleName }); }
  rejectMembership(id: number, reason = ''): Observable<RequestItem> { return this.http.post<RequestItem>(`${this.api}/membership-requests/${id}/reject`, { reason }); }
}
