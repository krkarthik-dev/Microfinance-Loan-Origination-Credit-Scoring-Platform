import { Component } from '@angular/core';

@Component({
  selector: 'app-unauthorized',
  template: `
    <div class="unauth-container">
      <div class="content">
        <h1>403</h1>
        <h2>Access Denied</h2>
        <p>You do not have permission to view this page.</p>
        <button routerLink="/">Return Home</button>
      </div>
    </div>
  `,
  styles: [`
    .unauth-container {
      height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f8f9fa;
      text-align: center;
      font-family: 'Inter', sans-serif;
    }
    .content h1 {
      font-size: 72px;
      margin: 0;
      color: #e03131;
    }
    .content h2 {
      margin: 10px 0;
      color: #343a40;
    }
    .content p {
      color: #868e96;
      margin-bottom: 24px;
    }
    button {
      padding: 10px 20px;
      border: none;
      background-color: #1e3c72;
      color: white;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 500;
    }
    button:hover {
      background-color: #152a51;
    }
  `]
})
export class UnauthorizedComponent {}
