export interface Organization {
  id: string;
  name: string;
  slug: string;
  logoUrl?: string;
  iconName?: string;
  adminEmail: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrganizationRequest {
  name: string;
  logoUrl?: string;
  adminEmail: string;
}

export interface InviteMemberRequest {
  email: string;
  role: string;
}
