package com.openisle.repository;

import com.openisle.model.ContributorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContributorConfigRepository extends JpaRepository<ContributorConfig, Long> {
    Optional<ContributorConfig> findByUserIname(String userIname);
}

