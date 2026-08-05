import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, SlicePipe } from '@angular/common';
import { EventService } from '../../../../core/services/event.service';
import { UserService } from '../../../../core/services/user.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UserResponse } from '../../../users/models/user.models';
import {
  EventRequest,
  EventResponse,
  EventRsvpResponse,
  EventVisibility,
  RsvpStatus
} from '../../models/event.models';

@Component({
  selector: 'app-events-list',
  standalone: true,
  imports: [FormsModule, DatePipe, SlicePipe],
  templateUrl: './events-list.html',
  styleUrl: './events-list.css'
})
export class EventsList implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly userService = inject(UserService);
  readonly auth = inject(AuthService);

  events = signal<EventResponse[]>([]);
  selectedEvent = signal<EventResponse | null>(null);
  loading = signal(false);
  error = signal('');

  showForm = signal(false);
  saving = signal(false);
  formError = signal('');
  editingId: number | null = null;

  title = '';
  description = '';
  location = '';
  date = '';
  time = '';
  endTime = '';
  visibility: EventVisibility = 'COMPANY';
  photoPreview = signal<string | null>(null);

  // Invite modal
  showInviteModal = signal(false);
  inviteLoading = signal(false);
  inviteError = signal('');
  invitations = signal<EventRsvpResponse[]>([]);
  companyUsers = signal<UserResponse[]>([]);
  selectedUserIds = signal<Set<number>>(new Set());
  sendingInvites = signal(false);

  // RSVP
  respondingStatus = signal<RsvpStatus | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.eventService.list(0, 50).subscribe({
      next: (page) => {
        const list = page.content ?? [];
        this.events.set(list);
        const current = this.selectedEvent();
        if (current) {
          const refreshed = list.find(e => e.id === current.id);
          this.selectedEvent.set(refreshed ?? (list[0] ?? null));
        } else if (list.length) {
          this.selectedEvent.set(list[0]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Failed to load events');
      }
    });
  }

  select(event: EventResponse): void {
    this.selectedEvent.set(event);
  }

  canManage(event: EventResponse): boolean {
    const me = this.auth.getUser();
    if (!me) return false;
    return event.createdByUserId === me.id || this.auth.hasPermission('EVENT_UPDATE');
  }

  canDelete(event: EventResponse): boolean {
    const me = this.auth.getUser();
    if (!me) return false;
    return event.createdByUserId === me.id || this.auth.hasPermission('EVENT_DELETE');
  }

  canInvite(event: EventResponse): boolean {
    const me = this.auth.getUser();
    if (!me) return false;
    return event.createdByUserId === me.id || this.auth.hasPermission('EVENT_INVITE');
  }

  isInvitee(event: EventResponse): boolean {
    return event.myRsvpStatus != null;
  }

  // ---------- Create / Edit ----------

  onPhotoSelected(input: HTMLInputElement): void {
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => this.photoPreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  clearPhoto(): void {
    this.photoPreview.set(null);
  }

  openCreateForm(): void {
    this.editingId = null;
    this.title = '';
    this.description = '';
    this.location = '';
    this.date = '';
    this.time = '';
    this.endTime = '';
    this.visibility = 'COMPANY';
    this.photoPreview.set(null);
    this.formError.set('');
    this.showForm.set(true);
  }

  openEditForm(event: EventResponse): void {
    this.editingId = event.id;
    this.title = event.title;
    this.description = event.description ?? '';
    this.location = event.location ?? '';
    const start = new Date(event.startTime);
    const end = new Date(event.endTime);
    this.date = start.toISOString().slice(0, 10);
    this.time = start.toISOString().slice(11, 16);
    this.endTime = end.toISOString().slice(11, 16);
    this.visibility = event.visibility;
    this.photoPreview.set(event.imageUrl ?? null);
    this.formError.set('');
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
  }

  submit(): void {
    if (!this.title.trim() || !this.date || !this.time || !this.endTime) {
      this.formError.set('Title, date, start time, and end time are required');
      return;
    }

    const payload: EventRequest = {
      title: this.title.trim(),
      description: this.description.trim() || undefined,
      location: this.location.trim() || undefined,
      startTime: `${this.date}T${this.time}:00`,
      endTime: `${this.date}T${this.endTime}:00`,
      visibility: this.visibility
    };

    this.saving.set(true);
    this.formError.set('');

    const request = this.editingId != null
      ? this.eventService.update(this.editingId, payload)
      : this.eventService.create(payload);

    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.showForm.set(false);
        this.selectedEvent.set(saved);
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(err.error?.message ?? 'Save failed');
      }
    });
  }

  remove(event: EventResponse): void {
    if (!confirm(`Delete "${event.title}"?`)) return;
    this.eventService.delete(event.id).subscribe({
      next: () => {
        this.selectedEvent.set(null);
        this.load();
      },
      error: (err) => this.error.set(err.error?.message ?? 'Delete failed')
    });
  }

  // ---------- Invitations ----------

  openInviteModal(event: EventResponse): void {
    this.inviteError.set('');
    this.selectedUserIds.set(new Set());
    this.showInviteModal.set(true);
    this.inviteLoading.set(true);

    this.eventService.listInvitations(event.id).subscribe({
      next: (invites) => {
        this.invitations.set(invites);
        this.loadCompanyUsersForInvite(event, invites);
      },
      error: (err) => {
        this.inviteLoading.set(false);
        this.inviteError.set(err.error?.message ?? 'Failed to load invitations');
      }
    });
  }

  private loadCompanyUsersForInvite(event: EventResponse, existingInvites: EventRsvpResponse[]): void {
    this.userService.listCompany(0, 200).subscribe({
      next: (page) => {
        const invitedIds = new Set(existingInvites.map(i => i.userId));
        const available = (page.content ?? []).filter(
          u => u.id !== event.createdByUserId && !invitedIds.has(u.id)
        );
        this.companyUsers.set(available);
        this.inviteLoading.set(false);
      },
      error: (err) => {
        this.inviteLoading.set(false);
        this.inviteError.set(err.error?.message ?? 'Failed to load company members');
      }
    });
  }

  closeInviteModal(): void {
    this.showInviteModal.set(false);
  }

  toggleUserSelection(userId: number): void {
    const current = new Set(this.selectedUserIds());
    if (current.has(userId)) {
      current.delete(userId);
    } else {
      current.add(userId);
    }
    this.selectedUserIds.set(current);
  }

  sendInvites(): void {
    const event = this.selectedEvent();
    const ids = Array.from(this.selectedUserIds());
    if (!event || ids.length === 0) return;

    this.sendingInvites.set(true);
    this.inviteError.set('');

    this.eventService.invite(event.id, { userIds: ids }).subscribe({
      next: () => {
        this.sendingInvites.set(false);
        this.selectedUserIds.set(new Set());
        this.openInviteModal(event); // refresh both lists
      },
      error: (err) => {
        this.sendingInvites.set(false);
        this.inviteError.set(err.error?.message ?? 'Failed to send invitations');
      }
    });
  }

  removeInvite(userId: number): void {
    const event = this.selectedEvent();
    if (!event) return;
    this.eventService.removeInvitation(event.id, userId).subscribe({
      next: () => this.openInviteModal(event),
      error: (err) => this.inviteError.set(err.error?.message ?? 'Failed to remove invitation')
    });
  }

  // ---------- RSVP ----------

  respond(status: RsvpStatus): void {
    const event = this.selectedEvent();
    if (!event) return;
    this.respondingStatus.set(status);
    this.eventService.rsvp(event.id, { status }).subscribe({
      next: () => {
        this.respondingStatus.set(null);
        this.load();
      },
      error: (err) => {
        this.respondingStatus.set(null);
        this.error.set(err.error?.message ?? 'Failed to update your RSVP');
      }
    });
  }
}