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
  probabilityOfDefault: number;

  // Documents
  panDocumentId: number;
  aadhaarDocumentId: number;
  incomeDocumentId: number;
  photoDocumentId: number;
  guarantorIdDocumentId: number;
  otherDocuments: any[];
}

export interface PendingKyc {
  userId: number;
  fullName: string;
  email: string;
  panNumber: string;
  aadhaarNumber: string;
  profileCreatedAt: string;
  panDocumentId: number;
  aadhaarDocumentId: number;
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

  /**
   * US20: Submit Loan Officer decision.
   */
  submitDecision(applicationNumber: string, payload: { decision: string, rejectionReason?: string, internalNotes?: string }): Observable<any> {
    return this.http.put(`${this.apiUrl}/applications/${applicationNumber}/decision`, payload);
  }

  /**
   * US21: Fetch all users pending KYC verification.
   */
  getPendingKyc(): Observable<PendingKyc[]> {
    return this.http.get<PendingKyc[]>(`${this.apiUrl}/kyc/pending`);
  }

  /**
   * US21: Submit KYC decision.
   */
  submitKycDecision(userId: number, payload: { decision: string, rejectionReason?: string }): Observable<any> {
    return this.http.put(`${this.apiUrl}/kyc/${userId}/decision`, payload);
  }

  /**
   * US22: Walk-In Account Generation
   */
  createDirectApplication(data: any): Observable<{email: string}> {
    return this.http.post<{email: string}>(`${this.apiUrl}/direct-application`, data);
  }

  submitDirectLoanApplication(email: string, formData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/direct-application/${email}/loan/submit`, formData, {
      observe: 'response',
      responseType: 'blob'
    });
  }
}
