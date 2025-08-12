package com.openisle.repository;

import com.openisle.model.LotteryPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LotteryPostRepository extends JpaRepository<LotteryPost, Long> {
    List<LotteryPost> findByEndTimeAfterAndWinnersIsEmpty(LocalDateTime now);

    List<LotteryPost> findByEndTimeBeforeAndWinnersIsEmpty(LocalDateTime now);
}
