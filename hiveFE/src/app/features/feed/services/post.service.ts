import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Comment, PageResponse, Post, ReactionType, VisibilityType } from '../models/post.models';

@Injectable({ providedIn: 'root' })
export class PostService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/posts`;

  listFeed(page = 0): Observable<PageResponse<Post>> {
    const params = new HttpParams().set('page', page).set('size', 10).set('sort', 'createdAt,desc');
    return this.http.get<PageResponse<Post>>(this.url, { params });
  }

  listSaved(page = 0): Observable<PageResponse<Post>> {
    const params = new HttpParams().set('page', page).set('size', 10).set('sort', 'createdAt,desc');
    return this.http.get<PageResponse<Post>>(`${this.url}/saved`, { params });
  }

  getById(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.url}/${id}`);
  }

  create(body: { content: string; visibilityType: VisibilityType; teamId?: number | null }): Observable<Post> {
    return this.http.post<Post>(this.url, body);
  }

  update(id: number, body: { content: string; visibilityType?: VisibilityType; teamId?: number | null }): Observable<Post> {
    return this.http.put<Post>(`${this.url}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  react(id: number, reactionType: ReactionType): Observable<Post> {
    return this.http.post<Post>(`${this.url}/${id}/reactions`, { reactionType });
  }

  listComments(postId: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.url}/${postId}/comments`);
  }

  addComment(postId: number, content: string): Observable<Comment> {
    return this.http.post<Comment>(`${this.url}/${postId}/comments`, { content });
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/comments/${commentId}`);
  }

  save(id: number): Observable<Post> {
    return this.http.post<Post>(`${this.url}/${id}/save`, {});
  }

  unsave(id: number): Observable<Post> {
    return this.http.delete<Post>(`${this.url}/${id}/save`);
  }
}
