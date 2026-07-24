import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

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
  imports: [CommonModule],
  templateUrl: './disbursement-queue.component.html',
  styleUrls: ['./disbursement-queue.component.css']
})
export class DisbursementQueueComponent implements OnInit {
  queueItems: DisbursementQueueItem[] = [];
  isLoading = true;
  errorMessage = '';

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
          this.queueItems = items;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error fetching disbursement queue', err);
          this.errorMessage = 'Failed to load disbursement queue.';
          this.isLoading = false;
        }
      });
  }

  markAsDisbursed(item: DisbursementQueueItem): void {
    if (!confirm(`Are you sure you want to mark ${item.applicationNumber} as disbursed? This will activate EMI repayment for ${item.applicantName}.`)) {
      return;
    }
    
    item.status = 'PROCESSING'; // optimistic UI update
    
    this.http.post(`${environment.apiUrl}/admin/disbursements/${item.id}/disburse`, {})
      .subscribe({
        next: () => {
          this.queueItems = this.queueItems.filter(q => q.id !== item.id);
        },
        error: (err) => {
          console.error('Error disbursing loan', err);
          item.status = 'PENDING_DISBURSEMENT'; // revert
          alert('Failed to process disbursement. Please try again.');
        }
      });
  }
}
