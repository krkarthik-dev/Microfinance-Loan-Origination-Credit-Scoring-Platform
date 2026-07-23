import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

export interface UploadedFile {
  file: File;
  name: string;
  size: number;
  type: string;
  previewUrl?: string;
}

@Component({
  selector: 'app-loan-application',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './loan-application.component.html',
  styleUrls: ['./loan-application.component.scss']
})
export class LoanApplicationComponent implements OnInit {
  currentStep = 1;
  totalSteps = 4;

  // Step 1 Form
  loanRequirementsForm!: FormGroup;

  // Step 2 Form
  guarantorForm!: FormGroup;

  // Step 3: Document Upload State
  incomeFile: UploadedFile | null = null;
  photoFile: UploadedFile | null = null;
  guarantorIdFile: UploadedFile | null = null;
  otherFiles: UploadedFile[] = [];

  // Drag-over states
  isDragOverIncome = false;
  isDragOverPhoto = false;
  isDragOverGuarantorId = false;
  isDragOverOther = false;

  // Upload errors
  incomeError: string | null = null;
  photoError: string | null = null;
  guarantorIdError: string | null = null;
  otherError: string | null = null;

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
      name:          ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z\s]+$/)]],
      address:       ['', [Validators.required, Validators.minLength(10), Validators.maxLength(255)]],
      city:          ['', [Validators.required, Validators.pattern(/^[a-zA-Z\s]+$/)]],
      zipCode:       ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      aadhaarNumber: ['', [Validators.required, Validators.pattern(/^\d{12}$/)]],
      panNumber:     ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/)]]
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
  get isGuarantorValid(): boolean { return this.guarantorForm.valid; }

  // ── Step 3: Mandatory doc check ──
  get isDocsStepValid(): boolean {
    return !!this.incomeFile && !!this.photoFile && !!this.guarantorIdFile;
  }

  // ── File Handling ──
  private readonly DOC_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
  private readonly IMG_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
  private readonly MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

  onDragOver(e: DragEvent, zone: string) {
    e.preventDefault();
    if (zone === 'income')      this.isDragOverIncome = true;
    if (zone === 'photo')       this.isDragOverPhoto = true;
    if (zone === 'guarantorId') this.isDragOverGuarantorId = true;
    if (zone === 'other')       this.isDragOverOther = true;
  }

  onDragLeave(e: DragEvent, zone: string) {
    e.preventDefault();
    if (zone === 'income')      this.isDragOverIncome = false;
    if (zone === 'photo')       this.isDragOverPhoto = false;
    if (zone === 'guarantorId') this.isDragOverGuarantorId = false;
    if (zone === 'other')       this.isDragOverOther = false;
  }

  onDrop(e: DragEvent, zone: string) {
    e.preventDefault();
    this.resetDragStates();
    if (e.dataTransfer?.files?.length) {
      this.handleFile(e.dataTransfer.files[0], zone);
    }
  }

  onFileChange(e: Event, zone: string) {
    const input = e.target as HTMLInputElement;
    if (input.files?.length) {
      this.handleFile(input.files[0], zone);
      input.value = '';
    }
  }

  private handleFile(file: File, zone: string) {
    // Choose accepted types
    const allowed = zone === 'photo' ? this.IMG_TYPES : this.DOC_TYPES;

    if (!allowed.includes(file.type)) {
      const msg = zone === 'photo'
        ? 'Only image files (JPG, PNG, WEBP) are allowed for the photograph.'
        : 'Only PDF, JPG, PNG, or WEBP files are allowed.';
      this.setError(zone, msg);
      return;
    }
    if (file.size > this.MAX_SIZE_BYTES) {
      this.setError(zone, 'File size must be under 5 MB.');
      return;
    }

    this.setError(zone, null);
    const uploaded: UploadedFile = { file, name: file.name, size: file.size, type: file.type };

    // Generate preview URL for images
    if (file.type.startsWith('image/')) {
      uploaded.previewUrl = URL.createObjectURL(file);
    }

    if (zone === 'income')      this.incomeFile = uploaded;
    if (zone === 'photo')       this.photoFile = uploaded;
    if (zone === 'guarantorId') this.guarantorIdFile = uploaded;
    if (zone === 'other')       this.otherFiles = [...this.otherFiles, uploaded];
  }

  removeFile(zone: string, index?: number) {
    if (zone === 'income')      { if (this.incomeFile?.previewUrl) URL.revokeObjectURL(this.incomeFile.previewUrl); this.incomeFile = null; }
    if (zone === 'photo')       { if (this.photoFile?.previewUrl) URL.revokeObjectURL(this.photoFile.previewUrl); this.photoFile = null; }
    if (zone === 'guarantorId') { if (this.guarantorIdFile?.previewUrl) URL.revokeObjectURL(this.guarantorIdFile.previewUrl); this.guarantorIdFile = null; }
    if (zone === 'other' && index !== undefined) {
      const f = this.otherFiles[index];
      if (f?.previewUrl) URL.revokeObjectURL(f.previewUrl);
      this.otherFiles = this.otherFiles.filter((_, i) => i !== index);
    }
  }

  private setError(zone: string, msg: string | null) {
    if (zone === 'income')      this.incomeError = msg;
    if (zone === 'photo')       this.photoError = msg;
    if (zone === 'guarantorId') this.guarantorIdError = msg;
    if (zone === 'other')       this.otherError = msg;
  }

  private resetDragStates() {
    this.isDragOverIncome = this.isDragOverPhoto = this.isDragOverGuarantorId = this.isDragOverOther = false;
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  // ── Navigation ──
  nextStep(): void {
    if (this.currentStep === 1 && this.loanRequirementsForm.invalid) {
      this.loanRequirementsForm.markAllAsTouched(); return;
    }
    if (this.currentStep === 2 && this.guarantorForm.invalid) {
      this.guarantorForm.markAllAsTouched(); return;
    }
    if (this.currentStep === 3 && !this.isDocsStepValid) return;

    if (this.currentStep < this.totalSteps) this.currentStep++;
  }

  prevStep(): void {
    if (this.currentStep > 1) this.currentStep--;
  }

  cancel(): void {
    this.router.navigate(['/applicant']);
  }
}
