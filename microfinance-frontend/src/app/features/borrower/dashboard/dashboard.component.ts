import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { BorrowerService, DashboardMetrics } from '../borrower.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  userName = '';
  metrics: DashboardMetrics | null = null;
  isLoading = true;
  errorMessage = '';
  validationMessage = '';

  constructor(
    private authService: AuthService,
    private borrowerService: BorrowerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    // Use part of email (sub) as fallback for name if user details don't have a specific name
    this.userName = user?.sub?.split('@')[0] || 'Borrower';
    this.fetchMetrics();
  }

  fetchMetrics(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    this.borrowerService.getDashboardMetrics().subscribe({
      next: (data) => {
        this.metrics = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load metrics', err);
        this.errorMessage = 'Data temporarily unavailable';
        this.isLoading = false;
      }
    });
  }

  onApplyClick(): void {
    if (this.metrics && !this.metrics.profileComplete) {
      this.validationMessage = 'Please complete your KYC Profile (PAN & Aadhaar) before applying for a new loan.';
      // Auto-hide the message after 5 seconds
      setTimeout(() => {
        this.validationMessage = '';
      }, 5000);
      return;
    }
    
    this.router.navigate(['/applicant/apply']);
  }
}
