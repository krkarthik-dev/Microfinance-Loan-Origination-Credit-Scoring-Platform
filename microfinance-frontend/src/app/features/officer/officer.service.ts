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
  // ML Score Data
  creditScore: number;
  riskTier: string;

  // Documents
  panDocumentId: number;
  aadhaarDocumentId: number;
  incomeDocumentId: number;
  photoDocumentId: number;
  guarantorIdDocumentId: number;
  otherDocuments: any[];
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

  /**
   * Fetches the comprehensive application detail.
   */
  getApplicationDetails(applicationNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/applications/${applicationNumber}/details`);
  }

  /**
   * Constructs the URL for viewing a KYC document.
   */
  getKycDocumentUrl(id: number): string {
    return `${this.apiUrl}/documents/kyc/${id}`;
  }

  /**
   * Constructs the URL for viewing a Loan document.
   */
  getLoanDocumentUrl(id: number): string {
    return `${this.apiUrl}/documents/loan/${id}`;
  }
}
