import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { OfficerService } from '../officer.service';
import { AuthService } from '../../../core/services/auth.service';
import { Subscription } from 'rxjs';
import { TokenService } from '../../../core/services/token.service';
import { DocumentViewerComponent } from '../../../shared/components/document-viewer/document-viewer.component';

@Component({
  selector: 'app-application-review',
  standalone: true,
  imports: [CommonModule, FormsModule, DocumentViewerComponent],
  templateUrl: './application-review.component.html',
  styleUrls: ['./application-review.component.scss']
})
export class ApplicationReviewComponent implements OnInit, OnDestroy {
  applicationNumber: string = '';
  details: any = null;
  isLoading = true;
  errorMessage = '';

  private sub?: Subscription;
  isAdmin: boolean = false;

  // Verification state (Toggle between 'Approve' and 'Review')
  // true = Approved, false = Pending Review
  verifications = {
    guarantor: false,
    panCard: false,
    aadhaarCard: false,
    incomeCert: false,
    guarantorId: false
  };

  // Document Viewer state
  activeDocumentUrl: string = '';
  activeDocumentTitle: string = 'Select a document to view';
  profilePhotoUrl: string = '';
  auditLogs: any[] = [];
  officerJustification = '';

  // Modal States
  activeModal: 'APPROVE' | 'REJECT' | 'ESCALATE' | 'REQUEST_INFO' | 'RECOMMEND_APPROVAL' | null = null;
  rejectionReason: string = '';
  internalNotes: string = '';
  isSubmitting = false;
  validationError: string = '';

  // US53: Targeted Information Request Configuration
  correctionChecklist = [
    { section: 'Profile Details', selected: false, comments: '' },
    { section: 'Guarantor Details', selected: false, comments: '' },
    { section: 'Loan Configuration', selected: false, comments: '' },
    { section: 'PAN Card', selected: false, comments: '' },
    { section: 'Aadhaar Card', selected: false, comments: '' },
    { section: 'Income Certificate', selected: false, comments: '' },
    { section: 'Photograph', selected: false, comments: '' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private officerService: OfficerService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.applicationNumber = this.route.snapshot.paramMap.get('id') || '';
    if (!this.applicationNumber) {
      this.router.navigate(['/officer']);
      return;
    }
    this.isAdmin = this.authService.getRole() === 'ROLE_ADMIN';
    this.loadDetails();
  }

  ngOnDestroy(): void {
    if (this.sub) this.sub.unsubscribe();
  }

  get isKycPending(): boolean {
    if (!this.details) return false;
    return !this.details.kycVerified && (this.details.status === 'PENDING_KYC' || this.details.kycStatus === 'PENDING');
  }

  isApprovingKyc = false;

  onApproveKycGateway(): void {
    if (!this.details?.applicantId) return;
    this.isApprovingKyc = true;
    this.officerService.submitKycDecision(this.details.applicantId, { decision: 'APPROVE' }).subscribe({
      next: () => {
        this.isApprovingKyc = false;
        alert('KYC Verified successfully. Transitioning to Underwriting Screen.');
        this.loadDetails();
      },
      error: (err) => {
        console.error('Failed to approve KYC', err);
        this.isApprovingKyc = false;
        alert('Failed to verify KYC. Please try again.');
      }
    });
  }

  get requiresDualApproval(): boolean {
    if (!this.details) return false;
    const isHighAmount = this.details.appliedAmount > 1000000;
    const isHighRisk = this.details.creditScore && this.details.creditScore.riskTier === 'HIGH';
    return isHighAmount || isHighRisk;
  }

  loadAuditHistory(): void {
    this.officerService.getAuditTrail(this.applicationNumber).subscribe({
      next: (logs) => {
        this.auditLogs = logs;
        const recommendLog = logs.reverse().find((log: any) => 
          log.action === 'PENDING_MANAGER_APPROVAL' && log.newValue.includes('RECOMMEND_APPROVAL')
        );
        if (recommendLog) {
          try {
            const parsed = JSON.parse(recommendLog.newValue);
            this.officerJustification = parsed.details || '';
          } catch (e) {
            this.officerJustification = recommendLog.newValue;
          }
        }
      },
      error: (err) => console.error('Failed to load audit trail', err)
    });
  }

  loadDetails(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.sub = this.officerService.getApplicationDetails(this.applicationNumber).subscribe({
      next: (data) => {
        this.details = data;
        this.isLoading = false;
        if (this.details?.photoDocumentId) {
          this.loadProfilePhoto(this.details.photoDocumentId);
        }
        if (this.isAdmin && this.details.status === 'PENDING_MANAGER_APPROVAL') {
          this.loadAuditHistory();
        }
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Failed to load application details.';
        this.isLoading = false;
      }
    });
  }

  isSectionUpdated(sectionName: string): boolean {
    if (!this.details || !this.details.recentlyCorrectedSections) return false;
    return this.details.recentlyCorrectedSections.some((s: string) => s.toLowerCase() === sectionName.toLowerCase());
  }

  loadProfilePhoto(id: number): void {
    this.officerService.getLoanDocumentBlob(id).subscribe({
      next: (blob: Blob) => {
        this.profilePhotoUrl = URL.createObjectURL(blob);
      },
      error: (err) => {
        console.error('Failed to load profile photo', err);
      }
    });
  }

  toggleVerification(section: keyof typeof this.verifications): void {
    this.verifications[section] = !this.verifications[section];
  }

  get allVerified(): boolean {
    return Object.values(this.verifications).every(v => v === true);
  }

  // ML Score Visual Helpers
  // Normalizing 300 to 900 as 0% to 100%
  getScorePercentage(score: number): number {
    if (!score) return 0;
    const min = 300;
    const max = 900;
    const normalized = Math.max(0, Math.min(100, ((score - min) / (max - min)) * 100));
    return normalized;
  }

  getScoreColor(score: number): string {
    if (!score) return '#e5e7eb'; // gray
    if (score >= 750) return '#10b981'; // green
    if (score >= 600) return '#f59e0b'; // yellow/orange
    return '#ef4444'; // red
  }

  viewKycDocument(id: number, title: string): void {
    if (!id) return;
    this.activeDocumentTitle = 'Loading...';
    
    if (this.activeDocumentUrl && this.activeDocumentUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.activeDocumentUrl);
    }

    this.officerService.getKycDocumentBlob(id).subscribe({
      next: (blob: Blob) => {
        this.activeDocumentUrl = URL.createObjectURL(blob);
        this.activeDocumentTitle = title;
      },
      error: (err) => {
        console.error('Failed to load document', err);
        this.activeDocumentTitle = 'Failed to load document';
        this.activeDocumentUrl = '';
      }
    });
  }

