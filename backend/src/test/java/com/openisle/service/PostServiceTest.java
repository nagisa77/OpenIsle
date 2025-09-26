package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.openisle.exception.RateLimitException;
import com.openisle.model.*;
import com.openisle.repository.*;
import com.openisle.search.SearchIndexEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;

class PostServiceTest {

  @Test
  void deletePostRemovesReads() {
    PostRepository postRepo = mock(PostRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    CategoryRepository catRepo = mock(CategoryRepository.class);
    TagRepository tagRepo = mock(TagRepository.class);
    LotteryPostRepository lotteryRepo = mock(LotteryPostRepository.class);
    PollPostRepository pollPostRepo = mock(PollPostRepository.class);
    PollVoteRepository pollVoteRepo = mock(PollVoteRepository.class);
    NotificationService notifService = mock(NotificationService.class);
    SubscriptionService subService = mock(SubscriptionService.class);
    CommentService commentService = mock(CommentService.class);
    CommentRepository commentRepo = mock(CommentRepository.class);
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    PostSubscriptionRepository subRepo = mock(PostSubscriptionRepository.class);
    NotificationRepository notificationRepo = mock(NotificationRepository.class);
    PostReadService postReadService = mock(PostReadService.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    EmailSender emailSender = mock(EmailSender.class);
    ApplicationContext context = mock(ApplicationContext.class);
    PointService pointService = mock(PointService.class);
    PostChangeLogService postChangeLogService = mock(PostChangeLogService.class);
    PointHistoryRepository pointHistoryRepository = mock(PointHistoryRepository.class);
    RedisTemplate redisTemplate = mock(RedisTemplate.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);

    PostService service = new PostService(
      postRepo,
      userRepo,
      catRepo,
      tagRepo,
      lotteryRepo,
      pollPostRepo,
      pollVoteRepo,
      notifService,
      subService,
      commentService,
      commentRepo,
      reactionRepo,
      subRepo,
      notificationRepo,
      postReadService,
      imageUploader,
      taskScheduler,
      emailSender,
      context,
      pointService,
      postChangeLogService,
      pointHistoryRepository,
      PublishMode.DIRECT,
      redisTemplate,
      searchIndexEventPublisher
    );
    when(context.getBean(PostService.class)).thenReturn(service);

    Post post = new Post();
    post.setId(1L);
    User author = new User();
    author.setId(1L);
    author.setRole(Role.USER);
    post.setAuthor(author);

    when(postRepo.findById(1L)).thenReturn(Optional.of(post));
    when(userRepo.findByUsername("alice")).thenReturn(Optional.of(author));
    when(commentRepo.findByPostAndParentIsNullOrderByCreatedAtAsc(post)).thenReturn(List.of());
    when(reactionRepo.findByPost(post)).thenReturn(List.of());
    when(subRepo.findByPost(post)).thenReturn(List.of());
    when(notificationRepo.findByPost(post)).thenReturn(List.of());
    when(pointHistoryRepository.findByPost(post)).thenReturn(List.of());

    service.deletePost(1L, "alice");

    verify(postReadService).deleteByPost(post);
    verify(postRepo).delete(post);
    verify(postChangeLogService).deleteLogsForPost(post);
  }

  @Test
  void deletePostByAdminNotifiesAuthor() {
    PostRepository postRepo = mock(PostRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    CategoryRepository catRepo = mock(CategoryRepository.class);
    TagRepository tagRepo = mock(TagRepository.class);
    LotteryPostRepository lotteryRepo = mock(LotteryPostRepository.class);
    PollPostRepository pollPostRepo = mock(PollPostRepository.class);
    PollVoteRepository pollVoteRepo = mock(PollVoteRepository.class);
    NotificationService notifService = mock(NotificationService.class);
    SubscriptionService subService = mock(SubscriptionService.class);
    CommentService commentService = mock(CommentService.class);
    CommentRepository commentRepo = mock(CommentRepository.class);
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    PostSubscriptionRepository subRepo = mock(PostSubscriptionRepository.class);
    NotificationRepository notificationRepo = mock(NotificationRepository.class);
    PostReadService postReadService = mock(PostReadService.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    EmailSender emailSender = mock(EmailSender.class);
    ApplicationContext context = mock(ApplicationContext.class);
    PointService pointService = mock(PointService.class);
    PostChangeLogService postChangeLogService = mock(PostChangeLogService.class);
    PointHistoryRepository pointHistoryRepository = mock(PointHistoryRepository.class);
    RedisTemplate redisTemplate = mock(RedisTemplate.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);

    PostService service = new PostService(
      postRepo,
      userRepo,
      catRepo,
      tagRepo,
      lotteryRepo,
      pollPostRepo,
      pollVoteRepo,
      notifService,
      subService,
      commentService,
      commentRepo,
      reactionRepo,
      subRepo,
      notificationRepo,
      postReadService,
      imageUploader,
      taskScheduler,
      emailSender,
      context,
      pointService,
      postChangeLogService,
      pointHistoryRepository,
      PublishMode.DIRECT,
      redisTemplate,
      searchIndexEventPublisher
    );
    when(context.getBean(PostService.class)).thenReturn(service);

    Post post = new Post();
    post.setId(1L);
    post.setTitle("T");
    post.setContent("");
    User author = new User();
    author.setId(2L);
    author.setRole(Role.USER);
    post.setAuthor(author);

    User admin = new User();
    admin.setId(1L);
    admin.setRole(Role.ADMIN);

    when(postRepo.findById(1L)).thenReturn(Optional.of(post));
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(commentRepo.findByPostAndParentIsNullOrderByCreatedAtAsc(post)).thenReturn(List.of());
    when(reactionRepo.findByPost(post)).thenReturn(List.of());
    when(subRepo.findByPost(post)).thenReturn(List.of());
    when(notificationRepo.findByPost(post)).thenReturn(List.of());
    when(pointHistoryRepository.findByPost(post)).thenReturn(List.of());

    service.deletePost(1L, "admin");

    verify(notifService).createNotification(
      eq(author),
      eq(NotificationType.POST_DELETED),
      isNull(),
      isNull(),
      isNull(),
      eq(admin),
      isNull(),
      eq("T")
    );
  }

  @Test
  void createPostRespectsRateLimit() {
    PostRepository postRepo = mock(PostRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    CategoryRepository catRepo = mock(CategoryRepository.class);
    TagRepository tagRepo = mock(TagRepository.class);
    LotteryPostRepository lotteryRepo = mock(LotteryPostRepository.class);
    PollPostRepository pollPostRepo = mock(PollPostRepository.class);
    PollVoteRepository pollVoteRepo = mock(PollVoteRepository.class);
    NotificationService notifService = mock(NotificationService.class);
    SubscriptionService subService = mock(SubscriptionService.class);
    CommentService commentService = mock(CommentService.class);
    CommentRepository commentRepo = mock(CommentRepository.class);
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    PostSubscriptionRepository subRepo = mock(PostSubscriptionRepository.class);
    NotificationRepository notificationRepo = mock(NotificationRepository.class);
    PostReadService postReadService = mock(PostReadService.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    EmailSender emailSender = mock(EmailSender.class);
    ApplicationContext context = mock(ApplicationContext.class);
    PointService pointService = mock(PointService.class);
    PostChangeLogService postChangeLogService = mock(PostChangeLogService.class);
    PointHistoryRepository pointHistoryRepository = mock(PointHistoryRepository.class);
    RedisTemplate redisTemplate = mock(RedisTemplate.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);

    PostService service = new PostService(
      postRepo,
      userRepo,
      catRepo,
      tagRepo,
      lotteryRepo,
      pollPostRepo,
      pollVoteRepo,
      notifService,
      subService,
      commentService,
      commentRepo,
      reactionRepo,
      subRepo,
      notificationRepo,
      postReadService,
      imageUploader,
      taskScheduler,
      emailSender,
      context,
      pointService,
      postChangeLogService,
      pointHistoryRepository,
      PublishMode.DIRECT,
      redisTemplate,
      searchIndexEventPublisher
    );
    when(context.getBean(PostService.class)).thenReturn(service);

    when(postRepo.countByAuthorAfter(eq("alice"), any())).thenReturn(1L);

    assertThrows(RateLimitException.class, () ->
      service.createPost(
        "alice",
        1L,
        "t",
        "c",
        List.of(1L),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
      )
    );
  }

  @Test
  void deletePostRemovesPointHistoriesAndRecalculatesPoints() {
    PostRepository postRepo = mock(PostRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    CategoryRepository catRepo = mock(CategoryRepository.class);
    TagRepository tagRepo = mock(TagRepository.class);
    LotteryPostRepository lotteryRepo = mock(LotteryPostRepository.class);
    PollPostRepository pollPostRepo = mock(PollPostRepository.class);
    PollVoteRepository pollVoteRepo = mock(PollVoteRepository.class);
    NotificationService notifService = mock(NotificationService.class);
    SubscriptionService subService = mock(SubscriptionService.class);
    CommentService commentService = mock(CommentService.class);
    CommentRepository commentRepo = mock(CommentRepository.class);
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    PostSubscriptionRepository subRepo = mock(PostSubscriptionRepository.class);
    NotificationRepository notificationRepo = mock(NotificationRepository.class);
    PostReadService postReadService = mock(PostReadService.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    EmailSender emailSender = mock(EmailSender.class);
    ApplicationContext context = mock(ApplicationContext.class);
    PointService pointService = mock(PointService.class);
    PostChangeLogService postChangeLogService = mock(PostChangeLogService.class);
    PointHistoryRepository pointHistoryRepository = mock(PointHistoryRepository.class);
    RedisTemplate redisTemplate = mock(RedisTemplate.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);

    PostService service = new PostService(
      postRepo,
      userRepo,
      catRepo,
      tagRepo,
      lotteryRepo,
      pollPostRepo,
      pollVoteRepo,
      notifService,
      subService,
      commentService,
      commentRepo,
      reactionRepo,
      subRepo,
      notificationRepo,
      postReadService,
      imageUploader,
      taskScheduler,
      emailSender,
      context,
      pointService,
      postChangeLogService,
      pointHistoryRepository,
      PublishMode.DIRECT,
      redisTemplate,
      searchIndexEventPublisher
    );
    when(context.getBean(PostService.class)).thenReturn(service);

    Post post = new Post();
    post.setId(10L);
    User author = new User();
    author.setId(20L);
    author.setRole(Role.USER);
    post.setAuthor(author);

    User historyUser = new User();
    historyUser.setId(30L);

    PointHistory history = new PointHistory();
    history.setUser(historyUser);
    history.setPost(post);

    when(postRepo.findById(10L)).thenReturn(Optional.of(post));
    when(userRepo.findByUsername("author")).thenReturn(Optional.of(author));
    when(commentRepo.findByPostAndParentIsNullOrderByCreatedAtAsc(post)).thenReturn(List.of());
    when(reactionRepo.findByPost(post)).thenReturn(List.of());
    when(subRepo.findByPost(post)).thenReturn(List.of());
    when(notificationRepo.findByPost(post)).thenReturn(List.of());
    when(pointHistoryRepository.findByPost(post)).thenReturn(List.of(history));
    when(pointService.recalculateUserPoints(historyUser)).thenReturn(0);

    service.deletePost(10L, "author");

    ArgumentCaptor<List<PointHistory>> captor = ArgumentCaptor.forClass(List.class);
    verify(pointHistoryRepository).saveAll(captor.capture());
    List<PointHistory> savedHistories = captor.getValue();
    assertEquals(1, savedHistories.size());
    PointHistory savedHistory = savedHistories.get(0);
    assertNull(savedHistory.getPost());
    assertNotNull(savedHistory.getDeletedAt());
    assertTrue(savedHistory.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));

    verify(pointService).recalculateUserPoints(historyUser);
    verify(userRepo).saveAll(any());
  }

  @Test
  void finalizeLotteryNotifiesAuthor() {
    PostRepository postRepo = mock(PostRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    CategoryRepository catRepo = mock(CategoryRepository.class);
    TagRepository tagRepo = mock(TagRepository.class);
    LotteryPostRepository lotteryRepo = mock(LotteryPostRepository.class);
    PollPostRepository pollPostRepo = mock(PollPostRepository.class);
    PollVoteRepository pollVoteRepo = mock(PollVoteRepository.class);
    NotificationService notifService = mock(NotificationService.class);
    SubscriptionService subService = mock(SubscriptionService.class);
    CommentService commentService = mock(CommentService.class);
    CommentRepository commentRepo = mock(CommentRepository.class);
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    PostSubscriptionRepository subRepo = mock(PostSubscriptionRepository.class);
    NotificationRepository notificationRepo = mock(NotificationRepository.class);
    PostReadService postReadService = mock(PostReadService.class);
    ImageUploader imageUploader = mock(ImageUploader.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    EmailSender emailSender = mock(EmailSender.class);
    ApplicationContext context = mock(ApplicationContext.class);
    PointService pointService = mock(PointService.class);
    PostChangeLogService postChangeLogService = mock(PostChangeLogService.class);
    PointHistoryRepository pointHistoryRepository = mock(PointHistoryRepository.class);
    RedisTemplate redisTemplate = mock(RedisTemplate.class);
    SearchIndexEventPublisher searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);

    PostService service = new PostService(
      postRepo,
      userRepo,
      catRepo,
      tagRepo,
      lotteryRepo,
      pollPostRepo,
      pollVoteRepo,
      notifService,
      subService,
      commentService,
      commentRepo,
      reactionRepo,
      subRepo,
      notificationRepo,
      postReadService,
      imageUploader,
      taskScheduler,
      emailSender,
      context,
      pointService,
      postChangeLogService,
      pointHistoryRepository,
      PublishMode.DIRECT,
      redisTemplate,
      searchIndexEventPublisher
    );
    when(context.getBean(PostService.class)).thenReturn(service);

    User author = new User();
    author.setId(1L);
    User winner = new User();
    winner.setId(2L);

    LotteryPost lp = new LotteryPost();
    lp.setId(1L);
    lp.setAuthor(author);
    lp.setTitle("L");
    lp.setPrizeCount(1);
    lp.getParticipants().add(winner);

    when(lotteryRepo.findById(1L)).thenReturn(Optional.of(lp));

    service.finalizeLottery(1L);

    verify(notifService).createNotification(
      eq(winner),
      eq(NotificationType.LOTTERY_WIN),
      eq(lp),
      isNull(),
      isNull(),
      eq(author),
      isNull(),
      isNull()
    );
    verify(notifService).createNotification(
      eq(author),
      eq(NotificationType.LOTTERY_DRAW),
      eq(lp),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull()
    );
  }
}
