import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OfficerService } from '../officer.service';

@Component({
  selector: 'app-officer-repayment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './officer-repayment.component.html',
  styleUrls: ['./officer-repayment.component.scss']
})
export class OfficerRepaymentComponent implements OnInit {
  loanId: string = '';
  scheduleData: any = null;
  isLoading: boolean = true;
  errorMessage: string = '';
  successMessage: string = '';

  // Modal state
  showPaymentModal: boolean = false;
  selectedInstallment: any = null;
  paymentForm: FormGroup;
  isSubmitting: boolean = false;
  modalError: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private officerService: OfficerService,
    private fb: FormBuilder
  ) {
    this.paymentForm = this.fb.group({
      paidDate: [new Date().toISOString().split('T')[0], Validators.required],
      paymentMethod: ['Bank Transfer', Validators.required],
      referenceNumber: ['', Validators.required],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loanId = id;
        this.fetchSchedule(id);
      } else {
        this.handleError('No Loan ID provided.');
      }
    });
  }

  fetchSchedule(id: string): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.officerService.getOfficerRepaymentSchedule(id).subscribe({
      next: (data: any) => {
        this.scheduleData = data;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error fetching repayment schedule:', err);
        this.isLoading = false;
        this.errorMessage = 'Unable to retrieve repayment details for this loan.';
      }
    });
  }

  handleError(msg: string): void {
    this.isLoading = false;
    this.errorMessage = msg;
  }

  openPaymentModal(item: any): void {
    if (!item.isActionable) return;
    this.selectedInstallment = item;
    this.modalError = '';
    this.paymentForm.reset({
      paidDate: new Date().toISOString().split('T')[0],
      paymentMethod: 'Bank Transfer',
      referenceNumber: 'TXN-' + Math.floor(100000 + Math.random() * 900000),
      notes: ''
    });
    this.showPaymentModal = true;
  }

  closePaymentModal(): void {
    this.showPaymentModal = false;
    this.selectedInstallment = null;
  }

  submitPayment(): void {
    if (this.paymentForm.invalid || !this.selectedInstallment) return;

    this.isSubmitting = true;
    this.modalError = '';
    this.successMessage = '';

    this.officerService.collectInstallmentPayment(this.loanId, this.selectedInstallment.id, this.paymentForm.value).subscribe({
      next: (updatedSchedule: any) => {
        this.isSubmitting = false;
        this.scheduleData = updatedSchedule;
        this.successMessage = `Installment #${this.selectedInstallment.installmentNumber} successfully marked as PAID.`;
        this.closePaymentModal();
        setTimeout(() => this.successMessage = '', 5000);
      },
      error: (err: any) => {
        this.isSubmitting = false;
        this.modalError = err.error?.error || err.error || 'Failed to record payment collection.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/officer/command-center'], { fragment: 'disbursed' });
  }
}
