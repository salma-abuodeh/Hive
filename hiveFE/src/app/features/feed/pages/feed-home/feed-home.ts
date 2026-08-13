import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { AttachmentService } from '../../../../core/services/attachment.service';
import { PollService } from '../../../../core/services/poll.service';
import { EventService } from '../../../../core/services/event.service';
import { Team } from '../../../company/models/team.models';
import { PostCard } from '../../components/post-card/post-card';
import { PollFeedCard } from '../../../polls/components/poll-feed-card/poll-feed-card';
import { EventFeedCard } from '../../../events/components/event-feed-card/event-feed-card';
import { Icon } from '../../../../shared/components/icon/icon';
import { AttachmentResponse } from '../../../../shared/models/attachment.models';
import { Post, ReactionType, VisibilityType } from '../../models/post.models';
import { PostService } from '../../services/post.service';
import { PollRequest, PollResponse } from '../../../polls/models/poll.models';
import { EventRequest, EventResponse } from '../../../events/models/event.models';

export type PostType = 'POST' | 'POLL' | 'EVENT';

export type FeedItem =
  | { kind: 'POST'; key: string; createdAt: string; post: Post }
  | { kind: 'POLL'; key: string; createdAt: string; poll: PollResponse }
  | { kind: 'EVENT'; key: string; createdAt: string; event: EventResponse };

@Component({
  selector: 'app-feed-home',
  imports: [FormsModule, PostCard, PollFeedCard, EventFeedCard, Icon],
  templateUrl: './feed-home.html',
  styleUrl: './feed-home.css',
})
export class FeedHome implements OnInit {
  private postsApi = inject(PostService);
  private pollsApi = inject(PollService);
  private eventsApi = inject(EventService);
  private teamsApi = inject(TeamService);
  private attachmentService = inject(AttachmentService);
  auth = inject(AuthService);

  /** Unified, date-sorted feed of posts + polls + events. */
  feedItems = signal<FeedItem[]>([]);
  myTeams = signal<Team[]>([]);
  loading = signal(false);
  busy = signal(false);
  error = signal('');
  toast = signal('');
  editingId: number | null = null;

  // Independent pagination per source; merged client-side into one feed.
  private postsPage = -1;
  private pollsPage = -1;
  private eventsPage = -1;
  private postsLastPage = false;
  private pollsLastPage = false;
  private eventsLastPage = false;

  /** Facebook-style "what do you want to post" picker in the composer. */
  postType: PostType = 'POST';

  // ---- Post fields ----
  content = '';
  visibilityType: VisibilityType = 'COMPANY';
  teamId: number | null = null;

  // ---- Poll fields ----
  pollQuestion = '';
  pollDescription = '';
  pollOptions: string[] = ['', ''];
  pollAllowMultiple = false;
  pollClosesAtDate = '';
  pollClosesAtTime = '';

  // ---- Event fields ----
  eventTitle = '';
  eventDescription = '';
  eventLocation = '';
  eventStartDate = '';
  eventStartTime = '';
  eventEndDate = '';
  eventEndTime = '';

  // Files picked before the post exists — uploaded one-by-one right after publish()
  // succeeds and we have a real postId to attach them to. (Post type only.)
  stagedFiles: File[] = [];
  stagedPreviews = signal<{ file: File; previewUrl: string | null }[]>([]);
  composerError = signal('');

  ngOnInit(): void {
    const editPost = history.state?.['editPost'] as Post | undefined;
    if (editPost?.id) {
      this.editingId = editPost.id;
    }

    this.teamsApi.listMine().subscribe({
      next: (teams) => this.myTeams.set(teams),
      error: () => this.myTeams.set([]),
    });
    this.loadFeed(true);
  }

  companyName(): string {
    return this.auth.getUser()?.companies?.[0]?.name ?? 'your company';
  }

  initials(): string {
    const u = this.auth.getUser();
    if (!u) return '?';
    return `${u.firstName?.[0] ?? ''}${u.lastName?.[0] ?? ''}`.toUpperCase();
  }

  canCreatePoll(): boolean {
    return this.auth.hasPermission('POLL_CREATE');
  }

  // ---------- Composer: type picker ----------

  setPostType(type: PostType): void {
    this.postType = type;
    this.composerError.set('');
  }

