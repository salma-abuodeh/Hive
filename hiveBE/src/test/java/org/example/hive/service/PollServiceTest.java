package org.example.hive.service;

import org.example.hive.dto.request.VoteRequest;
import org.example.hive.exception.PollException;
import org.example.hive.repository.PollOptionRepository;
import org.example.hive.repository.PollRepository;
import org.example.hive.repository.PollVoteRepository;
import org.example.hive.mapper.PollMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollOptionRepository pollOptionRepository;

    @Mock
    private PollVoteRepository pollVoteRepository;

    @Mock
    private PollMapper pollMapper;

    @InjectMocks
    private PollService pollService;

    @Test
    @DisplayName("Should throw PollException when voting on missing poll")
    void vote_PollNotFound() {
        VoteRequest voteRequest = new VoteRequest();

        when(pollRepository.findByIdAndCompany_Id(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.vote(999L, 10L, 1L, voteRequest))
                .isInstanceOf(PollException.class)
                .hasMessage("Poll not found");
    }

    @Test
    @DisplayName("Should throw PollException when deleting a missing poll")
    void delete_PollNotFound() {
        when(pollRepository.findByIdAndCompany_Id(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.delete(999L, 10L, 1L))
                .isInstanceOf(PollException.class)
                .hasMessage("Poll not found");
    }
}