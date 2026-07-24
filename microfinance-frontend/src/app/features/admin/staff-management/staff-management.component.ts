import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService } from '../admin.service';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';

@Component({
  selector: 'app-staff-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DataTableComponent],
  templateUrl: './staff-management.component.html',
  styleUrls: ['./staff-management.component.scss']
})
export class StaffManagementComponent implements OnInit {
  staffList: any[] = [];
  isLoading = true;
  errorMessage = '';
  successMessage = '';

  staffColumns = [
    { key: 'username', label: 'Username' },
    { key: 'email', label: 'Email' },
    { key: 'role', label: 'Role' },
    { key: 'statusBadge', label: 'Status' }
  ];

  showCreateModal = false;
  createForm!: FormGroup;
  isCreating = false;
  generatedPassword = '';

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.fetchStaff();
    this.createForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      role: ['ROLE_OFFICER', [Validators.required]]
    });
  }

  fetchStaff(): void {
    this.isLoading = true;
    this.adminService.getAllStaff().subscribe({
      next: (data) => {
        this.staffList = data.map(staff => ({
          ...staff,
          statusBadge: staff.active ? 'Active' : 'Disabled'
        }));
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load staff', err);
        this.errorMessage = 'Failed to load staff list.';
        this.isLoading = false;
      }
    });
  }

  openCreateModal(): void {
    this.showCreateModal = true;
    this.createForm.reset({ role: 'ROLE_OFFICER' });
    this.generatedPassword = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  createStaff(): void {
    if (this.createForm.invalid) return;

    this.isCreating = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.adminService.createStaff(this.createForm.value).subscribe({
      next: (res) => {
        this.generatedPassword = res.temporaryPassword;
        this.successMessage = 'Staff account created successfully! Please copy the temporary password below.';
        this.isCreating = false;
        this.fetchStaff();
      },
      error: (err) => {
        console.error('Failed to create staff', err);
        this.errorMessage = err.error?.message || 'Failed to create staff account.';
        this.isCreating = false;
      }
    });
  }

  handleRowAction(event: any): void {
    // We don't have rowClick action specifically for buttons in data-table component yet,
    // so we'll just handle it differently or we can add custom action handling.
    // Wait, the data-table component doesn't support custom action buttons per row out of the box in the current generic implementation.
    // Let's implement custom row rendering in HTML instead of using app-data-table if we need complex buttons,
    // OR we just use the (rowClick) event to open an "Action Menu" modal for that user.
    // Let's use an Action Menu modal.
  }

  // Managing actions via selection
  selectedStaff: any = null;
  showActionModal = false;

  openActionModal(staff: any): void {
    this.selectedStaff = staff;
    this.showActionModal = true;
    this.generatedPassword = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeActionModal(): void {
    this.showActionModal = false;
    this.selectedStaff = null;
  }

  toggleStaffStatus(): void {
    if (!this.selectedStaff) return;
    this.errorMessage = '';
    this.successMessage = '';

    const obs = this.selectedStaff.active
      ? this.adminService.disableStaff(this.selectedStaff.id)
      : this.adminService.enableStaff(this.selectedStaff.id);

    obs.subscribe({
      next: () => {
        this.successMessage = `Staff account ${this.selectedStaff.active ? 'disabled' : 'enabled'} successfully.`;
        this.fetchStaff();
        this.closeActionModal();
      },
      error: (err) => {
        this.errorMessage = 'Failed to update staff status.';
      }
    });
  }

  resetPassword(): void {
    if (!this.selectedStaff) return;
    this.errorMessage = '';
    this.successMessage = '';

    if (confirm(`Are you sure you want to reset the password for ${this.selectedStaff.email}?`)) {
      this.adminService.resetStaffPassword(this.selectedStaff.id).subscribe({
        next: (res) => {
          this.generatedPassword = res.temporaryPassword;
          this.successMessage = 'Password reset successfully. Please copy the new temporary password.';
        },
        error: (err) => {
          this.errorMessage = 'Failed to reset password.';
        }
      });
    }
  }
}
