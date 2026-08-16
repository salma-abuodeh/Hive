import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../../shared/components/icon/icon';
import { AuthImage } from '../../../../shared/components/auth-image/auth-image';
import { ChatConversation } from '../../models/chat.models';
import { AttachmentService } from '../../../../core/services/attachment.service';

@Component({
  selector: 'app-chat-detail',
  imports: [Icon, AuthImage, FormsModule],
  templateUrl: './chat-detail.html',
  styleUrl: './chat-detail.css',
})
export class ChatDetail {
  private readonly supportedImageTypes = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);
  private readonly attachmentService = inject(AttachmentService);
  readonly conversation = input<ChatConversation | null>(null);
  readonly messageSent = output<{ content: string; files: File[] }>();
  readonly content = signal('');
  readonly files = signal<File[]>([]);
  readonly fileError = signal('');
  readonly previews = signal<{ file: File; url: string }[]>([]);
  readonly detailsOpen = signal(false);
  readonly attachments = computed(() => this.conversation()?.messages.flatMap((message) => message.attachments ?? []) ?? []);

  send(): void {
    const content = this.content().trim();
    const files = this.files();
    if (!content && files.length === 0) return;
    this.messageSent.emit({ content: content || 'Shared a photo', files });
    this.content.set('');
    this.clearFiles();
  }
  chooseFiles(event: Event): void {
    const selected = Array.from((event.target as HTMLInputElement).files ?? []);
    const files = selected.filter((file) => this.supportedImageTypes.has(file.type) && file.size <= 5 * 1024 * 1024);
    this.clearFiles();
    this.files.set(files);
    this.previews.set(files.map((file) => ({ file, url: URL.createObjectURL(file) })));
    this.fileError.set(files.length === selected.length ? '' : 'Choose images up to 5MB.');
  }
  removeFile(file: File): void {
    const preview = this.previews().find((item) => item.file === file);
    if (preview) URL.revokeObjectURL(preview.url);
    this.files.update((files) => files.filter((item) => item !== file));
    this.previews.update((previews) => previews.filter((item) => item.file !== file));
  }
  openAttachment(path: string): void { this.attachmentService.open(path); }
  toggleDetails(): void { this.detailsOpen.update((open) => !open); }
  closeDetails(): void { this.detailsOpen.set(false); }

  private clearFiles(): void {
    this.previews().forEach((preview) => URL.revokeObjectURL(preview.url));
    this.files.set([]);
    this.previews.set([]);
  }
}
