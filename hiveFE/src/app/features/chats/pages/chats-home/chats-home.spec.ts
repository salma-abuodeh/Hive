import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatsHome } from './chats-home';

describe('ChatsHome', () => {
  let fixture: ComponentFixture<ChatsHome>;
  let component: ChatsHome;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ChatsHome] }).compileComponents();
    fixture = TestBed.createComponent(ChatsHome);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders an empty conversation list and the initial picker state', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('No conversations yet.');
    expect(text).toContain('Pick a conversation');
  });

  it('shows a selected conversation when one is provided by future data', () => {
    component.selectConversation({
      id: 'engineering',
      type: 'team',
      name: 'Engineering Team',
      initials: 'EN',
      preview: 'Latest update',
      timestamp: '09:58',
      participants: [],
      messages: [{ id: 'message-1', sender: 'Rana', initials: 'RN', body: 'The latest build is ready.', sentAt: '09:58' }],
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Engineering Team');
    expect(fixture.nativeElement.textContent).toContain('The latest build is ready.');
  });
});
