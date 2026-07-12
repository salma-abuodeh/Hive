import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse
} from '../../features/auth/models/auth.models';

const TOKEN_KEY = 'hive_token';

@Injectable({providedIn: 'root'})
export class AuthService {
    readonly http = inject(HttpClient);
     readonly baseUrl = `${environment.apiUrl}/auth`;
     private readonly platformId = inject(PLATFORM_ID);


     login(payload: LoginRequest): Observable<LoginResponse> {
        return this.http.post<LoginResponse>(`${this.baseUrl}/login`, payload).pipe(
          tap(res => this.setToken(res.token))
        );
      }
      register(payload: RegisterRequest): Observable<RegisterResponse> {
        return this.http.post<RegisterResponse>(`${this.baseUrl}/register`, payload).pipe(
            tap(res => this.setToken(res.token)));}

            private setToken(token: string): void {
                if (!isPlatformBrowser(this.platformId)) {
                    return;
                }
                localStorage.setItem(TOKEN_KEY, token);
              }
              getToken(): string | null {
                if (!isPlatformBrowser(this.platformId)) {
                  return null;
                }
                return localStorage.getItem(TOKEN_KEY);
              }
              removeToken(): void {
                if (!isPlatformBrowser(this.platformId)) {
                    return;
                }
                localStorage.removeItem(TOKEN_KEY);
              }     
              isLoggedIn(): boolean {
                return !!this.getToken();
              }
              logout(): void {
                this.removeToken();
              }
}