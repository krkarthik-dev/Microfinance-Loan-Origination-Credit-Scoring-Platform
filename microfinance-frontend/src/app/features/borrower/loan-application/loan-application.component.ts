import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-loan-application',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './loan-application.component.html',
  styleUrls: ['./loan-application.component.scss']
})
export class LoanApplicationComponent implements OnInit {
  currentStep = 1;
  totalSteps = 3;

  // Step 1 Form
  loanRequirementsForm!: FormGroup;

  // Step 2 Form
  guarantorForm!: FormGroup;

  purposes = [
    'Agriculture',
    'Small Business Setup',
    'Medical Emergency',
    'Education',
    'Home Repair'
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForms();
  }

  private initForms(): void {
    this.loanRequirementsForm = this.fb.group({
      principalAmount: ['', [Validators.required, Validators.min(1000), Validators.max(500000)]],
      tenureMonths: [12, [Validators.required]],
      purpose: ['', Validators.required]
    });

    this.guarantorForm = this.fb.group({
      name:    ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z\s]+$/)]],
      city:    ['', [Validators.required, Validators.pattern(/^[a-zA-Z\s]+$/)]],
      zipCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  // ── Step 1 helpers ──
  get reqF() { return this.loanRequirementsForm.controls; }

  isReqInvalid(field: string): boolean {
    const c = this.loanRequirementsForm.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  // ── Step 2 helpers ──
  get guarF() { return this.guarantorForm.controls; }

  isGuarInvalid(field: string): boolean {
    const c = this.guarantorForm.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  get isGuarantorValid(): boolean {
    return this.guarantorForm.valid;
  }

  // ── Navigation ──
  nextStep(): void {
    if (this.currentStep === 1) {
      if (this.loanRequirementsForm.invalid) {
        this.loanRequirementsForm.markAllAsTouched();
        return;
      }
    } else if (this.currentStep === 2) {
      if (this.guarantorForm.invalid) {
        this.guarantorForm.markAllAsTouched();
        return;
      }
    }

    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  cancel(): void {
    this.router.navigate(['/applicant']);
  }
}
