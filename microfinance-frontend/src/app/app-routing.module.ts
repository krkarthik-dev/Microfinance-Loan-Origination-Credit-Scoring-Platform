import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HealthCheckComponent } from './pages/health-check/health-check.component';

/**
 * Application routing configuration.
 *
 * US01: Default route redirects to health check page.
 * Future US will add routes for borrower, officer, and admin modules.
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
  // Future lazy-loaded feature routes will be added here:
  // { path: 'login',    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule) },
  // { path: 'borrower', loadChildren: () => import('./features/borrower/borrower.module').then(m => m.BorrowerModule) },
  // { path: 'officer',  loadChildren: () => import('./features/officer/officer.module').then(m => m.OfficerModule) },
  // { path: 'admin',    loadChildren: () => import('./features/admin/admin.module').then(m => m.AdminModule) },
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
