import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BorrowerService, RepaymentSchedule } from '../borrower.service';

@Component({
  selector: 'app-active-loan-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './active-loan-detail.component.html',
  styleUrls: ['./active-loan-detail.component.scss']
})
export class ActiveLoanDetailComponent implements OnInit {
  loanId: string = '';
  scheduleData: RepaymentSchedule | null = null;
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private borrowerService: BorrowerService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('loanId');
      if (id) {
        this.loanId = id;
        this.fetchSchedule(id);
      } else {
        this.handleError('No Loan ID provided in the route.');
      }
    });
  }

  fetchSchedule(id: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.borrowerService.getRepaymentSchedule(id).subscribe({
      next: (data: any) => {
        this.scheduleData = data;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error fetching repayment schedule:', err);
        this.isLoading = false;
        if (err.status === 403) {
          this.errorMessage = 'Access Denied: You do not have permission to view this loan account.';
        } else if (err.status === 400 || err.status === 404) {
          this.errorMessage = err.error || 'This loan is not currently in active repayment.';
        } else {
          this.errorMessage = 'Unable to load repayment details at this time. Please try again later.';
        }
      }
    });
  }

  handleError(msg: string): void {
    this.isLoading = false;
    this.errorMessage = msg;
  }

  isDueSoonOrOverdue(dueDateStr: string): boolean {
    if (!dueDateStr) return false;
    const dueDate = new Date(dueDateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diffTime = dueDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    // Due within 3 days or already passed
    return diffDays <= 3;
  }

  goBack(): void {
    this.router.navigate(['/applicant/active-loans']);
  }
}
