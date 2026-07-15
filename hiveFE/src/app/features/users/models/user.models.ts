import { CompanySummary } from '../../auth/models/auth.models';

export interface UserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  roleName: string;
  active: boolean;
  createdAt: string;
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
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  password?: string;
  roleName?: string;
  active?: boolean;
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