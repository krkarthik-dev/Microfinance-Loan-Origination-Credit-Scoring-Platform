import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { BorrowerService } from '../borrower.service';
import { Subscription, interval } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';

interface TrackerStep {
  key: string;
  label: string;
}

@Component({
  selector: 'app-loan-tracking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './loan-tracking.component.html',
  styleUrls: ['./loan-tracking.component.scss']
})
export class LoanTrackingComponent implements OnInit, OnDestroy {
  applicationNumber: string | null = null;
  currentStatus: string = 'DRAFT';
  isRejected: boolean = false;
  
  private pollingSub?: Subscription;

  // AC2: Exact sequential flow
  readonly STEPS: TrackerStep[] = [
    { key: 'started', label: 'Application started' },
    { key: 'draft', label: 'Draft' },
    { key: 'submitted', label: 'Submitted' },
    { key: 'kyc', label: 'KYC verification' },
    { key: 'doc', label: 'Doc verification' },
    { key: 'approval', label: 'Approval' },
    { key: 'closing', label: 'Closing' },
    { key: 'disbursement', label: 'Disbursement' },
    { key: 'emi', label: 'EMI repayment' }
  ];

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
        this.isRejected = (this.currentStatus === 'REJECTED' || this.currentStatus === 'FINAL_REJECTED');
        
        // Stop polling if we reach terminal states where immediate async updates stop
        if (this.currentStatus === 'UNDER_REVIEW' || this.currentStatus === 'APPROVED' || 
            this.currentStatus === 'FINAL_APPROVED' || this.isRejected) {
          // In a real app we might still poll infrequently, but here we can stop aggressive polling
          // once ML finishes and it hits the Officer's queue (UNDER_REVIEW).
          // We'll keep polling so it can progress through Approval -> Closing, etc. if triggered by officer.
          // So let's NOT unsubscribe, keep it alive.
        }
      },
      error: (err) => {
        console.error('Failed to poll status', err);
      }
    });
  }

  /**
   * Maps the backend ApplicationStatus to the index of our 9-step tracker.
   */
  get currentStepIndex(): number {
    switch (this.currentStatus) {
      case 'DRAFT': return 1;
      case 'SUBMITTED': return 2;
      case 'RISK_ASSESSMENT': return 3; // KYC Verification
      case 'UNDER_REVIEW': 
      case 'ESCALATED': return 4;       // Doc Verification
      case 'APPROVED': 
      case 'FINAL_APPROVED': return 5;  // Approval
      case 'CLOSING': return 6;         // Closing
      case 'DISBURSEMENT': return 7;    // Disbursement
      case 'ACTIVE': return 8;          // EMI Repayment
      case 'REJECTED':
      case 'FINAL_REJECTED':
        return 4; // Stop at doc verification failure
      default: return 0;
    }
  }

  isStepCompleted(index: number): boolean {
    if (this.isRejected) {
      return index < this.currentStepIndex;
    }
    return index <= this.currentStepIndex;
  }

  isStepCurrent(index: number): boolean {
    return index === this.currentStepIndex && !this.isRejected;
  }

  isStepFailed(index: number): boolean {
    return this.isRejected && index === this.currentStepIndex;
  }

  goToDashboard(): void {
    this.router.navigate(['/applicant']);
  }
}
