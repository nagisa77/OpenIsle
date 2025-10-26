package com.openisle.service;

import com.openisle.config.CachingConfig;
import com.openisle.exception.NotFoundException;
import com.openisle.exception.RateLimitException;
import com.openisle.model.*;
import com.openisle.repository.CategoryProposalPostRepository;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.LotteryPostRepository;
import com.openisle.repository.NotificationRepository;
import com.openisle.repository.PointHistoryRepository;
import com.openisle.repository.PollPostRepository;
import com.openisle.repository.PollVoteRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.PostSubscriptionRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.TagRepository;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import com.openisle.service.EmailSender;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class PostService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final TagRepository tagRepository;
  private final LotteryPostRepository lotteryPostRepository;
  private final PollPostRepository pollPostRepository;
  private final CategoryProposalPostRepository categoryProposalPostRepository;
  private final PollVoteRepository pollVoteRepository;
  private PublishMode publishMode;
  private final NotificationService notificationService;
  private final SubscriptionService subscriptionService;
  private final CommentService commentService;
  private final CommentRepository commentRepository;
  private final ReactionRepository reactionRepository;
  private final PostSubscriptionRepository postSubscriptionRepository;
  private final NotificationRepository notificationRepository;
  private final PostReadService postReadService;
  private final ImageUploader imageUploader;
  private final TaskScheduler taskScheduler;
  private final EmailSender emailSender;
  private final ApplicationContext applicationContext;
  private final PointService pointService;
  private final PostChangeLogService postChangeLogService;
  private final PointHistoryRepository pointHistoryRepository;
  private final CategoryService categoryService;
  private final ConcurrentMap<Long, ScheduledFuture<?>> scheduledFinalizations =
    new ConcurrentHashMap<>();

  private final SearchIndexEventPublisher searchIndexEventPublisher;

  private static final int DEFAULT_PROPOSAL_APPROVE_THRESHOLD = 60;
  private static final int DEFAULT_PROPOSAL_QUORUM = 10;
  private static final long DEFAULT_PROPOSAL_DURATION_DAYS = 3;
  private static final List<String> DEFAULT_PROPOSAL_OPTIONS = List.of("同意", "反对");

  @Value("${app.website-url:https://www.open-isle.com}")
  private String websiteUrl;

  private final RedisTemplate redisTemplate;

  @org.springframework.beans.factory.annotation.Autowired
  public PostService(
    PostRepository postRepository,
    UserRepository userRepository,
    CategoryRepository categoryRepository,
    TagRepository tagRepository,
    LotteryPostRepository lotteryPostRepository,
    PollPostRepository pollPostRepository,
    CategoryProposalPostRepository categoryProposalPostRepository,
    PollVoteRepository pollVoteRepository,
    NotificationService notificationService,
    SubscriptionService subscriptionService,
    CommentService commentService,
    CommentRepository commentRepository,
    ReactionRepository reactionRepository,
    PostSubscriptionRepository postSubscriptionRepository,
    NotificationRepository notificationRepository,
    PostReadService postReadService,
    ImageUploader imageUploader,
    TaskScheduler taskScheduler,
    EmailSender emailSender,
    ApplicationContext applicationContext,
    PointService pointService,
    PostChangeLogService postChangeLogService,
    PointHistoryRepository pointHistoryRepository,
    @Value("${app.post.publish-mode:DIRECT}") PublishMode publishMode,
    RedisTemplate redisTemplate,
    SearchIndexEventPublisher searchIndexEventPublisher,
    CategoryService categoryService
  ) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
    this.categoryRepository = categoryRepository;
    this.tagRepository = tagRepository;
    this.lotteryPostRepository = lotteryPostRepository;
    this.pollPostRepository = pollPostRepository;
    this.categoryProposalPostRepository = categoryProposalPostRepository;
    this.pollVoteRepository = pollVoteRepository;
    this.notificationService = notificationService;
    this.subscriptionService = subscriptionService;
    this.commentService = commentService;
    this.commentRepository = commentRepository;
    this.reactionRepository = reactionRepository;
    this.postSubscriptionRepository = postSubscriptionRepository;
    this.notificationRepository = notificationRepository;
    this.postReadService = postReadService;
    this.imageUploader = imageUploader;
    this.taskScheduler = taskScheduler;
    this.emailSender = emailSender;
    this.applicationContext = applicationContext;
    this.pointService = pointService;
    this.postChangeLogService = postChangeLogService;
    this.pointHistoryRepository = pointHistoryRepository;
    this.publishMode = publishMode;

    this.redisTemplate = redisTemplate;
    this.searchIndexEventPublisher = searchIndexEventPublisher;
    this.categoryService = categoryService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void rescheduleLotteries() {
    LocalDateTime now = LocalDateTime.now();
    for (LotteryPost lp : lotteryPostRepository.findByEndTimeAfterAndWinnersIsEmpty(now)) {
      ScheduledFuture<?> future = taskScheduler.schedule(
        () -> applicationContext.getBean(PostService.class).finalizeLottery(lp.getId()),
        java.util.Date.from(lp.getEndTime().atZone(ZoneId.systemDefault()).toInstant())
      );
      scheduledFinalizations.put(lp.getId(), future);
    }
    for (LotteryPost lp : lotteryPostRepository.findByEndTimeBeforeAndWinnersIsEmpty(now)) {
      applicationContext.getBean(PostService.class).finalizeLottery(lp.getId());
    }
    for (PollPost pp : pollPostRepository.findByEndTimeAfterAndResultAnnouncedFalse(now)) {
      ScheduledFuture<?> future = taskScheduler.schedule(
        () -> applicationContext.getBean(PostService.class).finalizePoll(pp.getId()),
        java.util.Date.from(pp.getEndTime().atZone(ZoneId.systemDefault()).toInstant())
      );
      scheduledFinalizations.put(pp.getId(), future);
    }
    for (PollPost pp : pollPostRepository.findByEndTimeBeforeAndResultAnnouncedFalse(now)) {
      applicationContext.getBean(PostService.class).finalizePoll(pp.getId());
    }
    for (CategoryProposalPost cp : categoryProposalPostRepository.findByEndTimeAfterAndProposalStatus(
      now,
      CategoryProposalStatus.PENDING
    )) {
      if (cp.getEndTime() != null) {
        ScheduledFuture<?> future = taskScheduler.schedule(
          () -> applicationContext.getBean(PostService.class).finalizeProposal(cp.getId()),
          java.util.Date.from(cp.getEndTime().atZone(ZoneId.systemDefault()).toInstant())
        );
        scheduledFinalizations.put(cp.getId(), future);
      }
    }
    for (CategoryProposalPost cp : categoryProposalPostRepository.findByEndTimeBeforeAndProposalStatus(
      now,
      CategoryProposalStatus.PENDING
    )) {
      applicationContext.getBean(PostService.class).finalizeProposal(cp.getId());
    }
  }

  public PublishMode getPublishMode() {
    return publishMode;
  }

  public void setPublishMode(PublishMode publishMode) {
    this.publishMode = publishMode;
  }

  public List<Post> listLatestRssPosts(int limit) {
    Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
    return postRepository.findByStatusAndRssExcludedFalseOrderByCreatedAtDesc(
      PostStatus.PUBLISHED,
      pageable
    );
  }

  public Post excludeFromRss(Long id, String username) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    boolean oldFeatured = !Boolean.TRUE.equals(post.getRssExcluded());
    post.setRssExcluded(true);
    Post saved = postRepository.save(post);
    postChangeLogService.recordFeaturedChange(saved, user, oldFeatured, false);
    return saved;
  }

  public Post includeInRss(Long id, String username) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    boolean oldFeatured = !Boolean.TRUE.equals(post.getRssExcluded());
    post.setRssExcluded(false);
    Post saved = postRepository.save(post);
    postChangeLogService.recordFeaturedChange(saved, user, oldFeatured, true);
    notificationService.createNotification(
      saved.getAuthor(),
      NotificationType.POST_FEATURED,
      saved,
      null,
      null,
      null,
      null,
      null
    );
    pointService.awardForFeatured(saved.getAuthor().getUsername(), saved.getId());
    return saved;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  public Post createPost(
    String username,
    Long categoryId,
    String title,
    String content,
    List<Long> tagIds,
    PostType type,
    PostVisibleScopeType postVisibleScopeType,
    String prizeDescription,
    String prizeIcon,
    Integer prizeCount,
    Integer pointCost,
    LocalDateTime startTime,
    LocalDateTime endTime,
    java.util.List<String> options,
    Boolean multiple,
    String proposedName,
    String proposalDescription
  ) {
    // 限制访问次数
    boolean limitResult = isPostLimitReached(username);
    if (!limitResult) {
      throw new RateLimitException("Too many posts");
    }
    if (tagIds == null || tagIds.isEmpty()) {
      throw new IllegalArgumentException("At least one tag required");
    }
    if (tagIds.size() > 2) {
      throw new IllegalArgumentException("At most two tags allowed");
    }
    User author = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Category category = categoryRepository
      .findById(categoryId)
      .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
    if (tags.isEmpty()) {
      throw new IllegalArgumentException("Tag not found");
    }
    PostType actualType = type != null ? type : PostType.NORMAL;
    Post post;
    if (actualType == PostType.LOTTERY) {
      if (pointCost != null && (pointCost < 0 || pointCost > 100)) {
        throw new IllegalArgumentException("pointCost must be between 0 and 100");
      }
      LotteryPost lp = new LotteryPost();
      lp.setPrizeDescription(prizeDescription);
      lp.setPrizeIcon(prizeIcon);
      lp.setPrizeCount(prizeCount != null ? prizeCount : 0);
      lp.setPointCost(pointCost != null ? pointCost : 0);
      lp.setStartTime(startTime);
      lp.setEndTime(endTime);
      post = lp;
    } else if (actualType == PostType.POLL) {
      if (options == null || options.size() < 2) {
        throw new IllegalArgumentException("At least two options required");
      }
      PollPost pp = new PollPost();
      pp.setOptions(options);
      pp.setEndTime(endTime);
      pp.setMultiple(multiple != null && multiple);
      post = pp;
    } else if (actualType == PostType.PROPOSAL) {
      CategoryProposalPost cp = new CategoryProposalPost();
      if (proposedName == null || proposedName.isBlank()) {
        throw new IllegalArgumentException("Proposed name required");
      }
      String normalizedName = proposedName.trim();
      if (categoryProposalPostRepository.existsByProposedNameIgnoreCase(normalizedName)) {
        throw new IllegalArgumentException("Proposed name already exists: " + normalizedName);
      }
      cp.setProposedName(normalizedName);
      cp.setDescription(proposalDescription);
      cp.setApproveThreshold(DEFAULT_PROPOSAL_APPROVE_THRESHOLD);
      cp.setQuorum(DEFAULT_PROPOSAL_QUORUM);
      LocalDateTime now = LocalDateTime.now();
      cp.setStartAt(now);
      cp.setEndTime(now.plusDays(DEFAULT_PROPOSAL_DURATION_DAYS));
      cp.setOptions(new ArrayList<>(DEFAULT_PROPOSAL_OPTIONS));
      cp.setMultiple(false);
      post = cp;
    } else {
      post = new Post();
    }
    post.setType(actualType);
    post.setTitle(title);
    post.setContent(content);
    post.setAuthor(author);
    post.setCategory(category);
    post.setTags(new HashSet<>(tags));
    post.setStatus(publishMode == PublishMode.REVIEW ? PostStatus.PENDING : PostStatus.PUBLISHED);

    // 什么都没设置的情况下，默认为ALL
    if (Objects.isNull(postVisibleScopeType)) {
      post.setVisibleScope(PostVisibleScopeType.ALL);
    } else {
      post.setVisibleScope(postVisibleScopeType);
    }

    if (post instanceof LotteryPost) {
      post = lotteryPostRepository.save((LotteryPost) post);
    } else if (post instanceof CategoryProposalPost categoryProposalPost) {
      post = categoryProposalPostRepository.save(categoryProposalPost);
    } else if (post instanceof PollPost) {
      post = pollPostRepository.save((PollPost) post);
    } else {
      post = postRepository.save(post);
    }
    imageUploader.addReferences(imageUploader.extractUrls(content));
    if (post.getStatus() == PostStatus.PENDING) {
      java.util.List<User> admins = userRepository.findByRole(com.openisle.model.Role.ADMIN);
      for (User admin : admins) {
        notificationService.createNotification(
          admin,
          NotificationType.POST_REVIEW_REQUEST,
          post,
          null,
          null,
          author,
          null,
          null
        );
      }
      notificationService.createNotification(
        author,
        NotificationType.POST_REVIEW_REQUEST,
        post,
        null,
        null,
        null,
        null,
        null
      );
    }
    // notify followers of author
    for (User u : subscriptionService.getSubscribers(author.getUsername())) {
      if (!u.getId().equals(author.getId())) {
        notificationService.createNotification(
          u,
          NotificationType.FOLLOWED_POST,
          post,
          null,
          null,
          author,
          null,
          null
        );
      }
    }
    notificationService.notifyMentions(content, author, post, null);

    if (post instanceof LotteryPost lp && lp.getEndTime() != null) {
      ScheduledFuture<?> future = taskScheduler.schedule(
        () -> applicationContext.getBean(PostService.class).finalizeLottery(lp.getId()),
        java.util.Date.from(lp.getEndTime().atZone(ZoneId.systemDefault()).toInstant())
      );
      scheduledFinalizations.put(lp.getId(), future);
    } else if (post instanceof CategoryProposalPost cp && cp.getEndTime() != null) {
      ScheduledFuture<?> future = taskScheduler.schedule(
        () -> applicationContext.getBean(PostService.class).finalizeProposal(cp.getId()),
        java.util.Date.from(cp.getEndTime().atZone(ZoneId.systemDefault()).toInstant())
      );
      scheduledFinalizations.put(cp.getId(), future);
    } else if (post instanceof PollPost pp && pp.getEndTime() != null) {
      ScheduledFuture<?> future = taskScheduler.schedule(
        () -> applicationContext.getBean(PostService.class).finalizePoll(pp.getId()),
        java.util.Date.from(pp.getEndTime().atZone(ZoneId.systemDefault()).toInstant())
      );
      scheduledFinalizations.put(pp.getId(), future);
    }
    if (post.getStatus() == PostStatus.PUBLISHED) {
      searchIndexEventPublisher.publishPostSaved(post);
    }
    markPostLimit(author.getUsername());
    return post;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public void finalizeProposal(Long postId) {
    scheduledFinalizations.remove(postId);
    categoryProposalPostRepository
      .findById(postId)
      .ifPresent(cp -> {
        if (cp.getProposalStatus() != CategoryProposalStatus.PENDING) {
          return;
        }
        int totalParticipants = cp.getParticipants() != null ? cp.getParticipants().size() : 0;
        int approveVotes = 0;
        if (cp.getVotes() != null) {
          approveVotes = cp.getVotes().getOrDefault(0, 0);
        }
        boolean quorumMet = totalParticipants >= cp.getQuorum();
        int approvePercent = totalParticipants > 0 ? (approveVotes * 100) / totalParticipants : 0;
        boolean thresholdMet = approvePercent >= cp.getApproveThreshold();
        boolean approved = false;
        String rejectReason = null;
        if (quorumMet && thresholdMet) {
          cp.setProposalStatus(CategoryProposalStatus.APPROVED);
          approved = true;
        } else {
          cp.setProposalStatus(CategoryProposalStatus.REJECTED);
          String reason;
          if (!quorumMet && !thresholdMet) {
            reason = "未达到法定人数且赞成率不足";
          } else if (!quorumMet) {
            reason = "未达到法定人数";
          } else {
            reason = "赞成率不足";
          }
          cp.setRejectReason(reason);
          rejectReason = reason;
        }
        cp.setResultSnapshot(
          "approveVotes=" +
            approveVotes +
            ", totalParticipants=" +
            totalParticipants +
            ", approvePercent=" +
            approvePercent
        );
        categoryProposalPostRepository.save(cp);
        if (approved) {
          categoryService.createCategory(cp.getProposedName(), cp.getDescription(), "star", null);
        }
        if (cp.getAuthor() != null) {
          notificationService.createNotification(
            cp.getAuthor(),
            NotificationType.CATEGORY_PROPOSAL_RESULT_OWNER,
            cp,
            null,
            approved,
            null,
            null,
            approved ? null : rejectReason
          );
        }
        for (User participant : cp.getParticipants()) {
          if (
            cp.getAuthor() != null &&
            java.util.Objects.equals(participant.getId(), cp.getAuthor().getId())
          ) {
            continue;
          }
          notificationService.createNotification(
            participant,
            NotificationType.CATEGORY_PROPOSAL_RESULT_PARTICIPANT,
            cp,
            null,
            approved,
            null,
            null,
            approved ? null : rejectReason
          );
        }
        postChangeLogService.recordVoteResult(cp);
      });
  }

  /**
   * 检查用户是否达到发帖限制
   * @param username
   * @return true - 允许发帖，false - 已达限制
   */
  private boolean isPostLimitReached(String username) {
    String key = CachingConfig.LIMIT_CACHE_NAME + ":posts:" + username;
    String result = (String) redisTemplate.opsForValue().get(key);
    return StringUtils.isEmpty(result);
  }

  /**
   * 标记用户发帖，触发limit计时
   * @param username
   */
  private void markPostLimit(String username) {
    String key = CachingConfig.LIMIT_CACHE_NAME + ":posts:" + username;
    redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(5));
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  public void joinLottery(Long postId, String username) {
    LotteryPost post = lotteryPostRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (post.getParticipants().add(user)) {
      pointService.processLotteryJoin(user, post);
      lotteryPostRepository.save(post);
    }
  }

  public PollPost getPoll(Long postId) {
    return pollPostRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public PollPost votePoll(Long postId, String username, java.util.List<Integer> optionIndices) {
    PollPost post = pollPostRepository
      .findById(postId)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    if (post.getEndTime() != null && post.getEndTime().isBefore(LocalDateTime.now())) {
      throw new IllegalStateException("Poll has ended");
    }
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (post.getParticipants().contains(user)) {
      throw new IllegalArgumentException("User already voted");
    }
    if (optionIndices == null || optionIndices.isEmpty()) {
      throw new IllegalArgumentException("No options selected");
    }
    java.util.Set<Integer> unique = new java.util.HashSet<>(optionIndices);
    for (int optionIndex : unique) {
      if (optionIndex < 0 || optionIndex >= post.getOptions().size()) {
        throw new IllegalArgumentException("Invalid option");
      }
    }
    post.getParticipants().add(user);
    for (int optionIndex : unique) {
      post.getVotes().merge(optionIndex, 1, Integer::sum);
      PollVote vote = new PollVote();
      vote.setPost(post);
      vote.setUser(user);
      vote.setOptionIndex(optionIndex);
      pollVoteRepository.save(vote);
    }
    PollPost saved = pollPostRepository.save(post);
    if (post.getAuthor() != null && !post.getAuthor().getId().equals(user.getId())) {
      notificationService.createNotification(
        post.getAuthor(),
        NotificationType.POLL_VOTE,
        post,
        null,
        null,
        user,
        null,
        null
      );
    }
    return saved;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public void finalizePoll(Long postId) {
    scheduledFinalizations.remove(postId);
    pollPostRepository
      .findById(postId)
      .ifPresent(pp -> {
        if (pp instanceof CategoryProposalPost) {
          return;
        }
        if (pp.isResultAnnounced()) {
          return;
        }
        pp.setResultAnnounced(true);
        pollPostRepository.save(pp);
        if (pp.getAuthor() != null) {
          notificationService.createNotification(
            pp.getAuthor(),
            NotificationType.POLL_RESULT_OWNER,
            pp,
            null,
            null,
            null,
            null,
            null
          );
        }
        for (User participant : pp.getParticipants()) {
          notificationService.createNotification(
            participant,
            NotificationType.POLL_RESULT_PARTICIPANT,
            pp,
            null,
            null,
            null,
            null,
            null
          );
        }
        postChangeLogService.recordVoteResult(pp);
      });
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public void finalizeLottery(Long postId) {
    log.info("start to finalizeLottery for {}", postId);
    scheduledFinalizations.remove(postId);
    lotteryPostRepository
      .findById(postId)
      .ifPresent(lp -> {
        List<User> participants = new ArrayList<>(lp.getParticipants());
        if (participants.isEmpty()) {
          return;
        }
        Collections.shuffle(participants);
        int winnersCount = Math.min(lp.getPrizeCount(), participants.size());
        java.util.Set<User> winners = new java.util.HashSet<>(
          participants.subList(0, winnersCount)
        );
        log.info("winner count {}", winnersCount);
        lp.setWinners(winners);
        lotteryPostRepository.save(lp);
        for (User w : winners) {
          if (
            w.getEmail() != null &&
            !w.getDisabledEmailNotificationTypes().contains(NotificationType.LOTTERY_WIN)
          ) {
            emailSender.sendEmail(
              w.getEmail(),
              "你中奖了",
              "恭喜你在抽奖贴 \"" + lp.getTitle() + "\" 中获奖"
            );
          }
          notificationService.createNotification(
            w,
            NotificationType.LOTTERY_WIN,
            lp,
            null,
            null,
            lp.getAuthor(),
            null,
            null
          );
          notificationService.sendCustomPush(
            w,
            "你中奖了",
            String.format("%s/posts/%d", websiteUrl, lp.getId())
          );
        }
        if (lp.getAuthor() != null) {
          if (
            lp.getAuthor().getEmail() != null &&
            !lp
              .getAuthor()
              .getDisabledEmailNotificationTypes()
              .contains(NotificationType.LOTTERY_DRAW)
          ) {
            emailSender.sendEmail(
              lp.getAuthor().getEmail(),
              "抽奖已开奖",
              "您的抽奖贴 \"" + lp.getTitle() + "\" 已开奖"
            );
          }
          notificationService.createNotification(
            lp.getAuthor(),
            NotificationType.LOTTERY_DRAW,
            lp,
            null,
            null,
            null,
            null,
            null
          );
          notificationService.sendCustomPush(
            lp.getAuthor(),
            "抽奖已开奖",
            String.format("%s/posts/%d", websiteUrl, lp.getId())
          );
        }
        postChangeLogService.recordLotteryResult(lp);
      });
  }

  @Transactional
  public Post viewPost(Long id, String viewer) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    if (post.getStatus() != PostStatus.PUBLISHED) {
      if (viewer == null) {
        throw new com.openisle.exception.NotFoundException("User not found");
      }
      User viewerUser = userRepository
        .findByUsername(viewer)
        .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
      if (
        !viewerUser.getRole().equals(com.openisle.model.Role.ADMIN) &&
        !viewerUser.getId().equals(post.getAuthor().getId())
      ) {
        throw new com.openisle.exception.NotFoundException("Post not found");
      }
    }
    post.setViews(post.getViews() + 1);
    post = postRepository.save(post);
    if (viewer != null) {
      postReadService.recordRead(viewer, id);
    }
    if (viewer != null && !viewer.equals(post.getAuthor().getUsername())) {
      User viewerUser = userRepository.findByUsername(viewer).orElse(null);
      if (viewerUser != null) {
        notificationRepository.deleteByTypeAndFromUserAndPost(
          NotificationType.POST_VIEWED,
          viewerUser,
          post
        );
        notificationService.createNotification(
          post.getAuthor(),
          NotificationType.POST_VIEWED,
          post,
          null,
          null,
          viewerUser,
          null,
          null
        );
      }
    }
    return post;
  }

  public List<Post> listPosts() {
    return listPostsByCategories(null, null, null);
  }

  public List<Post> listPostsByViews(Integer page, Integer pageSize) {
    return listPostsByViews(null, null, page, pageSize);
  }

  public List<Post> listPostsByViews(
    java.util.List<Long> categoryIds,
    java.util.List<Long> tagIds,
    Integer page,
    Integer pageSize
  ) {
    boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();
    boolean hasTags = tagIds != null && !tagIds.isEmpty();

    java.util.List<Post> posts;

    if (!hasCategories && !hasTags) {
      posts = postRepository.findByStatusOrderByViewsDesc(PostStatus.PUBLISHED);
    } else if (hasCategories) {
      java.util.List<Category> categories = categoryRepository.findAllById(categoryIds);
      if (categories.isEmpty()) {
        return java.util.List.of();
      }
      if (hasTags) {
        java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.isEmpty()) {
          return java.util.List.of();
        }
        posts = postRepository.findByCategoriesAndAllTagsOrderByViewsDesc(
          categories,
          tags,
          PostStatus.PUBLISHED,
          tags.size()
        );
      } else {
        posts = postRepository.findByCategoryInAndStatusOrderByViewsDesc(
          categories,
          PostStatus.PUBLISHED
        );
      }
    } else {
      java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
      if (tags.isEmpty()) {
        return java.util.List.of();
      }
      posts = postRepository.findByAllTagsOrderByViewsDesc(tags, PostStatus.PUBLISHED, tags.size());
    }

    return paginate(sortByPinnedAndViews(posts), page, pageSize);
  }

  public List<Post> listPostsByLatestReply(Integer page, Integer pageSize) {
    return listPostsByLatestReply(null, null, page, pageSize);
  }

  public List<Post> listPostsByLatestReply(
    Long categoryId,
    List<Long> tagIds,
    Integer page,
    Integer pageSize
  ) {
    boolean hasCategory = categoryId != null;
    boolean hasTags = tagIds != null && !tagIds.isEmpty();

    List<Post> posts;

    if (!hasCategory && !hasTags) {
      posts = postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
    } else if (hasCategory) {
      Optional<Category> category = categoryRepository.findById(categoryId);
      if (category.isEmpty()) {
        return List.of();
      }
      if (hasTags) {
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.isEmpty()) {
          return List.of();
        }
        posts = postRepository.findByCategoriesAndAllTagsOrderByCreatedAtDesc(
                // TODO 临时写法
          Collections.singletonList(category.get()),
          tags,
          PostStatus.PUBLISHED,
          tags.size()
        );
      } else {
        posts = postRepository.findByCategoryInAndStatusOrderByCreatedAtDesc(
                // TODO 临时写法
          Collections.singletonList(category.get()),
          PostStatus.PUBLISHED
        );
      }
    } else {
      List<Tag> tags = tagRepository.findAllById(tagIds);
      if (tags.isEmpty()) {
        return new ArrayList<>();
      }
      posts = postRepository.findByAllTagsOrderByCreatedAtDesc(
        tags,
        PostStatus.PUBLISHED,
        tags.size()
      );
    }

    return paginate(sortByPinnedAndLastReply(posts), page, pageSize);
  }

  public List<Post> listPostsByCategories(
    java.util.List<Long> categoryIds,
    Integer page,
    Integer pageSize
  ) {
    if (categoryIds == null || categoryIds.isEmpty()) {
      java.util.List<Post> posts = postRepository.findByStatusOrderByCreatedAtDesc(
        PostStatus.PUBLISHED
      );
      return paginate(sortByPinnedAndCreated(posts), page, pageSize);
    }

    java.util.List<Category> categories = categoryRepository.findAllById(categoryIds);
    java.util.List<Post> posts = postRepository.findByCategoryInAndStatusOrderByCreatedAtDesc(
      categories,
      PostStatus.PUBLISHED
    );
    return paginate(sortByPinnedAndCreated(posts), page, pageSize);
  }

  public List<Post> getRecentPostsByUser(String username, int limit) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    Pageable pageable = PageRequest.of(0, limit);
    return postRepository.findByAuthorAndStatusOrderByCreatedAtDesc(
      user,
      PostStatus.PUBLISHED,
      pageable
    );
  }

  public java.time.LocalDateTime getLastPostTime(String username) {
    return postRepository.findLastPostTime(username);
  }

  public long getTotalViews(String username) {
    Long v = postRepository.sumViews(username);
    return v != null ? v : 0;
  }

  public List<Post> listPostsByTags(java.util.List<Long> tagIds, Integer page, Integer pageSize) {
    if (tagIds == null || tagIds.isEmpty()) {
      return java.util.List.of();
    }

    java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
    if (tags.isEmpty()) {
      return java.util.List.of();
    }

    java.util.List<Post> posts = postRepository.findByAllTagsOrderByCreatedAtDesc(
      tags,
      PostStatus.PUBLISHED,
      tags.size()
    );
    return paginate(sortByPinnedAndCreated(posts), page, pageSize);
  }

  public List<Post> listPostsByCategoriesAndTags(
    java.util.List<Long> categoryIds,
    java.util.List<Long> tagIds,
    Integer page,
    Integer pageSize
  ) {
    if (categoryIds == null || categoryIds.isEmpty() || tagIds == null || tagIds.isEmpty()) {
      return java.util.List.of();
    }

    java.util.List<Category> categories = categoryRepository.findAllById(categoryIds);
    java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
    if (categories.isEmpty() || tags.isEmpty()) {
      return java.util.List.of();
    }

    java.util.List<Post> posts = postRepository.findByCategoriesAndAllTagsOrderByCreatedAtDesc(
      categories,
      tags,
      PostStatus.PUBLISHED,
      tags.size()
    );
    return paginate(sortByPinnedAndCreated(posts), page, pageSize);
  }

  public List<Post> listFeaturedPosts(
    List<Long> categoryIds,
    List<Long> tagIds,
    Integer page,
    Integer pageSize
  ) {
    List<Post> posts;
    boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();
    boolean hasTags = tagIds != null && !tagIds.isEmpty();

    if (hasCategories && hasTags) {
      posts = listPostsByCategoriesAndTags(categoryIds, tagIds, null, null);
    } else if (hasCategories) {
      posts = listPostsByCategories(categoryIds, null, null);
    } else if (hasTags) {
      posts = listPostsByTags(tagIds, null, null);
    } else {
      posts = listPosts();
    }

    // 仅保留 getRssExcluded 为 0 且不为空
    // 若字段类型是 Boolean（包装类型），0 等价于 false：
    posts = posts
      .stream()
      .filter(p -> p.getRssExcluded() != null && !p.getRssExcluded())
      .toList();

    return paginate(sortByPinnedAndCreated(posts), page, pageSize);
  }

  /**
   * 默认的文章列表
   * @param ids
   * @param tids
   * @param page
   * @param pageSize
   * @return
   */
  public List<Post> defaultListPosts(
    List<Long> ids,
    List<Long> tids,
    Integer page,
    Integer pageSize
  ) {
    boolean hasCategories = !CollectionUtils.isEmpty(ids);
    boolean hasTags = !CollectionUtils.isEmpty(tids);

    if (hasCategories && hasTags) {
      return listPostsByCategoriesAndTags(ids, tids, page, pageSize)
        .stream()
        .collect(Collectors.toList());
    }
    if (hasTags) {
      return listPostsByTags(tids, page, pageSize).stream().collect(Collectors.toList());
    }

    return listPostsByCategories(ids, page, pageSize).stream().collect(Collectors.toList());
  }

  public List<Post> listPendingPosts() {
    return postRepository.findByStatus(PostStatus.PENDING);
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  public Post approvePost(Long id) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    // publish all pending tags along with the post
    for (com.openisle.model.Tag tag : post.getTags()) {
      if (!tag.isApproved()) {
        tag.setApproved(true);
        tagRepository.save(tag);
        searchIndexEventPublisher.publishTagSaved(tag);
      }
    }
    post.setStatus(PostStatus.PUBLISHED);
    post = postRepository.save(post);
    searchIndexEventPublisher.publishPostSaved(post);
    notificationService.createNotification(
      post.getAuthor(),
      NotificationType.POST_REVIEWED,
      post,
      null,
      true,
      null,
      null,
      null
    );
    return post;
  }

  public Post rejectPost(Long id) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    // remove user created tags that are only linked to this post
    java.util.Set<com.openisle.model.Tag> tags = new java.util.HashSet<>(post.getTags());
    for (com.openisle.model.Tag tag : tags) {
      if (!tag.isApproved()) {
        long count = postRepository.countDistinctByTags_Id(tag.getId());
        if (count <= 1) {
          Long tagId = tag.getId();
          post.getTags().remove(tag);
          tagRepository.delete(tag);
          searchIndexEventPublisher.publishTagDeleted(tagId);
        }
      }
    }
    post.setStatus(PostStatus.REJECTED);
    post = postRepository.save(post);
    searchIndexEventPublisher.publishPostDeleted(post.getId());
    notificationService.createNotification(
      post.getAuthor(),
      NotificationType.POST_REVIEWED,
      post,
      null,
      false,
      null,
      null,
      null
    );
    return post;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  public Post pinPost(Long id, String username) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    java.time.LocalDateTime oldPinned = post.getPinnedAt();
    post.setPinnedAt(java.time.LocalDateTime.now());
    Post saved = postRepository.save(post);
    postChangeLogService.recordPinnedChange(saved, user, oldPinned, saved.getPinnedAt());
    return saved;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  public Post unpinPost(Long id, String username) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    java.time.LocalDateTime oldPinned = post.getPinnedAt();
    post.setPinnedAt(null);
    Post saved = postRepository.save(post);
    postChangeLogService.recordPinnedChange(saved, user, oldPinned, null);
    return saved;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  public Post closePost(Long id, String username) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (!user.getId().equals(post.getAuthor().getId()) && user.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("Unauthorized");
    }
    boolean oldClosed = post.isClosed();
    post.setClosed(true);
    Post saved = postRepository.save(post);
    postChangeLogService.recordClosedChange(saved, user, oldClosed, true);
    return saved;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  public Post reopenPost(Long id, String username) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (!user.getId().equals(post.getAuthor().getId()) && user.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("Unauthorized");
    }
    boolean oldClosed = post.isClosed();
    post.setClosed(false);
    Post saved = postRepository.save(post);
    postChangeLogService.recordClosedChange(saved, user, oldClosed, false);
    return saved;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public Post updatePost(
    Long id,
    String username,
    Long categoryId,
    String title,
    String content,
    List<Long> tagIds,
    PostVisibleScopeType postVisibleScopeType
  ) {
    if (tagIds == null || tagIds.isEmpty()) {
      throw new IllegalArgumentException("At least one tag required");
    }
    if (tagIds.size() > 2) {
      throw new IllegalArgumentException("At most two tags allowed");
    }
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (!user.getId().equals(post.getAuthor().getId()) && user.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("Unauthorized");
    }
    Category category = categoryRepository
      .findById(categoryId)
      .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
    if (tags.isEmpty()) {
      throw new IllegalArgumentException("Tag not found");
    }
    String oldTitle = post.getTitle();
    String oldContent = post.getContent();
    Category oldCategory = post.getCategory();
    java.util.Set<com.openisle.model.Tag> oldTags = new java.util.HashSet<>(post.getTags());
    post.setTitle(title);
    post.setContent(content);
    post.setCategory(category);
    post.setTags(new java.util.HashSet<>(tags));
    PostVisibleScopeType oldVisibleScope = post.getVisibleScope();
    post.setVisibleScope(postVisibleScopeType);
    Post updated = postRepository.save(post);
    imageUploader.adjustReferences(oldContent, content);
    notificationService.notifyMentions(content, user, updated, null);
    if (!java.util.Objects.equals(oldTitle, title)) {
      postChangeLogService.recordTitleChange(updated, user, oldTitle, title);
    }
    if (!java.util.Objects.equals(oldContent, content)) {
      postChangeLogService.recordContentChange(updated, user, oldContent, content);
    }
    if (!java.util.Objects.equals(oldCategory.getId(), category.getId())) {
      postChangeLogService.recordCategoryChange(
        updated,
        user,
        oldCategory.getName(),
        category.getName()
      );
    }
    java.util.Set<com.openisle.model.Tag> newTags = new java.util.HashSet<>(tags);
    if (!oldTags.equals(newTags)) {
      postChangeLogService.recordTagChange(updated, user, oldTags, newTags);
    }
    if (!java.util.Objects.equals(oldVisibleScope, postVisibleScopeType)) {
      postChangeLogService.recordVisibleScopeChange(
        updated,
        user,
        oldVisibleScope,
        postVisibleScopeType
      );
    }
    if (updated.getStatus() == PostStatus.PUBLISHED) {
      searchIndexEventPublisher.publishPostSaved(updated);
    }
    return updated;
  }

  @CacheEvict(value = CachingConfig.POST_CACHE_NAME, allEntries = true)
  @Transactional
  public void deletePost(Long id, String username) {
    Post post = postRepository
      .findById(id)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    User author = post.getAuthor();
    boolean adminDeleting = !user.getId().equals(author.getId()) && user.getRole() == Role.ADMIN;
    if (!user.getId().equals(author.getId()) && user.getRole() != Role.ADMIN) {
      throw new IllegalArgumentException("Unauthorized");
    }
    for (Comment c : commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post)) {
      commentService.deleteCommentCascade(c);
    }
    reactionRepository.findByPost(post).forEach(reactionRepository::delete);
    postSubscriptionRepository.findByPost(post).forEach(postSubscriptionRepository::delete);
    notificationRepository.deleteAll(notificationRepository.findByPost(post));
    postReadService.deleteByPost(post);
    imageUploader.removeReferences(imageUploader.extractUrls(post.getContent()));
    List<PointHistory> pointHistories = pointHistoryRepository.findByPost(post);
    Set<User> usersToRecalculate = pointHistories
      .stream()
      .map(PointHistory::getUser)
      .collect(Collectors.toSet());
    if (!pointHistories.isEmpty()) {
      LocalDateTime deletedAt = LocalDateTime.now();
      for (PointHistory history : pointHistories) {
        history.setDeletedAt(deletedAt);
        history.setPost(null);
      }
      pointHistoryRepository.saveAll(pointHistories);
    }
    if (!usersToRecalculate.isEmpty()) {
      for (User affected : usersToRecalculate) {
        int newPoints = pointService.recalculateUserPoints(affected);
        affected.setPoint(newPoints);
      }
      userRepository.saveAll(usersToRecalculate);
    }
    if (post instanceof LotteryPost lp) {
      ScheduledFuture<?> future = scheduledFinalizations.remove(lp.getId());
      if (future != null) {
        future.cancel(false);
      }
    }
    String title = post.getTitle();
    Long postId = post.getId();
    postChangeLogService.deleteLogsForPost(post);
    postRepository.delete(post);
    searchIndexEventPublisher.publishPostDeleted(postId);
    if (adminDeleting) {
      notificationService.createNotification(
        author,
        NotificationType.POST_DELETED,
        null,
        null,
        null,
        user,
        null,
        title
      );
    }
  }

  public java.util.List<Post> getPostsByIds(java.util.List<Long> ids) {
    return postRepository.findAllById(ids);
  }

  public long countPostsByCategory(Long categoryId) {
    return postRepository.countByCategory_Id(categoryId);
  }

  public Map<Long, Long> countPostsByCategoryIds(List<Long> categoryIds) {
    Map<Long, Long> result = new HashMap<>();
    var dbResult = postRepository.countPostsByCategoryIds(categoryIds);
    dbResult.forEach(r -> {
      result.put(((Long) r[0]), ((Long) r[1]));
    });
    return result;
  }

  public long countPostsByTag(Long tagId) {
    return postRepository.countDistinctByTags_Id(tagId);
  }

  public Map<Long, Long> countPostsByTagIds(List<Long> tagIds) {
    Map<Long, Long> result = new HashMap<>();
    if (CollectionUtils.isEmpty(tagIds)) {
      return result;
    }
    var dbResult = postRepository.countPostsByTagIds(tagIds);
    dbResult.forEach(r -> {
      result.put(((Long) r[0]), ((Long) r[1]));
    });
    return result;
  }

  private java.util.List<Post> sortByPinnedAndCreated(java.util.List<Post> posts) {
    return posts
      .stream()
      .sorted(
        java.util.Comparator.comparing(
          Post::getPinnedAt,
          java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
        ).thenComparing(Post::getCreatedAt, java.util.Comparator.reverseOrder())
      )
      .toList();
  }

  private java.util.List<Post> sortByPinnedAndViews(java.util.List<Post> posts) {
    return posts
      .stream()
      .sorted(
        java.util.Comparator.comparing(
          Post::getPinnedAt,
          java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
        ).thenComparing(Post::getViews, java.util.Comparator.reverseOrder())
      )
      .toList();
  }

  private java.util.List<Post> sortByPinnedAndLastReply(java.util.List<Post> posts) {
    return posts
      .stream()
      .sorted(
        java.util.Comparator.comparing(
          Post::getPinnedAt,
          java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
        ).thenComparing(
          p -> {
            java.time.LocalDateTime t = commentRepository.findLastCommentTime(p);
            return t != null ? t : p.getCreatedAt();
          },
          java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
        )
      )
      .toList();
  }

  private List<Post> paginate(List<Post> posts, Integer page, Integer pageSize) {
    if (page == null || pageSize == null) {
      return posts;
    }
    int from = page * pageSize;
    if (from >= posts.size()) {
      return new ArrayList<>();
    }
    int to = Math.min(from + pageSize, posts.size());
    // 这里必须将list包装为arrayList类型，否则序列化会有问题
    // list.sublist返回的是内部类
    return new ArrayList<>(posts.subList(from, to));
  }
}
