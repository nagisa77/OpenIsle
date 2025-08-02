package com.openisle.repository;

import com.openisle.model.Notification;
import com.openisle.model.User;
import com.openisle.model.Post;
import com.openisle.model.Comment;
import com.openisle.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for Notification entities. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndReadOrderByCreatedAtDesc(User user, boolean read);
    long countByUserAndRead(User user, boolean read);
    List<Notification> findByPost(Post post);
    List<Notification> findByComment(Comment comment);

    void deleteByTypeAndFromUser(NotificationType type, User fromUser);

    void deleteByTypeAndFromUserAndPost(NotificationType type, User fromUser, Post post);
}
