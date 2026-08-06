package org.example.hive.repository;

import org.example.hive.model.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findAllByPoll_IdOrderByDisplayOrderAsc(Long pollId);

    void deleteAllByPoll_Id(Long pollId);
}