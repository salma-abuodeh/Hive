export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface CompanySummary {
  id: number;
  name: string;
}

export interface LoginResponse {
  token: string;
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  active: boolean;
  companies: CompanySummary[];
}

export interface RegisterResponse {
  token: string;
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  active: boolean;
  companies?: CompanySummary[];
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
}
