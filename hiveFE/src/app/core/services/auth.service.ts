import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import {
  CompanySummary,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse
} from '../../features/auth/models/auth.models';

const TOKEN_KEY = 'hive_token';
const USER_KEY = 'hive_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly baseUrl = `${environment.apiUrl}/auth`;
  private readonly usersUrl = `${environment.apiUrl}/users`;

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, payload).pipe(
      tap(res => this.saveSession(res))
    );
  }

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}/register`, payload).pipe(
      tap(res => this.saveSession(res))
    );
  }

  switchCompany(companyId: number): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.usersUrl}/me/switch-company`, { companyId })
      .pipe(tap((res) => this.saveSession(res)));
  }

  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem(TOKEN_KEY);
  }

  getUser(): LoginResponse | RegisterResponse | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  getCompanies(): CompanySummary[] {
    return this.getUser()?.companies ?? [];
  }

  getActiveCompanyId(): number | null {
    const user = this.getUser();
    if (!user) return null;
    if ('activeCompanyId' in user && user.activeCompanyId != null) {
      return user.activeCompanyId;
    }
    return user.companies?.[0]?.id ?? null;
  }

  getActiveCompany(): CompanySummary | null {
    const id = this.getActiveCompanyId();
    if (id == null) return null;
    return this.getCompanies().find((c) => c.id === id) ?? null;
  }

  getRole(): string | null {
    return this.getUser()?.role ?? null;
  }

  getPermissions(): string[] {
    return this.getUser()?.permissions ?? [];
  }

  hasPermission(permission: string): boolean {
    return this.getPermissions().includes(permission);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  isPlatformAdmin(): boolean {
    return this.hasPermission('PLATFORM_MANAGE');
  }

  isManager(): boolean {
    return this.hasPermission('USER_VIEW') && !this.isPlatformAdmin();
  }

  canManageUsers(): boolean {
    return this.hasPermission('USER_VIEW') || this.hasPermission('PLATFORM_MANAGE');
  }

  canManageTeams(): boolean {
    return this.hasPermission('TEAM_VIEW') || this.hasPermission('TEAM_CREATE');
  }

  private saveSession(res: LoginResponse | RegisterResponse): void {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res));
  }
  patchSession(partial: { firstName?: string; lastName?: string; email?: string }): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const current = this.getUser();
    if (!current) return;
    localStorage.setItem(USER_KEY, JSON.stringify({ ...current, ...partial }));
  }
  replaceSession(res: LoginResponse | RegisterResponse): void {
    this.saveSession(res);
  }
}
