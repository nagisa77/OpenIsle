package com.openisle.service;

import com.openisle.dto.MessageNotificationPayload;
import com.openisle.dto.ReactionDto;
import com.openisle.mapper.ReactionMapper;
import com.openisle.model.Comment;
import com.openisle.model.Message;
import com.openisle.model.NotificationType;
import com.openisle.model.Post;
import com.openisle.model.Reaction;
import com.openisle.model.ReactionType;
import com.openisle.model.User;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.MessageRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactionService {

  private final ReactionRepository reactionRepository;
  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final MessageRepository messageRepository;
  private final NotificationService notificationService;
  private final NotificationProducer notificationProducer;
  private final ReactionMapper reactionMapper;
  private final EmailSender emailSender;

  @Value("${app.website-url}")
  private String websiteUrl;

  @Transactional
  public Reaction reactToPost(String username, Long postId, ReactionType type) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Post post = postRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    java.util.Optional<Reaction> existing = reactionRepository.findByUserAndPostAndType(
      user,
      post,
      type
    );
    if (existing.isPresent()) {
      notificationService.deleteReactionNotification(user, post, null, type);
      reactionRepository.delete(existing.get());
      return null;
    }
    Reaction reaction = new Reaction();
    reaction.setUser(user);
    reaction.setPost(post);
    reaction.setType(type);
    reaction = reactionRepository.save(reaction);
    if (!user.getId().equals(post.getAuthor().getId())) {
      notificationService.createNotification(
        post.getAuthor(),
        NotificationType.REACTION,
        post,
        null,
        null,
        user,
        type,
        null
      );
    }
    return reaction;
  }

  @Transactional
  public Reaction reactToComment(String username, Long commentId, ReactionType type) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Comment comment = commentRepository
      .findById(commentId)
      .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    java.util.Optional<Reaction> existing = reactionRepository.findByUserAndCommentAndType(
      user,
      comment,
      type
    );
    if (existing.isPresent()) {
      notificationService.deleteReactionNotification(user, null, comment, type);
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
      notificationService.createNotification(
        comment.getAuthor(),
        NotificationType.REACTION,
        comment.getPost(),
        comment,
        null,
        user,
        type,
        null
      );
    }
    return reaction;
  }

  @Transactional
  public Reaction reactToMessage(String username, Long messageId, ReactionType type) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Message message = messageRepository
      .findById(messageId)
      .orElseThrow(() -> new IllegalArgumentException("Message not found"));
    java.util.Optional<Reaction> existing = reactionRepository.findByUserAndMessageAndType(
      user,
      message,
      type
    );

    Map<String, Object> syncPayload = new HashMap<>();
    syncPayload.put("eventType", "MESSAGE_REACTION");
    syncPayload.put("conversationId", message.getConversation().getId());
    syncPayload.put("messageId", message.getId());

    if (existing.isPresent()) {
      Reaction removed = existing.get();
      ReactionDto removedDto = reactionMapper.toDto(removed);
      reactionRepository.delete(removed);

      syncPayload.put("action", "REMOVED");
      syncPayload.put("reaction", removedDto);
      sendMessageReactionSync(user.getUsername(), syncPayload);

      return null;
    }

    Reaction reaction = new Reaction();
    reaction.setUser(user);
    reaction.setMessage(message);
    reaction.setType(type);
    reaction = reactionRepository.save(reaction);

    syncPayload.put("action", "ADDED");
    syncPayload.put("reaction", reactionMapper.toDto(reaction));
    sendMessageReactionSync(user.getUsername(), syncPayload);

    return reaction;
  }

  private void sendMessageReactionSync(String shardUsername, Map<String, Object> payload) {
    try {
      notificationProducer.sendNotification(new MessageNotificationPayload(shardUsername, payload));
    } catch (Exception e) {
      log.error("Failed to broadcast message reaction sync via RabbitMQ", e);
    }
  }

  public java.util.List<Reaction> getReactionsForPost(Long postId) {
    Post post = postRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    return reactionRepository.findByPost(post);
  }

  public java.util.List<Reaction> getReactionsForComment(Long commentId) {
    Comment comment = commentRepository
      .findById(commentId)
      .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    return reactionRepository.findByComment(comment);
  }

  public java.util.List<Long> topPostIds(String username, int limit) {
    return reactionRepository.findTopPostIds(
      username,
      org.springframework.data.domain.PageRequest.of(0, limit)
    );
  }

  public java.util.List<Long> topCommentIds(String username, int limit) {
    return reactionRepository.findTopCommentIds(
      username,
      org.springframework.data.domain.PageRequest.of(0, limit)
    );
  }

  public long countLikesSent(String username) {
    return reactionRepository.countLikesSent(username);
  }

  public long countLikesReceived(String username) {
    return reactionRepository.countLikesReceived(username);
  }
}
