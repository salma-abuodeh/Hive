import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { CompanyService } from '../../../../core/services/company.service';

@Component({
  selector: 'app-dashboard-home',
  imports: [FormsModule],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.css',
})
export class DashboardHome {
  readonly auth = inject(AuthService);
  private readonly companies = inject(CompanyService);
  readonly showCompanyForm = signal(false);
  readonly saving = signal(false);
  readonly message = signal('');
  companyName = '';
  companyType: 'COMPANY' | 'SCHOOL' = 'COMPANY';
  companyDomain = '';

  get firstName(): string {
    return this.auth.getUser()?.firstName || 'there';
  }

  get isPlatformAdmin(): boolean {
    return this.auth.isPlatformAdmin();
  }

  submitCompany(): void {
    if (!this.companyName.trim()) return;
    this.saving.set(true);
    this.message.set('');

    const payload = {
      name: this.companyName.trim(),
      type: this.companyType,
      domain: this.companyDomain.trim() || undefined,
    };

    if (this.isPlatformAdmin) {
      this.companies.create(payload).subscribe({
        next: (company) => {
          this.saving.set(false);
          this.showCompanyForm.set(false);
          this.message.set(`${company.name} has been created.`);
          this.companyName = '';
          this.companyDomain = '';
        },
        error: (err) => {
          this.saving.set(false);
          this.message.set(err.error?.message ?? 'We could not create the company. Please try again.');
        },
      });
      return;
    }

    this.companies.apply(payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.showCompanyForm.set(false);
        this.message.set(`Your application for ${this.companyName} has been submitted for review.`);
        this.companyName = '';
        this.companyDomain = '';
      },
      error: (err) => {
        this.saving.set(false);
        this.message.set(err.error?.message ?? 'We could not submit your application. Please try again.');
      },
    });
  }
}
