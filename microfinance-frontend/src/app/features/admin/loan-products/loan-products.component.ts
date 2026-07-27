import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService } from '../admin.service';
import { DataTableComponent, TableColumn } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-loan-products',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DataTableComponent],
  templateUrl: './loan-products.component.html',
  styleUrls: ['./loan-products.component.scss']
})
export class LoanProductsComponent implements OnInit {
  products: any[] = [];
  isLoading = true;
  errorMessage = '';
  successMessage = '';

  productColumns: TableColumn[] = [
    { key: 'productName', label: 'Product Name' },
    { key: 'interestRatePa', label: 'Base APR (%)' },
    { key: 'minAmount', label: 'Min Amount', format: 'rupee' },
    { key: 'maxAmount', label: 'Max Amount', format: 'rupee' },
    { key: 'minTenureMonths', label: 'Min Tenure (m)' },
    { key: 'maxTenureMonths', label: 'Max Tenure (m)' },
    { key: 'statusDisplay', label: 'Status', format: 'badge' }
  ];

  isModalOpen = false;
  isEditMode = false;
  editingProductId: number | null = null;
  editForm!: FormGroup;
  isSaving = false;

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.fetchProducts();
    this.editForm = this.fb.group({
      productName: ['', [Validators.required, Validators.maxLength(100)]],
      interestRatePa: ['', [Validators.required, Validators.min(0.01), Validators.max(100)]],
      minAmount: ['', [Validators.required, Validators.min(100)]],
      maxAmount: ['', [Validators.required, Validators.min(100)]],
      minTenureMonths: ['', [Validators.required, Validators.min(1)]],
      maxTenureMonths: ['', [Validators.required, Validators.min(1)]],
      active: [true]
    });
  }

  fetchProducts(): void {
    this.isLoading = true;
    this.adminService.getAllProducts().subscribe({
      next: (data) => {
        this.products = data.map(p => ({
          ...p,
          statusDisplay: p.active ? 'Active' : 'Inactive'
        }));
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load products', err);
        this.errorMessage = 'Failed to load loan products.';
        this.isLoading = false;
      }
    });
  }

  openNewModal(): void {
    this.isEditMode = false;
    this.editingProductId = null;
    this.editForm.reset({
      productName: '',
      interestRatePa: '',
      minAmount: '',
      maxAmount: '',
      minTenureMonths: '',
      maxTenureMonths: '',
      active: true
    });
    this.isModalOpen = true;
    this.successMessage = '';
    this.errorMessage = '';
  }

  editProduct(product: any): void {
    this.isEditMode = true;
    this.editingProductId = product.id;
    this.editForm.patchValue({
      productName: product.productName,
      interestRatePa: product.interestRatePa,
      minAmount: product.minAmount,
      maxAmount: product.maxAmount,
      minTenureMonths: product.minTenureMonths,
      maxTenureMonths: product.maxTenureMonths,
      active: product.active
    });
    this.isModalOpen = true;
    this.successMessage = '';
    this.errorMessage = '';
  }

  cancelEdit(): void {
    this.isModalOpen = false;
    this.isEditMode = false;
    this.editingProductId = null;
    this.editForm.reset();
  }

  saveProduct(): void {
    if (this.editForm.invalid) return;

    const val = this.editForm.value;
    if (val.maxAmount < val.minAmount) {
      this.errorMessage = 'Maximum amount cannot be less than minimum amount.';
      return;
    }
    if (val.maxTenureMonths < val.minTenureMonths) {
      this.errorMessage = 'Maximum tenure cannot be less than minimum tenure.';
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const obs$ = this.isEditMode && this.editingProductId
      ? this.adminService.updateProduct(this.editingProductId, val)
      : this.adminService.createProduct(val);

    obs$.subscribe({
      next: (res) => {
        this.successMessage = `Successfully ${this.isEditMode ? 'updated' : 'created'} loan product: ${res.productName}`;
        this.isSaving = false;
        this.isModalOpen = false;
        this.fetchProducts();
      },
      error: (err) => {
        console.error('Failed to save product', err);
        this.errorMessage = err.error?.error || 'Failed to save configuration. Please check the inputs.';
        this.isSaving = false;
      }
    });
  }

  toggleStatus(product: any, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    const newStatus = !product.active;
    this.adminService.toggleProductStatus(product.id, newStatus).subscribe({
      next: (res) => {
        this.successMessage = `Product '${res.productName}' status toggled to ${newStatus ? 'Active' : 'Inactive'}.`;
        this.fetchProducts();
      },
      error: (err) => {
        console.error('Failed to toggle status', err);
        this.errorMessage = err.error?.error || 'Failed to toggle status.';
      }
    });
  }

  deleteProduct(product: any, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (!confirm(`Are you sure you want to delete '${product.productName}'?`)) return;

    this.adminService.deleteProduct(product.id).subscribe({
      next: () => {
        this.successMessage = `Product '${product.productName}' deleted successfully.`;
        this.fetchProducts();
      },
      error: (err) => {
        console.error('Failed to delete product', err);
        this.errorMessage = err.error?.error || 'Failed to delete product. It may be linked to historical applications.';
      }
    });
  }
}
