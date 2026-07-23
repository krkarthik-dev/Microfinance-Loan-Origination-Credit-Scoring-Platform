import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { OfficerService, ApplicationSummary, PendingKyc } from '../officer.service';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { TokenService } from '../../../core/services/token.service';

@Component({
  selector: 'app-command-center',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './command-center.component.html',
  styleUrls: ['./command-center.component.scss']
})
export class CommandCenterComponent implements OnInit, OnDestroy {
  queue: ApplicationSummary[] = [];
  isLoading = true;
  errorMessage = '';
  
  // Sorting state
  sortColumn: keyof ApplicationSummary | 'creditScore' = 'creditScore'; 
  sortDirection: 'asc' | 'desc' = 'desc'; // Default: High Score (Low Risk) at top

  // KYC Queue
  pendingKyc: PendingKyc[] = [];
  isKycLoading = true;
  kycErrorMessage = '';

  // Tabs
  activeTab: 'LOANS' | 'KYC' = 'LOANS';

  private queueSub?: Subscription;

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
    this.fetchPendingKyc();
  }

  ngOnDestroy(): void {
    if (this.queueSub) {
      this.queueSub.unsubscribe();
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
  sortBy(column: keyof ApplicationSummary): void {
    if (this.sortColumn === column) {
      // Toggle direction
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
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

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
