package com.openisle.repository;

import com.openisle.model.User;
import com.openisle.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findBySubscriber(User subscriber);
    List<UserSubscription> findByTarget(User target);
    Optional<UserSubscription> findBySubscriberAndTarget(User subscriber, User target);
    long countByTarget(User target);
    long countBySubscriber(User subscriber);
}
