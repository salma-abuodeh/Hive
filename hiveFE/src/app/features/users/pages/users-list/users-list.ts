import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CreateUserRequest, UserResponse } from '../../models/user.models';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './users-list.html',
  styleUrl: './users-list.css'
})
export class UsersList implements OnInit {
  private readonly userService = inject(UserService);
  readonly auth = inject(AuthService);

  users = signal<UserResponse[]>([]);
  loading = signal(false);
  saving = signal(false);
  error = signal('');
  formError = signal('');
  formSuccess = signal('');
  title = signal('Users');

  firstName = '';
  lastName = '';
  email = '';
  password = '';
  roleName = 'Employee';
  companyId: number | null = null;

  ngOnInit(): void {
    if (!this.auth.canManageUsers()) {
      this.error.set('You do not have access to this page');
      return;
    }
    this.title.set(this.auth.isPlatformAdmin() ? 'All users' : 'Company users');
    this.load();
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
      }
    });
  }

  create(): void {
    this.formError.set('');
    this.formSuccess.set('');
    this.saving.set(true);

    const payload: CreateUserRequest = {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password,
      roleName: this.roleName
    };

    if (this.auth.isPlatformAdmin() && this.companyId != null) {
      payload.companyIds = [this.companyId];
    }

    const request = this.auth.isPlatformAdmin()
      ? this.userService.create(payload)
      : this.userService.createCompanyUser(payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.formSuccess.set('User created');
        this.firstName = '';
        this.lastName = '';
        this.email = '';
        this.password = '';
        this.roleName = 'Employee';
        this.companyId = null;
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(err.error?.message ?? 'Create failed');
      }
    });
  }
  editingId: number | null = null;
  editFirstName = '';
  editLastName = '';
  editEmail = '';
  editRoleName = 'Employee';
  editActive = true;
  editPassword = '';

  startEdit(u: UserResponse): void {
    this.editingId = u.id;
    this.editFirstName = u.firstName;
    this.editLastName = u.lastName;
    this.editEmail = u.email;
    this.editRoleName = u.roleName;
    this.editActive = u.active;
    this.editPassword = '';
    this.formError.set('');
    this.formSuccess.set('');
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(): void {
    if (this.editingId == null) return;
    this.saving.set(true);
    this.formError.set('');

    const payload: any = {
      firstName: this.editFirstName,
      lastName: this.editLastName,
      email: this.editEmail,
      roleName: this.editRoleName,
      active: this.editActive
    };
    if (this.editPassword) payload.password = this.editPassword;

    const id = this.editingId;
    const request = this.auth.isPlatformAdmin()
      ? this.userService.update(id, payload)
      : this.userService.updateCompanyUser(id, payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.editingId = null;
        this.formSuccess.set('User updated');
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(err.error?.message ?? 'Update failed');
      }
    });
  }

  toggleActive(u: UserResponse): void {
    const payload = { active: !u.active };
    const request = this.auth.isPlatformAdmin()
      ? this.userService.update(u.id, payload)
      : this.userService.updateCompanyUser(u.id, payload);
    request.subscribe({
      next: () => this.load(),
      error: (err) => this.formError.set(err.error?.message ?? 'Status update failed')
    });
  }

  remove(u: UserResponse): void {
    if (!confirm(`Delete ${u.email}?`)) return;
    const request = this.auth.isPlatformAdmin()
      ? this.userService.delete(u.id)
      : this.userService.deleteCompanyUser(u.id);
    request.subscribe({
      next: () => this.load(),
      error: (err) => this.formError.set(err.error?.message ?? 'Delete failed')
    });
  }
}