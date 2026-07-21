import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JobTitle } from '../../features/users/models/user.models';

@Injectable({ providedIn: 'root' })
export class JobTitleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/companies/me/job-titles`;

  list(includeInactive = false): Observable<JobTitle[]> {
    const params = new HttpParams().set('includeInactive', includeInactive);
    return this.http.get<JobTitle[]>(this.baseUrl, { params });
  }

  create(title: string): Observable<JobTitle> {
    return this.http.post<JobTitle>(this.baseUrl, { title });
  }

  update(id: number, payload: { title?: string; active?: boolean }): Observable<JobTitle> {
    return this.http.put<JobTitle>(`${this.baseUrl}/${id}`, payload);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
