import { DatePipe } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { EventService } from '../../../../core/services/event.service';
import { AuthImage } from '../../../../shared/components/auth-image/auth-image';
import { Icon } from '../../../../shared/components/icon/icon';
import { EventResponse, RsvpStatus } from '../../models/event.models';

@Component({
  selector: 'app-event-feed-card',
  imports: [DatePipe, AuthImage, Icon],
  templateUrl: './event-feed-card.html',
  styleUrl: './event-feed-card.css',
})
export class EventFeedCard {
  private readonly eventsApi = inject(EventService);
  private readonly auth = inject(AuthService);

  event = input.required<EventResponse>();

  /** Parent (FeedHome) swaps its copy of the event in after an RSVP or delete. */
  updated = output<EventResponse>();
  deleted = output<EventResponse>();

  responding = signal<RsvpStatus | null>(null);

  initials(): string {
    const parts = this.event().createdByName.trim().split(/\s+/);
    return `${parts[0]?.[0] ?? ''}${parts[1]?.[0] ?? ''}`.toUpperCase() || '?';
  }

  isInvitee(): boolean {
    return this.event().myRsvpStatus != null;
  }

  isPast(): boolean {
    return new Date(this.event().endTime).getTime() < Date.now();
  }

  canDelete(): boolean {
    const me = this.auth.getUser();
    if (!me) return false;
    return this.event().createdByUserId === me.id || this.auth.hasPermission('EVENT_DELETE');
  }

  respond(status: RsvpStatus): void {
    if (this.responding()) return;
    this.responding.set(status);
    this.eventsApi.rsvp(this.event().id, { status }).subscribe({
      next: (rsvp) => {
        this.updated.emit({ ...this.event(), myRsvpStatus: rsvp.status });
        this.responding.set(null);
      },
      error: () => this.responding.set(null),
    });
  }

  remove(): void {
    if (!confirm('Delete this event?')) return;
    this.eventsApi.delete(this.event().id).subscribe({
      next: () => this.deleted.emit(this.event()),
    });
  }
}