  viewLoanDocument(id: number, title: string): void {
    if (!id) return;
    this.activeDocumentTitle = 'Loading...';
    
    if (this.activeDocumentUrl && this.activeDocumentUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.activeDocumentUrl);
    }

    this.officerService.getLoanDocumentBlob(id).subscribe({
      next: (blob: Blob) => {
        this.activeDocumentUrl = URL.createObjectURL(blob);
        this.activeDocumentTitle = title;
      },
      error: (err) => {
        console.error('Failed to load document', err);
        this.activeDocumentTitle = 'Failed to load document';
        this.activeDocumentUrl = '';
      }
    });
  }

  closeDocument(): void {
    if (this.activeDocumentUrl && this.activeDocumentUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.activeDocumentUrl);
    }
    this.activeDocumentUrl = '';
    this.activeDocumentTitle = 'Select a document to view';
  }

  // Underwriting Actions
  openModal(type: 'APPROVE' | 'REJECT' | 'ESCALATE' | 'REQUEST_INFO' | 'RECOMMEND_APPROVAL'): void {
    if (type === 'APPROVE' && !this.allVerified) {
      this.validationError = 'Please Review/Approve all pending documents and guarantor details before approving the application.';
      // auto-clear the error after 5 seconds
      setTimeout(() => this.validationError = '', 5000);
      return;
    }
    
    this.validationError = '';
    this.activeModal = type;
    this.rejectionReason = '';
    this.internalNotes = '';
    
    if (type === 'REQUEST_INFO') {
      this.correctionChecklist.forEach(item => {
        item.selected = false;
        item.comments = '';
      });
    }
  }

  closeModal(): void {
    this.activeModal = null;
  }

  submitDecision(): void {
    if (this.activeModal === 'REJECT' && !this.rejectionReason) {
      alert('Please select a rejection reason.');
      return;
    }
    if (this.activeModal === 'ESCALATE' && !this.internalNotes.trim()) {
      alert('Please provide internal notes for escalation.');
      return;
    }
    if ((this.activeModal === 'RECOMMEND_APPROVAL' || (this.isAdmin && this.activeModal === 'APPROVE')) && !this.internalNotes.trim()) {
      alert('Please provide an approval justification / executive sign-off notes.');
      return;
    }

    let selectedCorrections: any[] = [];
    if (this.activeModal === 'REQUEST_INFO') {
      selectedCorrections = this.correctionChecklist.filter(c => c.selected);
      if (selectedCorrections.length === 0) {
        alert('Please select at least one section or document for correction.');
        return;
      }
      for (const item of selectedCorrections) {
        if (!item.comments.trim()) {
          alert(`Please provide comments for the selected section: ${item.section}`);
          return;
        }
      }
    }

    this.isSubmitting = true;
    
    const payload: any = {
      decision: this.activeModal!,
      rejectionReason: this.activeModal === 'REJECT' ? this.rejectionReason : undefined,
      internalNotes: (this.activeModal === 'ESCALATE' || this.activeModal === 'RECOMMEND_APPROVAL' || (this.isAdmin && this.activeModal === 'APPROVE')) ? this.internalNotes : undefined
    };

    if (this.activeModal === 'REQUEST_INFO') {
      payload.correctionRequests = selectedCorrections.map(c => ({
        section: c.section,
        comments: c.comments
      }));
      // Just put something for internalNotes since the backend audit log parses it.
      payload.internalNotes = "Requested granular corrections";
    }

    this.officerService.submitDecision(this.applicationNumber, payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.closeModal();
        alert(`Application successfully ${payload.decision.toLowerCase()}d.`);
        this.router.navigate(['/officer']);
      },
      error: (err) => {
        console.error(err);
        this.isSubmitting = false;
        alert('Failed to submit decision. Please try again.');
      }
    });
  }

  goBack(): void {
    if (this.isAdmin) {
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.router.navigate(['/officer']);
    }
  }

  initiateDisbursement(): void {
    if (!confirm('Are you sure you want to disburse this loan? This action cannot be undone.')) {
      return;
    }

    this.isSubmitting = true;
    this.officerService.initiateDisbursement(this.applicationNumber).subscribe({
      next: () => {
        alert('Loan successfully disbursed!');
        this.router.navigate(['/officer']);
      },
      error: (err) => {
        console.error('Failed to disburse', err);
        alert(err.error || 'Failed to initiate disbursement.');
        this.isSubmitting = false;
      }
    });
  }
}
