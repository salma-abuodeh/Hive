import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Team } from '../../../company/models/team.models';
import { Post, ReactionType, VisibilityType } from '../../models/post.models';

@Component({
  selector: 'app-post-card',
  imports: [RouterLink, DatePipe, FormsModule],
  templateUrl: './post-card.html',
  styleUrl: './post-card.css',
})
export class PostCard {
  post = input.required<Post>();
  editing = input(false);
  saving = input(false);
  teams = input<Team[]>([]);
  companyName = input('your company');

  react = output<{ post: Post; type: ReactionType }>();
  saveToggle = output<Post>();
  share = output<Post>();
  edit = output<Post>();
  remove = output<Post>();
  saveEdit = output<{ content: string; visibilityType: VisibilityType; teamId: number | null }>();
  cancelEdit = output<void>();

  editContent = '';
  editVisibility: VisibilityType = 'COMPANY';
  editTeamId: number | null = null;

  reactions: { type: ReactionType; emoji: string }[] = [
    { type: 'LIKE', emoji: '👍' },
    { type: 'LOVE', emoji: '❤️' },
    { type: 'LAUGHING', emoji: '😂' },
    { type: 'SAD', emoji: '😢' },
    { type: 'ANGRY', emoji: '😡' },
  ];

  startEdit(): void {
    const p = this.post();
    this.editContent = p.content;
    this.editVisibility = p.visibilityType;
    this.editTeamId = p.teamId ?? null;
    this.edit.emit(p);
  }

  submitEdit(): void {
    this.saveEdit.emit({
      content: this.editContent.trim(),
      visibilityType: this.editVisibility,
      teamId: this.editVisibility === 'TEAM' ? this.editTeamId : null,
    });
  }
}
