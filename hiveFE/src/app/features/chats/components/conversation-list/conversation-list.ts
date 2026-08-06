import { Component, input, output } from '@angular/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { ChatConversation, ConversationType, conversationTypeLabels } from '../../models/chat.models';
import { ConversationRow } from '../conversation-row/conversation-row';

@Component({
  selector: 'app-conversation-list',
  imports: [ConversationRow, Icon],
  templateUrl: './conversation-list.html',
  styleUrl: './conversation-list.css',
})
export class ConversationList {
  readonly conversations = input.required<ChatConversation[]>();
  readonly selectedId = input<string | null>(null);
  readonly conversationSelected = output<ChatConversation>();
  readonly categories: ConversationType[] = ['team', 'direct', 'group'];
  readonly labels = conversationTypeLabels;

  conversationsFor(type: ConversationType): ChatConversation[] {
    return this.conversations().filter((conversation) => conversation.type === type);
  }
}
