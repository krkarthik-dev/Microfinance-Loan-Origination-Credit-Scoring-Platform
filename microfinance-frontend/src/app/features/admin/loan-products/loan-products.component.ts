import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService } from '../admin.service';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

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

  productColumns = [
    { key: 'productName', label: 'Product Name' },
    { key: 'minAmount', label: 'Min Amount', type: 'currency' },
    { key: 'maxAmount', label: 'Max Amount', type: 'currency' },
    { key: 'interestRatePa', label: 'APR (%)' },
    { key: 'active', label: 'Status' }
  ];

  editingProduct: any = null;
  editForm!: FormGroup;
  isSaving = false;

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.fetchProducts();
    this.editForm = this.fb.group({
      interestRatePa: ['', [Validators.required, Validators.min(0), Validators.max(100)]]
    });
  }

  fetchProducts(): void {
    this.isLoading = true;
    this.adminService.getAllProducts().subscribe({
      next: (data) => {
        // Transform boolean to string for better display in generic data table if needed
        this.products = data.map(p => ({
          ...p,
          active: p.active ? 'Active' : 'Inactive'
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

  editProduct(product: any): void {
    this.editingProduct = product;
    this.editForm.patchValue({
      interestRatePa: product.interestRatePa
    });
    this.successMessage = '';
    this.errorMessage = '';
  }

  cancelEdit(): void {
    this.editingProduct = null;
    this.editForm.reset();
  }

  saveProduct(): void {
    if (this.editForm.invalid || !this.editingProduct) return;

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const newRate = this.editForm.value.interestRatePa;

    this.adminService.updateProductApr(this.editingProduct.id, newRate).subscribe({
      next: (updatedProduct) => {
        this.successMessage = `Successfully updated APR for ${updatedProduct.productName} to ${updatedProduct.interestRatePa}%`;
        this.isSaving = false;
        this.editingProduct = null;
        this.fetchProducts(); // Refresh grid
      },
      error: (err) => {
        console.error('Failed to update APR', err);
        this.errorMessage = 'Failed to update APR. Please try again.';
        this.isSaving = false;
      }
    });
  }
}
