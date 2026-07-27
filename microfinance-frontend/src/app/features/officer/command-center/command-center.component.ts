import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { OfficerService, ApplicationSummary, PendingKyc, OfficerDisbursedLoan, PasswordResetSummary } from '../officer.service';
import { Subscription } from 'rxjs';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { TokenService } from '../../../core/services/token.service';
import { DataTableComponent, TableColumn } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-command-center',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DataTableComponent],
  templateUrl: './command-center.component.html',
  styleUrls: ['./command-center.component.scss']
})
export class CommandCenterComponent implements OnInit, OnDestroy {
  queue: ApplicationSummary[] = [];
  approvedQueue: ApplicationSummary[] = [];
  rejectedQueue: ApplicationSummary[] = [];
  isLoading = true;
  errorMessage = '';

  loanColumns: TableColumn[] = [
    { key: 'applicationNumber', label: 'Application ID', class: 'font-mono text-indigo', sortable: true },
    { key: 'applicantName', label: 'Applicant Name', class: 'font-bold', sortable: true, valueGetter: (row: any) => `${row.applicantFirstName} ${row.applicantLastName}` },
    { key: 'appliedAmount', label: 'Amount', format: 'rupee', sortable: true },
    { key: 'purpose', label: 'Purpose', sortable: true },
    { key: 'creditScore', label: 'ML Score', format: 'score', sortable: true },
    { key: 'riskTier', label: 'Risk Tier', format: 'badge', sortable: true },
    { key: 'submittedAt', label: 'Date Applied', format: 'date', sortable: true }
  ];
  
  // Sorting state
  sortColumn: keyof ApplicationSummary | 'creditScore' = 'creditScore'; 
  sortDirection: 'asc' | 'desc' = 'desc'; // Default: High Score (Low Risk) at top

  // KYC Queue
  pendingKyc: PendingKyc[] = [];
  isKycLoading = true;
  kycErrorMessage = '';

  kycColumns: TableColumn[] = [
    { key: 'fullName', label: 'Applicant Name', class: 'font-bold' },
    { key: 'email', label: 'Email' },
    { key: 'panNumber', label: 'PAN Number', class: 'font-mono' },
    { key: 'aadhaarNumber', label: 'Aadhaar Number', class: 'font-mono' },
    { key: 'profileCreatedAt', label: 'Profile Created', format: 'date' },
    { key: 'action', label: 'Action', format: 'action' }
  ];

  // Disbursed Loans Queue (US56 Officer)
  disbursedQueue: OfficerDisbursedLoan[] = [];
  isDisbursedLoading = false;
  disbursedErrorMessage = '';
  disbursedColumns: TableColumn[] = [
    { key: 'loanId', label: 'Loan ID', class: 'font-mono text-indigo', sortable: true },
    { key: 'borrowerName', label: 'Borrower Name', class: 'font-bold', sortable: true },
    { key: 'totalDisbursed', label: 'Total Disbursed', format: 'rupee', sortable: true },
    { key: 'nextEmiDueDate', label: 'Next EMI Due Date', format: 'date', sortable: true },
    { key: 'nextEmiAmount', label: 'Next EMI Amount', format: 'rupee', sortable: true },
    { key: 'status', label: 'Status', format: 'badge', sortable: true }
  ];

  // Password Resets Queue
  pendingPasswordResets: PasswordResetSummary[] = [];
  isPasswordResetsLoading = false;
  passwordResetsErrorMessage = '';
  passwordResetsSuccessMessage = '';
  passwordResetsColumns: TableColumn[] = [
    { key: 'requestId', label: 'Request ID', class: 'font-mono text-indigo font-bold', sortable: true },
    { key: 'fullName', label: 'Applicant Name', class: 'font-bold', valueGetter: (row: any) => `${row.firstName || ''} ${row.lastName || ''}`.trim() || 'N/A' },
    { key: 'email', label: 'Email' },
    { key: 'createdAt', label: 'Requested Date', format: 'date', sortable: true },
    { key: 'status', label: 'Status', format: 'badge', sortable: true },
    { key: 'tempPassword', label: 'Temp Password', class: 'font-mono font-bold text-green', valueGetter: (row: any) => row.tempPassword || '---' },
    { key: 'action', label: 'Action', format: 'action', actionLabel: 'Approve' }
  ];

