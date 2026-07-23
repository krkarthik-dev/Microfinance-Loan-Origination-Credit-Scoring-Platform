export interface LoginRequest {
  email?: string;
  password?: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  email: string;
  role: string;
}

export interface UserClaims {
  sub: string; // email
  role: string; // e.g., ROLE_APPLICANT
  iat: number; // issued at
  exp: number; // expiration
}
