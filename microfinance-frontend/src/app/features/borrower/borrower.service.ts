import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoanActivity {
  loanId: string;
  requestedAmount: number;
  dateApplied: string;
  status: string;
}

export interface UserProfile {
  firstName: string;
  lastName: string;
  dateOfBirth: string; // YYYY-MM-DD
  gender: string;
  phoneNumber: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  pincode: string;
  panNumber?: string;
  aadhaarNumber?: string;
  employmentType: string;
  monthlyIncome: number;
}

export interface KycUploadResponse {
  id: number;
  documentType: string;
  fileName: string;
  fileSizeBytes: number;
  verified: boolean;
  uploadedAt: string;
}

export interface ActiveLoan {
  loanId: string;
  principalAmount: number;
  disbursedDate: string;
  currentOutstandingBalance: number;
  tenureMonths: number;
  monthlyEmi: number;
  interestRate: number;
  purpose?: string;
}

export interface EmiScheduleItem {
  id: number;
  installmentNumber: number;
  dueDate: string;
  principalComponent: number;
  interestComponent: number;
  totalEmi: number;
  remainingBalance: number;
  status: string;
  paidDate?: string;
  paymentMethod?: string;
  referenceNumber?: string;
  collectedByUsername?: string;
  isActionable?: boolean;
}

export interface RepaymentSchedule {
  loanId: string;
  borrowerName: string;
  principalAmount: number;
  interestRate: number;
  tenureMonths: number;
  nextEmiAmount: number;
  nextEmiDueDate: string;
  totalOutstandingBalance: number;
  loanStatus: string;
  schedule: EmiScheduleItem[];
}

export interface DashboardMetrics {
  activeLoans: number;
  totalOutstanding: number;
  pendingApplications: number;
  profileComplete: boolean;
  recentActivity: LoanActivity[];
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

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/profile`);
  }

  updateProfile(profile: UserProfile): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/profile`, profile);
  }

  uploadKycDocument(file: File, documentType: string): Observable<KycUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    return this.http.post<KycUploadResponse>(`${this.apiUrl}/kyc/upload`, formData);
  }

  getKycDocumentMetadata(documentType: string): Observable<KycUploadResponse> {
    return this.http.get<KycUploadResponse>(`${this.apiUrl}/kyc/${documentType}`);
  }

  /**
   * Submits a complete loan application as multipart/form-data.
   * Returns a Blob (the generated PDF) along with headers (application number).
   */
  submitLoanApplication(formData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/loan/submit`, formData, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  /**
   * Retrieves the current tracking status for a loan application.
   */
  getLoanStatus(applicationNumber: string): Observable<{ status: string, isDirect?: boolean }> {
    return this.http.get<{ status: string, isDirect?: boolean }>(`${this.apiUrl}/loan/${applicationNumber}/status`);
  }

  getOfficerInfoRequestNotes(applicationNumber: string): Observable<{notes: string}> {
    return this.http.get<{notes: string}>(`${this.apiUrl}/loan/${applicationNumber}/info-request`);
  }

  getCorrectionRequests(applicationNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/loan/${applicationNumber}/correction-requests`);
  }

  submitCorrectionDocuments(applicationNumber: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/loan/${applicationNumber}/upload-correction`, formData);
  }

  getLoanAuditTrail(applicationNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/loan/${applicationNumber}/audit-trail`);
  }

  resubmitCorrections(applicationNumber: string, formData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/loan/${applicationNumber}/resubmit-corrections`, formData);
  }

  getActiveLoans(): Observable<ActiveLoan[]> {
    return this.http.get<ActiveLoan[]>(`${this.apiUrl}/active-loans`);
  }

  getRepaymentSchedule(applicationNumber: string): Observable<RepaymentSchedule> {
    return this.http.get<RepaymentSchedule>(`${this.apiUrl}/active-loans/${applicationNumber}/repayment-schedule`);
  }

  withdrawApplication(applicationNumber: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/loan/${applicationNumber}/withdraw`, {});
  }
}
