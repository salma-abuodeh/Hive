export interface LoginRequest {
    email: string;
    password: string;
  }

  
export interface LoginResponse {
    token: string;
  }

  export type CompanyType = 'COMPANY' | 'SCHOOL';
export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  companyName: string;
  companyType: CompanyType;
  companyDomain?: string;
}
export interface RegisterResponse {
  token: string;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  companyId: number;
  companyName: string;
}
export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
}