export type ConversationType = 'team' | 'direct' | 'group';

export interface ChatMessage {
  id: string | number;
  sender: string;
  initials: string;
  body: string;
  sentAt: string;
  isMine?: boolean;
  attachments?: ChatAttachment[];
}

export interface ChatAttachment {
  id: number;
  url: string;
  contentType: string;
  sizeBytes: number;
  attachmentType: 'IMAGE' | 'DOCUMENT';
}

export interface ConversationApiResponse {
  id: number;
  conversationType: 'TEAM' | 'DIRECT' | 'GROUP';
  name: string | null;
  participants: { userId: number; firstName: string; lastName: string }[];
  lastMessage: MessageApiResponse | null;
  unreadCount: number;
  createdAt: string;
}

export interface MessageApiResponse {
  id: number;
  conversationId: number;
  senderUserId: number;
  senderName: string;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM';
  createdAt: string;
  attachments: ChatAttachment[];
}

export interface PageResponse<T> { content: T[]; }

export interface CompanyMember {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface ChatParticipant {
  userId: number;
  firstName: string;
  lastName: string;
}

export interface ChatConversation {
  id: string;
  type: ConversationType;
  name: string;
  initials: string;
  preview: string;
  timestamp: string;
  unreadCount?: number;
  participants: ChatParticipant[];
  messages: ChatMessage[];
}

export const conversationTypeLabels: Record<ConversationType, string> = {
  team: 'Team conversations',
  direct: 'Direct messages',
  group: 'Your groups',
};
