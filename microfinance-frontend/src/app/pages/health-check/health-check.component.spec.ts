import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { HealthCheckComponent } from './health-check.component';
import { HealthService, HealthResponse } from '../../core/services/health.service';

/**
 * Unit tests for HealthCheckComponent.
 *
 * HealthService is mocked using jasmine.createSpyObj to isolate
 * component logic from HTTP calls.
 */
describe('HealthCheckComponent', () => {
  let component: HealthCheckComponent;
  let fixture: ComponentFixture<HealthCheckComponent>;
  let healthServiceSpy: jasmine.SpyObj<HealthService>;

  const mockHealthResponse: HealthResponse = {
    status: 'UP',
    service: 'Microfinance Loan Origination Platform',
    version: '1.0.0',
    timestamp: '2026-07-23 12:00:00'
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('HealthService', ['getHealth']);

    await TestBed.configureTestingModule({
      declarations: [HealthCheckComponent],
      imports: [HttpClientTestingModule],
      providers: [
        { provide: HealthService, useValue: spy }
      ]
    }).compileComponents();

    healthServiceSpy = TestBed.inject(HealthService) as jasmine.SpyObj<HealthService>;
  });

  // Set up component after configuring the spy response
  function createComponent(response = of(mockHealthResponse)): void {
    healthServiceSpy.getHealth.and.returnValue(response);
    fixture = TestBed.createComponent(HealthCheckComponent);
    component = fixture.componentInstance;
    // Suppress component's console.error during error-state tests.
    // This prevents Karma from treating logged errors as test failures.
    spyOn(console, 'error');
    fixture.detectChanges(); // triggers ngOnInit → checkHealth()
  }

  it('should create the component', () => {
    createComponent();
    expect(component).toBeTruthy();
  });

  it('should call getHealth() on init', () => {
    createComponent();
    expect(healthServiceSpy.getHealth).toHaveBeenCalledTimes(1);
  });

  it('should set healthData when API call succeeds', () => {
    createComponent();
    expect(component.healthData).toEqual(mockHealthResponse);
  });

  it('should set isLoading to false after successful API call', () => {
    createComponent();
    expect(component.isLoading).toBeFalse();
  });

  it('should set hasError to false on successful response', () => {
    createComponent();
    expect(component.hasError).toBeFalse();
  });

  it('should set hasError to true when API call fails', () => {
    createComponent(throwError(() => new Error('Connection refused')));
    expect(component.hasError).toBeTrue();
  });

  it('should set isLoading to false after API error', () => {
    createComponent(throwError(() => new Error('Connection refused')));
    expect(component.isLoading).toBeFalse();
  });

  it('should set errorMessage when API call fails', () => {
    createComponent(throwError(() => new Error('Connection refused')));
    expect(component.errorMessage).toBeTruthy();
    expect(component.errorMessage.length).toBeGreaterThan(0);
  });

  it('should call getHealth() again when checkHealth() is called manually', () => {
    createComponent();
    component.checkHealth();
    expect(healthServiceSpy.getHealth).toHaveBeenCalledTimes(2);
  });

  it('should have healthData with status UP on success', () => {
    createComponent();
    expect(component.healthData?.status).toBe('UP');
  });
});
