import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../features/users/models/user.models';
import { PollRequest, PollResponse, VoteRequest } from '../../features/polls/models/poll.models';

@Injectable({ providedIn: 'root' })
export class PollService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/polls`;

  list(page = 0, size = 20): Observable<PageResponse<PollResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<PollResponse>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<PollResponse> {
    return this.http.get<PollResponse>(`${this.baseUrl}/${id}`);
  }

  create(payload: PollRequest): Observable<PollResponse> {
    return this.http.post<PollResponse>(this.baseUrl, payload);
  }

  update(id: number, payload: PollRequest): Observable<PollResponse> {
    return this.http.put<PollResponse>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  vote(id: number, payload: VoteRequest): Observable<PollResponse> {
    return this.http.put<PollResponse>(`${this.baseUrl}/${id}/vote`, payload);
  }

  unvote(id: number): Observable<PollResponse> {
    return this.http.delete<PollResponse>(`${this.baseUrl}/${id}/vote`);
  }
}