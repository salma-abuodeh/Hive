import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Icon } from '../icon/icon';

@Component({
  selector: 'app-header',
  imports: [RouterLink, Icon],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  private readonly auth = inject(AuthService);

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
}
