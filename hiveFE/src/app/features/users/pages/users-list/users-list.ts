import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { Team } from '../../../company/models/team.models';
import { CreateUserRequest, UpdateUserRequest, UserResponse } from '../../models/user.models';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './users-list.html',
  styleUrl: './users-list.css',
})
export class UsersList implements OnInit {
  private readonly userService = inject(UserService);
  private readonly teamService = inject(TeamService);
  private readonly platformId = inject(PLATFORM_ID);
  readonly auth = inject(AuthService);

  users = signal<UserResponse[]>([]);
  teams = signal<Team[]>([]);
  loading = signal(false);
  saving = signal(false);
  error = signal('');
  formError = signal('');
  toast = signal<string | null>(null);
  title = signal('Users');

  readonly showCreate = signal(false);
  readonly showEdit = signal(false);

  firstName = '';
  lastName = '';
  email = '';
  password = '';
  roleName = 'Employee';
  companyId: number | null = null;
  jobTitle = '';
  teamIds: number[] = [];

  editingId: number | null = null;
  editFirstName = '';
  editLastName = '';
  editEmail = '';
  editRoleName = 'Employee';
  editActive = true;
  editPassword = '';
  editJobTitle = '';
  editTeamIds: number[] = [];

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    if (!this.auth.canManageUsers()) {
      this.error.set('You do not have access to this page');
      return;
    }
    this.title.set(this.auth.isPlatformAdmin() ? 'All users' : 'Company users');
    this.load();
    if (this.canAssignTeams()) {
      this.loadTeams();
    }
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

  loadTeams(): void {
    this.teamService.list().subscribe({
      next: (teams) => this.teams.set(teams.filter((t) => t.active)),
      error: () => this.teams.set([]),
    });
  }

  editingUser(): UserResponse | null {
    if (this.editingId == null) return null;
    return this.users().find((u) => u.id === this.editingId) ?? null;
  }

  openCreate(): void {
    this.closeEdit();
    this.formError.set('');
    this.firstName = '';
    this.lastName = '';
    this.email = '';
    this.password = '';
    this.roleName = 'Employee';
    this.companyId = null;
    this.jobTitle = '';
    this.teamIds = [];
    this.showCreate.set(true);
  }

  closeCreate(): void {
    this.showCreate.set(false);
    this.formError.set('');
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

    const title = this.jobTitle.trim();
    if (title) {
      payload.jobTitle = title;
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
    this.closeCreate();
    this.editingId = u.id;
    this.editFirstName = u.firstName;
    this.editLastName = u.lastName;
    this.editEmail = u.email;
    this.editRoleName = u.roleName || 'Employee';
    this.editActive = u.active;
    this.editPassword = '';
    this.editJobTitle = u.jobTitle ?? '';
    this.editTeamIds = (u.teams ?? []).map((t) => t.id);
    this.formError.set('');
    this.showEdit.set(true);
  }

  closeEdit(): void {
    this.showEdit.set(false);
    this.editingId = null;
    this.formError.set('');
    this.saving.set(false);
  }

  cancelEdit(): void {
    this.closeEdit();
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
      jobTitle: this.editJobTitle.trim(),
    };
    if (this.editPassword) payload.password = this.editPassword;
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
        this.closeEdit();
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
      next: () => {
        this.showToast(u.active ? 'User deactivated' : 'User activated');
        this.load();
      },
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
