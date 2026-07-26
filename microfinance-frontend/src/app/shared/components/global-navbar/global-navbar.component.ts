import { Component, OnInit, OnDestroy, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { BorrowerService } from '../../../features/borrower/borrower.service';
import { OfficerService } from '../../../features/officer/officer.service';
import { AdminService } from '../../../features/admin/admin.service';
import { environment } from '../../../../environments/environment';

interface NotificationItem {
  id: number;
  message: string;
  linkUrl: string;
  isRead: boolean;
  createdAt: string;
}

@Component({
  selector: 'app-global-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './global-navbar.component.html',
  styleUrls: ['./global-navbar.component.scss']
})
export class GlobalNavbarComponent implements OnInit, OnDestroy {
  role: string | null = null;
  userName: string = 'User';
  userInitials: string = 'U';

  showProfileMenu = false;
  showNotifications = false;

  notifications: NotificationItem[] = [];
  unreadCount = 0;
  pendingKycCount = 0;
  pendingEscalationsCount = 0;
  isMobileMenuOpen = false;
  
  // Search Widget
  searchLoanId: string = '';
  searchError: string = '';

  private notifSub?: Subscription;
  private officerSub?: Subscription;
  private adminSub?: Subscription;

  constructor(
    private authService: AuthService,
    private borrowerService: BorrowerService,
    private officerService: OfficerService,
    private adminService: AdminService,
    private router: Router,
    private http: HttpClient,
    private eRef: ElementRef
  ) {}

  ngOnInit(): void {
    this.role = this.authService.getRole();
    const user = this.authService.getCurrentUser();
    
    if (user && user.sub) {
      this.userName = user.sub.split('@')[0];
      this.userInitials = this.userName.substring(0, 2).toUpperCase();
    }

    if (this.role === 'ROLE_APPLICANT') {
      this.fetchNotifications();
    }
    
    if (this.role === 'ROLE_OFFICER') {
      this.officerSub = this.officerService.pendingKycCount$.subscribe(
        count => this.pendingKycCount = count
      );
      // Fetch initial data which triggers the subject
      this.officerService.getPendingKyc().subscribe();
    }

    if (this.role === 'ROLE_ADMIN') {
      this.adminSub = this.adminService.pendingEscalationsCount$.subscribe(
        count => this.pendingEscalationsCount = count
      );
      this.adminService.getDashboardMetrics().subscribe();
    }
  }

  getHomeRoute(): string {
    switch (this.role) {
      case 'ROLE_APPLICANT': return '/applicant';
      case 'ROLE_OFFICER': return '/officer';
      case 'ROLE_ADMIN': return '/admin/dashboard';
      default: return '/';
    }
  }

  ngOnDestroy(): void {
    if (this.notifSub) {
      this.notifSub.unsubscribe();
    }
    if (this.officerSub) {
      this.officerSub.unsubscribe();
    }
    if (this.adminSub) {
      this.adminSub.unsubscribe();
    }
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
    if (this.isMobileMenuOpen) {
      this.showProfileMenu = false;
      this.showNotifications = false;
    }
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  @HostListener('document:click', ['$event'])
  onClick(event: Event): void {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.showProfileMenu = false;
      this.showNotifications = false;
      this.isMobileMenuOpen = false;
    }
  }

  toggleProfileMenu(): void {
    this.showProfileMenu = !this.showProfileMenu;
    this.showNotifications = false;
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    this.showProfileMenu = false;
  }

  logout(): void {
    this.isMobileMenuOpen = false;
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  onSearchTrack(event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    this.searchError = '';
    const trimmedId = this.searchLoanId.trim();
    if (!trimmedId || trimmedId.length < 5 || !/^[A-Za-z0-9-]+$/.test(trimmedId)) {
      this.searchError = 'Invalid ID format';
      return;
    }

    // Attempt to fetch status to validate ownership
    this.borrowerService.getLoanStatus(trimmedId).subscribe({
      next: () => {
        this.searchLoanId = '';
        this.closeMobileMenu();
        this.router.navigate(['/applicant/loan', trimmedId, 'tracking']);
      },
      error: (err) => {
        if (err.status === 403 || err.status === 404) {
          this.searchError = 'Invalid Loan ID or access denied';
        } else {
          this.searchError = 'Unable to track right now';
        }
      }
    });
  }

  onApplyClick(event: Event): void {
    event.preventDefault();
    this.borrowerService.getDashboardMetrics().subscribe({
      next: (metrics) => {
        if (metrics.profileComplete) {
          this.router.navigate(['/applicant/apply']);
        } else {
          this.router.navigate(['/applicant'], { fragment: 'kyc-warning' });
        }
      },
      error: (err) => console.error('Failed to verify KYC status', err)
    });
  }

  fetchNotifications(): void {
    this.notifSub = this.http.get<NotificationItem[]>(`${environment.apiUrl}/notifications`).subscribe({
      next: (data) => {
        this.notifications = data;
        this.unreadCount = data.filter(n => !n.isRead).length;
      },
      error: (err) => console.error('Failed to load notifications', err)
    });
  }

  handleNotificationClick(notif: NotificationItem): void {
    this.showNotifications = false;
    if (!notif.isRead) {
      this.http.put(`${environment.apiUrl}/notifications/${notif.id}/read`, {}).subscribe({
        next: () => {
          notif.isRead = true;
          this.unreadCount = Math.max(0, this.unreadCount - 1);
          if (notif.linkUrl) {
            this.router.navigateByUrl(notif.linkUrl);
          }
        },
        error: (err) => {
          console.error('Error marking as read', err);
          if (notif.linkUrl) {
            this.router.navigateByUrl(notif.linkUrl);
          }
        }
      });
    } else if (notif.linkUrl) {
      this.router.navigateByUrl(notif.linkUrl);
    }
  }
}
