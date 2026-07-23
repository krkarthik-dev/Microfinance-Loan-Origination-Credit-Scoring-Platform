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
  loanForm!: FormGroup;
  
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
    this.initForm();
  }

  private initForm(): void {
    this.loanForm = this.fb.group({
      principalAmount: ['', [Validators.required, Validators.min(1000), Validators.max(50000)]],
      tenureMonths: [12, [Validators.required]],
      purpose: ['', Validators.required]
    });
  }

  get f() {
    return this.loanForm.controls;
  }

  isInvalid(controlName: string): boolean {
    const control = this.loanForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  nextStep(): void {
    if (this.currentStep === 1) {
      if (this.loanForm.invalid) {
        this.loanForm.markAllAsTouched();
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
