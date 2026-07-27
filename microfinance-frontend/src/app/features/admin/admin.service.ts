import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AuditLog {
  id: number;
  entityType: string;
  entityId: number;
  action: string;
  performedBy: string;
  createdAt: string;
  summary: string;
}

export interface AdminDashboardMetrics {
  mtdDisbursedAmount: number;
  pendingEscalations: number;
  systemRejectionRate: number;
  totalApplications: number;
  recentActivity: AuditLog[];
  riskDistribution: { [key: string]: number };
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiUrl}/admin`;
  public pendingEscalationsCount$ = new BehaviorSubject<number>(0);

  constructor(private http: HttpClient) {}

  getDashboardMetrics(): Observable<AdminDashboardMetrics> {
    return this.http.get<AdminDashboardMetrics>(`${this.apiUrl}/dashboard/metrics`).pipe(
      tap(data => this.pendingEscalationsCount$.next(data.pendingEscalations || 0))
    );
  }

  getAllProducts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/products`);
  }

  updateProductApr(id: number, interestRatePa: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/products/${id}/apr`, { interestRatePa });
  }

  createProduct(productData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/products`, productData);
  }

  updateProduct(id: number, productData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/products/${id}`, productData);
  }

  toggleProductStatus(id: number, active: boolean): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/products/${id}/status`, { active });
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/products/${id}`);
  }

  getAllStaff(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/staff`);
  }

  createStaff(staffData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/staff`, staffData);
  }

  disableStaff(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/staff/${id}/disable`, {});
  }

  enableStaff(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/staff/${id}/enable`, {});
  }

  resetStaffPassword(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/staff/${id}/reset-password`, {});
  }
}
