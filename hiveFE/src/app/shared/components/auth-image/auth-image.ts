import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject, signal } from '@angular/core';
import { AttachmentService } from '../../../core/services/attachment.service';

/**
 * Renders an attachment (avatar, event cover, post/comment image, ...) that lives
 * behind an authenticated endpoint like /attachments/{id}. A plain <img src="...">
 * can't send the Authorization header, so this fetches the file as a blob and
 * swaps in an object URL. Falls back to projected content (e.g. initials) while
 * loading, on error, or when `src` is empty.
 *
 * Usage:
 *   <app-auth-image [src]="user.avatarUrl" alt="Avatar">
 *     <span class="hex avatar">{{ initials() }}</span>
 *   </app-auth-image>
 */
@Component({
  selector: 'app-auth-image',
  standalone: true,
  templateUrl: './auth-image.html',
  styleUrl: './auth-image.css',
})
export class AuthImage implements OnChanges, OnDestroy {
  private readonly attachmentService = inject(AttachmentService);

  @Input() src: string | null | undefined = null;
  @Input() alt = '';

  objectUrl = signal<string | null>(null);
  loading = signal(false);

  private currentObjectUrl: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if ('src' in changes) {
      this.load();
    }
  }

  ngOnDestroy(): void {
    this.revoke();
  }

  private load(): void {
    this.revoke();
    const path = this.src;
    if (!path) {
      this.objectUrl.set(null);
      return;
    }

    this.loading.set(true);
    this.attachmentService.getObjectUrl(path).subscribe({
      next: (url) => {
        this.currentObjectUrl = url;
        this.objectUrl.set(url);
        this.loading.set(false);
      },
      error: () => {
        this.objectUrl.set(null);
        this.loading.set(false);
      },
    });
  }

  private revoke(): void {
    if (this.currentObjectUrl) {
      URL.revokeObjectURL(this.currentObjectUrl);
      this.currentObjectUrl = null;
    }
  }
}