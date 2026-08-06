import { Component, OnInit, inject, signal } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { OnboardingService, RequestItem } from '../../../../core/services/onboarding.service';

@Component({ selector: 'app-management-home', templateUrl: './management-home.html', styleUrl: './management-home.css' })
export class ManagementHome implements OnInit {
  readonly auth = inject(AuthService);
  private readonly onboarding = inject(OnboardingService);
  readonly companyApplications = signal<RequestItem[]>([]);
  readonly membershipRequests = signal<RequestItem[]>([]);
  readonly message = signal('');
  readonly loading = signal(true);

  get isPlatformAdmin(): boolean { return this.auth.isPlatformAdmin(); }
  get isManager(): boolean { return this.auth.isManager(); }
  ngOnInit(): void { this.loadRequests(); }

  loadRequests(): void {
    this.loading.set(true);
    if (this.isPlatformAdmin) {
      this.onboarding.pendingCompanyApplications().subscribe({ next: items => { this.companyApplications.set(items); this.loading.set(false); }, error: () => { this.message.set('Could not load company applications.'); this.loading.set(false); } });
      return;
    }
    this.onboarding.pendingMembershipRequests().subscribe({ next: items => { this.membershipRequests.set(items); this.loading.set(false); }, error: () => { this.message.set('Could not load join requests.'); this.loading.set(false); } });
  }

  approveCompany(id: number): void { this.onboarding.approveCompany(id).subscribe({ next: () => { this.message.set('Company application approved.'); this.loadRequests(); }, error: err => this.message.set(err.error?.message ?? 'Could not approve the application.') }); }
  rejectCompany(id: number): void { const reason = typeof window === 'undefined' ? '' : window.prompt('Reason (optional):') ?? ''; this.onboarding.rejectCompany(id, reason).subscribe({ next: () => { this.message.set('Company application rejected.'); this.loadRequests(); }, error: err => this.message.set(err.error?.message ?? 'Could not reject the application.') }); }
  approveMember(id: number, role: 'Employee' | 'Manager'): void { this.onboarding.approveMembership(id, role).subscribe({ next: () => { this.message.set('Member approved.'); this.loadRequests(); }, error: err => this.message.set(err.error?.message ?? 'Could not approve the join request.') }); }
  rejectMember(id: number): void { const reason = typeof window === 'undefined' ? '' : window.prompt('Reason (optional):') ?? ''; this.onboarding.rejectMembership(id, reason).subscribe({ next: () => { this.message.set('Join request rejected.'); this.loadRequests(); }, error: err => this.message.set(err.error?.message ?? 'Could not reject the join request.') }); }
}
