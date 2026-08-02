package org.example.hive.mapper;

import org.example.hive.dto.response.PollOptionResponse;
import org.example.hive.dto.response.PollResponse;
import org.example.hive.model.Poll;
import org.example.hive.model.PollOption;
import org.example.hive.model.PollVote;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class PollMapper {

    private PollMapper() {
    }

    public static PollResponse toResponse(Poll poll, List<PollOption> options, List<PollVote> allVotes, Long requesterId) {
        Map<Long, Long> countByOption = allVotes.stream()
                .collect(Collectors.groupingBy(v -> v.getOption().getId(), Collectors.counting()));

        Set<Long> myVotedOptionIds = allVotes.stream()
                .filter(v -> v.getUser().getId().equals(requesterId))
                .map(v -> v.getOption().getId())
                .collect(Collectors.toSet());

        List<PollOptionResponse> optionResponses = options.stream()
                .map(o -> new PollOptionResponse(
                        o.getId(),
                        o.getOptionText(),
                        countByOption.getOrDefault(o.getId(), 0L),
                        myVotedOptionIds.contains(o.getId())
                ))
                .toList();

        return new PollResponse(
                poll.getId(),
                poll.getCompany().getId(),
                poll.getCreatedBy().getId(),
                poll.getCreatedBy().getFirstName() + " " + poll.getCreatedBy().getLastName(),
                poll.getQuestion(),
                poll.getDescription(),
                poll.getAllowMultiple(),
                poll.getClosesAt(),
                poll.getVisibility(),
                poll.getActive(),
                poll.getCreatedAt(),
                poll.getUpdatedAt(),
                allVotes.size(),
                optionResponses
        );
    }
}