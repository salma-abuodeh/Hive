import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NotificationsHome } from './notifications-home';

describe('NotificationsHome', () => {
  let component: NotificationsHome;
  let fixture: ComponentFixture<NotificationsHome>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationsHome],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationsHome);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
