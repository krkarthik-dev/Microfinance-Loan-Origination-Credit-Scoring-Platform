import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { BorrowerService } from '../borrower.service';
import { Subscription, interval } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { LifecycleTrackerComponent } from '../../../shared/components/lifecycle-tracker/lifecycle-tracker.component';

@Component({
  selector: 'app-loan-tracking',
  standalone: true,
  imports: [CommonModule, LifecycleTrackerComponent],
  templateUrl: './loan-tracking.component.html',
  styleUrls: ['./loan-tracking.component.scss']
})
export class LoanTrackingComponent implements OnInit, OnDestroy {
  applicationNumber: string | null = null;
  currentStatus: string = 'DRAFT';
  isRejected: boolean = false;
  isDirect: boolean = false;
  errorMessage: string = '';
  
  // US49: Actionable Resolution Panel
  officerNotes: string = '';
  correctionFile: File | null = null;
  isSubmittingCorrection: boolean = false;
  correctionSuccess: boolean = false;
  correctionError: string = '';
  private hasFetchedNotes: boolean = false;

  // US53: Granular Correction Requests
  correctionRequests: any[] = [];

  // US50: Status History
  auditTrail: any[] = [];
  historyExpanded: boolean = false;
  private previousStatus: string = '';

  // US63: Application Withdrawal
  canWithdraw: boolean = false;
  showWithdrawModal: boolean = false;
  isWithdrawing: boolean = false;
  withdrawError: string = '';

  private pollingSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private borrowerService: BorrowerService
  ) {}

  ngOnInit(): void {
    this.applicationNumber = this.route.snapshot.paramMap.get('id');
    if (!this.applicationNumber) {
      this.router.navigate(['/applicant']);
      return;
    }

    this.startPolling();
  }

  ngOnDestroy(): void {
    if (this.pollingSub) {
      this.pollingSub.unsubscribe();
    }
  }

  /**
   * AC4: Polls the backend every 3 seconds to check for asynchronous state updates.
   */
  private startPolling(): void {
    this.pollingSub = interval(3000).pipe(
      startWith(0),
      switchMap(() => this.borrowerService.getLoanStatus(this.applicationNumber!))
    ).subscribe({
      next: (res) => {
        this.currentStatus = res.status;
        this.isDirect = res.isDirect === true;
        
        const withdrawableStates = ['DRAFT', 'SUBMITTED', 'PENDING_KYC', 'UNDER_REVIEW', 'INFO_REQUESTED', 'PENDING_MANAGER_APPROVAL'];
        this.canWithdraw = withdrawableStates.includes(this.currentStatus);

        // Fetch audit trail if status changed or it's the first fetch
        if (this.currentStatus !== this.previousStatus) {
          this.previousStatus = this.currentStatus;
          this.fetchAuditTrail();
        }
        this.isDirect = res.isDirect === true;
        this.isRejected = this.currentStatus === 'REJECTED';

        if (this.currentStatus === 'INFO_REQUESTED' && !this.hasFetchedNotes) {
          this.hasFetchedNotes = true;
          this.fetchOfficerNotes();
        }

        // Reset fetch flag if status moves away from INFO_REQUESTED (e.g. after submission)
        if (this.currentStatus !== 'INFO_REQUESTED') {
          this.hasFetchedNotes = false;
          this.correctionSuccess = false;
        }

        if (this.currentStatus === 'ACTIVE_REPAYMENT' || this.isRejected || this.currentStatus === 'WITHDRAWN') {
          this.pollingSub?.unsubscribe();
        }
      },
      error: (err) => {
        console.error('Failed to poll status', err);
        this.errorMessage = 'Unable to load tracking details';
        if (this.pollingSub) {
          this.pollingSub.unsubscribe();
        }
      }
    });
  }

  goToDashboard(): void {
    this.router.navigate(['/applicant']);
  }

  fetchAuditTrail(): void {
    if (!this.applicationNumber) return;
    this.borrowerService.getLoanAuditTrail(this.applicationNumber).subscribe({
      next: (res) => {
        // Reverse chronological order for history
        this.auditTrail = res.reverse();
      },
      error: (err) => {
        console.error('Failed to fetch audit trail', err);
      }
    });
  }

  toggleHistory(): void {
    this.historyExpanded = !this.historyExpanded;
  }

  formatAction(action: string): string {
    return action.replace(/_/g, ' ');
  }

  fetchOfficerNotes(): void {
    if (!this.applicationNumber) return;
    
    // Fetch generic note first (US49)
    this.borrowerService.getOfficerInfoRequestNotes(this.applicationNumber).subscribe({
      next: (res) => {
        this.officerNotes = res.notes;
      },
      error: (err) => {
        console.error('Failed to fetch officer notes', err);
        this.officerNotes = 'Please review your application and provide the requested information.';
      }
    });

    // US53: Fetch specific granular correction requests
    this.borrowerService.getCorrectionRequests(this.applicationNumber).subscribe({
      next: (res) => {
        this.correctionRequests = res;
      },
      error: (err) => console.error('Failed to fetch correction requests', err)
    });
  }

  onFileDropped(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.correctionFile = event.dataTransfer.files[0];
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onFileSelected(event: any): void {
    if (event.target.files && event.target.files.length > 0) {
      this.correctionFile = event.target.files[0];
    }
  }

  submitCorrection(): void {
    if (!this.applicationNumber || !this.correctionFile) return;
    
    this.isSubmittingCorrection = true;
    this.correctionError = '';
    
    this.borrowerService.submitCorrectionDocuments(this.applicationNumber, this.correctionFile).subscribe({
      next: () => {
        this.isSubmittingCorrection = false;
        this.correctionSuccess = true;
        this.correctionFile = null;
        // The polling will automatically pick up the new state 'UNDER_REVIEW'
      },
      error: (err) => {
        console.error('Correction upload failed', err);
        this.isSubmittingCorrection = false;
        this.correctionError = 'Failed to submit correction. Please try again.';
      }
    });
  }

  openCorrectionWorkspace(): void {
    if (this.applicationNumber) {
      this.router.navigate(['/applicant/loan', this.applicationNumber, 'corrections']);
    }
  }

  openWithdrawModal(): void {
    this.showWithdrawModal = true;
    this.withdrawError = '';
  }

  closeWithdrawModal(): void {
    if (!this.isWithdrawing) {
      this.showWithdrawModal = false;
      this.withdrawError = '';
    }
  }

  confirmWithdraw(): void {
    if (!this.applicationNumber || !this.canWithdraw) return;
    this.isWithdrawing = true;
    this.withdrawError = '';

    this.borrowerService.withdrawApplication(this.applicationNumber).subscribe({
      next: (res) => {
        this.isWithdrawing = false;
        this.showWithdrawModal = false;
        this.currentStatus = 'WITHDRAWN';
        this.canWithdraw = false;
        if (this.pollingSub) {
          this.pollingSub.unsubscribe();
        }
        this.fetchAuditTrail();
      },
      error: (err) => {
        console.error('Failed to withdraw application', err);
        this.isWithdrawing = false;
        this.withdrawError = err.error?.error || 'Failed to withdraw application. Please try again.';
      }
    });
  }
}
