import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Comment,
  CreateCommentRequest,
  CreatePostRequest,
  PageResponse,
  Post,
  UpdatePostRequest,
} from '../models/post.models';

@Injectable({ providedIn: 'root' })
export class PostService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/posts`;

  listFeed(page = 0, size = 10): Observable<PageResponse<Post>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    return this.http.get<PageResponse<Post>>(this.baseUrl, { params });
  }

  listSaved(page = 0, size = 10): Observable<PageResponse<Post>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    return this.http.get<PageResponse<Post>>(`${this.baseUrl}/saved`, { params });
  }

  getById(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.baseUrl}/${id}`);
  }

  create(payload: CreatePostRequest): Observable<Post> {
    return this.http.post<Post>(this.baseUrl, payload);
  }

  update(id: number, payload: UpdatePostRequest): Observable<Post> {
    return this.http.put<Post>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  like(id: number): Observable<Post> {
    return this.http.post<Post>(`${this.baseUrl}/${id}/reactions`, {});
  }

  unlike(id: number): Observable<Post> {
    return this.http.delete<Post>(`${this.baseUrl}/${id}/reactions`);
  }

  listComments(postId: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.baseUrl}/${postId}/comments`);
  }

  addComment(postId: number, payload: CreateCommentRequest): Observable<Comment> {
    return this.http.post<Comment>(`${this.baseUrl}/${postId}/comments`, payload);
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/comments/${commentId}`);
  }

  save(id: number): Observable<Post> {
    return this.http.post<Post>(`${this.baseUrl}/${id}/save`, {});
  }

  unsave(id: number): Observable<Post> {
    return this.http.delete<Post>(`${this.baseUrl}/${id}/save`);
  }
}
