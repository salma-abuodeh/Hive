import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { JobTitleService } from '../../../../core/services/job-title.service';
import { TeamService } from '../../../../core/services/team.service';
import { Team } from '../../../company/models/team.models';
import { CreateUserRequest, JobTitle, UpdateUserRequest, UserResponse } from '../../models/user.models';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './users-list.html',
  styleUrl: './users-list.css',
})
export class UsersList implements OnInit {
  private readonly userService = inject(UserService);
  private readonly jobTitleService = inject(JobTitleService);
  private readonly teamService = inject(TeamService);
  private readonly platformId = inject(PLATFORM_ID);
  readonly auth = inject(AuthService);

  users = signal<UserResponse[]>([]);
  jobTitles = signal<JobTitle[]>([]);
  teams = signal<Team[]>([]);
  loading = signal(false);
  saving = signal(false);
  error = signal('');
  formError = signal('');
  toast = signal<string | null>(null);
  jobTitleError = signal('');
  title = signal('Users');

  readonly showCreate = signal(false);
  readonly showTitles = signal(false);

  firstName = '';
  lastName = '';
  email = '';
  password = '';
  roleName = 'Employee';
  companyId: number | null = null;
  jobTitleId: number | null = null;
  teamIds: number[] = [];
  newJobTitle = '';

