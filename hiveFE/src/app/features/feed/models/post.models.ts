import { AttachmentResponse } from '../../../shared/models/attachment.models';

export type VisibilityType = 'COMPANY' | 'TEAM';
export type ReactionType = 'LIKE' | 'LOVE' | 'LAUGHING' | 'SAD' | 'ANGRY';

export interface Post {
  id: number;
  content: string;
  postType: string;
  visibilityType: VisibilityType;
  teamId?: number | null;
  teamName?: string | null;
  authorId: number;
  authorFirstName: string;
  authorLastName: string;
  authorRoleName?: string | null;
  authorJobTitle?: string | null;
  likeCount: number;
  commentCount: number;
  likedByMe: boolean;
  myReaction?: ReactionType | null;
  savedByMe: boolean;
  ownedByMe: boolean;
  createdAt: string;
  updatedAt: string;
  attachments: AttachmentResponse[];
}

export interface Comment {
  id: number;
  postId: number;
  content: string;
  authorId: number;
  authorFirstName: string;
  authorLastName: string;
  ownedByMe: boolean;
  createdAt: string;
  updatedAt: string;
  attachments: AttachmentResponse[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}