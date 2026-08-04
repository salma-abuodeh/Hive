package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PollOptionResponse {
    private Long id;
    private String text;
    private long voteCount;
    private boolean votedByMe;
}