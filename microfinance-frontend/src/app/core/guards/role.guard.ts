import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Guard that enforces Role-Based Access Control (RBAC).
 * Checks if the authenticated user's role matches the allowed roles for the route.
 */
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    
    const requiredRoles = route.data['roles'] as Array<string>;
    const userRole = this.authService.getRole();

    // If route has no specific roles, allow it (fallback to AuthGuard)
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    // If user has one of the required roles, allow access
    if (userRole && requiredRoles.includes(userRole)) {
      return true;
    }

    // Role mismatch -> redirect to unauthorized page
    this.router.navigate(['/unauthorized']);
    return false;
  }
}
