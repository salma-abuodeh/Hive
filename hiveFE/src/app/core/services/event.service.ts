import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../features/users/models/user.models';
import {
  EventRequest,
  EventResponse,
  EventRsvpResponse,
  InviteUsersRequest,
  RsvpUpdateRequest
} from '../../features/events/models/event.models';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/events`;

  list(page = 0, size = 20): Observable<PageResponse<EventResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<EventResponse>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.baseUrl}/${id}`);
  }

  create(payload: EventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(this.baseUrl, payload);
  }

  update(id: number, payload: EventRequest): Observable<EventResponse> {
    return this.http.put<EventResponse>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  invite(id: number, payload: InviteUsersRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/invitations`, payload);
  }

  listInvitations(id: number): Observable<EventRsvpResponse[]> {
    return this.http.get<EventRsvpResponse[]>(`${this.baseUrl}/${id}/invitations`);
  }

  removeInvitation(id: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/invitations/${userId}`);
  }

  rsvp(id: number, payload: RsvpUpdateRequest): Observable<EventRsvpResponse> {
    return this.http.put<EventRsvpResponse>(`${this.baseUrl}/${id}/rsvp`, payload);
  }
}