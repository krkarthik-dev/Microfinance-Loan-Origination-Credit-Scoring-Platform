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
}
