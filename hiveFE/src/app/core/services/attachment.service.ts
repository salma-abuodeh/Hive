import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AttachmentContext, AttachmentResponse } from '../../shared/models/attachment.models';

// Mirrors LocalDiskStorageService on the backend — keep in sync.
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const DOCUMENT_TYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
];
const IMAGE_ONLY_CONTEXTS = new Set<AttachmentContext>(['AVATAR', 'EVENT_COVER']);
const MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /**
   * Client-side mirror of the backend's validation, so we can reject bad files
   * before spending an upload round-trip. The backend re-validates regardless.
   */
  validate(file: File, context: AttachmentContext): string | null {
    if (file.size > MAX_SIZE_BYTES) {
      return 'File exceeds 5MB limit';
    }
    const isImage = IMAGE_TYPES.includes(file.type);
    const isDocument = DOCUMENT_TYPES.includes(file.type);

    if (IMAGE_ONLY_CONTEXTS.has(context)) {
      return isImage ? null : 'Only JPEG, PNG, WEBP, or GIF images are allowed here';
    }
    return isImage || isDocument
      ? null
      : 'Unsupported file type. Allowed: JPEG, PNG, WEBP, GIF, PDF, DOC, DOCX, XLS, XLSX';
  }

  // ---------- Avatar ----------

  uploadAvatar(file: File): Observable<AttachmentResponse> {
    return this.upload('/users/me/avatar', file);
  }

  deleteAvatar(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/users/me/avatar`);
  }

  // ---------- Event cover ----------

  uploadEventCover(eventId: number, file: File): Observable<AttachmentResponse> {
    return this.upload(`/events/${eventId}/cover`, file);
  }

  deleteEventCover(eventId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/events/${eventId}/cover`);
  }

  // ---------- Post attachments ----------

  uploadPostAttachment(postId: number, file: File): Observable<AttachmentResponse> {
    return this.upload(`/posts/${postId}/attachments`, file);
  }

  deletePostAttachment(postId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/posts/${postId}/attachments/${attachmentId}`);
  }

  // ---------- Comment attachments ----------

  uploadCommentAttachment(commentId: number, file: File): Observable<AttachmentResponse> {
    return this.upload(`/posts/comments/${commentId}/attachments`, file);
  }

  deleteCommentAttachment(commentId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/posts/comments/${commentId}/attachments/${attachmentId}`);
  }

  /**
   * Attachment downloads require the Authorization header, so a plain <img src>
   * can't hit them directly. Fetch as a blob (the auth interceptor attaches the
   * token) and hand back an object URL — see AuthImage, which does this
   * generically for any attachment path.
   */
  getObjectUrl(attachmentPath: string): Observable<string> {
    const url = attachmentPath.startsWith('http') ? attachmentPath : `${this.baseUrl}${attachmentPath}`;
    return this.http.get(url, { responseType: 'blob' }).pipe(map((blob) => URL.createObjectURL(blob)));
  }

  private upload(path: string, file: File): Observable<AttachmentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<AttachmentResponse>(`${this.baseUrl}${path}`, formData);
  }
  /** Opens an attachment (e.g. a PDF/DOC in a post) in a new tab, auth header intact. */
open(attachmentPath: string): void {
  this.getObjectUrl(attachmentPath).subscribe((url) => {
    window.open(url, '_blank');
  });
}
}