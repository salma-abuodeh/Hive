export type ConversationType = 'team' | 'direct' | 'group';

export interface ChatMessage {
  id: string;
  sender: string;
  initials: string;
  body: string;
  sentAt: string;
  isMine?: boolean;
}

export interface ChatConversation {
  id: string;
  type: ConversationType;
  name: string;
  initials: string;
  preview: string;
  timestamp: string;
  unreadCount?: number;
  messages: ChatMessage[];
}

export const conversationTypeLabels: Record<ConversationType, string> = {
  team: 'Team conversations',
  direct: 'Direct messages',
  group: 'Your groups',
};
