import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DispatchApiService } from './dispatch-api.service';
import { environment } from '@environments/environment';

describe('DispatchApiService', () => {
  let service: DispatchApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(DispatchApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('calls dashboard kpis endpoint', () => {
    service.getKpis().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/dispatch/dashboard/kpis`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('calls dispatch-config general endpoint', () => {
    service.getDispatchConfigGeneral().subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/dispatch/dispatch-config/general`);
    expect(req.request.method).toBe('GET');
    req.flush({ meta: { activeVersion: 1, optimisticLock: 1, redisConfigVersion: 1 }, data: {} });
  });
});
