import { Component, OnInit } from '@angular/core';
import { HealthService, HealthResponse } from '../../core/services/health.service';

/**
 * HealthCheckComponent
 *
 * Displays real-time backend connectivity status by consuming
 * the GET /api/health endpoint via HealthService.
 *
 * This component proves end-to-end connectivity between the Angular
 * frontend and the Spring Boot REST API (AC4 of US01).
 */
@Component({
  selector: 'app-health-check',
  templateUrl: './health-check.component.html',
  styleUrls: ['./health-check.component.scss']
})
export class HealthCheckComponent implements OnInit {

  healthData: HealthResponse | null = null;
  isLoading = true;
  hasError = false;
  errorMessage = '';

  constructor(private healthService: HealthService) {}

  ngOnInit(): void {
    this.checkHealth();
  }

  /**
   * Calls the backend health endpoint and updates component state.
   */
  checkHealth(): void {
    this.isLoading = true;
    this.hasError = false;

    this.healthService.getHealth().subscribe({
      next: (data: HealthResponse) => {
        this.healthData = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.hasError = true;
        this.isLoading = false;
        this.errorMessage = 'Backend unreachable. Ensure Spring Boot is running on port 8080.';
        console.error('Health check failed:', err);
      }
    });
  }
}
