import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HealthCheckComponent } from './pages/health-check/health-check.component';
import { LoginComponent } from './pages/login/login.component';
import { UnauthorizedComponent } from './pages/unauthorized/unauthorized.component';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';

/**
 * Application routing configuration.
 */
const routes: Routes = [
  {
    path: '',
    redirectTo: 'health',
    pathMatch: 'full'
  },
  {
    path: 'health',
    component: HealthCheckComponent,
    title: 'System Health | Microfinance Platform'
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'Sign In | Microfinance Platform'
  },
  {
    path: 'unauthorized',
    component: UnauthorizedComponent,
    title: 'Access Denied'
  },
  // Stub routes to test AC4 (Routing Guards)
  {
    path: 'applicant',
    component: HealthCheckComponent, // Stub destination
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] }
  },
  {
    path: 'officer',
    component: HealthCheckComponent, // Stub destination
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_OFFICER', 'ROLE_ADMIN'] }
  },
  {
    path: 'admin',
    component: HealthCheckComponent, // Stub destination
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] }
  },
  {
    path: '**',
    redirectTo: 'health'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
