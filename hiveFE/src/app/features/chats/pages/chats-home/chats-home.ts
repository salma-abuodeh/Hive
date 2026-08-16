import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatDetail } from '../../components/chat-detail/chat-detail';
import { ConversationList } from '../../components/conversation-list/conversation-list';
import { ChatAttachment, ChatConversation, ChatMessage, CompanyMember, ConversationApiResponse, ConversationType, MessageApiResponse } from '../../models/chat.models';
import { ChatService } from '../../../../core/services/chat.service';
import { ChatSocketService } from '../../../../core/services/chat-socket.service';
import { AttachmentService } from '../../../../core/services/attachment.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-chats-home',
  imports: [ConversationList, ChatDetail, FormsModule],
  templateUrl: './chats-home.html',
  styleUrl: './chats-home.css',
})
export class ChatsHome implements OnInit, OnDestroy {
  private readonly chatService = inject(ChatService);
  private readonly socket = inject(ChatSocketService);
  private readonly attachmentService = inject(AttachmentService);
  private readonly auth = inject(AuthService);
  readonly conversations = signal<ChatConversation[]>([]);
  readonly selectedConversation = signal<ChatConversation | null>(null);
  readonly members = signal<CompanyMember[]>([]);
  readonly newChatOpen = signal(false);
  readonly targetUserId = signal<number | null>(null);
  readonly newChatError = signal('');

  ngOnInit(): void {
    this.loadConversations();
    this.chatService.companyMembers().subscribe({ next: (members) => this.members.set(members.filter((member) => member.id !== this.auth.getUser()?.id)) });
    this.socket.messages$.subscribe((message) => this.receiveMessage(message));
  }
  ngOnDestroy(): void { this.socket.unsubscribe(); }

  selectConversation(conversation: ChatConversation): void {
    this.selectedConversation.set(conversation);
    this.socket.subscribe(conversation.id);
    this.chatService.messages(conversation.id).subscribe({
      // The backend returns newest-first; render a conversation in chronological order.
      next: (page) => this.updateConversation(conversation.id, (chat) => ({ ...chat, messages: [...page.content].reverse().map((message) => this.toMessage(message)) })),
    });
    this.chatService.markRead(conversation.id).subscribe();
  }

  sendMessage(event: { content: string; files: File[] }): void {
    const conversation = this.selectedConversation();
    if (!conversation) return;
    this.chatService.send(conversation.id, event.content).subscribe({
      next: (message) => {
        this.receiveMessage(message);
        event.files.forEach((file) => this.attachmentService.uploadMessageAttachment(message.id, file).subscribe({
          next: (attachment) => this.updateMessageAttachments(conversation.id, message.id, attachment),
        }));
      },
    });
  }

  openNewChat(): void { this.newChatError.set(''); this.newChatOpen.set(true); }
  closeNewChat(): void { this.newChatOpen.set(false); this.targetUserId.set(null); }
  createDirectChat(): void {
    const targetUserId = this.targetUserId();
    if (targetUserId == null) { this.newChatError.set('Choose a company member.'); return; }
    this.chatService.createDirect(targetUserId).subscribe({
      next: (conversation) => {
        const chat = this.toConversation(conversation);
        this.conversations.update((items) => items.some((item) => item.id === chat.id) ? items : [chat, ...items]);
        this.closeNewChat();
        this.selectConversation(chat);
      },
      error: (err) => this.newChatError.set(err.error?.message ?? 'Could not start this conversation.'),
    });
  }

  private loadConversations(): void {
    this.chatService.list().subscribe({
      next: (items) => this.conversations.set(items.map((item) => this.toConversation(item))),
    });
  }
  private receiveMessage(message: MessageApiResponse): void {
    const id = String(message.conversationId);
    this.updateConversation(id, (chat) => {
      if (chat.messages.some((existing) => String(existing.id) === String(message.id))) return chat;
      return { ...chat, messages: [...chat.messages, this.toMessage(message)], preview: message.content, timestamp: this.formatTime(message.createdAt) };
    });
  }
  private updateMessageAttachments(conversationId: string, messageId: number, attachment: ChatAttachment): void {
    this.updateConversation(conversationId, (chat) => ({
      ...chat,
      messages: chat.messages.map((message) => String(message.id) === String(messageId)
        ? { ...message, attachments: [...(message.attachments ?? []), attachment] } : message),
    }));
  }
  private updateConversation(id: string, updater: (chat: ChatConversation) => ChatConversation): void {
    this.conversations.update((items) => items.map((chat) => chat.id === id ? updater(chat) : chat));
    this.selectedConversation.update((chat) => chat?.id === id ? updater(chat) : chat);
  }
  private toConversation(item: ConversationApiResponse): ChatConversation {
    const type = item.conversationType.toLowerCase() as ConversationType;
    const selfId = this.auth.getUser()?.id;
    const others = item.participants.filter((participant) => participant.userId !== selfId);
    const derivedName = others.map((participant) => `${participant.firstName} ${participant.lastName}`).join(', ');
    const name = item.name || derivedName || 'Conversation';
    return { id: String(item.id), type, name, initials: this.initials(name), preview: item.lastMessage?.content ?? '', timestamp: item.lastMessage ? this.formatTime(item.lastMessage.createdAt) : this.formatTime(item.createdAt), unreadCount: item.unreadCount, participants: item.participants, messages: [] };
  }
  private toMessage(message: MessageApiResponse): ChatMessage {
    return { id: message.id, sender: message.senderName, initials: this.initials(message.senderName), body: message.content, sentAt: this.formatTime(message.createdAt), isMine: message.senderUserId === this.auth.getUser()?.id, attachments: message.attachments };
  }
  private initials(name: string): string { return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase(); }
  private formatTime(value: string): string { return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); }
}
