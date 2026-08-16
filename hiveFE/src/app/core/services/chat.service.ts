import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CompanyMember, ConversationApiResponse, MessageApiResponse, PageResponse } from '../../features/chats/models/chat.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/conversations`;

  list(): Observable<ConversationApiResponse[]> { return this.http.get<ConversationApiResponse[]>(this.baseUrl); }
  companyMembers(): Observable<CompanyMember[]> { return this.http.get<CompanyMember[]>(`${environment.apiUrl}/users/company/members`); }
  createDirect(targetUserId: number): Observable<ConversationApiResponse> {
    return this.http.post<ConversationApiResponse>(`${this.baseUrl}/direct`, { targetUserId });
  }
  messages(conversationId: string): Observable<PageResponse<MessageApiResponse>> {
    return this.http.get<PageResponse<MessageApiResponse>>(`${this.baseUrl}/${conversationId}/messages`, { params: { page: 0, size: 100, sort: 'createdAt,asc' } });
  }
  send(conversationId: string, content: string): Observable<MessageApiResponse> {
    return this.http.post<MessageApiResponse>(`${this.baseUrl}/${conversationId}/messages`, { content, messageType: 'TEXT' });
  }
  markRead(conversationId: string): Observable<void> { return this.http.put<void>(`${this.baseUrl}/${conversationId}/read`, {}); }
}
