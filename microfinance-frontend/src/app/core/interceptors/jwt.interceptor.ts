import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TokenService } from '../services/token.service';
import { Router } from '@angular/router';

/**
 * Intercepts all outgoing HTTP requests and appends the JWT token to the Authorization header.
 * Also catches 401 Unauthorized responses and redirects to the login page.
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  constructor(private tokenService: TokenService, private router: Router) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    
    // Get the JWT token from the TokenService
    const token = this.tokenService.getToken();

    // If we have a token, clone the request and add the Authorization header
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    // Pass the modified request to the next handler
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // If the backend returns 401 Unauthorized, the token is invalid or expired
        if (error.status === 401) {
          this.tokenService.removeToken();
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
