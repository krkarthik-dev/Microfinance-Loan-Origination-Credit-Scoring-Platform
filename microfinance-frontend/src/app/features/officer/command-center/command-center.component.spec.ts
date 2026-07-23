import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CommandCenterComponent } from './command-center.component';

describe('CommandCenterComponent', () => {
  let component: CommandCenterComponent;
  let fixture: ComponentFixture<CommandCenterComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CommandCenterComponent]
    });
    fixture = TestBed.createComponent(CommandCenterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
