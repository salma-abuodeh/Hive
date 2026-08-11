import { Component, signal } from '@angular/core';
import { ChatDetail } from '../../components/chat-detail/chat-detail';
import { ConversationList } from '../../components/conversation-list/conversation-list';
import { ChatConversation } from '../../models/chat.models';

@Component({
  selector: 'app-chats-home',
  imports: [ConversationList, ChatDetail],
  templateUrl: './chats-home.html',
  styleUrl: './chats-home.css',
})
export class ChatsHome {
  readonly conversations: ChatConversation[] = [];
  readonly selectedConversation = signal<ChatConversation | null>(null);

  selectConversation(conversation: ChatConversation): void {
    this.selectedConversation.set(conversation);
  }
}
