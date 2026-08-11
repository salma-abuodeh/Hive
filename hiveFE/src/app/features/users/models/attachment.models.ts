export type AttachmentType = 'IMAGE' | 'DOCUMENT';

export type AttachmentContext =
  | 'AVATAR'
  | 'COMPANY_LOGO'
  | 'TEAM_LOGO'
  | 'EVENT_COVER'
  | 'POST'
  | 'COMMENT'
  | 'CHAT_MESSAGE'
  | 'COMPANY_APPLICATION_DOCUMENT'
  | 'MEMBERSHIP_REQUEST_DOCUMENT';

export interface AttachmentResponse {
  id: number;
  url: string;
  contentType: string;
  sizeBytes: number;
  attachmentType: AttachmentType;
}