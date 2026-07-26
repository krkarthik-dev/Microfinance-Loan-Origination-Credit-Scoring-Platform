import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

interface TrackerStep {
  key: string;
  label: string;
}

@Component({
  selector: 'app-lifecycle-tracker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stepper-wrapper">
      <div class="step" *ngFor="let step of STEPS; let i = index"
           [class.completed]="isStepCompleted(i)"
           [class.active]="isStepCurrent(i)"
           [class.failed]="isStepFailed(i)"
           [class.warning]="isStepWarning(i)"
           [class.terminal]="isStepTerminal(i)"
           [class.locked]="!isStepCompleted(i) && !isStepCurrent(i) && !isStepFailed(i) && !isStepWarning(i) && !isStepTerminal(i)">
        
        <div class="step-circle">
          <span *ngIf="isStepCompleted(i)">✓</span>
          <span *ngIf="isStepFailed(i)">✕</span>
          <span *ngIf="isStepWarning(i)">!</span>
          <span *ngIf="isStepTerminal(i)">—</span>
          <span *ngIf="!isStepCompleted(i) && !isStepFailed(i) && !isStepWarning(i) && !isStepTerminal(i)">{{ i + 1 }}</span>
        </div>
        <div class="step-label">{{ step.label }}</div>
        
        <!-- AC2: Tracker Node Timestamps / AC4: Pending Estimates -->
        <div class="step-meta" *ngIf="isStepCompleted(i)">
          {{ getStepTimestamp(step.key) | date:'MMM d, h:mm a' }}
        </div>
        <div class="step-meta pending-sla" *ngIf="isStepCurrent(i) && !isStepWarning(i)">
          Typically takes 1-2 business days
        </div>
        
        <!-- Connecting Line -->
        <div class="step-line" *ngIf="i < STEPS.length - 1"></div>
      </div>
    </div>
  `,
  styles: [`
    .stepper-wrapper {
      display: flex;
      justify-content: space-between;
      margin-top: 2rem;
      position: relative;
    }

    .step {
      display: flex;
      flex-direction: column;
      align-items: center;
      position: relative;
      flex: 1;
      
      &:last-child {
        flex: 0;
      }
    }

    .step-circle {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background-color: var(--bg-white, #fff);
      border: 2px solid #d1d5db;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      color: #9ca3af;
      z-index: 2;
      transition: all 0.3s ease;
    }

    .step-label {
      margin-top: 1rem;
      font-size: 0.875rem;
      font-weight: 500;
      color: #6b7280;
      text-align: center;
      max-width: 100px;
    }

    .step-meta {
      margin-top: 0.25rem;
      font-size: 0.75rem;
      color: #9ca3af;
      text-align: center;
      max-width: 110px;
      line-height: 1.2;
    }
    
    .step-meta.pending-sla {
      color: var(--primary-color);
      font-style: italic;
    }

    .step-line {
      position: absolute;
      top: 20px;
      left: calc(50% + 20px);
      width: calc(100% - 40px);
      height: 3px;
      background-color: #e5e7eb;
      z-index: 1;
      transition: all 0.3s ease;
    }

    /* States */
    .step.completed {
      .step-circle {
        background-color: var(--secondary-color);
        border-color: var(--secondary-color);
        color: white;
        box-shadow: 0 0 10px rgba(16, 185, 129, 0.4);
      }
      .step-label {
        color: var(--text-main);
        font-weight: 700;
      }
      .step-line {
        background-color: var(--secondary-color);
      }
    }

    .step.active {
      .step-circle {
        border-color: var(--primary-color);
        color: var(--primary-color);
        border-width: 3px;
        background-color: var(--bg-white);
        animation: pulse-ring 2s infinite cubic-bezier(0.4, 0, 0.2, 1);
      }
      .step-label {
        color: var(--primary-color);
        font-weight: 800;
      }
    }

    .step.failed {
      .step-circle {
        background-color: var(--danger);
        border-color: var(--danger);
        color: white;
        box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);
      }
      .step-label {
        color: var(--danger);
        font-weight: 800;
      }
    }

    .step.warning {
      .step-circle {
        background-color: var(--warning);
        border-color: var(--warning);
        color: white;
        box-shadow: 0 0 10px rgba(245, 158, 11, 0.4);
      }
      .step-label {
        color: var(--warning);
        font-weight: 800;
      }
    }

    .step.terminal {
      .step-circle {
        background-color: var(--text-muted);
        border-color: var(--text-muted);
        color: white;
      }
      .step-label {
        color: var(--text-muted);
        font-weight: 700;
      }
    }

    /* ── RESPONSIVE ────────────────────────────────────────────────────────── */
    @media (max-width: 768px) {
      .stepper-wrapper {
        flex-direction: column;
        align-items: flex-start;
        padding-left: 1rem;
      }

      .step {
        flex-direction: row;
        width: 100%;
        margin-bottom: 2.5rem;
        justify-content: flex-start;
        
        &:last-child {
          margin-bottom: 0;
        }
      }

      .step-line {
        top: 40px;
        left: 19px;
        width: 3px;
        height: calc(100% + 2.5rem - 40px);
      }

      .step-label {
        margin-top: 0;
        margin-left: 1.5rem;
        text-align: left;
        max-width: none;
        padding-top: 0.5rem; // Align nicely with circle
      }
    }
  `]
})
export class LifecycleTrackerComponent implements OnChanges {
  @Input() currentStatus: string = 'DRAFT';
  @Input() isRejected: boolean = false;
  @Input() isDirect: boolean = false;
  @Input() auditTrail: any[] = [];

  readonly STEPS: TrackerStep[] = [
    { key: 'draft', label: 'Draft' },
    { key: 'submitted', label: 'Submitted' },
    { key: 'kyc', label: 'KYC' },
    { key: 'review', label: 'Under Review' },
    { key: 'approval', label: 'Approval' },
    { key: 'closing', label: 'Closing' },
    { key: 'repayment', label: 'Repayment' }
  ];

  currentStepIndex: number = 0;

  ngOnChanges(): void {
    this.calculateStepIndex();
  }

  private calculateStepIndex(): void {
    switch (this.currentStatus) {
      case 'DRAFT': this.currentStepIndex = 0; break;
      case 'SUBMITTED': this.currentStepIndex = 1; break;
      case 'PENDING_KYC': this.currentStepIndex = 2; break;
      case 'UNDER_REVIEW':
      case 'INFO_REQUESTED': this.currentStepIndex = 3; break;
      case 'PENDING_MANAGER_APPROVAL':
      case 'APPROVED': this.currentStepIndex = 4; break;
      case 'CLOSING': this.currentStepIndex = 5; break;
      case 'ACTIVE_REPAYMENT': this.currentStepIndex = 6; break;
      case 'REJECTED':
      case 'WITHDRAWN': this.currentStepIndex = 3; break;
      default: this.currentStepIndex = 0; break;
    }
  }

  isStepCompleted(index: number): boolean {
    if (this.isRejected) {
      return index < this.currentStepIndex;
    }
    return index <= this.currentStepIndex;
  }

  isStepCurrent(index: number): boolean {
    return index === this.currentStepIndex && !this.isRejected &&
      this.currentStatus !== 'INFO_REQUESTED' && this.currentStatus !== 'WITHDRAWN';
  }

  isStepFailed(index: number): boolean {
    return this.isRejected && index === this.currentStepIndex;
  }

  isStepWarning(index: number): boolean {
    return this.currentStatus === 'INFO_REQUESTED' && index === this.currentStepIndex;
  }

  isStepTerminal(index: number): boolean {
    return this.currentStatus === 'WITHDRAWN' && index === this.currentStepIndex;
  }

  getStepTimestamp(stepKey: string): string | null {
    if (!this.auditTrail || this.auditTrail.length === 0) return null;
    
    // Mapping frontend tracker keys to backend ApplicationStatus names
    let backendAction = '';
    switch (stepKey) {
      case 'draft': return null; // Created date is usually available, but we track from Submitted
      case 'submitted': backendAction = 'SUBMITTED'; break;
      case 'kyc': backendAction = 'PENDING_KYC'; break;
      case 'review': backendAction = 'UNDER_REVIEW'; break;
      case 'approval': backendAction = 'PENDING_MANAGER_APPROVAL'; break;
      case 'closing': backendAction = 'CLOSING'; break;
      case 'repayment': backendAction = 'ACTIVE_REPAYMENT'; break;
      default: return null;
    }

    // Find the first instance this state was entered
    const log = this.auditTrail.find(a => a.action === backendAction || a.newValue === backendAction);
    return log ? log.createdAt : null;
  }
}
