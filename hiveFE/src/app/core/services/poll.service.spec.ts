import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PollService } from './poll.service';
import { environment } from '../../../environments/environment';
import { PollRequest, PollResponse } from '../../features/polls/models/poll.models';

describe('PollService', () => {
  let service: PollService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/polls`;

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
    totalVotes: 0,
    options: [
      { id: 10, text: 'Pizza', voteCount: 0, votedByMe: false },
      { id: 11, text: 'Tacos', voteCount: 0, votedByMe: false }
    ]
  };

  const payload: PollRequest = {
    question: 'Where should we have lunch?',
    allowMultiple: false,
    visibility: 'COMPANY',
    teamId: null,
    options: [{ text: 'Pizza' }, { text: 'Tacos' }]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PollService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(PollService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('should list polls with pagination params', () => {
    service.list(2, 25).subscribe((res) => {
      expect(res.content).toEqual([mockPoll]);
    });

    const req = http.expectOne((request) =>
      request.url === baseUrl
      && request.params.get('page') === '2'
      && request.params.get('size') === '25'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [mockPoll] });
  });

  it('should fetch a poll by id', () => {
    service.getById(1).subscribe((res) => expect(res).toEqual(mockPoll));

    const req = http.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPoll);
  });

  it('should create a poll', () => {
    service.create(payload).subscribe((res) => expect(res).toEqual(mockPoll));

    const req = http.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockPoll);
  });

  it('should update a poll', () => {
    service.update(1, payload).subscribe((res) => expect(res).toEqual(mockPoll));

    const req = http.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush(mockPoll);
  });

  it('should delete a poll', () => {
    service.delete(1).subscribe((res) => expect(res).toBeNull());

    const req = http.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should submit a vote', () => {
    service.vote(1, { optionIds: [10] }).subscribe((res) => expect(res).toEqual(mockPoll));

    const req = http.expectOne(`${baseUrl}/1/vote`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ optionIds: [10] });
    req.flush(mockPoll);
  });

  it('should remove a vote', () => {
    service.unvote(1).subscribe((res) => expect(res).toEqual(mockPoll));

    const req = http.expectOne(`${baseUrl}/1/vote`);
    expect(req.request.method).toBe('DELETE');
    req.flush(mockPoll);
  });
});
