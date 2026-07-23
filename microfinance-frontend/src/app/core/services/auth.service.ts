import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { LoginRequest, LoginResponse } from '../../shared/models/auth.model';
import { TokenService } from './token.service';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/api/auth`;

  constructor(
    private http: HttpClient,
    private tokenService: TokenService,
    private router: Router
  ) {}

  /**
   * Send login credentials to the backend and store the JWT upon success.
   */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        if (response && response.token) {
          this.tokenService.setToken(response.token);
        }
      })
    );
  }

  /**
   * Clear the token and redirect to login
   */
  logout(): void {
    this.tokenService.removeToken();
    this.router.navigate(['/login']);
  }

  /**
   * Check if the user is currently authenticated
   */
  isLoggedIn(): boolean {
    return this.tokenService.hasValidToken();
  }

  /**
   * Get the current user's role
   */
  getRole(): string | null {
    return this.tokenService.getRole();
  }
}
