export type PollVisibility = 'COMPANY' | 'TEAM' | 'PRIVATE';

export interface PollOptionRequest {
  text: string;
}

export interface PollOptionResponse {
  id: number;
  text: string;
  voteCount: number;
  votedByMe: boolean;
}

export interface PollRequest {
  question: string;
  description?: string;
  allowMultiple?: boolean;
  closesAt?: string;
  teamId?: number | null;
  visibility?: PollVisibility;
  options: PollOptionRequest[];
}

export interface PollResponse {
  id: number;
  companyId: number;
  teamId: number | null;
  createdByUserId: number;
  createdByName: string;
  question: string;
  description: string | null;
  allowMultiple: boolean;
  closesAt: string | null;
  visibility: PollVisibility;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  totalVotes: number;
  options: PollOptionResponse[];
}

export interface VoteRequest {
  optionIds: number[];
}