  editingId: number | null = null;
  editFirstName = '';
  editLastName = '';
  editEmail = '';
  editRoleName = 'Employee';
  editActive = true;
  editPassword = '';
  editJobTitleId: number | null = null;
  editTeamIds: number[] = [];

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    if (!this.auth.canManageUsers()) {
      this.error.set('You do not have access to this page');
      return;
    }
    this.title.set(this.auth.isPlatformAdmin() ? 'All users' : 'Company users');
    this.load();
    if (!this.auth.isPlatformAdmin() || this.auth.getUser()?.companies?.length) {
      this.loadJobTitles();
    }
    if (this.canAssignTeams()) {
      this.loadTeams();
    }
  }

  canManageTitles(): boolean {
    return !this.auth.isPlatformAdmin() || !!this.auth.getUser()?.companies?.length;
  }

  canAssignTeams(): boolean {
    return this.auth.canManageTeams();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    const request = this.auth.isPlatformAdmin()
      ? this.userService.listAll(0, 50)
      : this.userService.listCompany(0, 50);

    request.subscribe({
      next: (page) => {
        this.users.set(page.content ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Failed to load users');
      },
    });
  }

  loadJobTitles(): void {
    this.jobTitleService.list(true).subscribe({
      next: (titles) => this.jobTitles.set(titles),
      error: () => this.jobTitles.set([]),
    });
  }

  loadTeams(): void {
    this.teamService.list().subscribe({
      next: (teams) => this.teams.set(teams.filter((t) => t.active)),
      error: () => this.teams.set([]),
    });
  }

  activeJobTitles(): JobTitle[] {
    return this.jobTitles().filter((t) => t.active);
  }

  openCreate(): void {
    this.formError.set('');
    this.firstName = '';
    this.lastName = '';
    this.email = '';
    this.password = '';
    this.roleName = 'Employee';
    this.companyId = null;
    this.jobTitleId = null;
    this.teamIds = [];
    this.showCreate.set(true);
  }

  closeCreate(): void {
    this.showCreate.set(false);
    this.formError.set('');
  }

  openTitles(): void {
    this.jobTitleError.set('');
    this.newJobTitle = '';
    this.showTitles.set(true);
  }

  closeTitles(): void {
    this.showTitles.set(false);
    this.jobTitleError.set('');
  }

  addJobTitle(): void {
    const title = this.newJobTitle.trim();
    if (!title) return;
    this.jobTitleError.set('');
    this.jobTitleService.create(title).subscribe({
      next: (created) => {
        this.jobTitles.update((list) =>
          [...list, created].sort((a, b) => a.title.localeCompare(b.title))
        );
        this.newJobTitle = '';
        this.showToast('Job title added');
      },
      error: (err) => this.jobTitleError.set(err.error?.message ?? 'Could not add job title'),
    });
  }

  deactivateJobTitle(title: JobTitle): void {
    this.jobTitleService.deactivate(title.id).subscribe({
      next: () => {
        this.jobTitles.update((list) =>
          list.map((t) => (t.id === title.id ? { ...t, active: false } : t))
        );
        this.showToast('Job title removed');
      },
      error: (err) => this.jobTitleError.set(err.error?.message ?? 'Could not remove job title'),
    });
  }

  toggleTeam(id: number, selected: boolean): void {
    if (selected) {
      if (!this.teamIds.includes(id)) this.teamIds = [...this.teamIds, id];
    } else {
      this.teamIds = this.teamIds.filter((t) => t !== id);
    }
  }

  toggleEditTeam(id: number, selected: boolean): void {
    if (selected) {
      if (!this.editTeamIds.includes(id)) this.editTeamIds = [...this.editTeamIds, id];
    } else {
      this.editTeamIds = this.editTeamIds.filter((t) => t !== id);
    }
  }

  isTeamSelected(id: number): boolean {
    return this.teamIds.includes(id);
  }

  isEditTeamSelected(id: number): boolean {
    return this.editTeamIds.includes(id);
  }

  create(): void {
    this.formError.set('');
    this.saving.set(true);

    const payload: CreateUserRequest = {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password,
      roleName: this.roleName,
    };

    if (this.jobTitleId != null) {
      payload.jobTitleId = this.jobTitleId;
    }
    if (this.canAssignTeams()) {
      payload.teamIds = [...this.teamIds];
    }

    if (this.auth.isPlatformAdmin() && this.companyId != null) {
      payload.companyIds = [this.companyId];
    }

    const request = this.auth.isPlatformAdmin()
      ? this.userService.create(payload)
      : this.userService.createCompanyUser(payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeCreate();
        this.showToast('User created');
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(err.error?.message ?? 'Create failed');
      },
    });
  }

  startEdit(u: UserResponse): void {
    this.editingId = u.id;
    this.editFirstName = u.firstName;
    this.editLastName = u.lastName;
    this.editEmail = u.email;
    this.editRoleName = u.roleName || 'Employee';
    this.editActive = u.active;
    this.editPassword = '';
    this.editJobTitleId = u.jobTitleId ?? null;
    this.editTeamIds = (u.teams ?? []).map((t) => t.id);
    this.formError.set('');
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(): void {
    if (this.editingId == null) return;
    this.saving.set(true);
    this.formError.set('');

    const payload: UpdateUserRequest = {
      firstName: this.editFirstName,
      lastName: this.editLastName,
      email: this.editEmail,
      roleName: this.editRoleName,
      active: this.editActive,
    };
    if (this.editPassword) payload.password = this.editPassword;
    if (this.editJobTitleId != null) payload.jobTitleId = this.editJobTitleId;
    if (this.canAssignTeams()) {
      payload.teamIds = [...this.editTeamIds];
    }

    const id = this.editingId;
    const request = this.auth.isPlatformAdmin()
      ? this.userService.update(id, payload)
      : this.userService.updateCompanyUser(id, payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.editingId = null;
        this.showToast('User updated');
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(err.error?.message ?? 'Update failed');
      },
    });
  }

  toggleActive(u: UserResponse): void {
    const payload = { active: !u.active };
    const request = this.auth.isPlatformAdmin()
      ? this.userService.update(u.id, payload)
      : this.userService.updateCompanyUser(u.id, payload);
    request.subscribe({
      next: () => this.load(),
      error: (err) => this.formError.set(err.error?.message ?? 'Status update failed'),
    });
  }

  remove(u: UserResponse): void {
    if (!confirm(`Delete ${u.email}?`)) return;
    const request = this.auth.isPlatformAdmin()
      ? this.userService.delete(u.id)
      : this.userService.deleteCompanyUser(u.id);
    request.subscribe({
      next: () => {
        this.showToast('User deleted');
        this.load();
      },
      error: (err) => this.formError.set(err.error?.message ?? 'Delete failed'),
    });
  }

  initials(u: UserResponse): string {
    return `${u.firstName?.charAt(0) ?? ''}${u.lastName?.charAt(0) ?? ''}`.toUpperCase();
  }

  private showToast(message: string): void {
    this.toast.set(message);
    setTimeout(() => {
      if (this.toast() === message) this.toast.set(null);
    }, 2200);
  }
}
