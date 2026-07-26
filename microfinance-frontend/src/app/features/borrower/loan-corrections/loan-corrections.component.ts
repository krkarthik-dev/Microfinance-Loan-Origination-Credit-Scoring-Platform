import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BorrowerService, UserProfile } from '../borrower.service';

@Component({
  selector: 'app-loan-corrections',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './loan-corrections.component.html',
  styleUrls: ['./loan-corrections.component.scss']
})
export class LoanCorrectionsComponent implements OnInit {
  applicationNumber: string = '';
  isLoading: boolean = true;
  isSubmitting: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  correctionRequests: any[] = [];

  // Flagged Sections
  showProfile: boolean = false;
  showGuarantor: boolean = false;
  showLoanConfig: boolean = false;
  showPan: boolean = false;
  showAadhaar: boolean = false;
  showIncome: boolean = false;
  showPhoto: boolean = false;

  // Officer Comments
  profileComment: string = '';
  guarantorComment: string = '';
  loanConfigComment: string = '';
  panComment: string = '';
  aadhaarComment: string = '';
  incomeComment: string = '';
  photoComment: string = '';

  // Form Models
  // Guarantor Details
  guarantorName: string = '';
  guarantorAddress: string = '';
  guarantorCity: string = '';
  guarantorZip: string = '';

  // Loan Configuration
  appliedAmount: number = 0;
  tenureMonths: number = 12;
  purpose: string = '';

  // Profile Details
  phone: string = '';
  addressLine1: string = '';
  city: string = '';
  state: string = '';
  pincode: string = '';

  // Files
  panFile: File | null = null;
  aadhaarFile: File | null = null;
  incomeCertFile: File | null = null;
  photoFile: File | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private borrowerService: BorrowerService
  ) {}

  ngOnInit(): void {
    this.applicationNumber = this.route.snapshot.paramMap.get('id') || '';
    if (!this.applicationNumber) {
      this.router.navigate(['/applicant']);
      return;
    }
    this.loadCorrectionsAndData();
  }

  loadCorrectionsAndData(): void {
    this.isLoading = true;
    this.borrowerService.getCorrectionRequests(this.applicationNumber).subscribe({
      next: (requests) => {
        this.correctionRequests = requests;
        if (!requests || requests.length === 0) {
          // No pending corrections, go to tracking
          this.router.navigate(['/applicant/loan', this.applicationNumber, 'tracking']);
          return;
        }

        requests.forEach(req => {
          const sec = req.section?.toLowerCase() || '';
          if (sec.includes('profile')) {
            this.showProfile = true;
            this.profileComment = req.comments;
          } else if (sec.includes('guarantor')) {
            this.showGuarantor = true;
            this.guarantorComment = req.comments;
          } else if (sec.includes('configuration') || sec.includes('loan')) {
            this.showLoanConfig = true;
            this.loanConfigComment = req.comments;
          } else if (sec.includes('pan')) {
            this.showPan = true;
            this.panComment = req.comments;
          } else if (sec.includes('aadhaar')) {
            this.showAadhaar = true;
            this.aadhaarComment = req.comments;
          } else if (sec.includes('income')) {
            this.showIncome = true;
            this.incomeComment = req.comments;
          } else if (sec.includes('photo')) {
            this.showPhoto = true;
            this.photoComment = req.comments;
          }
        });

        // Pre-fill profile data if profile is flagged
        if (this.showProfile) {
          this.borrowerService.getProfile().subscribe({
            next: (profile) => {
              if (profile) {
                this.phone = profile.phoneNumber || '';
                this.addressLine1 = profile.addressLine1 || '';
                this.city = profile.city || '';
                this.state = profile.state || '';
                this.pincode = profile.pincode || '';
              }
            }
          });
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load corrections', err);
        this.errorMessage = 'Could not load requested corrections. Please try again later.';
        this.isLoading = false;
      }
    });
  }

  onFileSelected(event: any, type: 'pan' | 'aadhaar' | 'income' | 'photo'): void {
    const file = event.target.files[0];
    if (file) {
      if (type === 'pan') this.panFile = file;
      else if (type === 'aadhaar') this.aadhaarFile = file;
      else if (type === 'income') this.incomeCertFile = file;
      else if (type === 'photo') this.photoFile = file;
    }
  }

  onFileDropped(event: DragEvent, type: 'pan' | 'aadhaar' | 'income' | 'photo'): void {
    event.preventDefault();
    if (event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      if (type === 'pan') this.panFile = file;
      else if (type === 'aadhaar') this.aadhaarFile = file;
      else if (type === 'income') this.incomeCertFile = file;
      else if (type === 'photo') this.photoFile = file;
      event.dataTransfer.clearData();
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  isFormValid(): boolean {
    if (this.showGuarantor && (!this.guarantorName.trim() || !this.guarantorAddress.trim() || !this.guarantorCity.trim() || !this.guarantorZip.trim())) {
      return false;
    }
    if (this.showLoanConfig && (!this.appliedAmount || this.appliedAmount <= 0 || !this.tenureMonths || this.tenureMonths <= 0 || !this.purpose.trim())) {
      return false;
    }
    if (this.showProfile && (!this.phone.trim() || !this.addressLine1.trim() || !this.city.trim() || !this.state.trim() || !this.pincode.trim())) {
      return false;
    }
    if (this.showPan && !this.panFile) return false;
    if (this.showAadhaar && !this.aadhaarFile) return false;
    if (this.showIncome && !this.incomeCertFile) return false;
    if (this.showPhoto && !this.photoFile) return false;

    return true;
  }

  submitCorrections(): void {
    if (!this.isFormValid()) {
      this.errorMessage = 'Please complete all flagged fields and upload all requested replacement documents before submitting.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const formData = new FormData();
    if (this.showGuarantor) {
      formData.append('guarantorName', this.guarantorName);
      formData.append('guarantorAddress', this.guarantorAddress);
      formData.append('guarantorCity', this.guarantorCity);
      formData.append('guarantorZip', this.guarantorZip);
    }
    if (this.showLoanConfig) {
      formData.append('appliedAmount', this.appliedAmount.toString());
      formData.append('tenureMonths', this.tenureMonths.toString());
      formData.append('purpose', this.purpose);
    }
    if (this.showProfile) {
      formData.append('phone', this.phone);
      formData.append('addressLine1', this.addressLine1);
      formData.append('city', this.city);
      formData.append('state', this.state);
      formData.append('pincode', this.pincode);
    }
    if (this.showPan && this.panFile) formData.append('panFile', this.panFile);
    if (this.showAadhaar && this.aadhaarFile) formData.append('aadhaarFile', this.aadhaarFile);
    if (this.showIncome && this.incomeCertFile) formData.append('incomeCertFile', this.incomeCertFile);
    if (this.showPhoto && this.photoFile) formData.append('photoFile', this.photoFile);

    this.borrowerService.resubmitCorrections(this.applicationNumber, formData).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage = 'Corrections submitted successfully! Returning to loan tracker...';
        setTimeout(() => {
          this.router.navigate(['/applicant/loan', this.applicationNumber, 'tracking']);
        }, 2000);
      },
      error: (err) => {
        console.error('Failed to submit corrections', err);
        this.isSubmitting = false;
        this.errorMessage = err.error?.error || 'Failed to submit corrections. Please try again.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/applicant/loan', this.applicationNumber, 'tracking']);
  }
}
