import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // If already logged in, redirect based on role
    if (this.authService.isLoggedIn()) {
      this.redirectBasedOnRole();
      return;
    }

    this.loginForm = this.fb.group({
      email: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: (response: any) => {
        this.isLoading = false;
        if (response.mustChangePassword) {
          this.router.navigate(['/force-change-password'], { state: { loginResponse: response } });
        } else {
          this.redirectBasedOnRole();
        }
      },
      error: (error) => {
        this.isLoading = false;
        if (error.status === 401) {
          this.errorMessage = 'Invalid email or password.';
        } else {
          this.errorMessage = 'An error occurred. Please try again later.';
        }
      }
    });
  }

  private redirectBasedOnRole(): void {
    const role = this.authService.getRole();
    switch (role) {
      case 'ROLE_ADMIN':
        this.router.navigate(['/admin/dashboard']);
        break;
      case 'ROLE_OFFICER':
        this.router.navigate(['/officer']);
        break;
      case 'ROLE_APPLICANT':
        this.router.navigate(['/applicant']);
        break;
      default:
        this.router.navigate(['/']);
    }
  }

  showForgotPasswordModal = false;
  forgotPasswordEmail = '';
  isForgotSubmitting = false;
  forgotPasswordMessage = '';
  forgotPasswordError = '';

  openForgotPassword(): void {
    this.showForgotPasswordModal = true;
    this.forgotPasswordEmail = this.loginForm.get('email')?.value || '';
    this.forgotPasswordMessage = '';
    this.forgotPasswordError = '';
  }

  closeForgotPassword(): void {
    this.showForgotPasswordModal = false;
  }

  submitForgotPassword(): void {
    if (!this.forgotPasswordEmail || !this.forgotPasswordEmail.trim()) {
      this.forgotPasswordError = 'Please enter your registered email address.';
      return;
    }
    this.isForgotSubmitting = true;
    this.forgotPasswordError = '';
    this.forgotPasswordMessage = '';

    this.authService.forgotPassword(this.forgotPasswordEmail.trim()).subscribe({
      next: (res) => {
        this.isForgotSubmitting = false;
        this.forgotPasswordMessage = res.message || `Contact bank with this request id ${res.requestId} for temp password`;
      },
      error: (err) => {
        this.isForgotSubmitting = false;
        this.forgotPasswordError = err.error?.message || 'Failed to submit forgot password request. Please verify your email.';
      }
    });
  }
}
