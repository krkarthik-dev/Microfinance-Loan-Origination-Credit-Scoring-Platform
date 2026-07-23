import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HealthService, HealthResponse } from './health.service';
import { environment } from '../../../environments/environment';

/**
 * Unit tests for HealthService.
 *
 * Uses HttpClientTestingModule to mock HTTP calls without
 * making real network requests.
 */
describe('HealthService', () => {
  let service: HealthService;
  let httpMock: HttpTestingController;

  const mockHealthResponse: HealthResponse = {
    status: 'UP',
    service: 'Microfinance Loan Origination Platform',
    version: '1.0.0',
    timestamp: '2026-07-23 12:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [HealthService]
    });

    service = TestBed.inject(HealthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify no unexpected HTTP calls were made
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call the correct health endpoint URL', () => {
    service.getHealth().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/health`);
    expect(req.request.method).toBe('GET');
    req.flush(mockHealthResponse);
  });

  it('should return health response with status UP', () => {
    let result: HealthResponse | undefined;

    service.getHealth().subscribe(data => {
      result = data;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/health`);
    req.flush(mockHealthResponse);

    expect(result).toBeTruthy();
    expect(result?.status).toBe('UP');
  });

  it('should return correct service name in response', () => {
    let result: HealthResponse | undefined;

    service.getHealth().subscribe(data => {
      result = data;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/health`);
    req.flush(mockHealthResponse);

    expect(result?.service).toBe('Microfinance Loan Origination Platform');
  });

  it('should return correct version in response', () => {
    let result: HealthResponse | undefined;

    service.getHealth().subscribe(data => {
      result = data;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/health`);
    req.flush(mockHealthResponse);

    expect(result?.version).toBe('1.0.0');
  });

  it('should use GET HTTP method', () => {
    service.getHealth().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/health`);
    expect(req.request.method).toEqual('GET');
    req.flush(mockHealthResponse);
  });
});
