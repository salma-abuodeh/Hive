import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UpdateMeRequest } from '../../models/user.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit {
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);

  firstName = '';
  lastName = '';
  email = '';
  password = '';
  currentPassword = '';

  loading = signal(false);
  saving = signal(false);
  error = signal('');
  success = signal('');

  ngOnInit(): void {
    this.loading.set(true);
    this.userService.getMe().subscribe({
      next: (user) => {
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.email = user.email;
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Failed to load profile');
      }
    });
  }

  save(): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const payload: UpdateMeRequest = {
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email
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
        this.success.set('Profile updated');
        this.authService.patchSession({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email
        });
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.message ?? 'Update failed');
      }
    });
  }
}