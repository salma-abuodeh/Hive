import { Component, input } from '@angular/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { ChatConversation } from '../../models/chat.models';

@Component({
  selector: 'app-chat-detail',
  imports: [Icon],
  templateUrl: './chat-detail.html',
  styleUrl: './chat-detail.css',
})
export class ChatDetail {
  readonly conversation = input<ChatConversation | null>(null);
}
