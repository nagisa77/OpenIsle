package com.openisle.repository;

import com.openisle.model.PushSubscription;
import com.openisle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    List<PushSubscription> findByUser(User user);
    void deleteByUserAndEndpoint(User user, String endpoint);
}
