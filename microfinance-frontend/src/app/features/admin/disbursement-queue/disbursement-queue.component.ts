import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { DataTableComponent, TableColumn } from '../../../shared/components/data-table/data-table.component';

interface DisbursementQueueItem {
  id: number;
  applicationId: number;
  applicationNumber: string;
  applicantName: string;
  approvedAmount: number;
  status: string;
  queuedAt: string;
}

@Component({
  selector: 'app-disbursement-queue',
  standalone: true,
  imports: [CommonModule, RouterModule, DataTableComponent],
  templateUrl: './disbursement-queue.component.html',
  styleUrls: ['./disbursement-queue.component.scss']
})
export class DisbursementQueueComponent implements OnInit {
  queueItems: any[] = [];
  isLoading = true;
  errorMessage = '';

  columns: TableColumn[] = [
    { key: 'applicationNumber', label: 'App Number', class: 'font-medium font-mono' },
    { key: 'applicantName', label: 'Applicant' },
    { key: 'approvedAmount', label: 'Approved Amount', format: 'rupee' },
    { key: 'queuedAt', label: 'Queued Date', format: 'date' },
    { key: 'status', label: 'Status', format: 'badge' },
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadQueue();
  }

  loadQueue(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.http.get<DisbursementQueueItem[]>(`${environment.apiUrl}/admin/disbursements/queue`)
      .subscribe({
        next: (items) => {
          this.queueItems = items.map(item => ({
            ...item,
            status: item.status.replace('_', ' '),
            _raw: item
          }));
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error fetching disbursement queue', err);
          this.errorMessage = 'Failed to load disbursement queue.';
          this.isLoading = false;
        }
      });
  }

  onActionClick(item: any): void {
    const raw = item._raw || item;
    if (!confirm(`Are you sure you want to mark ${raw.applicationNumber} as disbursed? This will activate EMI repayment for ${raw.applicantName}.`)) {
      return;
    }

    this.http.post(`${environment.apiUrl}/admin/disbursements/${raw.id}/disburse`, {})
      .subscribe({
        next: () => {
          this.loadQueue();
        },
        error: (err) => {
          console.error('Error disbursing loan', err);
          alert('Failed to process disbursement. Please try again.');
        }
      });
  }
}
