import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CompanySummary } from '../../../features/auth/models/auth.models';
import { NavMenuItem } from '../../models/nav-menu-item';
import { Icon } from '../icon/icon';
import { MenuItem } from '../menu-item/menu-item';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, MenuItem, Icon],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly orgMenuOpen = signal(false);
  readonly switching = signal(false);

  private readonly workspaceItems: NavMenuItem[] = [
    { name: 'Dashboard', url: '/dashboard', icon: 'dashboard' },
    { name: 'Feed', url: '/feed', icon: 'feed' },
    { name: 'Events', url: '/events', icon: 'events' },
    { name: 'Saved', url: '/saved', icon: 'saved' },
    { name: 'Notifications', url: '/notifications', icon: 'notifications' },
    { name: 'Management', url: '/management', icon: 'people', requiresManagement: true },
    { name: 'People', url: '/users', icon: 'people', requiresManageUsers: true },
    { name: 'Company', url: '/company', icon: 'company', requiresManageTeams: true },
  ];

  readonly accountItems: NavMenuItem[] = [
    { name: 'My Profile', url: '/profile', icon: 'profile' },
  ];

  readonly visibleWorkspaceItems = computed(() =>
    this.workspaceItems.filter(
      (item) =>
        (this.auth.getActiveCompanyId() != null || item.url === '/dashboard' || item.requiresManagement) &&
        (!item.requiresManageUsers || this.auth.canManageUsers()) &&
        (!item.requiresManageTeams || this.auth.canManageTeams()) &&
        (!item.requiresManagement || this.auth.isPlatformAdmin() || this.auth.isManager())
    )
  );

  companies(): CompanySummary[] {
    return this.auth.getCompanies();
  }

  companyName(): string {
    return this.auth.getActiveCompany()?.name ?? 'Your workspace';
  }

  companyInitials(): string {
    const name = this.companyName();
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
  }

  isActiveCompany(company: CompanySummary): boolean {
    return company.id === this.auth.getActiveCompanyId();
  }

  switchWorkspace(company: CompanySummary, event: Event): void {
    event.stopPropagation();
    if (this.isActiveCompany(company) || this.switching()) {
      this.orgMenuOpen.set(false);
      return;
    }
    this.switching.set(true);
    this.auth.switchCompany(company.id).subscribe({
      next: () => {
        this.orgMenuOpen.set(false);
        window.location.reload();
      },
      error: () => this.switching.set(false),
    });
  }

  closeOrgMenuAndNavigate(): void {
    this.orgMenuOpen.set(false);
  }

  toggleOrgMenu(event: Event): void {
    event.stopPropagation();
    this.orgMenuOpen.update((v) => !v);
  }

  @HostListener('document:click')
  closeOrgMenu(): void {
    this.orgMenuOpen.set(false);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}