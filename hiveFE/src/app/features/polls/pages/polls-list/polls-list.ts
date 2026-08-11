import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { PollService } from '../../../../core/services/poll.service';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { Team } from '../../../company/models/team.models';
import { PollRequest, PollResponse, PollVisibility } from '../../models/poll.models';

@Component({
  selector: 'app-polls-list',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './polls-list.html',
  styleUrl: './polls-list.css'
})
export class PollsList implements OnInit {
  private readonly pollService = inject(PollService);
  private readonly teamService = inject(TeamService);
  readonly auth = inject(AuthService);

  polls = signal<PollResponse[]>([]);
  myTeams = signal<Team[]>([]);
  selectedPoll = signal<PollResponse | null>(null);
  loading = signal(false);
  error = signal('');

  showForm = signal(false);
  saving = signal(false);
  formError = signal('');
  editingId: number | null = null;

  question = '';
  description = '';
  allowMultiple = false;
  closesAtDate = '';
  closesAtTime = '';
  visibility: PollVisibility = 'COMPANY';
  teamId: number | null = null;
  optionTexts: string[] = ['', ''];

  selectedOptionIds = signal<Set<number>>(new Set());
  voting = signal(false);

  ngOnInit(): void {
    this.teamService.listMine().subscribe({
      next: (teams) => this.myTeams.set(teams),
      error: () => this.myTeams.set([]),
    });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.pollService.list(0, 50).subscribe({
      next: (page) => {
        const list = page.content ?? [];
        this.polls.set(list);
        const current = this.selectedPoll();
        if (current) {
          const refreshed = list.find(p => p.id === current.id);
          this.selectPoll(refreshed ?? (list[0] ?? null));
        } else if (list.length) {
          this.selectPoll(list[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Failed to load polls');
      }
    });
  }

  selectPoll(poll: PollResponse | null): void {
    this.selectedPoll.set(poll);
    const votedIds = poll?.options.filter(o => o.votedByMe).map(o => o.id) ?? [];
    this.selectedOptionIds.set(new Set(votedIds));
  }

  canManage(poll: PollResponse): boolean {
    const me = this.auth.getUser();
    if (!me) return false;
    return poll.createdByUserId === me.id || this.auth.hasPermission('POLL_UPDATE');
  }

  canDelete(poll: PollResponse): boolean {
    const me = this.auth.getUser();
    if (!me) return false;
    return poll.createdByUserId === me.id || this.auth.hasPermission('POLL_DELETE');
  }

  hasVoted(poll: PollResponse): boolean {
    return poll.options.some(o => o.votedByMe);
  }

  isClosed(poll: PollResponse): boolean {
    return !!poll.closesAt && new Date(poll.closesAt) < new Date();
  }

  optionPercentage(optionVotes: number, totalVotes: number): number {
    if (totalVotes === 0) return 0;
    return Math.round((optionVotes / totalVotes) * 100);
  }

  // ---------- Create / Edit ----------

  openCreateForm(): void {
    this.editingId = null;
    this.question = '';
    this.description = '';
    this.allowMultiple = false;
    this.closesAtDate = '';
    this.closesAtTime = '';
    this.visibility = 'COMPANY';
    this.teamId = null;
    this.optionTexts = ['', ''];
    this.formError.set('');
    this.showForm.set(true);
  }

  openEditForm(poll: PollResponse): void {
    this.editingId = poll.id;
    this.question = poll.question;
    this.description = poll.description ?? '';
    this.allowMultiple = poll.allowMultiple;
    this.visibility = poll.visibility;
    this.teamId = poll.teamId;
    this.optionTexts = poll.options.map(o => o.text);
    if (poll.closesAt) {
      const d = new Date(poll.closesAt);
      this.closesAtDate = this.formatDateInput(d);
      this.closesAtTime = this.formatTimeInput(d);
    } else {
      this.closesAtDate = '';
      this.closesAtTime = '';
    }
    this.formError.set('');
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
  }

  addOption(): void {
    if (this.optionTexts.length >= 10) return;
    this.optionTexts.push('');
  }

  removeOption(index: number): void {
    if (this.optionTexts.length <= 2) return;
    this.optionTexts.splice(index, 1);
  }

  onVisibilityChange(): void {
    if (this.visibility !== 'TEAM') {
      this.teamId = null;
    } else if (this.myTeams().length && this.teamId == null) {
      this.teamId = this.myTeams()[0].id;
    }
  }

  submit(): void {
    const trimmedOptions = this.optionTexts.map(t => t.trim()).filter(t => t.length > 0);

    if (!this.question.trim()) {
      this.formError.set('Question is required');
      return;
    }
    if (trimmedOptions.length < 2) {
      this.formError.set('A poll needs at least 2 options');
      return;
    }
    if (this.visibility === 'TEAM' && this.teamId == null) {
      this.formError.set('Choose a team for team visibility');
      return;
    }

    const payload: PollRequest = {
      question: this.question.trim(),
      description: this.description.trim() || undefined,
      allowMultiple: this.allowMultiple,
      closesAt: this.closesAtDate && this.closesAtTime
        ? `${this.closesAtDate}T${this.closesAtTime}:00`
        : undefined,
      teamId: this.visibility === 'TEAM' ? this.teamId : null,
      visibility: this.visibility,
      options: trimmedOptions.map(text => ({ text }))
    };

    this.saving.set(true);
    this.formError.set('');

    const request = this.editingId != null
      ? this.pollService.update(this.editingId, payload)
      : this.pollService.create(payload);

    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.showForm.set(false);
        this.selectPoll(saved);
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(err.error?.message ?? 'Save failed');
      }
    });
  }

