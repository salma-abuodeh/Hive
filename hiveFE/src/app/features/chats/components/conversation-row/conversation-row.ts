import { Component, input, output } from '@angular/core';
import { ChatConversation } from '../../models/chat.models';

@Component({
  selector: 'app-conversation-row',
  templateUrl: './conversation-row.html',
  styleUrl: './conversation-row.css',
})
export class ConversationRow {
  readonly conversation = input.required<ChatConversation>();
  readonly active = input(false);
  readonly selected = output<ChatConversation>();
}
