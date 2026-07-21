import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NavMenuItem } from '../../models/nav-menu-item';
import { MenuItem } from '../menu-item/menu-item';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, MenuItem],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly orgMenuOpen = signal(false);

  private readonly workspaceItems: NavMenuItem[] = [
    { name: 'Dashboard', url: '/dashboard', icon: 'dashboard' },
    { name: 'Feed', url: '/feed', icon: 'feed' },
    { name: 'Saved', url: '/saved', icon: 'saved' },
    { name: 'Community', url: '/community', icon: 'community' },
    { name: 'Notifications', url: '/notifications', icon: 'notifications' },
    { name: 'People', url: '/users', icon: 'people', requiresManageUsers: true },
  ];

  readonly accountItems: NavMenuItem[] = [
    { name: 'My Profile', url: '/profile', icon: 'profile' },
  ];

  readonly visibleWorkspaceItems = computed(() =>
    this.workspaceItems.filter(
      (item) => !item.requiresManageUsers || this.auth.canManageUsers()
    )
  );

  companyName(): string {
    const companies = this.auth.getUser()?.companies;
    return companies?.length ? companies[0].name : 'Your workspace';
  }

  companyInitials(): string {
    const name = this.companyName();
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
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
