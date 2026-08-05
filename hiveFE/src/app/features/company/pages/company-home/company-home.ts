import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { UserService } from '../../../../core/services/user.service';
import { UserResponse } from '../../../users/models/user.models';
import { Team } from '../../models/team.models';

@Component({
  selector: 'app-company-home',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './company-home.html',
  styleUrl: './company-home.css',
})
export class CompanyHome implements OnInit {
  private readonly teamsApi = inject(TeamService);
  private readonly usersApi = inject(UserService);
  private readonly platformId = inject(PLATFORM_ID);
  readonly auth = inject(AuthService);

  readonly teams = signal<Team[]>([]);
  readonly companyUsers = signal<UserResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly toast = signal<string | null>(null);

  readonly showCreate = signal(false);
  readonly managing = signal<Team | null>(null);

  newName = '';
  newDescription = '';
  editName = '';
  editDescription = '';
  addUserId: number | null = null;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (!this.auth.canManageTeams()) {
      this.error.set('You do not have access to this page');
      return;
    }
    this.load();
    this.loadCompanyUsers();
  }

  companyLabel(): string {
    const companies = this.auth.getUser()?.companies;
    return companies?.length ? companies[0].name : 'Your company';
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.teamsApi.list().subscribe({
      next: (teams) => {
        this.teams.set(teams);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Could not load teams');
      },
    });
  }

  loadCompanyUsers(): void {
    this.usersApi.listCompany(0, 100).subscribe({
      next: (page) => this.companyUsers.set(page.content ?? []),
      error: () => this.companyUsers.set([]),
    });
  }

  initials(name: string): string {
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
  }

  memberInitials(first: string, last: string): string {
    return `${first?.charAt(0) ?? ''}${last?.charAt(0) ?? ''}`.toUpperCase() || '?';
  }

  openCreate(): void {
    this.newName = '';
    this.newDescription = '';
    this.showCreate.set(true);
  }

  createTeam(): void {
    const name = this.newName.trim();
    if (!name || this.saving()) return;
    this.saving.set(true);
    this.teamsApi.create({ name, description: this.newDescription.trim() || undefined }).subscribe({
      next: (team) => {
        this.teams.update((list) =>
          [...list, team].sort((a, b) => a.name.localeCompare(b.name))
        );
        this.saving.set(false);
        this.showCreate.set(false);
        this.showToast('Team created');
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Could not create team');
      },
    });
  }

  openManage(team: Team): void {
    this.addUserId = null;
    this.editName = team.name;
    this.editDescription = team.description ?? '';
    this.teamsApi.getById(team.id).subscribe({
      next: (detail) => {
        this.managing.set(detail);
        this.editName = detail.name;
        this.editDescription = detail.description ?? '';
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Could not load team'),
    });
  }

  closeManage(): void {
    this.managing.set(null);
    this.editName = '';
    this.editDescription = '';
  }

  saveTeamDetails(): void {
    const team = this.managing();
    const name = this.editName.trim();
    if (!team || !name || this.saving()) return;

    this.saving.set(true);
    this.error.set(null);
    this.teamsApi
      .update(team.id, {
        name,
        description: this.editDescription.trim() || undefined,
      })
      .subscribe({
        next: (updated) => {
          this.managing.set(updated);
          this.teams.update((list) =>
            list
              .map((t) =>
                t.id === updated.id
                  ? {
                      ...t,
                      name: updated.name,
                      description: updated.description,
                      memberCount: updated.memberCount,
                    }
                  : t
              )
              .sort((a, b) => a.name.localeCompare(b.name))
          );
          this.editName = updated.name;
          this.editDescription = updated.description ?? '';
          this.saving.set(false);
          this.showToast('Team updated');
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(err?.error?.message ?? 'Could not update team');
        },
      });
  }

  availableUsers(): UserResponse[] {
    const team = this.managing();
    if (!team?.members) return this.companyUsers();
    const memberIds = new Set(team.members.map((m) => m.userId));
    return this.companyUsers().filter((u) => !memberIds.has(u.id) && u.active);
  }

  addMember(): void {
    const team = this.managing();
    if (!team || this.addUserId == null || this.saving()) return;
    this.saving.set(true);
    this.teamsApi.addMembers(team.id, [this.addUserId]).subscribe({
      next: (updated) => {
        this.managing.set(updated);
        this.teams.update((list) =>
          list.map((t) => (t.id === updated.id ? { ...t, memberCount: updated.memberCount } : t))
        );
        this.addUserId = null;
        this.saving.set(false);
        this.showToast('Member added');
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Could not add member');
      },
    });
  }

  removeMember(userId: number): void {
    const team = this.managing();
    if (!team || this.saving()) return;
    this.saving.set(true);
    this.teamsApi.removeMember(team.id, userId).subscribe({
      next: (updated) => {
        this.managing.set(updated);
        this.teams.update((list) =>
          list.map((t) => (t.id === updated.id ? { ...t, memberCount: updated.memberCount } : t))
        );
        this.saving.set(false);
        this.showToast('Member removed');
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Could not remove member');
      },
    });
  }

  deactivateTeam(team: Team): void {
    if (!confirm(`Deactivate ${team.name}?`)) return;
    this.teamsApi.update(team.id, { active: false }).subscribe({
      next: () => {
        this.teams.update((list) => list.filter((t) => t.id !== team.id));
        if (this.managing()?.id === team.id) this.managing.set(null);
        this.showToast('Team deactivated');
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Could not deactivate team'),
    });
  }

  private showToast(message: string): void {
    this.toast.set(message);
    setTimeout(() => {
      if (this.toast() === message) this.toast.set(null);
    }, 2200);
  }
}
