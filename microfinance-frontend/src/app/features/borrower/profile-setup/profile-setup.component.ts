import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { BorrowerService, UserProfile, KycUploadResponse } from '../borrower.service';
import { environment } from '../../../../environments/environment';

// Custom validator for minimum age
function minimumAgeValidator(minAge: number) {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    
    const dob = new Date(control.value);
    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
      age--;
    }
    
    return age >= minAge ? null : { 'minimumAge': { requiredAge: minAge, actualAge: age } };
  };
}

@Component({
  selector: 'app-profile-setup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './profile-setup.component.html',
  styleUrls: ['./profile-setup.component.scss']
})
export class ProfileSetupComponent implements OnInit {
  profileForm!: FormGroup;
  isLoading = true;
  isSaving = false;
  isEditMode = true;
  hasExistingProfile = false;
  successMessage = '';
  errorMessage = '';

  employmentTypes = [
    'Salaried',
    'Self-Employed',
    'Daily Wage',
    'Business Owner',
    'Unemployed',
    'Student',
    'Retired'
  ];

  // US10 Document State
  panDocument: KycUploadResponse | null = null;
  aadhaarDocument: KycUploadResponse | null = null;
  
  isUploadingPan = false;
  isUploadingAadhaar = false;
  
  panUploadError: string | null = null;
  aadhaarUploadError: string | null = null;
  
  apiUrl = environment.apiUrl;

  // Track drag states
  isPanDragOver = false;
  isAadhaarDragOver = false;

  constructor(
    private fb: FormBuilder,
    private borrowerService: BorrowerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    try {
      this.initForm();
      this.loadProfile();
    } catch (e) {
      console.error('Error in ngOnInit ProfileSetup:', e);
      this.errorMessage = 'An error occurred initializing the form.';
      this.isLoading = false;
    }
  }

  // Safe getters for template
  getControl(name: string): AbstractControl | null {
    return this.profileForm ? this.profileForm.get(name) : null;
  }

  isInvalid(name: string): boolean {
    const ctrl = this.getControl(name);
    return ctrl ? !!(ctrl.invalid && ctrl.touched) : false;
  }

  hasError(name: string, errorName: string): boolean {
    const ctrl = this.getControl(name);
    return ctrl ? !!(ctrl.errors?.[errorName] && ctrl.touched) : false;
  }

