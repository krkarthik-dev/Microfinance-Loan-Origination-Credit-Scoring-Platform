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
        this.isRejected = this.currentStatus === 'REJECTED';

        if (this.currentStatus === 'ACTIVE_REPAYMENT' || this.isRejected || this.currentStatus === 'WITHDRAWN') {
          this.pollingSub?.unsubscribe();
        }
      },
      error: (err) => {
        console.error('Failed to poll status', err);
      }
    });
  }

  goToDashboard(): void {
    this.router.navigate(['/applicant']);
  }
}
