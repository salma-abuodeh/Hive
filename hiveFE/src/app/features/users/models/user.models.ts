import { CompanySummary } from '../../auth/models/auth.models';

export interface TeamSummary {
  id: number;
  name: string;
}

export interface CompanyMembership {
  id: number;
  name: string;
  roleName?: string | null;
  jobTitle?: string | null;
  teams?: TeamSummary[];
  active?: boolean;
}

export interface UserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  roleName: string;
  jobTitle?: string | null;
  teams?: TeamSummary[];
  activeCompanyId?: number | null;
  companies?: CompanyMembership[];
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
  jobTitle?: string | null;
  teamIds?: number[];
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  password?: string;
  roleName?: string;
  active?: boolean;
  jobTitle?: string | null;
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
