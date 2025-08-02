package com.openisle.repository;

import com.openisle.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Activity findByType(com.openisle.model.ActivityType type);
}
