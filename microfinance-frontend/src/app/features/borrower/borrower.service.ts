import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardMetrics {
  activeLoans: number;
  totalOutstanding: number;
  pendingApplications: number;
}

@Injectable({
  providedIn: 'root'
})
export class BorrowerService {
  private apiUrl = `${environment.apiUrl}/applicant`;

  constructor(private http: HttpClient) {}

  getDashboardMetrics(): Observable<DashboardMetrics> {
    return this.http.get<DashboardMetrics>(`${this.apiUrl}/dashboard`);
  }
}
