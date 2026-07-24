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
    profile: false,
    loanInfo: false,
    guarantor: false,
    panCard: false,
    aadhaarCard: false,
    incomeCert: false,
    photo: false,
    guarantorId: false,
    mlData: false
  };

  // Document Viewer state
  activeDocumentUrl: string = '';
  activeDocumentTitle: string = 'Select a document to view';

  // Modal States
  activeModal: 'APPROVE' | 'REJECT' | 'ESCALATE' | 'REQUEST_INFO' | null = null;
  rejectionReason: string = '';
  internalNotes: string = '';
  isSubmitting = false;

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

  loadDetails(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.sub = this.officerService.getApplicationDetails(this.applicationNumber).subscribe({
      next: (data) => {
        this.details = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Failed to load application details.';
        this.isLoading = false;
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

  // Underwriting Actions
  openModal(type: 'APPROVE' | 'REJECT' | 'ESCALATE' | 'REQUEST_INFO'): void {
    this.activeModal = type;
    this.rejectionReason = '';
    this.internalNotes = '';
  }

  closeModal(): void {
    this.activeModal = null;
  }

  submitDecision(): void {
    if (this.activeModal === 'REJECT' && !this.rejectionReason) {
      alert('Please select a rejection reason.');
      return;
    }
    if ((this.activeModal === 'ESCALATE' || this.activeModal === 'REQUEST_INFO') && !this.internalNotes.trim()) {
      alert(this.activeModal === 'REQUEST_INFO' ? 'Please specify the documents needed.' : 'Please provide internal notes for escalation.');
      return;
    }

    this.isSubmitting = true;
    
    const payload = {
      decision: this.activeModal!,
      rejectionReason: this.activeModal === 'REJECT' ? this.rejectionReason : undefined,
      internalNotes: (this.activeModal === 'ESCALATE' || this.activeModal === 'REQUEST_INFO') ? this.internalNotes : undefined
    };

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
}
