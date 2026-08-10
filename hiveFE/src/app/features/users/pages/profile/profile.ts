import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { AttachmentService } from '../../../../core/services/attachment.service';
import { AuthImage } from '../../../../shared/components/auth-image/auth-image';
import { CompanyMembership, TeamSummary, UpdateMeRequest } from '../../models/user.models';

type ProfileTab = 'about' | 'posts';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, AuthImage],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly attachmentService = inject(AttachmentService);
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

  avatarUrl = signal<string | null>(null);
  avatarUploading = signal(false);
  avatarError = signal('');

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
        this.avatarUrl.set(user.avatarUrl ?? null);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Failed to load profile');
      },
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // allow re-selecting the same file later
    if (!file) return;

    const validationError = this.attachmentService.validate(file);
    if (validationError) {
      this.avatarError.set(validationError);
      return;
    }

    this.avatarError.set('');
    this.avatarUploading.set(true);
    this.attachmentService.uploadAvatar(file).subscribe({
      next: (attachment) => {
        this.avatarUrl.set(attachment.url);
        this.avatarUploading.set(false);
      },
      error: (err) => {
        this.avatarUploading.set(false);
        this.avatarError.set(err.error?.message ?? 'Failed to upload photo');
      },
    });
  }

  removeAvatar(): void {
    this.avatarError.set('');
    this.avatarUploading.set(true);
    this.attachmentService.deleteAvatar().subscribe({
      next: () => {
        this.avatarUrl.set(null);
        this.avatarUploading.set(false);
      },
      error: (err) => {
        this.avatarUploading.set(false);
        this.avatarError.set(err.error?.message ?? 'Failed to remove photo');
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