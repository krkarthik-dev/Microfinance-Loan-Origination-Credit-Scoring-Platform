import { ComponentFixture, TestBed } from '@angular/core/testing';

import { KycReviewComponent } from './kyc-review.component';

describe('KycReviewComponent', () => {
  let component: KycReviewComponent;
  let fixture: ComponentFixture<KycReviewComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [KycReviewComponent]
    });
    fixture = TestBed.createComponent(KycReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
