import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { OfficerService } from '../officer.service';
import { Subscription } from 'rxjs';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-application-review',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './application-review.component.html',
  styleUrls: ['./application-review.component.scss']
})
export class ApplicationReviewComponent implements OnInit, OnDestroy {
  applicationNumber: string = '';
  details: any = null;
  isLoading = true;
  errorMessage = '';

  private sub?: Subscription;

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
  activeDocumentUrl: SafeResourceUrl | null = null;
  activeDocumentTitle: string = 'Select a document to view';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private officerService: OfficerService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.applicationNumber = this.route.snapshot.paramMap.get('id') || '';
    if (!this.applicationNumber) {
      this.router.navigate(['/officer']);
      return;
    }
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
    const url = this.officerService.getKycDocumentUrl(id);
    this.activeDocumentUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    this.activeDocumentTitle = title;
  }

  viewLoanDocument(id: number, title: string): void {
    if (!id) return;
    const url = this.officerService.getLoanDocumentUrl(id);
    this.activeDocumentUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    this.activeDocumentTitle = title;
  }

  goBack(): void {
    this.router.navigate(['/officer']);
  }
}
