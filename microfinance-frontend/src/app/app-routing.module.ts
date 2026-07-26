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
    path: 'register', 
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) 
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
    path: 'applicant/active-loans',
    loadComponent: () => import('./features/borrower/active-loans/active-loans.component').then(m => m.ActiveLoansComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] },
    title: 'Active Loans | Microfinance'
  },
  {
    path: 'borrower/active-loans',
    redirectTo: 'applicant/active-loans',
    pathMatch: 'full'
  },
  {
    path: 'applicant/active-loans/:loanId',
    loadComponent: () => import('./features/borrower/active-loan-detail/active-loan-detail.component').then(m => m.ActiveLoanDetailComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] },
    title: 'Repayment Dashboard | Microfinance'
  },
  {
    path: 'borrower/active-loans/:loanId',
    redirectTo: 'applicant/active-loans/:loanId',
    pathMatch: 'full'
  },
  {
    path: 'applicant/loan/:id/tracking',
    loadComponent: () => import('./features/borrower/loan-tracking/loan-tracking.component').then(m => m.LoanTrackingComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] },
    title: 'Loan Tracking | Microfinance'
  },
  {
    path: 'applicant/loan/:id/corrections',
    loadComponent: () => import('./features/borrower/loan-corrections/loan-corrections.component').then(m => m.LoanCorrectionsComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] },
    title: 'Correction Workspace | Microfinance'
  },
  {
    path: 'applicant/profile',
    loadComponent: () => import('./features/borrower/profile-setup/profile-setup.component').then(m => m.ProfileSetupComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_APPLICANT'] },
    title: 'Profile & KYC | Microfinance'
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
    path: 'officer/loan/:id/repayment',
    loadComponent: () => import('./features/officer/officer-repayment/officer-repayment.component').then(m => m.OfficerRepaymentComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_OFFICER', 'ROLE_ADMIN'] },
    title: 'Repayment Collection | Microfinance'
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
    path: 'admin/dashboard',
    loadComponent: () => import('./features/admin/executive-dashboard/executive-dashboard.component').then(m => m.ExecutiveDashboardComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    title: 'Executive Dashboard | Microfinance'
  },
  {
    path: 'admin/products',
    loadComponent: () => import('./features/admin/loan-products/loan-products.component').then(m => m.LoanProductsComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    title: 'Loan Products | Microfinance'
  },
  {
    path: 'admin/staff',
    loadComponent: () => import('./features/admin/staff-management/staff-management.component').then(m => m.StaffManagementComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    title: 'Staff Management | Microfinance'
  },
  {
    path: 'admin/disbursements',
    loadComponent: () => import('./features/admin/disbursement-queue/disbursement-queue.component').then(m => m.DisbursementQueueComponent),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    title: 'Disbursement Queue | Microfinance'
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    anchorScrolling: 'enabled',
    scrollPositionRestoration: 'enabled',
    scrollOffset: [0, 80] // Account for the sticky global navbar
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
