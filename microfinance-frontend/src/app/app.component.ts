import { Component } from '@angular/core';

/**
 * Root application component.
 * Renders the router outlet which loads feature components based on route.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'Microfinance Loan Origination Platform';
}
