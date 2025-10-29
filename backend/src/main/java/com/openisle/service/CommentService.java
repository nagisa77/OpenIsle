package com.openisle.service;

import com.openisle.config.CachingConfig;
import com.openisle.exception.RateLimitException;
import com.openisle.model.Comment;
import com.openisle.model.CommentSort;
import com.openisle.model.NotificationType;
import com.openisle.model.PointHistory;
import com.openisle.model.Post;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.CommentSubscriptionRepository;
import com.openisle.repository.NotificationRepository;
import com.openisle.repository.PointHistoryRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import com.openisle.service.NotificationService;
import com.openisle.service.PointService;
import com.openisle.service.SubscriptionService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final SubscriptionService subscriptionService;
  private final ReactionRepository reactionRepository;
  private final CommentSubscriptionRepository commentSubscriptionRepository;
  private final NotificationRepository notificationRepository;
  private final PointHistoryRepository pointHistoryRepository;
  private final PointService pointService;
  private final ImageUploader imageUploader;
  private final SearchIndexEventPublisher searchIndexEventPublisher;

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public Comment addComment(String username, Long postId, String content) {
    log.debug("addComment called by user {} for post {}", username, postId);
    long recent = commentRepository.countByAuthorAfter(
      username,
      java.time.LocalDateTime.now().minusMinutes(1)
    );
    if (recent >= 3) {
      log.debug("Rate limit exceeded for user {}", username);
      throw new RateLimitException("Too many comments");
    }
    User author = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Post post = postRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    if (post.isClosed()) {
      throw new IllegalStateException("Post closed");
    }
    Comment comment = new Comment();
    comment.setAuthor(author);
    comment.setPost(post);
    comment.setContent(content);
    comment = commentRepository.save(comment);
    log.debug("Comment {} saved for post {}", comment.getId(), postId);

    // Update post comment statistics
    updatePostCommentStats(post);

    imageUploader.addReferences(imageUploader.extractUrls(content));
    if (!author.getId().equals(post.getAuthor().getId())) {
      notificationService.createNotification(
        post.getAuthor(),
        NotificationType.COMMENT_REPLY,
        post,
        comment,
        null,
        null,
        null,
        null
      );
    }
    for (User u : subscriptionService.getPostSubscribers(postId)) {
      if (!u.getId().equals(author.getId())) {
        notificationService.createNotification(
          u,
          NotificationType.POST_UPDATED,
          post,
          comment,
          null,
          null,
          null,
          null
        );
      }
    }
    for (User u : subscriptionService.getSubscribers(author.getUsername())) {
      if (!u.getId().equals(author.getId())) {
        notificationService.createNotification(
          u,
          NotificationType.USER_ACTIVITY,
          post,
          comment,
          null,
          null,
          null,
          null
        );
      }
    }
    notificationService.notifyMentions(content, author, post, comment);
    log.debug("addComment finished for comment {}", comment.getId());
    searchIndexEventPublisher.publishCommentSaved(comment);
    return comment;
  }

  public java.time.LocalDateTime getLastCommentTimeOfUserByUserId(Long userId) {
    // 根据用户id查询该用户最后回复时间
    return commentRepository.findLastCommentTimeOfUserByUserId(userId);
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public Comment addReply(String username, Long parentId, String content) {
    log.debug("addReply called by user {} for parent comment {}", username, parentId);
    long recent = commentRepository.countByAuthorAfter(
      username,
      java.time.LocalDateTime.now().minusMinutes(1)
    );
    if (recent >= 3) {
      log.debug("Rate limit exceeded for user {}", username);
      throw new RateLimitException("Too many comments");
    }
    User author = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Comment parent = commentRepository
      .findById(parentId)
      .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    if (parent.getPost().isClosed()) {
      throw new IllegalStateException("Post closed");
    }
    Comment comment = new Comment();
    comment.setAuthor(author);
    comment.setPost(parent.getPost());
    comment.setParent(parent);
    comment.setContent(content);
    comment = commentRepository.save(comment);
    log.debug("Reply {} saved for parent {}", comment.getId(), parentId);

    // Update post comment statistics
    updatePostCommentStats(parent.getPost());

    imageUploader.addReferences(imageUploader.extractUrls(content));
    if (!author.getId().equals(parent.getAuthor().getId())) {
      notificationService.createNotification(
        parent.getAuthor(),
        NotificationType.COMMENT_REPLY,
        parent.getPost(),
        comment,
        null,
        null,
        null,
        null
      );
    }
    for (User u : subscriptionService.getCommentSubscribers(parentId)) {
      if (!u.getId().equals(author.getId())) {
        notificationService.createNotification(
          u,
          NotificationType.COMMENT_REPLY,
          parent.getPost(),
          comment,
          null,
          null,
          null,
          null
        );
      }
    }
    for (User u : subscriptionService.getPostSubscribers(parent.getPost().getId())) {
      if (!u.getId().equals(author.getId())) {
        notificationService.createNotification(
          u,
          NotificationType.POST_UPDATED,
          parent.getPost(),
          comment,
          null,
          null,
          null,
          null
        );
      }
    }
    for (User u : subscriptionService.getSubscribers(author.getUsername())) {
      if (!u.getId().equals(author.getId())) {
        notificationService.createNotification(
          u,
          NotificationType.USER_ACTIVITY,
          parent.getPost(),
          comment,
          null,
          null,
          null,
          null
        );
      }
    }
    notificationService.notifyMentions(content, author, parent.getPost(), comment);
    log.debug("addReply finished for comment {}", comment.getId());
    searchIndexEventPublisher.publishCommentSaved(comment);
    return comment;
  }

  public List<Comment> getCommentsForPost(Long postId, CommentSort sort) {
    log.debug("getCommentsForPost called for post {} with sort {}", postId, sort);
    Post post = postRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    List<Comment> list = commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post);
    java.util.List<Comment> pinned = new java.util.ArrayList<>();
    java.util.List<Comment> others = new java.util.ArrayList<>();
    for (Comment c : list) {
      if (c.getPinnedAt() != null) {
        pinned.add(c);
      } else {
        others.add(c);
      }
    }
    pinned.sort(java.util.Comparator.comparing(Comment::getPinnedAt).reversed());
    if (sort == CommentSort.NEWEST) {
      others.sort(java.util.Comparator.comparing(Comment::getCreatedAt).reversed());
    } else if (sort == CommentSort.MOST_INTERACTIONS) {
      others.sort((a, b) -> Integer.compare(interactionCount(b), interactionCount(a)));
    }
    java.util.List<Comment> result = new java.util.ArrayList<>();
    result.addAll(pinned);
    result.addAll(others);
    log.debug("getCommentsForPost returning {} comments", result.size());
    return result;
  }

  public List<Comment> getReplies(Long parentId) {
    log.debug("getReplies called for parent {}", parentId);
    Comment parent = commentRepository
      .findById(parentId)
      .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    List<Comment> replies = commentRepository.findByParentOrderByCreatedAtAsc(parent);
    log.debug("getReplies returning {} replies for parent {}", replies.size(), parentId);
    return replies;
  }

  public Comment getComment(Long commentId) {
    log.debug("getComment called for id {}", commentId);
    return commentRepository
      .findById(commentId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Comment not found"));
  }

  public List<Comment> getCommentsBefore(Comment comment) {
    log.debug("getCommentsBefore called for comment {}", comment.getId());
    List<Comment> comments = commentRepository.findByPostAndCreatedAtLessThanOrderByCreatedAtAsc(
      comment.getPost(),
      comment.getCreatedAt()
    );
    log.debug(
      "getCommentsBefore returning {} comments for comment {}",
      comments.size(),
      comment.getId()
    );
    return comments;
  }

  public List<Comment> getRecentCommentsByUser(String username, int limit) {
    log.debug("getRecentCommentsByUser called for user {} with limit {}", username, limit);
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Pageable pageable = PageRequest.of(0, limit);
    List<Comment> comments = commentRepository.findByAuthorOrderByCreatedAtDesc(user, pageable);
    log.debug(
      "getRecentCommentsByUser returning {} comments for user {}",
      comments.size(),
      username
    );
    return comments;
  }

  public java.util.List<User> getParticipants(Long postId, int limit) {
    log.debug("getParticipants called for post {} with limit {}", postId, limit);
    Post post = postRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    java.util.LinkedHashSet<User> set = new java.util.LinkedHashSet<>();
    set.add(post.getAuthor());
    set.addAll(commentRepository.findDistinctAuthorsByPost(post));
    java.util.List<User> list = new java.util.ArrayList<>(set);
    java.util.List<User> result = list.subList(0, Math.min(limit, list.size()));
    log.debug("getParticipants returning {} users for post {}", result.size(), postId);
    return result;
  }

  public java.util.List<Comment> getCommentsByIds(java.util.List<Long> ids) {
    log.debug("getCommentsByIds called for ids {}", ids);
    java.util.List<Comment> comments = commentRepository.findAllById(ids);
    log.debug("getCommentsByIds returning {} comments", comments.size());
    return comments;
  }

  public java.time.LocalDateTime getLastCommentTime(Long postId) {
    log.debug("getLastCommentTime called for post {}", postId);
    Post post = postRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    java.time.LocalDateTime time = commentRepository.findLastCommentTime(post);
    log.debug("getLastCommentTime for post {} is {}", postId, time);
    return time;
  }

  public long countComments(Long postId) {
    log.debug("countComments called for post {}", postId);
    long count = commentRepository.countByPostId(postId);
    log.debug("countComments for post {} is {}", postId, count);
    return count;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public void deleteComment(String username, Long id) {
    log.debug("deleteComment called by user {} for comment {}", username, id);
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Comment comment = commentRepository
      .findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    if (!user.getId().equals(comment.getAuthor().getId()) && user.getRole() != Role.ADMIN) {
      log.debug("User {} not authorized to delete comment {}", username, id);
      throw new IllegalArgumentException("Unauthorized");
    }
    deleteCommentCascade(comment);
    log.debug("deleteComment completed for comment {}", id);
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public void deleteCommentCascade(Comment comment) {
    log.debug("deleteCommentCascade called for comment {}", comment.getId());
    List<Comment> replies = commentRepository.findByParentOrderByCreatedAtAsc(comment);
    for (Comment c : replies) {
      deleteCommentCascade(c);
    }

    // 逻辑删除相关的积分历史记录，并收集受影响的用户
    List<PointHistory> pointHistories = pointHistoryRepository.findByComment(comment);
    // 收集需要重新计算积分的用户
    Set<User> usersToRecalculate = pointHistories
      .stream()
      .map(PointHistory::getUser)
      .collect(Collectors.toSet());

    // 删除其他相关数据
    reactionRepository.findByComment(comment).forEach(reactionRepository::delete);
    commentSubscriptionRepository
      .findByComment(comment)
      .forEach(commentSubscriptionRepository::delete);
    notificationRepository.deleteAll(notificationRepository.findByComment(comment));
    imageUploader.removeReferences(imageUploader.extractUrls(comment.getContent()));

    // 逻辑删除评论
    Post post = comment.getPost();
    Long commentId = comment.getId();
    commentRepository.delete(comment);
    searchIndexEventPublisher.publishCommentDeleted(commentId);
    // 删除积分历史
    pointHistoryRepository.deleteAll(pointHistories);

    // Update post comment statistics
    updatePostCommentStats(post);

    // 重新计算受影响用户的积分
    if (!usersToRecalculate.isEmpty()) {
      for (User user : usersToRecalculate) {
        int newPoints = pointService.recalculateUserPoints(user);
        user.setPoint(newPoints);
        log.debug("Recalculated points for user {}: {}", user.getUsername(), newPoints);
      }
      userRepository.saveAll(usersToRecalculate);
    }

    log.debug("deleteCommentCascade removed comment {}", comment.getId());
  }

  @Transactional
  public Comment pinComment(String username, Long id) {
    Comment c = commentRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Comment not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (!user.getId().equals(c.getPost().getAuthor().getId()) && user.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("Unauthorized");
    }
    c.setPinnedAt(LocalDateTime.now());
    return commentRepository.save(c);
  }

  @Transactional
  public Comment unpinComment(String username, Long id) {
    Comment c = commentRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Comment not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (!user.getId().equals(c.getPost().getAuthor().getId()) && user.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("Unauthorized");
    }
    c.setPinnedAt(null);
    return commentRepository.save(c);
  }

  private int interactionCount(Comment comment) {
    int reactions = reactionRepository.findByComment(comment).size();
    int replies = commentRepository.findByParentOrderByCreatedAtAsc(comment).size();
    return reactions + replies;
  }

  /**
   * Update post comment statistics (comment count and last reply time)
   */
  public void updatePostCommentStats(Post post) {
    long commentCount = commentRepository.countByPostId(post.getId());
    post.setCommentCount(commentCount);

    LocalDateTime lastReplyAt = commentRepository.findLastCommentTime(post);
    if (lastReplyAt == null) {
      post.setLastReplyAt(post.getCreatedAt());
    } else {
      post.setLastReplyAt(lastReplyAt);
    }
    postRepository.save(post);

    log.debug(
      "Updated post {} stats: commentCount={}, lastReplyAt={}",
      post.getId(),
      commentCount,
      lastReplyAt
    );
  }
}
