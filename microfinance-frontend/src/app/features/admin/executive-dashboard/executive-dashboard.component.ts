import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../../shared/shared.module';
import { AdminService, AdminDashboardMetrics } from '../admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { MetricCardComponent } from '../../../shared/components/metric-card/metric-card.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-executive-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, SharedModule, MetricCardComponent, DataTableComponent],
  templateUrl: './executive-dashboard.component.html',
  styleUrls: ['./executive-dashboard.component.scss']
})
export class ExecutiveDashboardComponent implements OnInit {
  metrics: AdminDashboardMetrics | null = null;
  isLoading = true;
  errorMessage = '';

  auditColumns = [
    { key: 'createdAt', label: 'Timestamp', type: 'date' },
    { key: 'performedBy', label: 'User' },
    { key: 'action', label: 'Action' },
    { key: 'entityType', label: 'Entity' },
    { key: 'entityId', label: 'ID' }
  ];

  // Colors for risk tiers
  riskColors: { [key: string]: string } = {
    'LOW': '#10b981', // green
    'MEDIUM': '#f59e0b', // amber
    'HIGH': '#ef4444' // red
  };

  constructor(
    private adminService: AdminService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.fetchMetrics();
  }

  fetchMetrics(): void {
    this.isLoading = true;
    this.adminService.getDashboardMetrics().subscribe({
      next: (data) => {
        this.metrics = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load admin metrics', err);
        this.errorMessage = 'Could not load executive dashboard data.';
        this.isLoading = false;
      }
    });
  }

  get totalRiskCount(): number {
    if (!this.metrics?.riskDistribution) return 0;
    return Object.values(this.metrics.riskDistribution).reduce((a, b) => a + b, 0);
  }

  getRiskPercentage(count: number): number {
    const total = this.totalRiskCount;
    return total > 0 ? (count / total) * 100 : 0;
  }

  get riskTiers() {
    if (!this.metrics?.riskDistribution) return [];
    return Object.entries(this.metrics.riskDistribution).map(([tier, count]) => ({
      tier,
      count,
      percentage: this.getRiskPercentage(count as number)
    }));
  }

  logout(): void {
    this.authService.logout();
  }
}
