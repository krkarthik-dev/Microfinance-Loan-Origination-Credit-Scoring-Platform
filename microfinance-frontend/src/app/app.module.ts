import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HealthCheckComponent } from './pages/health-check/health-check.component';
import { LoginComponent } from './pages/login/login.component';
import { UnauthorizedComponent } from './pages/unauthorized/unauthorized.component';
import { JwtInterceptor } from './core/interceptors/jwt.interceptor';
import { ForceChangePasswordComponent } from './features/auth/force-change-password/force-change-password.component';
import { DisbursementQueueComponent } from './features/admin/disbursement-queue/disbursement-queue.component';
import { GlobalNavbarComponent } from './shared/components/global-navbar/global-navbar.component';

/**
 * Root application module for the Microfinance Loan Origination Platform.
 *
 * HttpClientModule is imported here so HealthService (and all future
 * API services) can inject HttpClient throughout the application.
 *
 * Feature modules (BorrowerModule, OfficerModule, AdminModule) will be
 * added as lazy-loaded modules in future User Stories.
 */
@NgModule({
  declarations: [
    AppComponent,
    HealthCheckComponent,
    LoginComponent,
    UnauthorizedComponent,
    ForceChangePasswordComponent
  ],
  imports: [
    BrowserModule,
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule,
    AppRoutingModule,
    GlobalNavbarComponent
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
