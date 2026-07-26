import { DatePipe } from '@angular/common';
import { Component, HostListener, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Post, REACTION_OPTIONS, ReactionType } from '../../models/post.models';

@Component({
  selector: 'app-post-card',
  imports: [RouterLink, DatePipe],
  templateUrl: './post-card.html',
  styleUrl: './post-card.css',
})
export class PostCard {
  readonly post = input.required<Post>();
  readonly showActions = input(true);

  readonly react = output<{ post: Post; type: ReactionType }>();
  readonly saveToggle = output<Post>();
  readonly share = output<Post>();
  readonly edit = output<Post>();
  readonly remove = output<Post>();

  readonly pickerOpen = signal(false);
  readonly reactionOptions = REACTION_OPTIONS;

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
