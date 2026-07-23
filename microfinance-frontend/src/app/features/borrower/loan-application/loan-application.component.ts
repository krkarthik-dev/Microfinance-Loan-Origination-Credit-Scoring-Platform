import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { BorrowerService } from '../borrower.service';
import { OfficerService } from '../../officer/officer.service';

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
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
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

  // Step 3 — Document upload state
  incomeFile: UploadedFile | null = null;
  photoFile: UploadedFile | null = null;
  guarantorIdFile: UploadedFile | null = null;
  otherFiles: UploadedFile[] = [];
  isDragOverIncome = false;
  isDragOverPhoto = false;
  isDragOverGuarantorId = false;
  isDragOverOther = false;
  incomeError: string | null = null;
  photoError: string | null = null;
  guarantorIdError: string | null = null;
  otherError: string | null = null;

  // Step 4 — Review, signature & submission
  termsAccepted = false;
  signatureFile: UploadedFile | null = null;
  isDragOverSignature = false;
  signatureError: string | null = null;
  isSubmitting = false;
  submissionError: string | null = null;

  // Success state
  submittedAppNumber: string | null = null;
  pdfBlobUrl: string | null = null;

  // Officer Mode
  isOfficerMode = false;
  borrowerEmail: string | null = null;

  purposes = [
    'Agriculture', 'Small Business Setup', 'Medical Emergency',
    'Education', 'Home Repair'
  ];

  private readonly DOC_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
  private readonly IMG_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
  private readonly MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private borrowerService: BorrowerService,
    private officerService: OfficerService
  ) {}

  ngOnInit(): void { 
    this.initForms(); 
    this.route.paramMap.subscribe(params => {
      if (params.has('email')) {
        this.isOfficerMode = true;
        this.borrowerEmail = params.get('email');
      }
    });
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

  // ── Step 1 ──
  get reqF() { return this.loanRequirementsForm.controls; }
  isReqInvalid(f: string): boolean {
    const c = this.loanRequirementsForm.get(f);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  // ── Step 2 ──
  get guarF() { return this.guarantorForm.controls; }
  isGuarInvalid(f: string): boolean {
    const c = this.guarantorForm.get(f);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }
  get isGuarantorValid(): boolean { return this.guarantorForm.valid; }

  // ── Step 3 ──
  get isDocsStepValid(): boolean {
    return !!this.incomeFile && !!this.photoFile && !!this.guarantorIdFile;
  }

  // ── Step 4 ──
  get isSubmitEnabled(): boolean {
    return this.termsAccepted && !!this.signatureFile && !this.isSubmitting;
  }

  // ── File Handling ──
  onDragOver(e: DragEvent, zone: string) {
    e.preventDefault();
    if (zone === 'income')      this.isDragOverIncome = true;
    if (zone === 'photo')       this.isDragOverPhoto = true;
    if (zone === 'guarantorId') this.isDragOverGuarantorId = true;
    if (zone === 'other')       this.isDragOverOther = true;
    if (zone === 'signature')   this.isDragOverSignature = true;
  }

  onDragLeave(e: DragEvent, zone: string) {
    e.preventDefault();
    this.resetDragStates();
  }

  onDrop(e: DragEvent, zone: string) {
    e.preventDefault();
    this.resetDragStates();
    if (e.dataTransfer?.files?.length) this.handleFile(e.dataTransfer.files[0], zone);
  }

  onFileChange(e: Event, zone: string) {
    const input = e.target as HTMLInputElement;
    if (input.files?.length) { this.handleFile(input.files[0], zone); input.value = ''; }
  }

  private handleFile(file: File, zone: string) {
    const allowed = (zone === 'photo' || zone === 'signature') ? this.IMG_TYPES : this.DOC_TYPES;
    const label   = (zone === 'photo' || zone === 'signature') ? 'image files (JPG, PNG, WEBP)' : 'PDF, JPG, PNG, or WEBP files';

    if (!allowed.includes(file.type)) { this.setError(zone, `Only ${label} are allowed.`); return; }
    if (file.size > this.MAX_SIZE_BYTES) { this.setError(zone, 'File size must be under 5 MB.'); return; }

    this.setError(zone, null);
    const uploaded: UploadedFile = { file, name: file.name, size: file.size, type: file.type };
    if (file.type.startsWith('image/')) uploaded.previewUrl = URL.createObjectURL(file);

    if (zone === 'income')      this.incomeFile = uploaded;
    if (zone === 'photo')       this.photoFile = uploaded;
    if (zone === 'guarantorId') this.guarantorIdFile = uploaded;
    if (zone === 'signature')   this.signatureFile = uploaded;
    if (zone === 'other')       this.otherFiles = [...this.otherFiles, uploaded];
  }

  removeFile(zone: string, index?: number) {
    const revoke = (f: UploadedFile | null) => { if (f?.previewUrl) URL.revokeObjectURL(f.previewUrl); };
    if (zone === 'income')      { revoke(this.incomeFile); this.incomeFile = null; }
    if (zone === 'photo')       { revoke(this.photoFile); this.photoFile = null; }
    if (zone === 'guarantorId') { revoke(this.guarantorIdFile); this.guarantorIdFile = null; }
    if (zone === 'signature')   { revoke(this.signatureFile); this.signatureFile = null; }
    if (zone === 'other' && index !== undefined) {
      revoke(this.otherFiles[index]);
      this.otherFiles = this.otherFiles.filter((_, i) => i !== index);
    }
  }

  private setError(zone: string, msg: string | null) {
    if (zone === 'income')      this.incomeError = msg;
    if (zone === 'photo')       this.photoError = msg;
    if (zone === 'guarantorId') this.guarantorIdError = msg;
    if (zone === 'signature')   this.signatureError = msg;
    if (zone === 'other')       this.otherError = msg;
  }

  private resetDragStates() {
    this.isDragOverIncome = this.isDragOverPhoto = this.isDragOverGuarantorId =
    this.isDragOverOther = this.isDragOverSignature = false;
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

  prevStep(): void { if (this.currentStep > 1) this.currentStep--; }

  // ── Submission (AC3, AC4) ──
  submitApplication(): void {
    if (!this.isSubmitEnabled) return;

    this.isSubmitting = true;
    this.submissionError = null;

    const fd = new FormData();
    const lf = this.loanRequirementsForm.value;
    const gf = this.guarantorForm.value;

    fd.append('principalAmount', lf.principalAmount);
    fd.append('tenureMonths',    lf.tenureMonths);
    fd.append('purpose',         lf.purpose);

    fd.append('guarantorName',    gf.name);
    fd.append('guarantorAddress', gf.address);
    fd.append('guarantorCity',    gf.city);
    fd.append('guarantorZip',     gf.zipCode);
    fd.append('guarantorAadhaar', gf.aadhaarNumber);
    fd.append('guarantorPan',     gf.panNumber);

    fd.append('incomeCert',  this.incomeFile!.file, this.incomeFile!.name);
    fd.append('photo',       this.photoFile!.file,  this.photoFile!.name);
    fd.append('guarantorId', this.guarantorIdFile!.file, this.guarantorIdFile!.name);
    this.otherFiles.forEach(f => fd.append('otherDocs', f.file, f.name));
    fd.append('signature',   this.signatureFile!.file, this.signatureFile!.name);

    const submitObservable = this.isOfficerMode
      ? this.officerService.submitDirectLoanApplication(this.borrowerEmail!, fd)
      : this.borrowerService.submitLoanApplication(fd);

    submitObservable.subscribe({
      next: (response: any) => {
        this.isSubmitting = false;
        // Extract application number from response header
        const appNumber = response.headers.get('X-Application-Number');
        if (appNumber) {
          if (this.isOfficerMode) {
             // Create an object URL for the PDF blob to download it automatically
             const blob = new Blob([response.body], { type: 'application/pdf' });
             const url = window.URL.createObjectURL(blob);
             const a = document.createElement('a');
             a.href = url;
             a.download = `${appNumber}_application.pdf`;
             document.body.appendChild(a);
             a.click();
             document.body.removeChild(a);
             window.URL.revokeObjectURL(url);
             
             // Then redirect back to officer dashboard
             this.router.navigate(['/officer']);
          } else {
             // AC1: Redirect to tracking page for online borrower
             this.router.navigate(['/applicant/loan', appNumber, 'tracking']);
          }
        } else {
          this.submissionError = 'Submission succeeded, but tracking ID was missing.';
        }
      },
      error: (err) => {
        this.isSubmitting = false;
        this.submissionError = 'Submission failed. Please try again.';
        console.error('Submission error:', err);
      }
    });
  }

  cancel(): void { 
    if (this.isOfficerMode) {
       this.router.navigate(['/officer']);
    } else {
       this.router.navigate(['/applicant']);
    }
  }
}
