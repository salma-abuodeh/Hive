import { DatePipe } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { PollService } from '../../../../core/services/poll.service';
import { Icon } from '../../../../shared/components/icon/icon';
import { PollOptionResponse, PollResponse } from '../../models/poll.models';

@Component({
  selector: 'app-poll-feed-card',
  imports: [DatePipe, Icon],
  templateUrl: './poll-feed-card.html',
  styleUrl: './poll-feed-card.css',
})
export class PollFeedCard {
  private readonly pollsApi = inject(PollService);
  private readonly auth = inject(AuthService);

  poll = input.required<PollResponse>();

  /** Parent (FeedHome) swaps its copy of the poll in on every vote/unvote/delete. */
  updated = output<PollResponse>();
  deleted = output<PollResponse>();

  voting = signal(false);

  initials(): string {
    const parts = this.poll().createdByName.trim().split(/\s+/);
    return `${parts[0]?.[0] ?? ''}${parts[1]?.[0] ?? ''}`.toUpperCase() || '?';
  }

  isClosed(): boolean {
    const closesAt = this.poll().closesAt;
    return !!closesAt && new Date(closesAt).getTime() < Date.now();
  }

  hasVoted(): boolean {
    return this.poll().options.some((o) => o.votedByMe);
  }

  percentage(count: number): number {
    const total = this.poll().totalVotes;
    return total > 0 ? Math.round((count / total) * 100) : 0;
  }

  canDelete(): boolean {
    const me = this.auth.getUser();
    if (!me) return false;
    return this.poll().createdByUserId === me.id || this.auth.hasPermission('POLL_DELETE');
  }

  /** Facebook-style instant voting: tapping an option votes (or updates the vote) immediately. */
  pickOption(option: PollOptionResponse): void {
    if (this.isClosed() || this.voting()) return;
    const poll = this.poll();

    let optionIds: number[];
    if (poll.allowMultiple) {
      const current = new Set(poll.options.filter((o) => o.votedByMe).map((o) => o.id));
      if (current.has(option.id)) {
        current.delete(option.id);
      } else {
        current.add(option.id);
      }
      optionIds = [...current];
    } else {
      optionIds = poll.options.find((o) => o.id === option.id)?.votedByMe ? [] : [option.id];
    }

    this.voting.set(true);
    const request = optionIds.length
      ? this.pollsApi.vote(poll.id, { optionIds })
      : this.pollsApi.unvote(poll.id);

    request.subscribe({
      next: (result) => {
        this.updated.emit(result);
        this.voting.set(false);
      },
      error: () => this.voting.set(false),
    });
  }

  clearVote(): void {
    if (this.voting()) return;
    this.voting.set(true);
    this.pollsApi.unvote(this.poll().id).subscribe({
      next: (result) => {
        this.updated.emit(result);
        this.voting.set(false);
      },
      error: () => this.voting.set(false),
    });
  }

  remove(): void {
    if (!confirm('Delete this poll?')) return;
    this.pollsApi.delete(this.poll().id).subscribe({
      next: () => this.deleted.emit(this.poll()),
    });
  }
}
