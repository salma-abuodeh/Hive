import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Post } from '../../models/post.models';

@Component({
  selector: 'app-post-card',
  imports: [RouterLink, DatePipe],
  templateUrl: './post-card.html',
  styleUrl: './post-card.css',
})
export class PostCard {
  readonly post = input.required<Post>();
  readonly showActions = input(true);

  readonly likeToggle = output<Post>();
  readonly saveToggle = output<Post>();
  readonly share = output<Post>();
  readonly edit = output<Post>();
  readonly remove = output<Post>();

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
}
