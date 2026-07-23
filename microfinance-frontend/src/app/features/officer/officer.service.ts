import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ApplicationSummary {
  applicationNumber: string;
  applicantFirstName: string;
  applicantLastName: string;
  appliedAmount: number;
  tenureMonths: number;
  purpose: string;
  submittedAt: string;
  creditScore: number;
  riskTier: string;
}

@Injectable({
  providedIn: 'root'
})
export class OfficerService {
  private apiUrl = `${environment.apiUrl}/officer`;

  constructor(private http: HttpClient) {}

  /**
   * Fetches all applications currently in the UNDER_REVIEW state.
   */
  getQueue(): Observable<ApplicationSummary[]> {
    return this.http.get<ApplicationSummary[]>(`${this.apiUrl}/applications/queue`);
  }
}
