import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { BorrowerService, DashboardMetrics } from '../borrower.service';
import { ProfileSetupComponent } from '../profile-setup/profile-setup.component';
import { MetricCardComponent } from '../../../shared/components/metric-card/metric-card.component';
import { DataTableComponent, TableColumn } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, ProfileSetupComponent, MetricCardComponent, DataTableComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  userName = '';
  metrics: DashboardMetrics | null = null;
  isLoading = true;
  errorMessage: string = '';
  validationMessage: string = '';
  showProfilePopup: boolean = false;

  activityColumns: TableColumn[] = [
    { key: 'loanId', label: 'Loan ID', class: 'font-medium' },
    { key: 'requestedAmount', label: 'Requested Amount', format: 'currency' },
    { key: 'dateApplied', label: 'Date Applied', format: 'date' },
    { key: 'status', label: 'Current Status', format: 'badge' }
  ];

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

  openProfilePopup(): void {
    this.showProfilePopup = true;
  }

  closeProfilePopup(): void {
    this.showProfilePopup = false;
    this.fetchMetrics(); // Refresh metrics in case KYC was completed
  }

  logout(): void {
    this.authService.logout();
  }
}
