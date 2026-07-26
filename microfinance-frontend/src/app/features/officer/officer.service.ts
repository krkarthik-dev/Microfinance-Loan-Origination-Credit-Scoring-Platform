import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
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

export interface OfficerDisbursedLoan {
  loanId: string;
  borrowerName: string;
  totalDisbursed: number;
  nextEmiDueDate: string;
  nextEmiAmount: number;
  status: string;
  totalOutstanding: number;
}

@Injectable({
  providedIn: 'root'
})
export class OfficerService {
  private apiUrl = `${environment.apiUrl}/officer`;
  public pendingKycCount$ = new BehaviorSubject<number>(0);

  constructor(private http: HttpClient) {}

  /**
   * Fetches all applications currently in the UNDER_REVIEW state.
   */
  getQueue(): Observable<ApplicationSummary[]> {
    return this.http.get<ApplicationSummary[]>(`${this.apiUrl}/applications/queue`);
  }

  /**
   * Fetches approved applications
   */
  getApprovedQueue(): Observable<ApplicationSummary[]> {
    return this.http.get<ApplicationSummary[]>(`${this.apiUrl}/applications/approved`);
  }

  /**
   * Fetches rejected applications
   */
  getRejectedQueue(): Observable<ApplicationSummary[]> {
    return this.http.get<ApplicationSummary[]>(`${this.apiUrl}/applications/rejected`);
  }

  /**
   * Fetches the comprehensive application detail.
   */
  getApplicationDetails(applicationNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/applications/${applicationNumber}/details`);
  }

  /**
   * Fetches a KYC document as a Blob (required for JWT auth in iframes)
   */
  getKycDocumentBlob(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/documents/kyc/${id}`, { responseType: 'blob' });
  }

  /**
   * Fetches a Loan document as a Blob
   */
  getLoanDocumentBlob(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/documents/loan/${id}`, { responseType: 'blob' });
  }

  /**
   * US20: Submit Loan Officer decision.
   */
  submitDecision(applicationNumber: string, payload: { decision: string, rejectionReason?: string, internalNotes?: string, correctionRequests?: any[] }): Observable<any> {
    return this.http.put(`${this.apiUrl}/applications/${applicationNumber}/decision`, payload);
  }

  /**
   * US21: Fetch all users pending KYC verification.
   */
  getPendingKyc(): Observable<PendingKyc[]> {
    return this.http.get<PendingKyc[]>(`${this.apiUrl}/kyc/pending`).pipe(
      tap(data => this.pendingKycCount$.next(data.length))
    );
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

  /**
   * Initiates disbursement for an approved application
   */
  initiateDisbursement(applicationNumber: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/applications/${applicationNumber}/disburse`, {});
  }

  /**
   * Fetches the audit trail for an application
   */
  getAuditTrail(applicationNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/applications/${applicationNumber}/audit-trail`);
  }

  getDisbursedLoans(): Observable<OfficerDisbursedLoan[]> {
    return this.http.get<OfficerDisbursedLoan[]>(`${this.apiUrl}/disbursed-loans`);
  }

  getOfficerRepaymentSchedule(applicationNumber: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/loans/${applicationNumber}/repayment-schedule`);
  }

  collectInstallmentPayment(applicationNumber: string, installmentId: number, payload: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/loans/${applicationNumber}/installments/${installmentId}/pay`, payload);
  }
}