  private initForm(): void {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.maxLength(50)]],
      dateOfBirth: ['', [Validators.required, minimumAgeValidator(18)]],
      gender: ['', Validators.required],
      phoneNumber: ['', [Validators.required, Validators.pattern('^\\+?[1-9]\\d{1,14}$')]],
      addressLine1: ['', [Validators.required, Validators.maxLength(255)]],
      addressLine2: ['', Validators.maxLength(255)],
      city: ['', [Validators.required, Validators.maxLength(100)]],
      state: ['', [Validators.required, Validators.maxLength(100)]],
      pincode: ['', [Validators.required, Validators.maxLength(10)]],
      panNumber: ['', [Validators.pattern('^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$')]],
      aadhaarNumber: ['', [Validators.pattern('^\\d{12}$')]],
      employmentType: ['', Validators.required],
      monthlyIncome: ['', [Validators.required, Validators.min(0)]]
    });
  }

  private loadProfile(): void {
    this.isLoading = true;
    this.borrowerService.getProfile().subscribe({
      next: (profile) => {
        if (profile && typeof profile === 'object' && Object.keys(profile).length > 0 && profile.firstName) {
          this.hasExistingProfile = true;
          this.isEditMode = false;
          this.profileForm.patchValue(profile);
          this.profileForm.disable();
          // Load document metadata if profile exists
          this.loadDocumentMetadata();
        } else {
          this.hasExistingProfile = false;
          this.isEditMode = true;
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading profile', err);
        this.errorMessage = 'Failed to load profile data.';
        this.isLoading = false;
      }
    });
  }

  toggleEditMode(): void {
    this.isEditMode = true;
    this.profileForm.enable();
    
    // If PAN or Aadhaar already exist, keep them read-only (locked for KYC)
    if (this.hasExistingProfile) {
      if (this.profileForm.get('panNumber')?.value) {
        this.profileForm.get('panNumber')?.disable();
      }
      if (this.profileForm.get('aadhaarNumber')?.value) {
        this.profileForm.get('aadhaarNumber')?.disable();
      }
    }
  }

  onCancel(): void {
    if (this.hasExistingProfile) {
      this.isEditMode = false;
      this.profileForm.disable();
      this.errorMessage = '';
    } else {
      this.router.navigate(['/applicant']);
    }
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      this.errorMessage = 'Please fix the highlighted errors before saving.';
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const profileData: UserProfile = this.profileForm.getRawValue();
    if (profileData.panNumber) {
      profileData.panNumber = profileData.panNumber.toUpperCase();
    }

    this.borrowerService.updateProfile(profileData).subscribe({
      next: () => {
        this.isSaving = false;
        this.successMessage = 'Profile updated successfully!';
        this.hasExistingProfile = true;
        this.isEditMode = false;
        this.profileForm.disable();
        // Load document metadata to show upload zones
        this.loadDocumentMetadata();
        setTimeout(() => {
          this.router.navigate(['/applicant']);
        }, 1500);
      },
      error: (err) => {
        console.error('Error saving profile', err);
        this.errorMessage = 'Failed to save profile. Please try again.';
        this.isSaving = false;
      }
    });
  }

  // ==========================================
  // US10 Document Upload Logic
  // ==========================================

  loadDocumentMetadata() {
    this.borrowerService.getKycDocumentMetadata('PAN').subscribe({
      next: (res) => this.panDocument = res,
      error: () => this.panDocument = null
    });
    this.borrowerService.getKycDocumentMetadata('AADHAAR').subscribe({
      next: (res) => this.aadhaarDocument = res,
      error: () => this.aadhaarDocument = null
    });
  }

  onDragOver(event: DragEvent, type: 'PAN' | 'AADHAAR') {
    event.preventDefault();
    if (type === 'PAN') this.isPanDragOver = true;
    if (type === 'AADHAAR') this.isAadhaarDragOver = true;
  }

  onDragLeave(event: DragEvent, type: 'PAN' | 'AADHAAR') {
    event.preventDefault();
    if (type === 'PAN') this.isPanDragOver = false;
    if (type === 'AADHAAR') this.isAadhaarDragOver = false;
  }

  onDrop(event: DragEvent, type: 'PAN' | 'AADHAAR') {
    event.preventDefault();
    if (type === 'PAN') this.isPanDragOver = false;
    if (type === 'AADHAAR') this.isAadhaarDragOver = false;

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.handleFileSelect(event.dataTransfer.files[0], type);
    }
  }

  onFileSelected(event: Event, type: 'PAN' | 'AADHAAR') {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelect(input.files[0], type);
    }
  }

  async handleFileSelect(file: File, type: 'PAN' | 'AADHAAR') {
    // 1. Validation
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
    if (!allowedTypes.includes(file.type)) {
      this.setUploadError(type, 'Only JPEG, PNG, WEBP, and PDF files are allowed.');
      return;
    }
    
    // Strict 5MB limit
    if (file.size > 5 * 1024 * 1024) {
      this.setUploadError(type, 'File must be smaller than 5MB.');
      return;
    }

    this.setUploadError(type, null);
    
    if (type === 'PAN') this.isUploadingPan = true;
    if (type === 'AADHAAR') this.isUploadingAadhaar = true;

    try {
      // 2. Compress Image (Bypass for PDF)
      let fileToUpload = file;
      if (file.type !== 'application/pdf') {
        fileToUpload = await this.compressImage(file);
      }
      
      // 3. Upload to backend
      const docTypeString = type === 'PAN' ? 'PAN' : 'AADHAAR';
      this.borrowerService.uploadKycDocument(fileToUpload, docTypeString).subscribe({
        next: (res) => {
          if (type === 'PAN') {
            this.panDocument = res;
            this.isUploadingPan = false;
          } else {
            this.aadhaarDocument = res;
            this.isUploadingAadhaar = false;
          }
        },
        error: (err) => {
          this.setUploadError(type, err.error?.message || 'Failed to upload document.');
          if (type === 'PAN') this.isUploadingPan = false;
          if (type === 'AADHAAR') this.isUploadingAadhaar = false;
        }
      });
    } catch (err) {
      this.setUploadError(type, 'Failed to process image.');
      if (type === 'PAN') this.isUploadingPan = false;
      if (type === 'AADHAAR') this.isUploadingAadhaar = false;
    }
  }

  setUploadError(type: 'PAN' | 'AADHAAR', msg: string | null) {
    if (type === 'PAN') this.panUploadError = msg;
    if (type === 'AADHAAR') this.aadhaarUploadError = msg;
  }

  compressImage(file: File): Promise<File> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = (event) => {
        const img = new Image();
        img.src = event.target?.result as string;
        img.onload = () => {
          const canvas = document.createElement('canvas');
          let width = img.width;
          let height = img.height;
          
          // Max dimensions
          const MAX_WIDTH = 1200;
          const MAX_HEIGHT = 1200;
          
          if (width > height && width > MAX_WIDTH) {
            height *= MAX_WIDTH / width;
            width = MAX_WIDTH;
          } else if (height > MAX_HEIGHT) {
            width *= MAX_HEIGHT / height;
            height = MAX_HEIGHT;
          }
          
          canvas.width = width;
          canvas.height = height;
          
          const ctx = canvas.getContext('2d');
          if (!ctx) return reject('No canvas context');
          
          ctx.drawImage(img, 0, 0, width, height);
          
          canvas.toBlob((blob) => {
            if (blob) {
              const newFile = new File([blob], file.name, {
                type: 'image/jpeg',
                lastModified: Date.now()
              });
              resolve(newFile);
            } else {
              reject('Blob conversion failed');
            }
          }, 'image/jpeg', 0.8); // 80% quality
        };
        img.onerror = (err) => reject(err);
      };
      reader.onerror = (err) => reject(err);
    });
  }

  getPreviewUrl(documentType: string): string {
    // Generate unique URL with timestamp to bust cache after re-upload
    return `${this.apiUrl}/applicant/kyc/${documentType}/view?t=${new Date().getTime()}`;
  }

  isPdf(doc: KycUploadResponse | null): boolean {
    if (!doc) return false;
    return doc.fileName.toLowerCase().endsWith('.pdf');
  }

  onSaveAndVerify(): void {
    // In the future, this can call an API to mark KYC as "pending officer review".
    // For now, it just acts as a confirmation and returns to dashboard.
    this.router.navigate(['/applicant']);
  }
}
