package com.openisle.service;

import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.User;
import com.openisle.model.NotificationType;
import com.openisle.model.CommentSort;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.UserRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.CommentSubscriptionRepository;
import com.openisle.repository.NotificationRepository;
import com.openisle.service.NotificationService;
import com.openisle.service.SubscriptionService;
import com.openisle.model.Role;
import com.openisle.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    private final ReactionRepository reactionRepository;
    private final CommentSubscriptionRepository commentSubscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final ImageUploader imageUploader;

    public Comment addComment(String username, Long postId, String content) {
        long recent = commentRepository.countByAuthorAfter(username,
                java.time.LocalDateTime.now().minusMinutes(1));
        if (recent >= 3) {
            throw new RateLimitException("Too many comments");
        }
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setContent(content);
        comment = commentRepository.save(comment);
        imageUploader.addReferences(imageUploader.extractUrls(content));
        if (!author.getId().equals(post.getAuthor().getId())) {
            notificationService.createNotification(post.getAuthor(), NotificationType.COMMENT_REPLY, post, comment, null, null, null, null);
        }
        for (User u : subscriptionService.getPostSubscribers(postId)) {
            if (!u.getId().equals(author.getId())) {
                notificationService.createNotification(u, NotificationType.POST_UPDATED, post, comment, null, null, null, null);
            }
        }
        for (User u : subscriptionService.getSubscribers(author.getUsername())) {
            if (!u.getId().equals(author.getId())) {
                notificationService.createNotification(u, NotificationType.USER_ACTIVITY, post, comment, null, null, null, null);
            }
        }
        notificationService.notifyMentions(content, author, post, comment);
        return comment;
    }

    public Comment addReply(String username, Long parentId, String content) {
        long recent = commentRepository.countByAuthorAfter(username,
                java.time.LocalDateTime.now().minusMinutes(1));
        if (recent >= 3) {
            throw new RateLimitException("Too many comments");
        }
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setPost(parent.getPost());
        comment.setParent(parent);
        comment.setContent(content);
        comment = commentRepository.save(comment);
        imageUploader.addReferences(imageUploader.extractUrls(content));
        if (!author.getId().equals(parent.getAuthor().getId())) {
            notificationService.createNotification(parent.getAuthor(), NotificationType.COMMENT_REPLY, parent.getPost(), comment, null, null, null, null);
        }
        for (User u : subscriptionService.getCommentSubscribers(parentId)) {
            if (!u.getId().equals(author.getId())) {
                notificationService.createNotification(u, NotificationType.COMMENT_REPLY, parent.getPost(), comment, null, null, null, null);
            }
        }
        for (User u : subscriptionService.getPostSubscribers(parent.getPost().getId())) {
            if (!u.getId().equals(author.getId())) {
                notificationService.createNotification(u, NotificationType.POST_UPDATED, parent.getPost(), comment, null, null, null, null);
            }
        }
        for (User u : subscriptionService.getSubscribers(author.getUsername())) {
            if (!u.getId().equals(author.getId())) {
                notificationService.createNotification(u, NotificationType.USER_ACTIVITY, parent.getPost(), comment, null, null, null, null);
            }
        }
        notificationService.notifyMentions(content, author, parent.getPost(), comment);
        return comment;
    }

    public List<Comment> getCommentsForPost(Long postId, CommentSort sort) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        List<Comment> list = commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post);
        if (sort == CommentSort.NEWEST) {
            list.sort(java.util.Comparator.comparing(Comment::getCreatedAt).reversed());
        } else if (sort == CommentSort.MOST_INTERACTIONS) {
            list.sort((a, b) -> Integer.compare(interactionCount(b), interactionCount(a)));
        }
        return list;
    }

    public List<Comment> getReplies(Long parentId) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        return commentRepository.findByParentOrderByCreatedAtAsc(parent);
    }

    public List<Comment> getRecentCommentsByUser(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Pageable pageable = PageRequest.of(0, limit);
        return commentRepository.findByAuthorOrderByCreatedAtDesc(user, pageable);
    }

    public java.util.List<User> getParticipants(Long postId, int limit) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        java.util.LinkedHashSet<User> set = new java.util.LinkedHashSet<>();
        set.add(post.getAuthor());
        set.addAll(commentRepository.findDistinctAuthorsByPost(post));
        java.util.List<User> list = new java.util.ArrayList<>(set);
        return list.subList(0, Math.min(limit, list.size()));
    }

    public java.util.List<Comment> getCommentsByIds(java.util.List<Long> ids) {
        return commentRepository.findAllById(ids);
    }

    public java.time.LocalDateTime getLastCommentTime(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        return commentRepository.findLastCommentTime(post);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteComment(String username, Long id) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        if (!user.getId().equals(comment.getAuthor().getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Unauthorized");
        }
        deleteCommentCascade(comment);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteCommentCascade(Comment comment) {
        List<Comment> replies = commentRepository.findByParentOrderByCreatedAtAsc(comment);
        for (Comment c : replies) {
            deleteCommentCascade(c);
        }
        reactionRepository.findByComment(comment).forEach(reactionRepository::delete);
        commentSubscriptionRepository.findByComment(comment).forEach(commentSubscriptionRepository::delete);
        notificationRepository.deleteAll(notificationRepository.findByComment(comment));
        imageUploader.removeReferences(imageUploader.extractUrls(comment.getContent()));
        commentRepository.delete(comment);
    }

    private int interactionCount(Comment comment) {
        int reactions = reactionRepository.findByComment(comment).size();
        int replies = commentRepository.findByParentOrderByCreatedAtAsc(comment).size();
        return reactions + replies;
    }
}
