import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateTeamRequest, Team, UpdateTeamRequest } from '../../features/company/models/team.models';

@Injectable({ providedIn: 'root' })
export class TeamService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/teams`;

  list(): Observable<Team[]> {
    return this.http.get<Team[]>(this.baseUrl);
  }

  listMine(): Observable<Team[]> {
    return this.http.get<Team[]>(`${this.baseUrl}/mine`);
  }

  getById(id: number): Observable<Team> {
    return this.http.get<Team>(`${this.baseUrl}/${id}`);
  }

  create(payload: CreateTeamRequest): Observable<Team> {
    return this.http.post<Team>(this.baseUrl, payload);
  }

  update(id: number, payload: UpdateTeamRequest): Observable<Team> {
    return this.http.put<Team>(`${this.baseUrl}/${id}`, payload);
  }

  addMembers(id: number, userIds: number[]): Observable<Team> {
    return this.http.post<Team>(`${this.baseUrl}/${id}/members`, { userIds });
  }

  removeMember(id: number, userId: number): Observable<Team> {
    return this.http.delete<Team>(`${this.baseUrl}/${id}/members/${userId}`);
  }
}
