import { Injectable, inject } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { MessageApiResponse } from '../../features/chats/models/chat.models';

@Injectable({ providedIn: 'root' })
export class ChatSocketService {
  private readonly auth = inject(AuthService);
  private readonly incomingSubject = new Subject<MessageApiResponse>();
  readonly messages$ = this.incomingSubject.asObservable();
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private activeConversationId: string | null = null;

  subscribe(conversationId: string): void {
    if (this.activeConversationId === conversationId && this.client?.connected) return;
    this.unsubscribe();
    const token = this.auth.getToken();
    if (!token) return;
    this.activeConversationId = conversationId;

    void this.connect(conversationId, token);
  }

  private async connect(conversationId: string, token: string): Promise<void> {
    // sockjs-client expects Node's `global` variable during module loading.
    // Loading it only when chat is opened keeps it out of the initial bundle;
    // aliasing it here makes the browser module safe to load.
    const browserGlobal = globalThis as typeof globalThis & { global?: typeof globalThis };
    browserGlobal.global ??= globalThis;
    const { default: SockJS } = await import('sockjs-client');

    // The user may have selected another conversation while SockJS loaded.
    if (this.activeConversationId !== conversationId) return;
    this.client = new Client({
      webSocketFactory: () => new SockJS(`${environment.apiUrl}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      debug: () => undefined,
      onConnect: () => this.subscription = this.client?.subscribe(
        `/topic/conversations/${conversationId}`,
        (frame: IMessage) => this.receive(frame),
      ) ?? null,
    });
    this.client.activate();
  }

  unsubscribe(): void {
    this.subscription?.unsubscribe(); this.subscription = null;
    this.client?.deactivate(); this.client = null; this.activeConversationId = null;
  }
  private receive(frame: IMessage): void {
    try { this.incomingSubject.next(JSON.parse(frame.body) as MessageApiResponse); } catch { /* ignore malformed broker payload */ }
  }
}
