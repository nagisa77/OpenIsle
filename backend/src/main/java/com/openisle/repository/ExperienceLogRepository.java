package com.openisle.repository;

import com.openisle.model.ExperienceLog;
import com.openisle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExperienceLogRepository extends JpaRepository<ExperienceLog, Long> {
    Optional<ExperienceLog> findByUserAndLogDate(User user, LocalDate logDate);
}
