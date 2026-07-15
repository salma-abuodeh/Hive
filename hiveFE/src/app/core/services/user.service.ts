import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateUserRequest,
  PageResponse,
  UpdateMeRequest,
  UpdateUserRequest,
  UserResponse
} from '../../features/users/models/user.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/me`);
  }

  updateMe(payload: UpdateMeRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/me`, payload);
  }

  listAll(page = 0, size = 20, active?: boolean): Observable<PageResponse<UserResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (active !== undefined) params = params.set('active', active);
    return this.http.get<PageResponse<UserResponse>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  create(payload: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.baseUrl, payload);
  }

  update(id: number, payload: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  listCompany(page = 0, size = 20, active?: boolean): Observable<PageResponse<UserResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (active !== undefined) params = params.set('active', active);
    return this.http.get<PageResponse<UserResponse>>(`${this.baseUrl}/company`, { params });
  }

  getCompanyUser(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/company/${id}`);
  }

  createCompanyUser(payload: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/company`, payload);
  }

  updateCompanyUser(id: number, payload: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/company/${id}`, payload);
  }

  deleteCompanyUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/company/${id}`);
  }
}