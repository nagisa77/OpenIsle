package com.openisle.service;

import com.openisle.model.*;
import com.openisle.dto.NotificationPreferenceDto;
import com.openisle.repository.NotificationRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.openisle.service.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Map;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/** Service for creating and retrieving notifications. */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final PushNotificationService pushNotificationService;
    private final ReactionRepository reactionRepository;
    private final Executor notificationExecutor;

    @Value("${app.website-url:https://www.open-isle.com}")
    private String websiteUrl;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\[([^\\]]+)\\]");

    private String buildPayload(String body, String url) {
        // Ensure push notifications contain a link to the related resource so
        // that verifications can assert its presence and users can navigate
        // directly from the notification.
        if (url == null || url.isBlank()) {
            return body;
        }
        return body + ", 点击以查看: " + url;
    }

    public void sendCustomPush(User user, String body, String url) {
        pushNotificationService.sendNotification(user, buildPayload(body, url));
    }

    public Notification createNotification(User user, NotificationType type, Post post, Comment comment, Boolean approved) {
        return createNotification(user, type, post, comment, approved, null, null, null);
    }

    public Notification createNotification(User user, NotificationType type, Post post, Comment comment, Boolean approved,
                                           User fromUser, ReactionType reactionType, String content) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setPost(post);
        n.setComment(comment);
        n.setApproved(approved);
        n.setFromUser(fromUser);
        n.setReactionType(reactionType);
        n.setContent(content);
        if (type == NotificationType.POST_VIEWED && fromUser != null && post != null) {
            notificationRepository.deleteByTypeAndFromUserAndPost(type, fromUser, post);
        }
        n = notificationRepository.save(n);

//        Runnable asyncTask = () -> {
            if (type == NotificationType.COMMENT_REPLY && user.getEmail() != null && post != null && comment != null) {
                String url = String.format("%s/posts/%d#comment-%d", websiteUrl, post.getId(), comment.getId());
                emailSender.sendEmail(user.getEmail(), "有人回复了你", url);
                sendCustomPush(user, "有人回复了你", url);
            } else if (type == NotificationType.REACTION && comment != null) {
//                long count = reactionRepository.countReceived(comment.getAuthor().getUsername());
//                if (count % 5 == 0) {
//                    String url = websiteUrl + "/messages";
//                    sendCustomPush(comment.getAuthor(), "你有新的互动", url);
//                    if (comment.getAuthor().getEmail() != null) {
//                        emailSender.sendEmail(comment.getAuthor().getEmail(), "你有新的互动", "你有新的互动, 点击以查看: " + url);
//                    }
//                }
            } else if (type == NotificationType.REACTION && post != null) {
//                long count = reactionRepository.countReceived(post.getAuthor().getUsername());
//                if (count % 5 == 0) {
//                    String url = websiteUrl + "/messages";
//                    sendCustomPush(post.getAuthor(), "你有新的互动", url);
//                    if (post.getAuthor().getEmail() != null) {
//                        emailSender.sendEmail(post.getAuthor().getEmail(), "你有新的互动", "你有新的互动, 点击以查看: " + url);
//                    }
//                }
            }
//        };

//        if (TransactionSynchronizationManager.isSynchronizationActive()) {
//            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//                @Override
//                public void afterCommit() {
//                    notificationExecutor.execute(asyncTask);
//                }
//            });
//        } else {
//            notificationExecutor.execute(asyncTask);
//        }

        return n;
    }

    /**
     * Create notifications for all admins when a user submits a register request.
     * Old register request notifications from the same applicant are removed first.
     */
    @org.springframework.transaction.annotation.Transactional
    public void createRegisterRequestNotifications(User applicant, String reason) {
        notificationRepository.deleteByTypeAndFromUser(NotificationType.REGISTER_REQUEST, applicant);
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            createNotification(admin, NotificationType.REGISTER_REQUEST, null, null,
                    null, applicant, null, reason);
        }
    }

    /**
     * Create notifications for all admins when a user redeems an activity.
     * Old redeem notifications from the same user are removed first.
     */
    @org.springframework.transaction.annotation.Transactional
    public void createActivityRedeemNotifications(User user, String content) {
        notificationRepository.deleteByTypeAndFromUser(NotificationType.ACTIVITY_REDEEM, user);
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            createNotification(admin, NotificationType.ACTIVITY_REDEEM, null, null,
                    null, user, null, content);
        }
    }

    public List<NotificationPreferenceDto> listPreferences(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Set<NotificationType> disabled = user.getDisabledNotificationTypes();
        List<NotificationPreferenceDto> prefs = new ArrayList<>();
        for (NotificationType nt : NotificationType.values()) {
            NotificationPreferenceDto dto = new NotificationPreferenceDto();
            dto.setType(nt);
            dto.setEnabled(!disabled.contains(nt));
            prefs.add(dto);
        }
        return prefs;
    }

    public void updatePreference(String username, NotificationType type, boolean enabled) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Set<NotificationType> disabled = user.getDisabledNotificationTypes();
        if (enabled) {
            disabled.remove(type);
        } else {
            disabled.add(type);
        }
        userRepository.save(user);
    }

    public List<Notification> listNotifications(String username, Boolean read) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Set<NotificationType> disabled = user.getDisabledNotificationTypes();
        List<Notification> list;
        if (read == null) {
            list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        } else {
            list = notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, read);
        }
        return list.stream().filter(n -> !disabled.contains(n.getType())).collect(Collectors.toList());
    }

    public void markRead(String username, List<Long> ids) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        List<Notification> notifs = notificationRepository.findAllById(ids);
        for (Notification n : notifs) {
            if (n.getUser().getId().equals(user.getId())) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(notifs);
    }

    public long countUnread(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Set<NotificationType> disabled = user.getDisabledNotificationTypes();
        return notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, false).stream()
                .filter(n -> !disabled.contains(n.getType())).count();
    }

    public void notifyMentions(String content, User fromUser, Post post, Comment comment) {
        if (content == null || fromUser == null) {
            return;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> names = new HashSet<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        for (String name : names) {
            userRepository.findByUsername(name).ifPresent(target -> {
                if (!target.getId().equals(fromUser.getId())) {
                    createNotification(target, NotificationType.MENTION, post, comment, null, fromUser, null, null);
                }
            });
        }
    }
}
