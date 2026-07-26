export type VisibilityType = 'COMPANY' | 'TEAM';
export type PostType = 'TEXT';
export type ReactionType = 'LIKE' | 'LOVE' | 'LAUGHING' | 'SAD' | 'ANGRY';

export const REACTION_OPTIONS: { type: ReactionType; emoji: string; label: string }[] = [
  { type: 'LIKE', emoji: '👍', label: 'Like' },
  { type: 'LOVE', emoji: '❤️', label: 'Love' },
  { type: 'LAUGHING', emoji: '😂', label: 'Laughing' },
  { type: 'SAD', emoji: '😢', label: 'Sad' },
  { type: 'ANGRY', emoji: '😡', label: 'Angry' },
];

export interface Post {
  id: number;
  content: string;
  postType: PostType;
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
}

export interface CreatePostRequest {
  content: string;
  visibilityType: VisibilityType;
  teamId?: number | null;
}

export interface UpdatePostRequest {
  content: string;
  visibilityType?: VisibilityType;
  teamId?: number | null;
}

export interface CreateCommentRequest {
  content: string;
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
