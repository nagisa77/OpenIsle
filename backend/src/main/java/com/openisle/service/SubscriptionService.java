package com.openisle.service;

import com.openisle.model.*;
import com.openisle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final PostSubscriptionRepository postSubRepo;
    private final CommentSubscriptionRepository commentSubRepo;
    private final UserSubscriptionRepository userSubRepo;
    private final UserRepository userRepo;
    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final NotificationService notificationService;

    public void subscribePost(String username, Long postId) {
        User user = userRepo.findByUsername(username).orElseThrow();
        Post post = postRepo.findById(postId).orElseThrow();
        postSubRepo.findByUserAndPost(user, post).orElseGet(() -> {
            PostSubscription ps = new PostSubscription();
            ps.setUser(user);
            ps.setPost(post);
            if (!user.getId().equals(post.getAuthor().getId())) {
                notificationService.createNotification(post.getAuthor(),
                        NotificationType.POST_SUBSCRIBED, post, null, null, user, null, null);
            }
            return postSubRepo.save(ps);
        });
    }

    public void unsubscribePost(String username, Long postId) {
        User user = userRepo.findByUsername(username).orElseThrow();
        Post post = postRepo.findById(postId).orElseThrow();
        postSubRepo.findByUserAndPost(user, post).ifPresent(ps -> {
            postSubRepo.delete(ps);
            if (!user.getId().equals(post.getAuthor().getId())) {
                notificationService.createNotification(post.getAuthor(),
                        NotificationType.POST_UNSUBSCRIBED, post, null, null, user, null, null);
            }
        });
    }

    public void subscribeComment(String username, Long commentId) {
        User user = userRepo.findByUsername(username).orElseThrow();
        Comment comment = commentRepo.findById(commentId).orElseThrow();
        commentSubRepo.findByUserAndComment(user, comment).orElseGet(() -> {
            CommentSubscription cs = new CommentSubscription();
            cs.setUser(user);
            cs.setComment(comment);
            return commentSubRepo.save(cs);
        });
    }

    public void unsubscribeComment(String username, Long commentId) {
        User user = userRepo.findByUsername(username).orElseThrow();
        Comment comment = commentRepo.findById(commentId).orElseThrow();
        commentSubRepo.findByUserAndComment(user, comment).ifPresent(commentSubRepo::delete);
    }

    public void subscribeUser(String username, String targetName) {
        if (username.equals(targetName)) return;
        User subscriber = userRepo.findByUsername(username).orElseThrow();
        User target = findUser(targetName).orElseThrow();
        userSubRepo.findBySubscriberAndTarget(subscriber, target).orElseGet(() -> {
            UserSubscription us = new UserSubscription();
            us.setSubscriber(subscriber);
            us.setTarget(target);
            notificationService.createNotification(target,
                    NotificationType.USER_FOLLOWED, null, null, null, subscriber, null, null);
            return userSubRepo.save(us);
        });
    }

    public void unsubscribeUser(String username, String targetName) {
        User subscriber = userRepo.findByUsername(username).orElseThrow();
        User target = findUser(targetName).orElseThrow();
        userSubRepo.findBySubscriberAndTarget(subscriber, target).ifPresent(us -> {
            userSubRepo.delete(us);
            notificationService.createNotification(target,
                    NotificationType.USER_UNFOLLOWED, null, null, null, subscriber, null, null);
        });
    }

    public List<User> getSubscribedUsers(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        return userSubRepo.findBySubscriber(user).stream().map(UserSubscription::getTarget).toList();
    }

    public List<User> getSubscribers(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        return userSubRepo.findByTarget(user).stream().map(UserSubscription::getSubscriber).toList();
    }

    public List<User> getPostSubscribers(Long postId) {
        Post post = postRepo.findById(postId).orElseThrow();
        return postSubRepo.findByPost(post).stream().map(PostSubscription::getUser).toList();
    }

    public List<User> getCommentSubscribers(Long commentId) {
        Comment c = commentRepo.findById(commentId).orElseThrow();
        return commentSubRepo.findByComment(c).stream().map(CommentSubscription::getUser).toList();
    }


    public long countSubscribers(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        return userSubRepo.countByTarget(user);
    }

    public long countSubscribed(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        return userSubRepo.countBySubscriber(user);
    }

    public boolean isSubscribed(String subscriberName, String targetName) {
        if (subscriberName == null || targetName == null || subscriberName.equals(targetName)) {
            return false;
        }
        Optional<User> subscriber = userRepo.findByUsername(subscriberName);
        Optional<User> target = findUser(targetName);
        if (subscriber.isEmpty() || target.isEmpty()) {
            // 修改个人信息会出现，先不抛出错误
            return false;
        }
        return userSubRepo.findBySubscriberAndTarget(subscriber.get(), target.get()).isPresent();
    }

    public boolean isPostSubscribed(String username, Long postId) {
        if (username == null || postId == null) {
            return false;
        }
        User user = userRepo.findByUsername(username).orElseThrow();
        Post post = postRepo.findById(postId).orElseThrow();
        return postSubRepo.findByUserAndPost(user, post).isPresent();
    }

    private Optional<User> findUser(String identifier) {
        if (identifier.matches("\\d+")) {
            return userRepo.findById(Long.parseLong(identifier));
        }
        return userRepo.findByUsername(identifier);
    }
}
