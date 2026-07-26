import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { BorrowerService, ActiveLoan } from '../borrower.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-active-loans',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './active-loans.component.html',
  styleUrls: ['./active-loans.component.scss']
})
export class ActiveLoansComponent implements OnInit, OnDestroy {
  activeLoans: ActiveLoan[] = [];
  isLoading = true;
  errorMessage = '';
  private sub?: Subscription;

  constructor(
    private borrowerService: BorrowerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.fetchActiveLoans();
  }

  ngOnDestroy(): void {
    if (this.sub) {
      this.sub.unsubscribe();
    }
  }

  fetchActiveLoans(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.sub = this.borrowerService.getActiveLoans().subscribe({
      next: (loans) => {
        this.activeLoans = loans;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load active loans', err);
        this.errorMessage = 'Could not retrieve active loans at this time. Please try again later.';
        this.isLoading = false;
      }
    });
  }

  goToTracking(loanId: string): void {
    this.router.navigate(['/applicant/loan', loanId, 'tracking']);
  }

  goToRepayment(loanId: string): void {
    this.router.navigate(['/applicant/active-loans', loanId]);
  }

  goToDashboard(): void {
    this.router.navigate(['/applicant']);
  }
}
