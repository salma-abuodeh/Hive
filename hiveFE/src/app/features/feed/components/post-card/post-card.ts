import { DatePipe } from '@angular/common';
import { Component, HostListener, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Team } from '../../../company/models/team.models';
import { Post, REACTION_OPTIONS, ReactionType, VisibilityType } from '../../models/post.models';

export interface PostEditPayload {
  content: string;
  visibilityType: VisibilityType;
  teamId: number | null;
}

@Component({
  selector: 'app-post-card',
  imports: [RouterLink, DatePipe, FormsModule],
  templateUrl: './post-card.html',
  styleUrl: './post-card.css',
})
export class PostCard {
  readonly post = input.required<Post>();
  readonly showActions = input(true);
  readonly editing = input(false);
  readonly saving = input(false);
  readonly teams = input<Team[]>([]);
  readonly companyLabel = input('your company');

  readonly react = output<{ post: Post; type: ReactionType }>();
  readonly saveToggle = output<Post>();
  readonly share = output<Post>();
  readonly edit = output<Post>();
  readonly remove = output<Post>();
  readonly saveEdit = output<PostEditPayload>();
  readonly cancelEdit = output<void>();

  readonly pickerOpen = signal(false);
  readonly reactionOptions = REACTION_OPTIONS;

  editContent = '';
  editVisibility: VisibilityType = 'COMPANY';
  editTeamId: number | null = null;
  private wasEditing = false;

  constructor() {
    effect(() => {
      const isEditing = this.editing();
      const post = this.post();
      const teams = this.teams();

      if (isEditing && !this.wasEditing) {
        this.editContent = post.content;
        this.editVisibility = post.visibilityType;
        this.editTeamId = post.teamId ?? null;
        if (this.editVisibility === 'TEAM' && this.editTeamId == null && teams.length) {
          this.editTeamId = teams[0].id;
        }
      }
      this.wasEditing = isEditing;
    });
  }

  initials(post: Post): string {
    return `${post.authorFirstName?.charAt(0) ?? ''}${post.authorLastName?.charAt(0) ?? ''}`.toUpperCase();
  }

  authorName(post: Post): string {
    return `${post.authorFirstName} ${post.authorLastName}`.trim();
  }

  visibilityLabel(post: Post): string {
    if (post.visibilityType === 'TEAM') {
      return post.teamName ? `Team · ${post.teamName}` : 'Team';
    }
    return 'Everyone at company';
  }

  currentEmoji(post: Post): string {
    const type = post.myReaction ?? null;
    if (!type) return '👍';
    return this.reactionOptions.find((o) => o.type === type)?.emoji ?? '👍';
  }

  currentLabel(post: Post): string {
    const type = post.myReaction ?? null;
    if (!type) return 'React';
    return this.reactionOptions.find((o) => o.type === type)?.label ?? 'React';
  }

  onVisibilityChange(): void {
    if (this.editVisibility !== 'TEAM') {
      this.editTeamId = null;
    } else if (this.teams().length && this.editTeamId == null) {
      this.editTeamId = this.teams()[0].id;
    }
  }

  submitEdit(): void {
    const text = this.editContent.trim();
    if (!text || this.saving()) return;
    if (this.editVisibility === 'TEAM' && this.editTeamId == null) return;

    this.saveEdit.emit({
      content: text,
      visibilityType: this.editVisibility,
      teamId: this.editVisibility === 'TEAM' ? this.editTeamId : null,
    });
  }

  togglePicker(event: Event): void {
    event.stopPropagation();
    this.pickerOpen.update((open) => !open);
  }

  choose(type: ReactionType, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.pickerOpen.set(false);
    this.react.emit({ post: this.post(), type });
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    if (this.pickerOpen()) {
      this.pickerOpen.set(false);
    }
  }
}
