import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-metric-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="metric-card" [ngClass]="'card-' + type">
      <div class="card-icon">{{ icon }}</div>
      <div class="card-content">
        <h3>{{ title }}</h3>
        <div class="metric-value">
          <ng-container *ngIf="isCurrency; else regularValue">
            {{ value | currency:'USD' }}
          </ng-container>
          <ng-template #regularValue>
            {{ value }}
          </ng-template>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .metric-card {
      background: #fff;
      border: 1px solid #e5e7eb;
      border-radius: 12px;
      padding: 1.5rem;
      display: flex;
      align-items: center;
      gap: 1.25rem;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
      transition: transform 0.2s, box-shadow 0.2s;
    }
    
    .metric-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
    }
    
    .card-icon {
      font-size: 2.25rem;
      width: 56px;
      height: 56px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 12px;
      flex-shrink: 0;
    }
    
    .card-content h3 {
      margin: 0 0 0.25rem 0;
      font-size: 0.9rem;
      color: #6b7280;
      font-weight: 500;
    }
    
    .metric-value {
      font-size: 1.75rem;
      font-weight: 700;
      color: #111827;
      line-height: 1.2;
    }
    
    /* Specific Types */
    .card-active .card-icon {
      background: #eef2ff;
      color: #4f46e5;
    }
    
    .card-outstanding .card-icon {
      background: #ecfdf5;
      color: #059669;
    }
    
    .card-pending .card-icon {
      background: #fffbeb;
      color: #d97706;
    }
  `]
})
export class MetricCardComponent {
  @Input() title: string = '';
  @Input() value: any;
  @Input() icon: string = '';
  @Input() type: 'active' | 'outstanding' | 'pending' | 'default' = 'default';
  @Input() isCurrency: boolean = false;
}
