import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OfficerService, ApplicationSummary } from '../officer.service';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-command-center',
  standalone: true,
  imports: [CommonModule],
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

  private queueSub?: Subscription;

  constructor(
    private officerService: OfficerService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadQueue();
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

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
