import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PollsList } from './polls-list';
import { PollService } from '../../../../core/services/poll.service';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { PollResponse } from '../../models/poll.models';

describe('PollsList', () => {
  let component: PollsList;
  let fixture: ComponentFixture<PollsList>;
  let pollServiceSpy: jasmine.SpyObj<PollService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let teamServiceSpy: jasmine.SpyObj<TeamService>;

  const mockPoll: PollResponse = {
    id: 1,
    companyId: 1,
    teamId: null,
    createdByUserId: 100,
    createdByName: 'Test User',
    question: 'Where should we have lunch?',
    description: null,
    allowMultiple: false,
    closesAt: null,
    visibility: 'COMPANY',
    active: true,
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00',
    totalVotes: 2,
    options: [
      { id: 10, text: 'Pizza', voteCount: 1, votedByMe: false },
      { id: 11, text: 'Tacos', voteCount: 1, votedByMe: false }
    ]
  };

  beforeEach(async () => {
    pollServiceSpy = jasmine.createSpyObj('PollService', ['list', 'create', 'update', 'delete', 'vote', 'unvote']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getUser', 'hasPermission']);
    teamServiceSpy = jasmine.createSpyObj('TeamService', ['listMine']);

    pollServiceSpy.list.and.returnValue(of({
      content: [mockPoll],
      totalElements: 1,
      totalPages: 1,
      size: 50,
      number: 0,
      first: true,
      last: true,
      empty: false
    }));
    authServiceSpy.getUser.and.returnValue({ id: 100 } as any);
    authServiceSpy.hasPermission.and.returnValue(false);
    teamServiceSpy.listMine.and.returnValue(of([
      { id: 7, name: 'Product', active: true, memberCount: 4 }
    ]));

    await TestBed.configureTestingModule({
      imports: [PollsList],
      providers: [
        { provide: PollService, useValue: pollServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: TeamService, useValue: teamServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PollsList);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load polls on init and select the first one', () => {
    fixture.detectChanges();
    expect(teamServiceSpy.listMine).toHaveBeenCalled();
    expect(pollServiceSpy.list).toHaveBeenCalledWith(0, 50);
    expect(component.polls().length).toBe(1);
    expect(component.selectedPoll()?.id).toBe(1);
  });

  it('should surface an error message when loading fails', () => {
    pollServiceSpy.list.and.returnValue(throwError(() => ({ error: { message: 'Server exploded' } })));
    fixture.detectChanges();
    expect(component.error()).toBe('Server exploded');
    expect(component.loading()).toBeFalse();
  });

  describe('form validation', () => {
    beforeEach(() => fixture.detectChanges());

    it('should reject submit with a blank question', () => {
      component.question = '   ';
      component.optionTexts = ['Pizza', 'Tacos'];
      component.submit();
      expect(component.formError()).toBe('Question is required');
      expect(pollServiceSpy.create).not.toHaveBeenCalled();
    });

    it('should reject submit with fewer than 2 non-empty options', () => {
      component.question = 'Lunch?';
      component.optionTexts = ['Pizza', '   '];
      component.submit();
      expect(component.formError()).toBe('A poll needs at least 2 options');
      expect(pollServiceSpy.create).not.toHaveBeenCalled();
    });

    it('should call create with trimmed options when valid', () => {
      pollServiceSpy.create.and.returnValue(of(mockPoll));
      component.question = ' Lunch? ';
      component.optionTexts = [' Pizza ', ' Tacos ', '   '];
      component.visibility = 'COMPANY';
      component.submit();

      expect(pollServiceSpy.create).toHaveBeenCalledWith(jasmine.objectContaining({
        question: 'Lunch?',
        teamId: null,
        options: [{ text: 'Pizza' }, { text: 'Tacos' }]
      }));
    });

    it('should require a team when creating a team poll', () => {
      component.question = 'Team lunch?';
      component.optionTexts = ['Pizza', 'Tacos'];
      component.visibility = 'TEAM';
      component.teamId = null;

      component.submit();

      expect(component.formError()).toBe('Choose a team for team visibility');
      expect(pollServiceSpy.create).not.toHaveBeenCalled();
    });

    it('should include the team id when creating a team poll', () => {
      pollServiceSpy.create.and.returnValue(of({ ...mockPoll, visibility: 'TEAM', teamId: 7 }));
      component.question = 'Team lunch?';
      component.optionTexts = ['Pizza', 'Tacos'];
      component.visibility = 'TEAM';
      component.teamId = 7;

      component.submit();

      expect(pollServiceSpy.create).toHaveBeenCalledWith(jasmine.objectContaining({
        visibility: 'TEAM',
        teamId: 7
      }));
    });

    it('should populate fields when editing a poll', () => {
      const teamPoll = {
        ...mockPoll,
        description: 'Pick one',
        allowMultiple: true,
        visibility: 'TEAM' as const,
        teamId: 7,
        closesAt: '2026-01-02T14:30:00'
      };

      component.openEditForm(teamPoll);

      expect(component.editingId).toBe(1);
      expect(component.question).toBe('Where should we have lunch?');
      expect(component.description).toBe('Pick one');
      expect(component.allowMultiple).toBeTrue();
      expect(component.visibility).toBe('TEAM');
      expect(component.teamId).toBe(7);
      expect(component.closesAtDate).toBe('2026-01-02');
      expect(component.closesAtTime).toBe('14:30');
    });

    it('should call update when editing an existing poll', () => {
      pollServiceSpy.update.and.returnValue(of(mockPoll));
      component.openEditForm(mockPoll);
      component.question = 'Updated lunch?';
      component.optionTexts = ['Pizza', 'Tacos'];

      component.submit();

      expect(pollServiceSpy.update).toHaveBeenCalledWith(1, jasmine.objectContaining({
        question: 'Updated lunch?'
      }));
    });
  });

  describe('voting', () => {
    beforeEach(() => fixture.detectChanges());

    it('should replace selection for a single-choice poll', () => {
      component.selectPoll(mockPoll);
      component.toggleOption(10);
      component.toggleOption(11);
      expect(Array.from(component.selectedOptionIds())).toEqual([11]);
    });

    it('should accumulate selections for a multi-choice poll', () => {
      const multiPoll = { ...mockPoll, allowMultiple: true };
      component.selectPoll(multiPoll);
      component.toggleOption(10);
      component.toggleOption(11);
      expect(new Set(component.selectedOptionIds())).toEqual(new Set([10, 11]));
    });

    it('should call vote with selected option ids', () => {
      pollServiceSpy.vote.and.returnValue(of(mockPoll));
      component.selectPoll(mockPoll);
      component.toggleOption(10);
      component.submitVote();
      expect(pollServiceSpy.vote).toHaveBeenCalledWith(1, { optionIds: [10] });
    });

    it('should call unvote for the selected poll', () => {
      const votedPoll = {
        ...mockPoll,
        options: [
          { ...mockPoll.options[0], votedByMe: true },
          mockPoll.options[1]
        ]
      };
      pollServiceSpy.unvote.and.returnValue(of(mockPoll));
      component.selectPoll(votedPoll);

      component.clearVote();

      expect(pollServiceSpy.unvote).toHaveBeenCalledWith(1);
    });

    it('should not submit a vote with nothing selected', () => {
      component.selectPoll(mockPoll);
      component.selectedOptionIds.set(new Set());
      component.submitVote();
      expect(pollServiceSpy.vote).not.toHaveBeenCalled();
    });
  });

  describe('delete', () => {
    beforeEach(() => fixture.detectChanges());

    it('should delete a poll after confirmation', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      pollServiceSpy.delete.and.returnValue(of(void 0));

      component.remove(mockPoll);

      expect(pollServiceSpy.delete).toHaveBeenCalledWith(1);
      expect(pollServiceSpy.list).toHaveBeenCalledTimes(2);
    });

    it('should not delete a poll when confirmation is declined', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.remove(mockPoll);

      expect(pollServiceSpy.delete).not.toHaveBeenCalled();
    });
  });

  describe('permissions', () => {
    beforeEach(() => fixture.detectChanges());

    it('should allow the poll creator to manage it regardless of permissions', () => {
      expect(component.canManage(mockPoll)).toBeTrue();
    });

    it('should allow a non-creator with POLL_UPDATE to manage it', () => {
      authServiceSpy.getUser.and.returnValue({ id: 999 } as any);
      authServiceSpy.hasPermission.and.callFake((p: string) => p === 'POLL_UPDATE');
      expect(component.canManage(mockPoll)).toBeTrue();
    });

    it('should deny management to a non-creator without permission', () => {
      authServiceSpy.getUser.and.returnValue({ id: 999 } as any);
      authServiceSpy.hasPermission.and.returnValue(false);
      expect(component.canManage(mockPoll)).toBeFalse();
    });
  });
});
