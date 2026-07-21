import { CompanySummary } from '../../auth/models/auth.models';

export interface TeamSummary {
  id: number;
  name: string;
}

export interface UserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  roleName: string;
  jobTitle?: string | null;
  jobTitleId?: number | null;
  teams?: TeamSummary[];
  active: boolean;
  createdAt: string;
}

export interface JobTitle {
  id: number;
  title: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateMeRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  password?: string;
  currentPassword?: string;
}

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  roleName: string;
  companyIds?: number[];
  jobTitleId?: number | null;
  teamIds?: number[];
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  password?: string;
  roleName?: string;
  active?: boolean;
  jobTitleId?: number | null;
  teamIds?: number[];
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

export type { CompanySummary };