  canPublish(): boolean {
    if (this.postType === 'POST') return !!this.content.trim();
    if (this.postType === 'POLL') {
      return !!this.pollQuestion.trim() && this.pollOptions.map((o) => o.trim()).filter(Boolean).length >= 2;
    }
    return !!this.eventTitle.trim() && !!this.eventStartDate && !!this.eventStartTime && !!this.eventEndDate && !!this.eventEndTime;
  }

  onVisibilityChange(): void {
    if (this.visibilityType !== 'TEAM') {
      this.teamId = null;
    } else if (this.myTeams().length && this.teamId == null) {
      this.teamId = this.myTeams()[0].id;
    }
  }

  // ---------- Composer: poll fields ----------

  addPollOption(): void {
    if (this.pollOptions.length < 10) this.pollOptions.push('');
  }

  removePollOption(index: number): void {
    if (this.pollOptions.length > 2) this.pollOptions.splice(index, 1);
  }

  // ---------- Composer: attachments (post only) ----------

  onComposerFileSelected(input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    if (this.stagedFiles.length >= 10) {
      this.composerError.set('A post can have at most 10 attachments');
      return;
    }

    const validationError = this.attachmentService.validate(file, 'POST');
    if (validationError) {
      this.composerError.set(validationError);
      return;
    }

    this.composerError.set('');
    this.stagedFiles.push(file);

    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = () => {
        this.stagedPreviews.update((list) => [...list, { file, previewUrl: reader.result as string }]);
      };
      reader.readAsDataURL(file);
    } else {
      this.stagedPreviews.update((list) => [...list, { file, previewUrl: null }]);
    }
  }

  removeStagedFile(file: File): void {
    this.stagedFiles = this.stagedFiles.filter((f) => f !== file);
    this.stagedPreviews.update((list) => list.filter((p) => p.file !== file));
  }

  private clearComposerStaging(): void {
    this.stagedFiles = [];
    this.stagedPreviews.set([]);
    this.composerError.set('');
  }

  // ---------- Feed loading (merges posts + polls + events by date) ----------

  allLoaded(): boolean {
    return this.postsLastPage && this.pollsLastPage && this.eventsLastPage;
  }

  loadFeed(reset = false): void {
    if (this.loading()) return;
    if (!reset && this.allLoaded()) return;

    if (reset) {
      this.postsPage = -1;
      this.pollsPage = -1;
      this.eventsPage = -1;
      this.postsLastPage = false;
      this.pollsLastPage = false;
      this.eventsLastPage = false;
    }

    const nextPostsPage = this.postsLastPage ? null : this.postsPage + 1;
    const nextPollsPage = this.pollsLastPage ? null : this.pollsPage + 1;
    const nextEventsPage = this.eventsLastPage ? null : this.eventsPage + 1;

    this.loading.set(true);
    this.error.set('');

    forkJoin({
      posts: nextPostsPage != null ? this.postsApi.listFeed(nextPostsPage) : of(null),
      polls: nextPollsPage != null ? this.pollsApi.list(nextPollsPage) : of(null),
      events: nextEventsPage != null ? this.eventsApi.list(nextEventsPage) : of(null),
    }).subscribe({
      next: ({ posts, polls, events }) => {
        const items: FeedItem[] = reset ? [] : [...this.feedItems()];

        if (posts) {
          this.postsPage = posts.number;
          this.postsLastPage = posts.last;
          items.push(...posts.content.map((post) => this.toFeedItem('POST', post)));
        }
        if (polls) {
          this.pollsPage = polls.number;
          this.pollsLastPage = polls.last;
          items.push(...polls.content.map((poll) => this.toFeedItem('POLL', poll)));
        }
        if (events) {
          this.eventsPage = events.number;
          this.eventsLastPage = events.last;
          items.push(...events.content.map((event) => this.toFeedItem('EVENT', event)));
        }

        items.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.feedItems.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Could not load feed');
        this.loading.set(false);
      },
    });
  }

  private toFeedItem(kind: 'POST', data: Post): FeedItem;
  private toFeedItem(kind: 'POLL', data: PollResponse): FeedItem;
  private toFeedItem(kind: 'EVENT', data: EventResponse): FeedItem;
  private toFeedItem(kind: PostType, data: Post | PollResponse | EventResponse): FeedItem {
    const key = `${kind}-${data.id}`;
    if (kind === 'POST') {
      return { kind: 'POST', key, createdAt: (data as Post).createdAt, post: data as Post };
    }
    if (kind === 'POLL') {
      return { kind: 'POLL', key, createdAt: (data as PollResponse).createdAt, poll: data as PollResponse };
    }
    return { kind: 'EVENT', key, createdAt: (data as EventResponse).createdAt, event: data as EventResponse };
  }

  private combineDateTime(date: string, time: string): string | undefined {
    if (!date) return undefined;
    return new Date(`${date}T${time || '00:00'}`).toISOString();
  }

  // ---------- Publish ----------

  publish(): void {
    if (this.busy() || !this.canPublish()) return;
    if (this.postType === 'POST') {
      this.publishPost();
    } else if (this.postType === 'POLL') {
      this.publishPoll();
    } else {
      this.publishEvent();
    }
  }

  private publishPost(): void {
    const text = this.content.trim();
    if (!text) return;

    this.busy.set(true);
    this.postsApi
      .create({
        content: text,
        visibilityType: this.visibilityType,
        teamId: this.visibilityType === 'TEAM' ? this.teamId : null,
      })
      .subscribe({
        next: (post) => this.afterPublishPost(post),
        error: (err) => {
          this.error.set(err.error?.message ?? 'Could not publish');
          this.busy.set(false);
        },
      });
  }

  private afterPublishPost(post: Post): void {
    if (this.stagedFiles.length === 0) {
      this.finishPublishPost(post);
      return;
    }
    this.uploadStagedFiles(post, [...this.stagedFiles], []);
  }

  private uploadStagedFiles(post: Post, remaining: File[], uploaded: AttachmentResponse[]): void {
    if (remaining.length === 0) {
      this.finishPublishPost({ ...post, attachments: uploaded });
      return;
    }
    const [next, ...rest] = remaining;
    this.attachmentService.uploadPostAttachment(post.id, next).subscribe({
      next: (attachment) => this.uploadStagedFiles(post, rest, [...uploaded, attachment]),
      error: (err) => {
        // Post is already published; surface the error but don't block on the rest of the batch.
        this.error.set(err.error?.message ?? 'Post published, but one attachment failed to upload');
        this.finishPublishPost({ ...post, attachments: uploaded });
      },
    });
  }

  private finishPublishPost(post: Post): void {
    this.feedItems.update((list) => [this.toFeedItem('POST', post), ...list]);
    this.content = '';
    this.visibilityType = 'COMPANY';
    this.teamId = null;
    this.clearComposerStaging();
    this.busy.set(false);
    this.toast.set('Published');
  }

  private publishPoll(): void {
    const question = this.pollQuestion.trim();
    const options = this.pollOptions.map((o) => o.trim()).filter(Boolean);
    if (!question || options.length < 2) {
      this.composerError.set('A poll needs a question and at least 2 options');
      return;
    }

    this.busy.set(true);
    const payload: PollRequest = {
      question,
      description: this.pollDescription.trim() || undefined,
      allowMultiple: this.pollAllowMultiple,
      closesAt: this.combineDateTime(this.pollClosesAtDate, this.pollClosesAtTime),
      visibility: this.visibilityType,
      teamId: this.visibilityType === 'TEAM' ? this.teamId : null,
      options: options.map((text) => ({ text })),
    };

    this.pollsApi.create(payload).subscribe({
      next: (poll) => this.finishPublishPoll(poll),
      error: (err) => {
        this.error.set(err.error?.message ?? 'Could not publish poll');
        this.busy.set(false);
      },
    });
  }

  private finishPublishPoll(poll: PollResponse): void {
    this.feedItems.update((list) => [this.toFeedItem('POLL', poll), ...list]);
    this.pollQuestion = '';
    this.pollDescription = '';
    this.pollOptions = ['', ''];
    this.pollAllowMultiple = false;
    this.pollClosesAtDate = '';
    this.pollClosesAtTime = '';
    this.visibilityType = 'COMPANY';
    this.teamId = null;
    this.composerError.set('');
    this.busy.set(false);
    this.toast.set('Poll published');
  }

  private publishEvent(): void {
    const title = this.eventTitle.trim();
    const start = this.combineDateTime(this.eventStartDate, this.eventStartTime);
    const end = this.combineDateTime(this.eventEndDate, this.eventEndTime);
    if (!title || !start || !end) {
      this.composerError.set('An event needs a title, start time, and end time');
      return;
    }

    this.busy.set(true);
    const payload: EventRequest = {
      title,
      description: this.eventDescription.trim() || undefined,
      location: this.eventLocation.trim() || undefined,
      startTime: start,
      endTime: end,
      visibility: this.visibilityType,
      teamId: this.visibilityType === 'TEAM' ? this.teamId : null,
    };

    this.eventsApi.create(payload).subscribe({
      next: (event) => this.finishPublishEvent(event),
      error: (err) => {
        this.error.set(err.error?.message ?? 'Could not publish event');
        this.busy.set(false);
      },
    });
  }

  private finishPublishEvent(event: EventResponse): void {
    this.feedItems.update((list) => [this.toFeedItem('EVENT', event), ...list]);
    this.eventTitle = '';
    this.eventDescription = '';
    this.eventLocation = '';
    this.eventStartDate = '';
    this.eventStartTime = '';
    this.eventEndDate = '';
    this.eventEndTime = '';
    this.visibilityType = 'COMPANY';
    this.teamId = null;
    this.composerError.set('');
    this.busy.set(false);
    this.toast.set('Event published');
  }

  // ---------- Post-card callbacks ----------

  private updatePostInFeed(updated: Post): void {
    this.feedItems.update((list) =>
      list.map((item) => (item.kind === 'POST' && item.post.id === updated.id ? { ...item, post: updated } : item))
    );
  }

  private removePostFromFeed(id: number): void {
    this.feedItems.update((list) => list.filter((item) => !(item.kind === 'POST' && item.post.id === id)));
  }

  onEdit(post: Post): void {
    this.editingId = post.id;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  onSaveEdit(data: { content: string; visibilityType: VisibilityType; teamId: number | null }): void {
    if (!this.editingId || this.busy()) return;
    this.busy.set(true);

    this.postsApi.update(this.editingId, data).subscribe({
      next: (post) => {
        this.updatePostInFeed(post);
        this.editingId = null;
        this.busy.set(false);
        this.toast.set('Updated');
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Could not update');
        this.busy.set(false);
      },
    });
  }

  onDelete(post: Post): void {
    if (!confirm('Delete this post?')) return;
    this.postsApi.delete(post.id).subscribe({
      next: () => {
        this.removePostFromFeed(post.id);
        if (this.editingId === post.id) this.editingId = null;
      },
      error: (err) => this.error.set(err.error?.message ?? 'Could not delete'),
    });
  }

  onReact(e: { post: Post; type: ReactionType }): void {
    this.postsApi.react(e.post.id, e.type).subscribe({
      next: (updated) => this.updatePostInFeed(updated),
      error: (err) => this.error.set(err.error?.message ?? 'Could not react'),
    });
  }

  onSaveToggle(post: Post): void {
    const call = post.savedByMe ? this.postsApi.unsave(post.id) : this.postsApi.save(post.id);
    call.subscribe({
      next: (updated) => this.updatePostInFeed(updated),
      error: (err) => this.error.set(err.error?.message ?? 'Could not save'),
    });
  }

  onShare(): void {
    // Copying + social share links are now handled entirely inside PostCard's share menu.
    this.toast.set('Link copied');
  }

  onAttachmentsChanged(e: { post: Post; attachments: AttachmentResponse[] }): void {
    this.updatePostInFeed({ ...e.post, attachments: e.attachments });
  }

  onCommentCountChanged(e: { post: Post; delta: number }): void {
    this.updatePostInFeed({ ...e.post, commentCount: e.post.commentCount + e.delta });
  }

  // ---------- Poll-card / Event-card callbacks ----------

  onPollUpdated(poll: PollResponse): void {
    this.feedItems.update((list) =>
      list.map((item) => (item.kind === 'POLL' && item.poll.id === poll.id ? { ...item, poll } : item))
    );
  }

  onPollDeleted(poll: PollResponse): void {
    this.feedItems.update((list) => list.filter((item) => !(item.kind === 'POLL' && item.poll.id === poll.id)));
    this.toast.set('Poll deleted');
  }

  onEventUpdated(event: EventResponse): void {
    this.feedItems.update((list) =>
      list.map((item) => (item.kind === 'EVENT' && item.event.id === event.id ? { ...item, event } : item))
    );
  }

  onEventDeleted(event: EventResponse): void {
    this.feedItems.update((list) => list.filter((item) => !(item.kind === 'EVENT' && item.event.id === event.id)));
    this.toast.set('Event deleted');
  }
}