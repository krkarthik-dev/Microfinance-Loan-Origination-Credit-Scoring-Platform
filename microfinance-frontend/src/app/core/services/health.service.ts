import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Health response model matching the backend HealthResponse DTO.
 */
export interface HealthResponse {
  status: string;
  service: string;
  version: string;
  timestamp: string;
}

/**
 * Core service for checking backend API health.
 *
 * Calls GET /api/health on the Spring Boot backend to verify
 * end-to-end connectivity between Angular and the REST API.
 *
 * Provided at root level — singleton across the entire application.
 */
@Injectable({
  providedIn: 'root'
})
export class HealthService {

  private readonly healthUrl = `${environment.apiUrl}/health`;

  constructor(private http: HttpClient) {}

  /**
   * Calls the backend health check endpoint.
   * @returns Observable<HealthResponse> with status, service, version, timestamp
   */
  getHealth(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(this.healthUrl);
  }
}
