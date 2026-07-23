import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HealthCheckComponent } from './pages/health-check/health-check.component';
import { LoginComponent } from './pages/login/login.component';
import { UnauthorizedComponent } from './pages/unauthorized/unauthorized.component';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';
import { DashboardComponent } from './features/borrower/dashboard/dashboard.component';
import { ProfileSetupComponent } from './features/borrower/profile-setup/profile-setup.component';
import { ForceChangePasswordComponent } from './features/auth/force-change-password/force-change-password.component';

/**
 * Application routing configuration.
 */
const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
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
    path: 'force-change-password',
    component: ForceChangePasswordComponent,
    title: 'Change Password | Microfinance Platform'
  },
  {
    path: 'unauthorized',
    component: UnauthorizedComponent,
    title: 'Access Denied'
  },
  // Stub routes to test AC4 (Routing Guards)
  // Applicant Dashboard Route
  {
    path: 'applicant',
    component: DashboardComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] },
    title: 'Borrower Dashboard | Microfinance'
  },
  {
    path: 'applicant/apply',
    loadComponent: () => import('./features/borrower/loan-application/loan-application.component').then(m => m.LoanApplicationComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] },
    title: 'Loan Application | Microfinance'
  },
  {
    path: 'applicant/loan/:id/tracking',
    loadComponent: () => import('./features/borrower/loan-tracking/loan-tracking.component').then(m => m.LoanTrackingComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] },
    title: 'Loan Tracking | Microfinance'
  },
  {
    path: 'officer',
    loadComponent: () => import('./features/officer/command-center/command-center.component').then(m => m.CommandCenterComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_OFFICER', 'ROLE_ADMIN'] },
    title: 'Command Center | Microfinance'
  },
  {
    path: 'officer/loan/:id/review',
    loadComponent: () => import('./features/officer/application-review/application-review.component').then(m => m.ApplicationReviewComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_OFFICER', 'ROLE_ADMIN'] },
    title: 'Review Application | Microfinance'
  },
  {
    path: 'officer/kyc/:id',
    loadComponent: () => import('./features/officer/kyc-review/kyc-review.component').then(m => m.KycReviewComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_OFFICER', 'ROLE_ADMIN'] },
    title: 'KYC Review | Microfinance'
  },
  {
    path: 'officer/direct-application/:email/apply',
    loadComponent: () => import('./features/borrower/loan-application/loan-application.component').then(m => m.LoanApplicationComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_OFFICER'] },
    title: 'Direct Application | Microfinance'
  },
  {
    path: 'admin',
    component: HealthCheckComponent, // Stub destination
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] }
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
