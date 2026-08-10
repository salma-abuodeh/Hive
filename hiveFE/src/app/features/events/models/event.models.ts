export type EventVisibility = 'COMPANY' | 'TEAM' | 'PRIVATE';
export type RsvpStatus = 'INVITED' | 'ACCEPTED' | 'DECLINED' | 'MAYBE';

export interface EventResponse {
  id: number;
  companyId: number;
  teamId: number | null;
  createdByUserId: number;
  createdByName: string;
  title: string;
  description: string | null;
  location: string | null;
  startTime: string;
  endTime: string;
  visibility: EventVisibility;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  myRsvpStatus: RsvpStatus | null;
  coverUrl?: string | null;
}

export interface EventRequest {
  title: string;
  description?: string;
  location?: string;
  startTime: string;
  endTime: string;
  teamId?: number | null;
  visibility?: EventVisibility;
}

export interface InviteUsersRequest {
  userIds: number[];
}

export interface RsvpUpdateRequest {
  status: RsvpStatus;
}

export interface EventRsvpResponse {
  userId: number;
  userName: string;
  status: RsvpStatus;
  respondedAt: string;
}