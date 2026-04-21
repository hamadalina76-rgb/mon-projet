import { DispatchStoreService } from './dispatch-store.service';

describe('DispatchStoreService', () => {
  let service: DispatchStoreService;

  beforeEach(() => {
    service = new DispatchStoreService();
  });

  it('updates kpis in snapshot', () => {
    service.setKpis({
      firstCycleDispatchRate: 86,
      averageAssignmentDelaySeconds: 70,
      deliveriesPerCourierPerHour: 3.1,
      bundlingRate: 40,
      failureRate: 1.2,
      onTimeRate: 93,
    });
    expect(service.snapshot.kpis?.firstCycleDispatchRate).toBe(86);
  });
});
