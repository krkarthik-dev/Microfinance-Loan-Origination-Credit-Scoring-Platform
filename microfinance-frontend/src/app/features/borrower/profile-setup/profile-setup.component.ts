import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { BorrowerService, UserProfile } from '../borrower.service';

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
  @Output() closePopup = new EventEmitter<void>();
  
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
        setTimeout(() => {
          this.closePopup.emit();
        }, 1500);
      },
      error: (err) => {
        console.error('Error saving profile', err);
        this.errorMessage = 'Failed to save profile. Please try again.';
        this.isSaving = false;
      }
    });
  }
}
