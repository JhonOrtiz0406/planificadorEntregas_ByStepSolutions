export type UserRole = 'PLATFORM_ADMIN' | 'ORG_ADMIN' | 'ORG_EMPLOYEE' | 'ORG_DELIVERY';

export interface User {
  id: string;
  email: string;
  name: string;
  pictureUrl?: string;
  role: UserRole;
  organizationId?: string;
  organizationName?: string;
  orgIconName?: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}
