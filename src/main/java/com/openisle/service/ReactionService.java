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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    public Reaction reactToPost(String username, Long postId, ReactionType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        Reaction reaction = reactionRepository.findByUserAndPost(user, post)
                .orElseGet(Reaction::new);
        reaction.setUser(user);
        reaction.setPost(post);
        reaction.setType(type);
        reaction = reactionRepository.save(reaction);
        if (!user.getId().equals(post.getAuthor().getId())) {
            notificationService.createNotification(post.getAuthor(), NotificationType.REACTION, post, null, null);
        }
        return reaction;
    }

    public Reaction reactToComment(String username, Long commentId, ReactionType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        Reaction reaction = reactionRepository.findByUserAndComment(user, comment)
                .orElseGet(Reaction::new);
        reaction.setUser(user);
        reaction.setComment(comment);
        reaction.setPost(null);
        reaction.setType(type);
        reaction = reactionRepository.save(reaction);
        if (!user.getId().equals(comment.getAuthor().getId())) {
            notificationService.createNotification(comment.getAuthor(), NotificationType.REACTION, null, comment, null);
        }
        return reaction;
    }

    public java.util.List<Reaction> getReactionsForPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        return reactionRepository.findByPost(post);
    }

    public java.util.List<Reaction> getReactionsForComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        return reactionRepository.findByComment(comment);
    }
}
