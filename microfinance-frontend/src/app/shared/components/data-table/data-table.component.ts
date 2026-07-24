import { Component, Input, Output, EventEmitter, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TableColumn {
  key: string;
  label: string;
  format?: 'currency' | 'date' | 'badge' | 'text' | 'score' | 'rupee' | 'action';
  sortable?: boolean;
  class?: string;
  // If we need custom concatenation or nested object access (optional):
  valueGetter?: (row: any) => any;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="table-container">
      <table class="data-grid">
        <thead>
          <tr>
            <th 
              *ngFor="let col of columns" 
              (click)="onSort(col)"
              [class.sortable]="col.sortable">
              {{ col.label }}
              <span *ngIf="col.sortable && sortKey === col.key" class="sort-icon">
                {{ sortDirection === 'asc' ? '↑' : '↓' }}
              </span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let row of data" (click)="onRowClick(row)" [class.clickable]="isRowClickable">
            <td *ngFor="let col of columns" [ngClass]="col.class || ''">
              
              <!-- Regular Text -->
              <ng-container *ngIf="!col.format || col.format === 'text'">
                {{ getCellValue(row, col) }}
              </ng-container>

              <!-- Currency USD -->
              <ng-container *ngIf="col.format === 'currency'">
                {{ getCellValue(row, col) | currency:'USD' }}
              </ng-container>

              <!-- Currency INR (rupee symbol) -->
              <ng-container *ngIf="col.format === 'rupee'">
                &#8377;{{ getCellValue(row, col) | number }}
              </ng-container>

              <!-- Date -->
              <ng-container *ngIf="col.format === 'date'">
                {{ getCellValue(row, col) | date:'mediumDate' }}
              </ng-container>

              <!-- Status / Risk Badge -->
              <ng-container *ngIf="col.format === 'badge'">
                <span class="badge" [ngClass]="'badge-' + (getCellValue(row, col) | lowercase)">
                  {{ (getCellValue(row, col) || 'Unknown') | titlecase }}
                </span>
              </ng-container>

              <!-- ML Score Coloring -->
              <ng-container *ngIf="col.format === 'score'">
                <span class="font-bold" [ngClass]="getScoreClass(getCellValue(row, col))">
                  {{ getCellValue(row, col) }}
                </span>
              </ng-container>
              
              <!-- Action Button -->
              <ng-container *ngIf="col.format === 'action'">
                 <button class="btn btn-sm btn-primary" (click)="onActionClick($event, row)">Review</button>
              </ng-container>

            </td>
          </tr>
          
          <tr *ngIf="data.length === 0">
            <td [attr.colspan]="columns.length" class="text-center empty-state">
              {{ emptyMessage }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .table-container {
      background-color: var(--bg-white, #ffffff);
      border-radius: var(--border-radius-lg, 8px);
      box-shadow: var(--shadow-sm, 0 1px 2px 0 rgba(0, 0, 0, 0.05));
      border: 1px solid #e5e7eb;
      width: 100%;
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
    }

    .data-grid {
      width: 100%;
      min-width: 700px;
      border-collapse: collapse;
      text-align: left;
    }

    .data-grid thead {
      background-color: var(--bg-light, #f9fafb);
      border-bottom: 1px solid #e5e7eb;
    }

    .data-grid th {
      padding: 1rem 1.5rem;
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--text-muted, #6b7280);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .data-grid th.sortable {
      cursor: pointer;
    }
    
    .data-grid th.sortable:hover {
      background-color: #f3f4f6;
    }

    .data-grid tbody tr {
      border-bottom: 1px solid #f3f4f6;
      transition: background-color 0.2s ease;
    }

    .data-grid tbody tr.clickable:hover {
      background-color: #f9fafb;
      cursor: pointer;
    }

    .data-grid tbody tr:last-child {
      border-bottom: none;
    }

    .data-grid td {
      padding: 1.25rem 1.5rem;
      font-size: 1rem;
      color: var(--text-main, #111827);
      vertical-align: middle;
    }

    /* Badges */
    .badge {
      display: inline-flex;
      align-items: center;
      padding: 0.375rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    /* Colors derived from global theme standard */
    .badge-draft, .badge-submitted, .badge-under_review {
      background-color: rgba(79, 70, 229, 0.1);
      color: #4f46e5;
    }
    .badge-approved, .badge-final_approved, .badge-low {
      background-color: rgba(16, 185, 129, 0.1);
      color: #059669;
    }
    .badge-rejected, .badge-final_rejected, .badge-high {
      background-color: rgba(239, 68, 68, 0.1);
      color: #dc2626;
    }
    .badge-escalated, .badge-risk_assessment, .badge-medium {
      background-color: rgba(245, 158, 11, 0.1);
      color: #d97706;
    }

    .font-bold { font-weight: 700; }
    .font-medium { font-weight: 500; }
    .font-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
    
    .text-green { color: #059669; }
    .text-yellow { color: #d97706; }
    .text-red { color: #dc2626; }
    .text-indigo { color: #4f46e5; }
    
    .empty-state {
      padding: 3rem 1.5rem !important;
      color: #6b7280 !important;
      font-style: italic;
    }
  `]
})
export class DataTableComponent {
  @Input() columns: TableColumn[] = [];
  @Input() data: any[] = [];
  @Input() emptyMessage: string = 'No data available.';
  @Input() isRowClickable: boolean = false;
  
  @Output() rowClick = new EventEmitter<any>();
  @Output() actionClick = new EventEmitter<any>();
  @Output() sort = new EventEmitter<string>();

  sortKey: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  getCellValue(row: any, col: TableColumn): any {
    if (col.valueGetter) {
      return col.valueGetter(row);
    }
    return row[col.key];
  }

  onRowClick(row: any): void {
    if (this.isRowClickable) {
      this.rowClick.emit(row);
    }
  }
  
  onActionClick(event: Event, row: any): void {
    event.stopPropagation();
    this.actionClick.emit(row);
  }

  onSort(col: TableColumn): void {
    if (col.sortable) {
      if (this.sortKey === col.key) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortKey = col.key;
        this.sortDirection = 'asc';
      }
      this.sort.emit(this.sortKey);
    }
  }

  getScoreClass(score: number): string {
    if (score >= 750) return 'text-green';
    if (score >= 600) return 'text-yellow';
    return 'text-red';
  }
}
