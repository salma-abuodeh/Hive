package org.example.hive.repository;

import org.example.hive.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    List<PollVote> findAllByPoll_Id(Long pollId);

    List<PollVote> findAllByPoll_IdAndUser_Id(Long pollId, Long userId);

    long countByOption_Id(Long optionId);

    void deleteAllByPoll_IdAndUser_Id(Long pollId, Long userId);

    void deleteAllByPoll_Id(Long pollId);
}