  remove(poll: PollResponse): void {
    if (!confirm(`Delete "${poll.question}"?`)) return;
    this.pollService.delete(poll.id).subscribe({
      next: () => {
        this.selectedPoll.set(null);
        this.load();
      },
      error: (err) => this.error.set(err.error?.message ?? 'Delete failed')
    });
  }

  // ---------- Voting ----------

  toggleOption(optionId: number): void {
    const poll = this.selectedPoll();
    if (!poll) return;

    const current = new Set(this.selectedOptionIds());

    if (poll.allowMultiple) {
      if (current.has(optionId)) {
        current.delete(optionId);
      } else {
        current.add(optionId);
      }
    } else {
      current.clear();
      current.add(optionId);
    }

    this.selectedOptionIds.set(current);
  }

  submitVote(): void {
    const poll = this.selectedPoll();
    const ids = Array.from(this.selectedOptionIds());
    if (!poll || ids.length === 0) return;

    this.voting.set(true);
    this.pollService.vote(poll.id, { optionIds: ids }).subscribe({
      next: (updated) => {
        this.voting.set(false);
        this.selectPoll(updated);
        this.load();
      },
      error: (err) => {
        this.voting.set(false);
        this.error.set(err.error?.message ?? 'Failed to submit vote');
      }
    });
  }

  clearVote(): void {
    const poll = this.selectedPoll();
    if (!poll) return;

    this.voting.set(true);
    this.pollService.unvote(poll.id).subscribe({
      next: (updated) => {
        this.voting.set(false);
        this.selectPoll(updated);
        this.load();
      },
      error: (err) => {
        this.voting.set(false);
        this.error.set(err.error?.message ?? 'Failed to remove your vote');
      }
    });
  }

  private formatDateInput(date: Date): string {
    return [
      date.getFullYear(),
      this.padDatePart(date.getMonth() + 1),
      this.padDatePart(date.getDate())
    ].join('-');
  }

  private formatTimeInput(date: Date): string {
    return `${this.padDatePart(date.getHours())}:${this.padDatePart(date.getMinutes())}`;
  }

  private padDatePart(value: number): string {
    return String(value).padStart(2, '0');
  }
}
