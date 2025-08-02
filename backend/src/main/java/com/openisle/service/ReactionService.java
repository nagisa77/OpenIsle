package com.openisle.service;

import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.Reaction;
import com.openisle.model.ReactionType;
import com.openisle.model.User;
import com.openisle.model.NotificationType;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.UserRepository;
import com.openisle.service.NotificationService;
import com.openisle.service.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final EmailSender emailSender;

    @Value("${app.website-url}")
    private String websiteUrl;

    public Reaction reactToPost(String username, Long postId, ReactionType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        java.util.Optional<Reaction> existing =
                reactionRepository.findByUserAndPostAndType(user, post, type);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            return null;
        }
        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setPost(post);
        reaction.setType(type);
        reaction = reactionRepository.save(reaction);
        if (!user.getId().equals(post.getAuthor().getId())) {
            notificationService.createNotification(post.getAuthor(), NotificationType.REACTION, post, null, null, user, type, null);
        }
        return reaction;
    }

    public Reaction reactToComment(String username, Long commentId, ReactionType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        java.util.Optional<Reaction> existing =
                reactionRepository.findByUserAndCommentAndType(user, comment, type);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            return null;
        }
        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setComment(comment);
        reaction.setPost(null);
        reaction.setType(type);
        reaction = reactionRepository.save(reaction);
        if (!user.getId().equals(comment.getAuthor().getId())) {
            notificationService.createNotification(comment.getAuthor(), NotificationType.REACTION, comment.getPost(), comment, null, user, type, null);
        }
        return reaction;
    }

    public java.util.List<Reaction> getReactionsForPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        return reactionRepository.findByPost(post);
    }

    public java.util.List<Reaction> getReactionsForComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        return reactionRepository.findByComment(comment);
    }

    public java.util.List<Long> topPostIds(String username, int limit) {
        return reactionRepository.findTopPostIds(username, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    public java.util.List<Long> topCommentIds(String username, int limit) {
        return reactionRepository.findTopCommentIds(username, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    public long countLikesSent(String username) {
        return reactionRepository.countLikesSent(username);
    }

    public long countLikesReceived(String username) {
        return reactionRepository.countLikesReceived(username);
    }
}
