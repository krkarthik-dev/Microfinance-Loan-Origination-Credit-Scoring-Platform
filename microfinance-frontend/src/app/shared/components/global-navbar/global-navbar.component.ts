import { Component, OnInit, OnDestroy, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
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
  imports: [CommonModule, RouterModule],
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
  private notifSub?: Subscription;

  constructor(
    private authService: AuthService,
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
  }

  ngOnDestroy(): void {
    if (this.notifSub) {
      this.notifSub.unsubscribe();
    }
  }

  @HostListener('document:click', ['$event'])
  clickout(event: any) {
    if(!this.eRef.nativeElement.contains(event.target)) {
      this.showProfileMenu = false;
      this.showNotifications = false;
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
    this.authService.logout();
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
