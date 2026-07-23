import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-force-change-password',
  templateUrl: './force-change-password.component.html',
  styleUrls: ['./force-change-password.component.scss']
})
export class ForceChangePasswordComponent {
  passwordForm: FormGroup;
  error: string = '';
  loading: boolean = false;
  
  // We need the email and token from the auth service state, or from local storage.
  // Wait, AuthService.login() was intercepted. The response wasn't saved to localStorage yet.
  // We need to pass the login response to this component.
  // A simple way is to use Angular Router state.
  loginResponse: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['loginResponse']) {
      this.loginResponse = navigation.extras.state['loginResponse'];
    } else {
      // If accessed directly without state, redirect to login
      this.router.navigate(['/login']);
    }

    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  onSubmit() {
    if (this.passwordForm.invalid) return;

    this.loading = true;
    this.error = '';

    const payload = {
      email: this.loginResponse.email,
      oldPassword: this.passwordForm.value.oldPassword,
      newPassword: this.passwordForm.value.newPassword
    };

    this.authService.changePassword(payload).subscribe({
      next: () => {
        // Now save session and redirect
        this.loginResponse.mustChangePassword = false;
        this.authService.saveSession(this.loginResponse);
        this.router.navigate(['/borrower/dashboard']); // Direct applicant is a borrower
      },
      error: (err: any) => {
        this.error = 'Failed to change password. Please check your old password.';
        this.loading = false;
      }
    });
  }
}
