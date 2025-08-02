package com.openisle.repository;

import com.openisle.model.Post;
import com.openisle.model.PostSubscription;
import com.openisle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostSubscriptionRepository extends JpaRepository<PostSubscription, Long> {
    List<PostSubscription> findByPost(Post post);
    List<PostSubscription> findByUser(User user);
    Optional<PostSubscription> findByUserAndPost(User user, Post post);
}
