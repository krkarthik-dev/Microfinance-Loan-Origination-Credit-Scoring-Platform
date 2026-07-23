import { Component, OnInit } from '@angular/core';
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

  constructor(
    private authService: AuthService,
    private borrowerService: BorrowerService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    // Use part of email as fallback for name if user details don't have a specific name
    this.userName = user?.email?.split('@')[0] || 'Borrower';
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
}
