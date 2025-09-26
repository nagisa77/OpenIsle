package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.openisle.exception.RateLimitException;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.CommentSubscriptionRepository;
import com.openisle.repository.NotificationRepository;
import com.openisle.repository.PointHistoryRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import com.openisle.service.PointService;
import org.junit.jupiter.api.Test;

class CommentServiceTest {

  @Test
  void addCommentRespectsRateLimit() {
    CommentRepository commentRepo = mock(CommentRepository.class);
    PostRepository postRepo = mock(PostRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    NotificationService notifService = mock(NotificationService.class);
    SubscriptionService subService = mock(SubscriptionService.class);
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    CommentSubscriptionRepository subRepo = mock(CommentSubscriptionRepository.class);
    NotificationRepository nRepo = mock(NotificationRepository.class);
    PointHistoryRepository pointHistoryRepo = mock(PointHistoryRepository.class);
    PointService pointService = mock(PointService.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);

    CommentService service = new CommentService(
      commentRepo,
      postRepo,
      userRepo,
      notifService,
      subService,
      reactionRepo,
      subRepo,
      nRepo,
      pointHistoryRepo,
      pointService,
      imageUploader,
      searchIndexEventPublisher
    );

    when(commentRepo.countByAuthorAfter(eq("alice"), any())).thenReturn(3L);

    assertThrows(RateLimitException.class, () -> service.addComment("alice", 1L, "hi"));
  }
}
