import { Component, HostListener, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, Router, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css',
})
export class AppLayout {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly orgMenuOpen = signal(false);

  displayName(): string {
    const u = this.auth.getUser();
    if (!u) return 'Hive User';
    return `${u.firstName} ${u.lastName}`.trim() || 'Hive User';
  }

  userInitials(): string {
    const u = this.auth.getUser();
    if (!u) return 'H';
    const first = u.firstName?.charAt(0) ?? '';
    const last = u.lastName?.charAt(0) ?? '';
    return (first + last || 'H').toUpperCase();
  }

  roleLabel(): string {
    const role = this.auth.getRole() ?? 'Member';
    return role
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }

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
