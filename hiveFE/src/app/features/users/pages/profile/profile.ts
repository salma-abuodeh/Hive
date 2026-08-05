import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CompanyMembership, TeamSummary, UpdateMeRequest } from '../../models/user.models';

type ProfileTab = 'about' | 'posts';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  firstName = '';
  lastName = '';
  email = '';
  roleName = '';
  jobTitle = '';
  createdAt: string | null = null;
  password = '';
  currentPassword = '';

  companies = signal<CompanyMembership[]>([]);
  activeCompanyId = signal<number | null>(null);
  teams = signal<TeamSummary[]>([]);

  loading = signal(false);
  saving = signal(false);
  editing = signal(false);
  error = signal('');
  success = signal('');
  tab = signal<ProfileTab>('about');

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.userService.getMe().subscribe({
      next: (user) => {
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.email = user.email;
        this.roleName = user.roleName ?? this.authService.getRole() ?? '';
        this.jobTitle = user.jobTitle ?? '';
        this.createdAt = user.createdAt ?? null;
        this.companies.set(user.companies ?? []);
        this.activeCompanyId.set(user.activeCompanyId ?? null);
        this.teams.set(user.teams ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Failed to load profile');
      },
    });
  }

  initials(): string {
    return `${this.firstName?.charAt(0) ?? ''}${this.lastName?.charAt(0) ?? ''}`.toUpperCase() || '?';
  }

  fullName(): string {
    return `${this.firstName} ${this.lastName}`.trim();
  }

  activeCompany(): CompanyMembership | null {
    const id = this.activeCompanyId();
    const list = this.companies();
    if (id != null) {
      return list.find((c) => c.id === id) ?? null;
    }
    return list[0] ?? null;
  }

  companyName(): string {
    return this.activeCompany()?.name
      ?? this.authService.getActiveCompany()?.name
      ?? 'No company';
  }

  headline(): string {
    const active = this.activeCompany();
    const title = active?.jobTitle || this.jobTitle || active?.roleName || this.roleName;
    const parts = [title, this.companyName()].filter(Boolean);
    return parts.join(' · ');
  }

  isActiveCompany(company: CompanyMembership): boolean {
    return company.id === this.activeCompanyId();
  }

  setTab(tab: ProfileTab): void {
    this.tab.set(tab);
  }

  startEdit(): void {
    this.editing.set(true);
    this.error.set('');
    this.success.set('');
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.password = '';
    this.currentPassword = '';
    this.load();
  }

  save(): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const payload: UpdateMeRequest = {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
    };

    if (this.password) {
      payload.password = this.password;
      payload.currentPassword = this.currentPassword;
    }

    const session = this.authService.getUser();
    if (session && this.email !== session.email) {
      payload.currentPassword = this.currentPassword;
    }

    this.userService.updateMe(payload).subscribe({
      next: (user) => {
        this.saving.set(false);
        this.password = '';
        this.currentPassword = '';
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.email = user.email;
        this.roleName = user.roleName ?? this.roleName;
        this.jobTitle = user.jobTitle ?? this.jobTitle;
        this.companies.set(user.companies ?? []);
        this.activeCompanyId.set(user.activeCompanyId ?? null);
        this.teams.set(user.teams ?? []);
        this.success.set('Profile updated');
        this.editing.set(false);
        this.authService.patchSession({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
        });
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.message ?? 'Update failed');
      },
    });
  }
}
