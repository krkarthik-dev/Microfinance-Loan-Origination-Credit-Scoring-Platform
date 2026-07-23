import { Injectable } from '@angular/core';
import { UserClaims } from '../../shared/models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly TOKEN_KEY = 'jwt_token';

  constructor() {}

  /**
   * Save the JWT token to localStorage
   */
  setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  /**
   * Retrieve the JWT token from localStorage
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Remove the JWT token from localStorage (logout)
   */
  removeToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  /**
   * Decode the JWT payload without verifying the signature
   * (Signature is verified securely on the backend)
   */
  decodeToken(): UserClaims | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      // JWT format: header.payload.signature
      const payloadBase64 = token.split('.')[1];
      // atob() decodes base64
      const payloadJson = atob(payloadBase64);
      return JSON.parse(payloadJson) as UserClaims;
    } catch (e) {
      console.error('Error decoding token', e);
      return null;
    }
  }

  /**
   * Get the role from the token
   */
  getRole(): string | null {
    const decoded = this.decodeToken();
    return decoded ? decoded.role : null;
  }

  /**
   * Check if the token is expired
   */
  isTokenExpired(): boolean {
    const decoded = this.decodeToken();
    if (!decoded) return true;

    // exp is in seconds, Date.now() is in milliseconds
    const currentTime = Math.floor(Date.now() / 1000);
    return decoded.exp < currentTime;
  }

  /**
   * Check if the user has a valid (non-expired) token
   */
  hasValidToken(): boolean {
    const token = this.getToken();
    return !!token && !this.isTokenExpired();
  }
}
