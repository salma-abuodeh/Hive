import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { CompanyService } from '../../../../core/services/company.service';
import { OnboardingService, RequestItem } from '../../../../core/services/onboarding.service';

@Component({
  selector: 'app-dashboard-home',
  imports: [FormsModule],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.css',
})
export class DashboardHome implements OnInit {
  readonly auth = inject(AuthService);
  private readonly companies = inject(CompanyService);
  private readonly onboarding = inject(OnboardingService);
  readonly showCompanyForm = signal(false);
  readonly saving = signal(false);
  readonly message = signal('');
  companyName = '';
  companyType: 'COMPANY' | 'SCHOOL' = 'COMPANY';
  companyDomain = '';
  companyApplications = signal<RequestItem[]>([]);
  membershipRequests = signal<RequestItem[]>([]);
  joinableCompanies = signal<{ id: number; name: string; domain?: string }[]>([]);
  companySearch = signal('');

  get firstName(): string {
    return this.auth.getUser()?.firstName || 'there';
  }

  get isPlatformAdmin(): boolean {
    return this.auth.isPlatformAdmin();
  }

  get isManager(): boolean { return this.auth.isManager(); }

  filteredCompanies(): { id: number; name: string; domain?: string }[] {
    const search = this.companySearch().trim().toLowerCase();
    if (!search) return this.joinableCompanies();
    return this.joinableCompanies().filter((company) =>
      company.name.toLowerCase().includes(search) || company.domain?.toLowerCase().includes(search)
    );
  }

  ngOnInit(): void { this.loadRequests(); }

  loadRequests(): void {
    if (this.isPlatformAdmin) return;
    this.onboarding.myCompanyApplications().subscribe({ next: items => this.companyApplications.set(items) });
    this.onboarding.myMembershipRequests().subscribe({ next: items => this.membershipRequests.set(items) });
    this.onboarding.joinableCompanies().subscribe({ next: items => this.joinableCompanies.set(items) });
  }

  requestToJoin(companyId: number): void { this.onboarding.requestMembership(companyId).subscribe({ next: () => { this.message.set('Join request submitted.'); this.loadRequests(); }, error: err => this.message.set(err.error?.message ?? 'Could not submit join request.') }); }

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
        this.loadRequests();
      },
      error: (err) => {
        this.saving.set(false);
        this.message.set(err.error?.message ?? 'We could not submit your application. Please try again.');
      },
    });
  }
}
