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
           [class.locked]="!isStepCompleted(i) && !isStepCurrent(i) && !isStepFailed(i)">
        
        <div class="step-circle">
          <span *ngIf="isStepCompleted(i)">✓</span>
          <span *ngIf="isStepFailed(i)">✕</span>
          <span *ngIf="!isStepCompleted(i) && !isStepFailed(i)">{{ i + 1 }}</span>
        </div>
        <div class="step-label">{{ step.label }}</div>
        
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
        background-color: var(--secondary-color, #10b981);
        border-color: var(--secondary-color, #10b981);
        color: white;
      }
      .step-label {
        color: var(--text-main, #111827);
        font-weight: 600;
      }
      .step-line {
        background-color: var(--secondary-color, #10b981);
      }
    }

    .step.active {
      .step-circle {
        border-color: var(--primary-color, #4f46e5);
        color: var(--primary-color, #4f46e5);
        border-width: 3px;
        box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
      }
      .step-label {
        color: var(--primary-color, #4f46e5);
        font-weight: 700;
      }
    }

    .step.failed {
      .step-circle {
        background-color: var(--danger, #ef4444);
        border-color: var(--danger, #ef4444);
        color: white;
      }
      .step-label {
        color: var(--danger, #ef4444);
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

  readonly STEPS: TrackerStep[] = [
    { key: 'started', label: 'Application started' },
    { key: 'draft', label: 'Draft' },
    { key: 'submitted', label: 'Submitted' },
    { key: 'kyc', label: 'KYC verification' },
    { key: 'doc', label: 'Doc verification' },
    { key: 'approval', label: 'Approval' },
    { key: 'closing', label: 'Closing' },
    { key: 'disbursement', label: 'Disbursement' },
    { key: 'emi', label: 'EMI repayment' }
  ];

  currentStepIndex: number = 0;

  ngOnChanges(): void {
    this.calculateStepIndex();
  }

  private calculateStepIndex(): void {
    switch (this.currentStatus) {
      case 'DRAFT': this.currentStepIndex = 1; break;
      case 'SUBMITTED': this.currentStepIndex = 2; break;
      case 'RISK_ASSESSMENT': 
        this.currentStepIndex = this.isDirect ? 5 : 3; 
        break;
      case 'UNDER_REVIEW': 
      case 'ESCALATED': 
        this.currentStepIndex = this.isDirect ? 5 : 4; 
        break;
      case 'APPROVED': 
      case 'FINAL_APPROVED': this.currentStepIndex = 5; break;
      case 'CLOSING': this.currentStepIndex = 6; break;
      case 'DISBURSEMENT': this.currentStepIndex = 7; break;
      case 'ACTIVE': this.currentStepIndex = 8; break;
      case 'REJECTED':
      case 'FINAL_REJECTED':
        this.currentStepIndex = 4;
        break;
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
    return index === this.currentStepIndex && !this.isRejected;
  }

  isStepFailed(index: number): boolean {
    return this.isRejected && index === this.currentStepIndex;
  }
}
