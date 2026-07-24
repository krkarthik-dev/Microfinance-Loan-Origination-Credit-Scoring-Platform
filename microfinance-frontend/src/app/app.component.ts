import { Component, OnInit } from '@angular/core';
import { AuthService } from './core/services/auth.service';

/**
 * Root application component.
 * Renders the router outlet which loads feature components based on route.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'Microfinance Loan Origination Platform';

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }
}