  // Tabs
  activeTab: 'LOANS' | 'KYC' | 'APPROVED' | 'REJECTED' | 'DISBURSED' | 'PASSWORD_RESETS' = 'LOANS';

  private queueSub?: Subscription;
  private approvedSub?: Subscription;
  private rejectedSub?: Subscription;

  // Direct Application Modal
  showDirectApplicationModal = false;
  directAppForm: FormGroup;
  isSubmittingDirectApp = false;
  directAppError = '';
  directAppSuccess = false;

  constructor(
    private officerService: OfficerService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private tokenService: TokenService,
    private fb: FormBuilder
  ) {
    this.directAppForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      temporaryPassword: ['', Validators.required],
      dateOfBirth: ['', Validators.required],
      employmentType: ['SALARIED', Validators.required],
      monthlyIncome: ['', [Validators.required, Validators.min(0)]]
    });
  }

  ngOnInit(): void {
    this.loadQueue();
    this.loadApprovedQueue();
    this.loadRejectedQueue();
    this.loadDisbursedQueue();
    this.fetchPendingKyc();
    this.loadPasswordResetsQueue();

    // Listen to fragments for navigation commands
    this.route.fragment.subscribe(fragment => {
      if (fragment === 'create-direct-deal') {
        this.openDirectApplicationModal();
      } else if (fragment === 'pending-kyc') {
        this.activeTab = 'KYC';
      } else if (fragment === 'password-resets') {
        this.activeTab = 'PASSWORD_RESETS';
      }
    });
  }

  ngOnDestroy(): void {
    if (this.queueSub) {
      this.queueSub.unsubscribe();
    }
    if (this.approvedSub) {
      this.approvedSub.unsubscribe();
    }
    if (this.rejectedSub) {
      this.rejectedSub.unsubscribe();
    }
  }

  loadQueue(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    this.queueSub = this.officerService.getQueue().subscribe({
      next: (data) => {
        this.queue = data;
        this.sortQueue(); // Apply default ML risk sort
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load the review queue. Please try again.';
        this.isLoading = false;
        console.error(err);
      }
    });
  }

  /**
   * AC4: Manual Sorting Override
   */
  sortBy(column: string): void {
    const colKey = column as keyof ApplicationSummary;
    if (this.sortColumn === colKey) {
      // Toggle direction
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = colKey;
      this.sortDirection = 'desc'; // Default to desc on new column click
    }
    this.sortQueue();
  }

  private sortQueue(): void {
    if (!this.queue || this.queue.length === 0) return;

    this.queue.sort((a, b) => {
      let valA = a[this.sortColumn];
      let valB = b[this.sortColumn];

      // Handle nulls/undefined
      if (valA === null || valA === undefined) valA = '';
      if (valB === null || valB === undefined) valB = '';

      if (valA < valB) {
        return this.sortDirection === 'asc' ? -1 : 1;
      }
      if (valA > valB) {
        return this.sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }

  getSortIcon(column: keyof ApplicationSummary): string {
    if (this.sortColumn !== column) return '↕';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  goToReview(applicationNumber: string): void {
    this.router.navigate(['/officer/loan', applicationNumber, 'review']);
  }

  loadApprovedQueue(): void {
    this.approvedSub = this.officerService.getApprovedQueue().subscribe({
      next: (data) => {
        this.approvedQueue = data;
      },
      error: (err) => console.error('Error fetching approved queue', err)
    });
  }

  loadRejectedQueue(): void {
    this.rejectedSub = this.officerService.getRejectedQueue().subscribe({
      next: (data) => {
        this.rejectedQueue = data;
      },
      error: (err) => console.error('Error fetching rejected queue', err)
    });
  }

  loadDisbursedQueue(): void {
    this.isDisbursedLoading = true;
    this.disbursedErrorMessage = '';
    this.officerService.getDisbursedLoans().subscribe({
      next: (data) => {
        this.disbursedQueue = data;
        this.isDisbursedLoading = false;
      },
      error: (err) => {
        console.error('Failed to load disbursed queue', err);
        this.disbursedErrorMessage = 'Could not load disbursed loans.';
        this.isDisbursedLoading = false;
      }
    });
  }

  goToRepaymentManagement(loanId: string): void {
    this.router.navigate(['/officer/loan', loanId, 'repayment']);
  }

  // --- KYC QUEUE ---

  fetchPendingKyc(): void {
    this.isKycLoading = true;
    this.kycErrorMessage = '';
    
    this.officerService.getPendingKyc().subscribe({
      next: (data) => {
        this.pendingKyc = data;
        this.isKycLoading = false;
      },
      error: (err) => {
        console.error('Failed to load pending KYC', err);
        this.kycErrorMessage = 'Could not load pending KYC requests.';
        this.isKycLoading = false;
      }
    });
  }

  viewKycDetails(userId: number): void {
    // We will route to a new component for KYC review
    this.router.navigate(['/officer/kyc', userId]);
  }

  // --- Walk-in Application Logic ---
  
  openDirectApplicationModal(): void {
    this.showDirectApplicationModal = true;
    this.directAppSuccess = false;
    this.directAppError = '';
    this.directAppForm.reset({
      employmentType: 'SALARIED'
    });
  }

  closeDirectApplicationModal(): void {
    this.showDirectApplicationModal = false;
  }

  submitDirectApplication(): void {
    if (this.directAppForm.invalid) return;

    this.isSubmittingDirectApp = true;
    this.directAppError = '';
    this.directAppSuccess = false;

    this.officerService.createDirectApplication(this.directAppForm.value).subscribe({
      next: (response) => {
        this.isSubmittingDirectApp = false;
        this.directAppSuccess = true;
        setTimeout(() => {
          this.closeDirectApplicationModal();
          // Navigate to the direct application stepper with the email
          if (response && response.email) {
            this.router.navigate(['/officer/direct-application', response.email, 'apply']);
          }
        }, 1500);
      },
      error: (err) => {
        this.isSubmittingDirectApp = false;
        this.directAppError = err.error || 'Failed to create application.';
      }
    });
  }

  loadPasswordResetsQueue(): void {
    this.isPasswordResetsLoading = true;
    this.passwordResetsErrorMessage = '';
    this.officerService.getPasswordResetRequests().subscribe({
      next: (data) => {
        this.pendingPasswordResets = data;
        this.isPasswordResetsLoading = false;
      },
      error: (err) => {
        this.isPasswordResetsLoading = false;
        this.passwordResetsErrorMessage = 'Failed to load password reset requests.';
      }
    });
  }

  approvePasswordReset(row: PasswordResetSummary): void {
    if (row.status !== 'PENDING') {
      alert(`Request ${row.requestId} is already ${row.status}. Temp Password: ${row.tempPassword || 'N/A'}`);
      return;
    }
    if (!confirm(`Are you sure you want to approve password reset request ${row.requestId} for ${row.email}? This will generate a temporary password and force password change on next login.`)) {
      return;
    }
    this.officerService.approvePasswordResetRequest(row.requestId).subscribe({
      next: (res) => {
        this.passwordResetsSuccessMessage = res.message;
        this.loadPasswordResetsQueue();
      },
      error: (err) => {
        alert(err.error?.message || 'Failed to approve password reset request.');
      }
    });
  }
}
