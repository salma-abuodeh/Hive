import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './signup.html',
  styleUrl: './signup.css',
})
export class Signup {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  fullName = '';
  email = '';
  password = '';
  confirmPassword = '';
  acceptedTerms = false;

  error = signal('');
  loading = signal(false);
  strengthScore = signal(0);

  private readonly strengthLabels = ['Too weak', 'Weak', 'Fair', 'Good', 'Strong'];
  private readonly strengthColors = ['#e0e4e4', '#b3492f', '#f5a623', '#7d5533', '#4c7a4a'];

  onPasswordChange(value: string): void {
    this.strengthScore.set(this.scorePassword(value));
  }

  strengthPercent(): number {
    const score = this.strengthScore();
    return this.password ? (score / 4) * 100 : 0;
  }

  strengthColor(): string {
    return this.strengthColors[this.strengthScore()];
  }

  strengthLabel(): string {
    return this.password ? this.strengthLabels[this.strengthScore()] : '';
  }

  canSubmit(): boolean {
    return !!(
      this.fullName.trim() &&
      this.email &&
      this.password &&
      this.confirmPassword &&
      this.acceptedTerms
    );
  }

  signup(): void {
    if (!this.canSubmit()) return;

    if (this.password !== this.confirmPassword) {
      this.error.set('Passwords do not match');
      return;
    }

    const parts = this.fullName.trim().split(/\s+/);
    const firstName = parts[0] ?? '';
    const lastName = parts.slice(1).join(' ') || firstName;

    this.loading.set(true);
    this.error.set('');

    this.auth
      .register({
        firstName,
        lastName,
        email: this.email,
        password: this.password,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.message ?? 'Signup failed');
        },
      });
  }

  private scorePassword(pw: string): number {
    let score = 0;
    if (pw.length >= 8) score++;
    if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
    if (/\d/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;
    return score;
  }
}
