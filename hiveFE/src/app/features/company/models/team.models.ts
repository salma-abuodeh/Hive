export interface TeamMember {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  jobTitle?: string | null;
}

export interface Team {
  id: number;
  name: string;
  description?: string | null;
  active: boolean;
  memberCount: number;
  members?: TeamMember[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface UpdateTeamRequest {
  name?: string;
  description?: string;
  active?: boolean;
}
