import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { OfficerService, PendingKyc } from '../officer.service';

@Component({
  selector: 'app-kyc-review',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './kyc-review.component.html',
  styleUrls: ['./kyc-review.component.scss']
})
export class KycReviewComponent implements OnInit {
  userId!: number;
  details: PendingKyc | null = null;
  isLoading = true;
  errorMessage = '';

  activeDocumentUrl: SafeResourceUrl | null = null;
  activeDocumentTitle: string = 'Select a document to view';

  // Modal States
  activeModal: 'APPROVE' | 'REJECT' | null = null;
  rejectionReason: string = '';
  isSubmitting = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private officerService: OfficerService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.userId = +idParam;
      this.fetchDetails();
    } else {
      this.errorMessage = 'Invalid User ID';
      this.isLoading = false;
    }
  }

  fetchDetails(): void {
    this.officerService.getPendingKyc().subscribe({
      next: (list) => {
        const found = list.find(k => k.userId === this.userId);
        if (found) {
          this.details = found;
          // Auto load PAN document
          this.viewDocument(found.panDocumentId, 'PAN Card');
        } else {
          this.errorMessage = 'KYC details not found or already verified.';
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Failed to load details.';
        this.isLoading = false;
      }
    });
  }

  viewDocument(id: number, title: string): void {
    if (!id) return;
    const url = this.officerService.getKycDocumentUrl(id);
    this.activeDocumentUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    this.activeDocumentTitle = title;
  }

  openModal(type: 'APPROVE' | 'REJECT'): void {
    this.activeModal = type;
    this.rejectionReason = '';
  }

  closeModal(): void {
    this.activeModal = null;
  }

  submitDecision(): void {
    if (this.activeModal === 'REJECT' && !this.rejectionReason) {
      alert('Please select a rejection reason.');
      return;
    }

    this.isSubmitting = true;
    
    const payload = {
      decision: this.activeModal!,
      rejectionReason: this.activeModal === 'REJECT' ? this.rejectionReason : undefined
    };

    this.officerService.submitKycDecision(this.userId, payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.closeModal();
        alert(`KYC successfully ${payload.decision.toLowerCase()}d.`);
        this.router.navigate(['/officer']);
      },
      error: (err) => {
        console.error(err);
        this.isSubmitting = false;
        alert('Failed to submit decision.');
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/officer']);
  }
}
