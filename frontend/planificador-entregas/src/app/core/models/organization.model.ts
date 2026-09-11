export interface Organization {
  id: string;
  name: string;
  slug: string;
  logoUrl?: string;
  iconName?: string;
  adminEmail: string;
  category: string;
  adminFirstName?: string;
  adminLastName?: string;
  adminPhone?: string;
  organizationPhone?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrganizationRequest {
  name: string;
  logoUrl?: string;
  adminEmail: string;
  category: string;
  adminFirstName?: string;
  adminLastName?: string;
  adminPhone?: string;
  organizationPhone?: string;
}

export interface InviteMemberRequest {
  email: string;
  role: string;
}
