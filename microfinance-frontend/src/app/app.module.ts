import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { CommonModule } from '@angular/common';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HealthCheckComponent } from './pages/health-check/health-check.component';

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
    HealthCheckComponent
  ],
  imports: [
    BrowserModule,
    CommonModule,
    HttpClientModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}
