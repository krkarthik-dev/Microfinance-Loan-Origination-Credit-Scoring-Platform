import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoanProduct {
  id: number;
  productName: string;
  description: string;
  minAmount: number;
  maxAmount: number;
  interestRatePa: number;
  minTenureMonths: number;
  maxTenureMonths: number;
  processingFeePct: number;
  active: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getActiveProducts(): Observable<LoanProduct[]> {
    return this.http.get<LoanProduct[]>(this.apiUrl);
  }
}
