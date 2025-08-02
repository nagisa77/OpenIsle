package com.openisle.repository;

import com.openisle.model.AiFormatUsage;
import com.openisle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiFormatUsageRepository extends JpaRepository<AiFormatUsage, Long> {
    Optional<AiFormatUsage> findByUserAndUseDate(User user, LocalDate useDate);
}
